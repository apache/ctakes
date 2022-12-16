/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.drugner.cc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

import org.apache.ctakes.core.util.FSUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.refsem.MedicationDosage;
import org.apache.ctakes.typesystem.type.refsem.MedicationDuration;
import org.apache.ctakes.typesystem.type.refsem.MedicationForm;
import org.apache.ctakes.typesystem.type.refsem.MedicationFrequency;
import org.apache.ctakes.typesystem.type.refsem.MedicationRoute;
import org.apache.ctakes.typesystem.type.refsem.MedicationStatusChange;
import org.apache.ctakes.typesystem.type.refsem.MedicationStrength;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.drugner.type.ChunkAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationDosageModifier;
import org.apache.ctakes.typesystem.type.textsem.MedicationDurationModifier;
import org.apache.ctakes.typesystem.type.textsem.MedicationFormModifier;
import org.apache.ctakes.typesystem.type.textsem.MedicationFrequencyModifier;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationRouteModifier;
import org.apache.ctakes.typesystem.type.textsem.MedicationStatusChangeModifier;
import org.apache.ctakes.typesystem.type.textsem.MedicationStrengthModifier;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.util.Pair;
import org.apache.ctakes.typesystem.type.util.Pairs;
//import org.apache.ctakes.drugner.type.DrugMentionAnnotation;
import org.apache.ctakes.drugner.type.SubSectionAnnotation;

/**
 * UIMA annotator that preps the CAS for extraction into DB2.
 * 
 * @author
 */
