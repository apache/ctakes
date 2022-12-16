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
/**
 * 
 */
package org.apache.ctakes.smokingstatus.ae;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

import org.apache.ctakes.smokingstatus.i2b2.type.RecordSentence;

import org.apache.ctakes.smokingstatus.type.SmokingDocumentClassification;

import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.smokingstatus.Const;
import org.apache.ctakes.smokingstatus.util.ClassifiableEntry;
import org.apache.ctakes.smokingstatus.util.TruthValue;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.smokingstatus.type.libsvm.NominalAttributeValue;

/**
 * @author Mayo Clinic Intended as an annotator to generate ClassifiableEntries
 *         for SmokingStatus pipeline
 */
public class ClassifiableEntries extends JCasAnnotator_ImplBase {


	/**
	 * Name of configuration parameter that must be set to the filepath of the
	 * UIMA descriptor ProductionPostSentenceAggregate.xml
	 */
//	public static final String PARAM_SMOKING_STATUS_DESC_STEP1 = "UimaDescriptorStep1";
//	public static final String PARAM_SMOKING_STATUS_DESC_STEP2 = "UimaDescriptorStep2";
	public static final String PARAM_SMOKING_STATUS_DESC_STEP1KEY = "UimaDescriptorStep1Key";
	public static final String PARAM_SMOKING_STATUS_DESC_STEP2KEY = "UimaDescriptorStep2Key";

	/**
	 * Name of configuration parameter that must be set to the filepath of the
	 * delimited truth file. This is optional.
	 */
	public static final String PARAM_TRUTH_FILE = "TruthFile";

	/**
	 * Name of configuration parameter that describes the character delimiter
	 * used in the delimited truth file. This is optional.
	 */
	public static final String PARAM_TRUTH_FILE_DELIMITER = "TruthFileDelimiter";

	/**
	 * Name of configuration parameter that determines the allowed
	 * Classification values. This is optional and only gets used if a Truth
	 * file is specified.
	 */
	public static final String PARAM_ALLOWED_CLASSES = "AllowedClassifications";

	/**
	 * Name of configuration parameter that determines whether section headers
	 * will be parsed out and Segments made to cover the section text.
	 */
	public static final String PARAM_PARSE_SECTIONS = "ParseSections";