@PipeBitInfo(
		name = "Database File Writer",
		description = "Writes to a file that is compatible with Database import.",
		role = PipeBitInfo.Role.WRITER,
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class ConsumeNamedEntityRecordModel extends CasConsumer_ImplBase {
	private String iv_annotVerPropKey;


	protected ArrayList al = new ArrayList();
	File iv_outputDirectory;

	int keepTrackOfDupBegin = 0, keepTrackOfDupEnd = 0, milliWeek = 604800000,
			milliDay = 86400000;


	public void initialize() throws ResourceInitializationException {
		fileForIO = (String) (String) getConfigParameterValue("OutputDirectory");
		iv_outputDirectory = new File(fileForIO);
		iv_useCurrentMedsSectionOnly = (Boolean) getConfigParameterValue("useMedicationSectionOnly");
		pathToDrugInclusions = (String) getConfigParameterValue("filterGroupDrugs");
		iv_fileForInput = (String) getConfigParameterValue("locationForClinicRcdInput");
		String holdListMedSections  = (String) getConfigParameterValue("listMedicationSection");

		StringTokenizer nextMedSection = new StringTokenizer(holdListMedSections, "|");
		while(nextMedSection.hasMoreTokens()) {
			 iv_medicalSections.add(nextMedSection.nextToken());
			 
		}
		if (!iv_outputDirectory.exists() || !iv_outputDirectory.isDirectory()) {
			if (!iv_outputDirectory.exists())
				// try{
				(new File(fileForIO)).mkdir();
			else
				throw new ResourceInitializationException(
						new Exception(
								"Parameter setting 'OutputDirectory' does not point to an existing directory."));
		}

	}
	/**
	 * Stores annotation version as a property JCas object.
	 * 
	 * @param jcas
	 */
	private void storeAnnotationVersion(JCas jcas) {
	 	FSIterator<TOP> itr = jcas.getJFSIndexRepository().getAllIndexedFS(Pairs.type);
		if (itr == null || !itr.hasNext())
			return;

		Pairs props = (Pairs) itr.next(); 

		// create a new property array that is one item bigger
		FSArray propArr = props.getPairs();
		FSArray newPropArr = new FSArray(jcas, propArr.size() + 1);
		for (int i = 0; i < propArr.size(); i++) {
			newPropArr.set(i, propArr.get(i));
		}

		Pair annotVerProp = new Pair(jcas);    		
		annotVerProp.setAttribute(iv_annotVerPropKey);
		annotVerProp.setValue(String.valueOf(iv_annotVer));

		// add annotation version prop as last item in array
		newPropArr.set(newPropArr.size() - 1, annotVerProp);
		props.setPairs(newPropArr);
	}
	public void processCas(CAS cas) throws ResourceProcessException {
		vRevDate = "";
		vNoteDate = "";
		vClinicalNumber = "";
		gotValidDate = false;
		gotDup = false;
		clinicNumber = "";

//		TODO: Move to Common Type System
//		generateTokenNormForms(cas);
		assignNamedEntityFeats(cas);

		//storeAnnotationVersion(cas);
	}

	/**
	 * Stores annotation version as a property JCas object.
	 * 
	 * @param jcas
	 */
//	TODO: Move to Common Type System
//	private void storeAnnotationVersion(CAS cas)
//			throws ResourceProcessException {
//		try {
//			JCas jcas = cas.getJCas();
//			Iterator itr = jcas.getJFSIndexRepository().getAnnotationIndex(
//					Properties.type).iterator();
//			if (itr == null || !itr.hasNext())
//				return;
//
//			Properties props = (Properties) itr.next();
//
//			// create a new property array that is one item bigger
//			FSArray propArr = props.getPropArr();
//			FSArray newPropArr = new FSArray(jcas, propArr.size() + 1);
//			for (int i = 0; i < propArr.size(); i++) {
//				newPropArr.set(i, propArr.get(i));
//			}
//
//			Property annotVerProp = new Property(jcas);
//			annotVerProp.setKey(iv_annotVerPropKey);
//			annotVerProp.setValue(String.valueOf(iv_annotVer));
//
//			// add annotation version prop as last item in array
//			newPropArr.set(newPropArr.size() - 1, annotVerProp);
//			props.setPropArr(newPropArr);
//		} catch (Exception e) {
//			throw new ResourceProcessException(e);
//		}
//	}

	/**
	 * Generates normalized form for each token annotation.
	 */
//	TODO: Move to Common Type System	
//	private void generateTokenNormForms(CAS cas)
//			throws ResourceProcessException {
//		try {
//		    JCas jcas  = cas.getJCas().getView("plaintext");
//			JFSIndexRepository indexes = jcas.getJFSIndexRepository();
//			Iterator propertiesItr = indexes
//					.getAnnotationIndex(Properties.type).iterator();
//
//			while (propertiesItr.hasNext()) {
//				Properties props = (Properties) propertiesItr.next();
//				FSArray fsArr = props.getPropArr();
//				for (int i = 0; i < fsArr.size(); i++)
//				{
//					if (fsArr.get(i) != null) 
//					{
//					    Property fs = (Property) fsArr.get(i);
//					    
//					    if (fs.getKey().compareTo("REV_DATE") == 0) 
//					    {
//						gotValidDate = true;
//						vRevDate = fs.getValue();
//					    }
//					    else if (fs.getKey().compareTo("NOTE_DATE") == 0) 
//					    {
//						vNoteDate = fs.getValue();
//					    }
//					    else if (fs.getKey().compareTo("CLINICAL_NUMBER") == 0) 
//					    {
//						if (fs.getValue().length() < 8)
//						    vClinicalNumber = "0"+fs.getValue();
//						else 
//						    vClinicalNumber = fs.getValue();
//					    }
//					}
//				}
//			}
//			Map abbrMap = new HashMap();
//
//			Iterator docItr = indexes.getAnnotationIndex(DocumentID.type).iterator();
//			
//			while (docItr.hasNext()) 
//			{
//			    DocumentID doc = (DocumentID) docItr.next();
//			    if (gotValidDate)
//				clinicNumber = vClinicalNumber;
//			    abbrMap.put(new Integer(doc.getBegin()), doc);
//			}
//
//
//			Iterator btaItr = indexes.getAnnotationIndex(BaseToken.type).iterator();
//			while (btaItr.hasNext()) 
//			{
//			    BaseToken bta = (BaseToken) btaItr.next();
//			    String normForm = null;
//			    bta.setNormalizedForm(normForm);
//			}
//		} catch (Exception e) {
//		    e.printStackTrace();
//			throw new ResourceProcessException(e);
//		}
//	}

	/**
	 * Assigns typeID and segmentID values to Drug NamedEntities
	 */
	private void assignNamedEntityFeats(CAS cas)
			throws ResourceProcessException {
		try {
			
	        JCas jcas = cas.getCurrentView().getJCas();
		    //JCas jcas  = cas.getJCas().getView("plaintext");
	                
	        	//System.err.println("Document Id: "+DocumentIDAnnotationUtil.getDocumentID(jcas));
	        	
			boolean gotMeds = false;

			int trackMedOccur = 0;

			String medInfo = "";
			//int keepTrackOfUID = 0;
			
			JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		
			Set segmentSet = new HashSet();
			Iterator segmentItr = indexes.getAnnotationIndex(Segment.type).iterator();
			while (segmentItr.hasNext()) 
			{
			    Segment s = (Segment)segmentItr.next();
			    segmentSet.add(s);
			    //System.err.println("Segment :"+ s.getCoveredText());
			}
			
            Iterator nerItr = indexes.getAnnotationIndex(MedicationMention.type).iterator();
			while (nerItr.hasNext()) 
			{
			  MedicationMention neAnnot = (MedicationMention) nerItr.next();
//System.err.println("DrugNE :"+neAnnot.getCoveredText());			  
			  gotDup = false;

				// assign segment ID
				Iterator segItr = segmentSet.iterator();
				while (segItr.hasNext()) 
				{
					Segment seg = (Segment) segItr.next();
					if ((keepTrackOfDupBegin == neAnnot.getBegin()) && (keepTrackOfDupEnd == neAnnot.getEnd()))
					{ 
						gotDup = true;
					} 

					if ((neAnnot.getBegin() >= seg.getBegin())
							&& (neAnnot.getEnd() <= seg.getEnd()) && !gotDup) {

						// found segment for this NE
						String segmentID = seg.getId();
					
						if (iv_medicalSections.contains(segmentID)
								|| !iv_useCurrentMedsSectionOnly.booleanValue()) {

							if (!gotDup) {
								keepTrackOfDupBegin = neAnnot.getBegin();
								keepTrackOfDupEnd = neAnnot.getEnd();


								TimeMention startTimeMention = neAnnot.getStartDate();
								
								Date localDate = null;
								if (startTimeMention!=null) localDate = startTimeMention.getDate();
								String chunk = null;

								boolean foundChunk = false;
								Iterator findChunk = indexes
								.getAnnotationIndex(
										ChunkAnnotation.type)
										.iterator();

								try {
									while (findChunk.hasNext() && !foundChunk) 
									{
									    ChunkAnnotation ca = (ChunkAnnotation) findChunk.next();
										if (neAnnot.getBegin() >= ca.getBegin()
												&& neAnnot.getEnd() <= ca
												.getEnd()) {
											chunk = ca.getCoveredText()
											.replace('\n', ' ')
											.replace(',', ';');
											foundChunk = true;
										}
									}
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								String containedInSubSection = segmentID;
								Iterator subSectionItr = indexes.getAnnotationIndex(
										SubSectionAnnotation.type).iterator();
								while (subSectionItr.hasNext()) 
								{
								    SubSectionAnnotation ssAnnot = (SubSectionAnnotation) subSectionItr.next();
								    if (ssAnnot.getSubSectionBodyBegin() <= neAnnot.getBegin() && ssAnnot.getSubSectionBodyEnd() >= neAnnot.getEnd()) 
								    {
									Iterator textSpanInSs = FSUtil.getAnnotationsIteratorInSpan(jcas, WordToken.type, ssAnnot.getSubSectionHeaderBegin(), ssAnnot.getSubSectionHeaderEnd());
									String subSectionHeaderName = "";
										
									while (textSpanInSs.hasNext()) 
									{
									    WordToken wta = (WordToken) textSpanInSs.next();
									    subSectionHeaderName = subSectionHeaderName + " " + wta.getCoveredText();
									}
									containedInSubSection = containedInSubSection+"|"+subSectionHeaderName+"|"+ssAnnot.getStatus();
								    }
								}
								gotMeds = true;
								trackMedOccur++;

								Calendar calendar = Calendar.getInstance();

								SimpleDateFormat format = new SimpleDateFormat("MM'/'dd'/'yyyy");

								if(vRevDate != null && vRevDate.length() > 0)
								    calendar.setTimeInMillis(new Long(vRevDate).longValue());
								else if(vNoteDate != null && vNoteDate.length() > 0)
								    calendar.setTimeInMillis(new Long(vNoteDate).longValue());
								
								String globalDate = format.format(calendar.getTime());
//								if (localDate == null
//										|| localDate.length() < 1) {
//									localDate = globalDate;
//								}
	//							Iterator neItr = FSUtil.getAnnotationsIteratorInSpan(jcas, IdentifiedAnnotation.type, neAnnot.getBegin(), neAnnot.getEnd()+1);
								String neCui = "n/a";
								String status = "n/a";
								String rxNormCui = "n/a";

								FSArray ocArr = neAnnot.getOntologyConceptArr();
								if (ocArr != null)
								{
									for (int i = 0; i < ocArr.size(); i++)
									{
										OntologyConcept oc = (OntologyConcept) ocArr.get(i);
										neCui = oc.getCode();
										rxNormCui = oc.getOui();
									}
								}

								MedicationStrengthModifier strength = neAnnot.getMedicationStrength();
								MedicationStrength strengthTerm = (MedicationStrength) strength.getNormalizedForm();
								String strengthTermString = "null";
								if (strengthTerm != null) { 
									strengthTermString = strengthTerm.getNumber()+ " " +strengthTerm.getUnit();
								}
								
								String medicationDosageString = "null";
								MedicationDosageModifier dosageModifier = neAnnot.getMedicationDosage();
								if (dosageModifier != null) {
									MedicationDosage d = (MedicationDosage) dosageModifier.getNormalizedForm();
									if (d!=null) medicationDosageString = d.getValue();
								}
								String medicationFrequencyNumber = "null";
								MedicationFrequencyModifier freqModifier = neAnnot.getMedicationFrequency();
								if (freqModifier != null) {
									MedicationFrequency f = (MedicationFrequency) freqModifier.getNormalizedForm();
									if (f != null) medicationFrequencyNumber = f.getNumber()+" "+f.getUnit();
								}
								String duration = "null";
								MedicationDurationModifier durationModifier = neAnnot.getMedicationDuration();
								if (durationModifier != null) {
									MedicationDuration d = (MedicationDuration) durationModifier.getNormalizedForm();
									if (d!=null) duration = d.getValue();
								}

								String route = "null";
								MedicationRouteModifier routeModifier = neAnnot.getMedicationRoute();
								if (routeModifier != null) {
									MedicationRoute r = (MedicationRoute) routeModifier.getNormalizedForm();
									if (r != null) route = r.getValue();
								}
								String form = "null";
								MedicationFormModifier formModifier = neAnnot.getMedicationForm();
								if (formModifier != null) {
									MedicationForm f = (MedicationForm) formModifier.getNormalizedForm();
									if (f!=null) form = f.getValue();
								}
								
								String changeStatus = "null";
								MedicationStatusChangeModifier scModifier = neAnnot.getMedicationStatusChange();
								if (scModifier != null) {
									MedicationStatusChange sc = (MedicationStatusChange) scModifier.getNormalizedForm();
									if (sc!=null) changeStatus = sc.getValue();
								}
								
								medInfo = clinicNumber + "," +neAnnot.getCoveredText()  + "," + rxNormCui 
								+ ",\"" + neAnnot.getStartDate() + "\","
								+ globalDate + "," + medicationDosageString + "," +strengthTermString + "," 
								+ medicationFrequencyNumber + "," +  duration + "," + route + ","
								+  form + "," + status + "," 
								+ changeStatus + "," +neAnnot.getConfidence() + "," +containedInSubSection
								+ "," +docLinkId+"_"+docRevision+","+chunk;  
								store(fileForIO, medInfo);
							}
						}
					}

				}
			}

		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (Exception e) {
			throw new ResourceProcessException(e);
		}
	}

	/**
	 * Loads text from a file. Specialized to load array idAndDate and return it
	 * too
	 * 
	 * @param filename
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static List load(String filename) throws FileNotFoundException,
			IOException {

		String[][] idDate = null;
		List listIDandDates = new ArrayList();
		File f = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(f));
		br.readLine();// dummy line to go beyond columm headers
		br.readLine();
		String line = br.readLine();

		int index = 0;

		while ((line != null) && (line != "") && (line.length() > 0)) {
			int firstComma = line.indexOf(',');
			int lastComma = line.lastIndexOf(',');
			String id = line.substring(0, firstComma);
			String date = line.substring(lastComma + 1, line.indexOf("0:00"));
			idDate = new String[][] { { id }, { date } };
			listIDandDates.add(index, idDate);
			index++;
			line = br.readLine();
		}
		br.close();

		return listIDandDates;
	}

	/**
	 * Loads text from a file. Specialized to load array idAndDate and return it
	 * too
	 * 
	 * @param filename
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void store(String filename, String lineToStore)
			throws FileNotFoundException, IOException {
		int howMany = 132;
		boolean skipDate = false;
		boolean preExists = true;
		if (filename.endsWith(System.getProperty("file.separator")))
			filename = filename
					+ lineToStore.substring(0, lineToStore.indexOf(','))
					+ ".csv";
		else
			filename = filename + "/"
					+ lineToStore.substring(0, lineToStore.indexOf(','))
					+ ".csv";

		File f = new File(filename);
		if (!f.exists()){
			f.createNewFile();
			preExists = false;
		}
		BufferedReader br = new BufferedReader(new FileReader(f));
		br.readLine();// dummy line to go beyond columm headers
		br.close();

		ByteArrayOutputStream bout = new ByteArrayOutputStream(howMany * 4);

		DataOutputStream dout = new DataOutputStream(bout);
		FileOutputStream fos = new FileOutputStream(filename, true);
		if (!preExists){
			dout.writeBytes(drugHeaders);
		}
		if (!skipDate)
			dout.writeBytes(lineToStore + '\n');

		try {
			if (!skipDate) {
				bout.writeTo(fos);
				fos.flush();
			}
		} finally {
			fos.close();
		}

	}

	protected String parseStengthValue(Object strength) {

		String text = (String) strength;
		String strengthText = "";
		boolean containsNums = false;
		boolean doneHere = false;
		int textSize = text.length();
		int pos = 0;
		Integer posInt = null;
		String strengthString = "";
		while (!doneHere && (textSize > pos) && (textSize > 1)) {
			try {
				strengthString = text.substring(pos, pos + 1);
				/*
				 * if (numString.compareTo(".") == 0) { subText =
				 * text.substring(pos + 1, textSize); pos++; }
				 */
				Integer posNum = posInt.decode(strengthString);
				int checkInt = posNum.intValue();

				if ((checkInt >= 0) && (checkInt <= 9)) {
					containsNums = true;

				} else {

					strengthText = strengthText + strengthString;
				}
				pos++;

			}

			catch (NullPointerException npe) {
				return null;
			} catch (NumberFormatException nfe) {
				if (!containsNums)
					doneHere = true;
				else {
					pos++;
					strengthText = strengthText + strengthString;
				}
			}
		}
		return strengthText;

	}

	protected int parseIntValue(Object strength) {

		String text = (String) strength;
		String strengthNumText = "";
		String subText = "";
		boolean containsNums = false;
		boolean doneHere = false;
		int textSize = text.length();
		int pos = 0;
		Integer posInt = null;
		while (!doneHere && (textSize > pos) && (textSize > 1)) {
			try {
				String numString = text.substring(pos, pos + 1);

				Integer posNum = posInt.decode(numString);
				int checkInt = posNum.intValue();

				if ((checkInt >= 0) && (checkInt <= 9)) {
					containsNums = true;
					subText = text.substring(pos + 1, textSize);
					pos++;
					strengthNumText = strengthNumText + numString;

				} else
					return 0;
			}

			catch (NullPointerException npe) {
				return 0;
			} catch (NumberFormatException nfe) {
				if (!containsNums)
					return 0;
				else
					doneHere = true;

			}
		}
		if (strengthNumText != "")
			return new Integer(strengthNumText).intValue();
		else
			return 0;

	}
	private int iv_annotVer;
	private boolean gotValidDate = false;
	private boolean gotDup = false;
	private String vRevDate = null;
	private String vNoteDate = null;
	private String vClinicalNumber = null;
	private String clinicNumber = null;
	private String docLinkId = "";
	private String docRevision = "";
	private String iv_fileForInput = null;
	private String pathToDrugInclusions;
	private Set<String> iv_medicalSections = new HashSet();
	private String drugHeaders = "clinicNumber,drug_mention_text,rxnorm_cui,local_date,note_date,dosage,strength,frequency,frequency_unit,duration,route,form,status,change_status,certainty,section|subsection|status,documentId_revision\n";
	private Boolean iv_useCurrentMedsSectionOnly = new Boolean("true");
	public static final String PARAM_OUTPUTDIR = "OutputDirectory";

	private String fileForIO = new String(
			"R:\\Dept\\projects\\Text\\DrugProfile\\data\\psychiatry\\goldStandard\\work.csv");


}