	/**
	 * Sections NOT to be entered in ClassifiableEntries 
	 */
	public static final String PARAM_IGNORE_SECTIONS = "SectionsToIgnore";

	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		boolean windowsSystem = true;
		try {
			super.initialize(aContext);

			ResMgr = UIMAFramework.newDefaultResourceManager();
			iv_procEntryList = new ArrayList<ClassifiableEntry>();
			iv_entryIndexMap = new HashMap<String, List<ClassifiableEntry>>();
			iv_segList = new ArrayList<Segment>();

			// load (optional) truth data
			initTruthData();
			// What type of operating system type are we running on? Unix or Windows
			if (System.getProperty("file.separator").matches("/"))
				windowsSystem = false;
			
			// load TAE from XML descriptor
			FileResource fResrc = (FileResource) aContext.getResourceObject(PARAM_SMOKING_STATUS_DESC_STEP1KEY);
			File descFile = fResrc.getFile();
			taeSpecifierStep1 = UIMAFramework.getXMLParser().parseResourceSpecifier(
			new XMLInputSource(fResrc.getFile()));			

			fResrc = (FileResource) aContext.getResourceObject(PARAM_SMOKING_STATUS_DESC_STEP2KEY);
			descFile = fResrc.getFile();

			taeSpecifierStep2 = UIMAFramework.getXMLParser().parseResourceSpecifier(
					new XMLInputSource(fResrc.getFile()));			

			ra = new ResolutionAnnotator();
			ra.initialize(aContext);
			String dataPath  = aContext.getDataPath();
			System.out.println("descFile "+descFile.getAbsolutePath());
//			if (!descFile.getAbsolutePath().contains(apiMacroHome)) {
//				ClassLoader thisBundle = this.getClass().getClassLoader();
//				iv_logger.info("Using data path : "+dataPath);
//				if (!windowsSystem) {
//					//thisBundle.getSystemResources(dataPath);
//					dataPath = "\""+dataPath+"\"";
//					// This bundle is needed for Linux issues w/ embedded blanks in path names 
//					//this.getClass().getClassLoader().getResource(dataPath);
//					ResMgr.setExtensionClassPath(thisBundle, dataPath, true);
//				} else {
//					ResMgr.setExtensionClassPath(dataPath, true);
//				}
//			}
//			else if (iv_logger.isInfoEnabled()) {
//				iv_logger.warn("Shouldn't need to set the classpath "+descFile.getAbsolutePath());
//			}
			taeStep1 = UIMAFramework.produceAnalysisEngine(taeSpecifierStep1, ResMgr, null);
			taeStep2 = UIMAFramework.produceAnalysisEngine(taeSpecifierStep2, ResMgr, null);
			jcas_local = CasCreationUtils.createCas(taeStep1.getAnalysisEngineMetaData()).getJCas();
			
//			if (iv_logger.isInfoEnabled())
//				iv_logger.info("Loaded UIMA TAE from descriptor: "
//						+ descFile.getAbsolutePath().replaceAll(apiMacroHome, "."));

			// get sections to ignore
			String[] sections = (String[]) getContext().getConfigParameterValue(PARAM_IGNORE_SECTIONS);
			sectionsToIgnore = new HashSet<String>();
			for (int i = 0; i < sections.length; i++)
				sectionsToIgnore.add(sections[i]);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	private void initTruthData() throws Exception {
		String truthFilePath = (String) getContext().getConfigParameterValue(
				PARAM_TRUTH_FILE);
		if (truthFilePath != null && truthFilePath.length() > 0) {
			String delimiter = "\t";
			File truthFile = new File(truthFilePath);
			loadTruthData(truthFile, delimiter);

			String[] allowedArr = (String[]) getContext()
					.getConfigParameterValue(PARAM_ALLOWED_CLASSES);
			iv_allowedClassifications = new HashSet<String>();
			for (int i = 0; i < allowedArr.length; i++) {
				String classification = allowedArr[i];
				if (classification.equals(Const.CLASS_CURR_SMOKER)
						|| classification.equals(Const.CLASS_NON_SMOKER)
						|| classification.equals(Const.CLASS_PAST_SMOKER)
						|| classification.equals(Const.CLASS_SMOKER)
						|| classification.equals(Const.CLASS_UNKNOWN)) {
					iv_allowedClassifications.add(classification);
				} else {
					throw new Exception(
							"Invalid classification value for param "
									+ PARAM_ALLOWED_CLASSES + ":"
									+ classification);
				}
			}
		}
	}

	/**
	 * Parses the TRUTH file in delimited format. Stores data in maps.
	 * 
	 * @param truthFile
	 * @param delimiter
	 * @throws Exception
	 */
	private void loadTruthData(File truthFile, String delimiter)
			throws Exception {
		iv_truthMap = new HashMap<Integer, TruthValue>();

		BufferedReader br = new BufferedReader(new FileReader(truthFile));
		int lineNum = 1;
		String line = br.readLine();
		while (line != null) {
			StringTokenizer st = new StringTokenizer(line, delimiter);
			if (st.countTokens() == 4) {
				Integer recordID = new Integer(st.nextToken().trim());
				String truthVal = st.nextToken().trim();
				String sentence = st.nextToken().trim();
				// String section = st.nextToken().trim();

				String ssClass = null;
				if (truthVal.equals("CURRENT SMOKER")) {
					ssClass = Const.CLASS_CURR_SMOKER;
				} else if (truthVal.equals("PAST SMOKER")) {
					ssClass = Const.CLASS_PAST_SMOKER;
				} else if (truthVal.equals("SMOKER")) {
					ssClass = Const.CLASS_SMOKER;
				} else if (truthVal.equals("NON-SMOKER")) {
					ssClass = Const.CLASS_NON_SMOKER;
				} else if (truthVal.equals("UNKNOWN")) {
					ssClass = Const.CLASS_UNKNOWN;
				} else {
					throw new Exception("Invalid truth value for line:" + line);
				}

				TruthValue tVal = (TruthValue) iv_truthMap.get(recordID);
				if (tVal == null) {
					tVal = new TruthValue();
					tVal.iv_sentenceList = new ArrayList<String>();
					tVal.iv_classification = ssClass;
				}

				tVal.iv_sentenceList.add(sentence);

				iv_truthMap.put(recordID, tVal);

			} else {
				iv_logger.warn("Malformed line " + lineNum + ": " + line);
			}

			line = br.readLine();
			lineNum++;
		}
		br.close();

		if (iv_logger.isInfoEnabled())
			iv_logger.info("Truth data loaded for "
					+ iv_truthMap.keySet().size() + " records");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.analysis_engine.annotator.JTextAnnotator#process(org.
	 * apache.uima.jcas.impl.JCas,
	 * org.apache.uima.analysis_engine.ResultSpecification)
	 */
	public void process(JCas jcas) {
		// cleanup

		iv_entryIndexMap.clear();
		iv_procEntryList.clear();
		iv_segList.clear();

		List<ClassifiableEntry> entryList = new ArrayList<ClassifiableEntry>();
		String recordID = null;

		if (iv_logger.isInfoEnabled()) {
		 	JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		 	FSIterator<TOP> documentIDIterator = indexes.getAllIndexedFS(DocumentID.type);
			if (documentIDIterator.hasNext()) {
				DocumentID didAnn = (DocumentID) documentIDIterator.next();
				recordID = didAnn.getDocumentID();

				if (iv_logger.isInfoEnabled())
					iv_logger.info("Processing record [" + recordID + "]");
			}
		}

		Iterator<?> sentItr = jcas.getJFSIndexRepository().getAnnotationIndex(
				Sentence.type).iterator();
		while (sentItr.hasNext()) {
			Sentence sentAnn = (Sentence) sentItr.next();
			/**
			 * This is not to use the specified section. 
			 * 2-23-2009
			 * The sentence adjuster has a separate means to check for skipped segments, so
			 * this needs to be tested here as well.
			 * 9-8-2011
			 */
			// ---
//			if (sectionsToIgnore.contains(sentAnn.getSegmentId())) {
//				// System.out.println("---"+sentAnn.getSegmentId()+"|"+sentAnn.getCoveredText());
//				continue;
//			}
			Iterator<?> segItr = jcas.getJFSIndexRepository().getAnnotationIndex(Segment.type).iterator();
			Boolean skip = false;
			while (segItr.hasNext() && !skip) {
				Segment segment = (Segment) segItr.next();
				if (segment.getBegin() <= sentAnn.getBegin() && segment.getEnd() >= sentAnn.getEnd() 
						&& sectionsToIgnore.contains(segment.getId()))
					skip = true;

			}
			// ---
			if (!skip) {
				ClassifiableEntry entry = new ClassifiableEntry();
				entry.iv_recordID = recordID;
				entry.iv_begin = sentAnn.getBegin();
				entry.iv_end = sentAnn.getEnd();
				entry.iv_text = sentAnn.getCoveredText();
				entryList.add(entry);
			}
		}

		// collect segment annotations
		Iterator<?> segItr = jcas.getJFSIndexRepository().getAnnotationIndex(
				Segment.type).iterator();
		while (segItr.hasNext()) {
			Segment segAnn = (Segment) segItr.next();
			iv_segList.add(segAnn);
		}

		iv_entryIndexMap.put(recordID, entryList);

		buildProcEntryList();

		/**
		 * cycle through the procEntryList to process one sentence at a time
		 */

		try {
			for (iv_classifiableIdx = 0; iv_classifiableIdx < iv_procEntryList
					.size(); iv_classifiableIdx++) {
				jcas_local.reset();
				// create a new JCas object
				// jcas_local.setDocumentText(jcas.getDocumentText());

				// all sentences should be added to one list in iv_entryIndexMap
				ClassifiableEntry entry = (ClassifiableEntry) iv_procEntryList
						.get(iv_classifiableIdx);
				// add object to CAS that captures entry data
				RecordSentence rs = new RecordSentence(jcas_local);
				rs.setRecordID(entry.iv_recordID);

				/**
				 * To optimize processing, pcs classifier step will process just
				 * the sentence that was classified as "Known" by
				 * KURuleBasedClassifier. Document Text is thus restricted to
				 * contain Sentence Text and NOT the complete document text.
				 * Thus, begin and end cannot be directly copied from
				 * entry.iv_begin and entry.iv_end
				 */
				rs.setBegin(0);
				rs.setRecordTextBegin(0);
				rs.setEnd(entry.iv_text.length());
				rs.setRecordTextEnd(entry.iv_text.length());

				// No CAS Initiliazer
				jcas_local.setDocumentText(entry.iv_text);

				// get segment for the sentence, assume boundaries to be that of
				// the sentence
				Segment sa = getSegment(entry);
				if (sa != null) {
					Segment copy_sa = new Segment(jcas_local);
					copy_sa.setBegin(rs.getBegin());
					copy_sa.setEnd(rs.getEnd());
					copy_sa.setId(sa.getId());
					copy_sa.addToIndexes();
				} else {
					if (iv_logger.isDebugEnabled())
						iv_logger.error("Invalid Segment for sentence ["
								+ rs.getCoveredText() + "]");
				}

				// assign classification value if applicable
				// this only happens when a Truth data file was specified
				if (entry.iv_classification != null) {
					rs.setClassification(entry.iv_classification);
				}
				rs.addToIndexes();

				taeStep1.process(jcas_local);

				if (isSmokingStatusKnown(jcas_local))
					taeStep2.process(jcas_local);

				ra.process(jcas_local);

				performRecordResolution(jcas_local);
			}

			// final doc classification needs to be added to the original cas
			collectionProcessComplete(jcas);
		} catch (Exception aep) {
			try {
				throw new AnnotatorProcessException(aep);
			} catch (AnnotatorProcessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void destroy() {
		super.destroy();

		taeStep1.destroy();
		taeStep2.destroy();
	}

	/**
	 * determines of value set by KUClassifier
	 * 
	 * @param jcas_local
	 * @return
	 */
	private boolean isSmokingStatusKnown(JCas jcas_local) {
		boolean known = true;
		Iterator<?> nominalAttrItr = jcas_local.getJFSIndexRepository()
				.getAnnotationIndex(NominalAttributeValue.type).iterator();

		while (nominalAttrItr.hasNext()) {
			NominalAttributeValue nav = (NominalAttributeValue) nominalAttrItr
					.next();

			if (nav.getAttributeName().equalsIgnoreCase("smoking_status")
					&& nav.getNominalValue().equalsIgnoreCase("UNKNOWN"))
				known = false;
			// System.err.println("attr name:"+nav.getAttributeName()+" val:"+nav.getNominalValue());

		}

		return known;
	}

	private Segment getSegment(ClassifiableEntry rs) {
		Segment sa;
		for (int i = 0; i < iv_segList.size(); i++) {
			sa = (Segment) iv_segList.get(i);

			if (rs.iv_begin >= sa.getBegin() && rs.iv_end <= sa.getEnd())
				return sa;
		}

		return null;
	}

	private void performRecordResolution(JCas jcas_local)
			throws AnnotatorProcessException {
		// CAS represents a single sentence
		try {
			// should be only one RecordSentence object produced by
			// the I2B2XmlReader collection reader
			Iterator<?> rsItr = jcas_local.getJFSIndexRepository()
					.getAnnotationIndex(RecordSentence.type).iterator();

			if (rsItr.hasNext()) {

				// should be only one final NominalAttributeValue object
				// produced by
				// the ResolutionAnnotator.
				Iterator<?> navItr = jcas_local.getJFSIndexRepository()
						.getAnnotationIndex(NominalAttributeValue.type)
						.iterator();
				while (navItr.hasNext()) {
					NominalAttributeValue nav = (NominalAttributeValue) navItr
							.next();
					String classification = nav.getNominalValue();

					storeAssignedClasses(classification);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new AnnotatorProcessException(e);
		}

	}

	public void collectionProcessComplete(JCas jcas)
			throws ResourceProcessException, IOException {
		try {
			/**
			 * sort record IDs ascending For production environment, we must
			 * have just one record in the collection
			 */
			String finalClassification = resolveClassification();

			SmokingDocumentClassification docClass = new SmokingDocumentClassification(
					jcas);
			docClass.addToIndexes();
			docClass.setClassification(finalClassification);

			resetCounts();
		} catch (Exception e) {
			throw new ResourceProcessException(e);
		}
	}

	/**
	 * Given all the unique classifications for a given record, resolve it down
	 * to a single final classifcation.
	 * 
	 * @param cList
	 * @return
	 */
	private String resolveClassification() {
		// If (all sentences in a report are classified as UNKNOWN)
		// then mark that report as UNKNOWN;
		//

		if (iUnknownCtr > 0 && iSmokerCtr == 0 && iPastSmokerCtr == 0
				&& iCurrentCtr == 0 && iNonSmokerCtr == 0)
			return Const.CLASS_UNKNOWN;
		else if (iNonSmokerCtr >= 1 && iUnknownCtr >= 0 && iPastSmokerCtr == 0
				&& iCurrentCtr == 0 && iSmokerCtr == 0)
			return Const.CLASS_NON_SMOKER;
		else if (iCurrentCtr >= 1)
			return Const.CLASS_CURR_SMOKER;
		else if (iPastSmokerCtr >= 1 && iCurrentCtr <= 0)
			return Const.CLASS_PAST_SMOKER;
		else if (iSmokerCtr >= 1 && iCurrentCtr <= 0 && iPastSmokerCtr <= 0)
			return Const.CLASS_SMOKER;
		else
			return null;
	}

	private void storeAssignedClasses(String smokClass) {
		if (smokClass.equals(Const.CLASS_CURR_SMOKER))
			iCurrentCtr++;
		else if (smokClass.equals(Const.CLASS_NON_SMOKER))
			iNonSmokerCtr++;
		else if (smokClass.equals(Const.CLASS_PAST_SMOKER))
			iPastSmokerCtr++;
		else if (smokClass.equals(Const.CLASS_SMOKER))
			iSmokerCtr++;
		else if (smokClass.equals(Const.CLASS_UNKNOWN))
			iUnknownCtr++;
	}

	private void resetCounts() {
		iSmokerCtr = 0;
		iPastSmokerCtr = 0;
		iCurrentCtr = 0;
		iNonSmokerCtr = 0;
		iUnknownCtr = 0;
	}

	private void buildProcEntryList() {
		// assemble the final list of entries that will be considered
		// part of the collection to process
		int allowedCnt = 0;
		int disallowedCnt = 0;
		Iterator<String> recItr = iv_entryIndexMap.keySet().iterator();

		while (recItr.hasNext()) {
			String recordID = (String) recItr.next();
			Iterator<?> entryItr = ((List<?>) iv_entryIndexMap.get(recordID))
					.iterator();
			while (entryItr.hasNext()) {
				ClassifiableEntry entry = (ClassifiableEntry) entryItr.next();

				if ((iv_allowedClassifications == null)
						|| (iv_allowedClassifications
								.contains(entry.iv_classification))) {
					iv_procEntryList.add(entry);
					allowedCnt++;
				} else {
					if (iv_logger.isInfoEnabled())
						iv_logger.info("disallowed value:"
								+ entry.iv_classification);
					disallowedCnt++;
				}
				// System.out.println("****Sentence: " +entry.iv_text);
			}
		}

		int totalCnt = allowedCnt + disallowedCnt;
		if (iv_logger.isInfoEnabled()) {
			iv_logger.info("# total sentences: " + totalCnt);
			iv_logger.info("# allowed sentences: " + allowedCnt);
			iv_logger.info("# disallowed sentences: " + disallowedCnt);
		}
	}

	// index into the <x>annotations contained in a file, value increments as
	// the CollectionReader consumes objects
	private int iv_classifiableIdx;

	// list of ClassifiableEntry objects to be processed
	private List<ClassifiableEntry> iv_procEntryList;

	// list of segments
	private List<Segment> iv_segList;

	// Map used to index ALL ClassifiableEntry objects by their record ID
	// key = record ID (java.lang.Integer)
	// val = list of ClassifiableEntry objects
	private Map<String, List<ClassifiableEntry>> iv_entryIndexMap;

	// key = record ID (java.lang.Integer)
	// val = list of TruthValue objects
	private Map<Integer, TruthValue> iv_truthMap;

	// set of classification values of type java.lang.String
	// see org.apache.ctakes.smokingstatus.Const for proper String values
	private Set<String> iv_allowedClassifications;

	private AnalysisEngine taeStep1;
	private AnalysisEngine taeStep2;

	private ResourceSpecifier taeSpecifierStep1;
	private ResourceSpecifier taeSpecifierStep2;

	// LOG4J logger based on class name
	protected Logger iv_logger = Logger.getLogger(getClass().getName());

	// counters to track sentence classification
	private int iSmokerCtr;
	private int iPastSmokerCtr;
	private int iCurrentCtr;
	private int iNonSmokerCtr;
	private int iUnknownCtr;
	//private String apiMacroHome = "\\$main_root";
	private JCas jcas_local;
	private ResolutionAnnotator ra;
	private ResourceManager ResMgr;
	private Set<String> sectionsToIgnore;

}
