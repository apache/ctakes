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
package org.apache.ctakes.sideeffect.ae;

import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.core.util.FSUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.drugner.type.SubSectionAnnotation;
import org.apache.ctakes.sideeffect.type.SESentence;
import org.apache.ctakes.sideeffect.type.SideEffectAnnotation;
import org.apache.ctakes.sideeffect.util.PatternMatch;
import org.apache.ctakes.sideeffect.util.SEUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Rule-based side effect extraction. 
 * Identify side effects and causative drugs and add them to SideEffectAnnotation  
 * @author Mayo Clinic
 *
 */

/*
 * ===Notation of pattern used in rules===
 * + means one or more the previous item
 * ... means zero or more non-PSE/non-drug string 
 * *** means zero or more string (can be PSE/drug)
 */
public class SideEffectAnnotator extends JCasAnnotator_ImplBase
{	
	private class PotentialSideEffect {
		IdentifiedAnnotation ne; //NE of PSE
		IdentifiedAnnotation drug; //potential causative drug
		String sentence; //sentence that contains PSE and potential causative drug
		int senBegin, senEnd; //offset of sentence
	}
	
	private class SideEffect {
		PotentialSideEffect pse;
		String rule; //name of used rule
	}
	
	//side effect dictionary
	private Map<String, String> keyDrugMap = new HashMap<String, String>(); //key:(String)brand or generic drug, val:(String)generic drug 
	private Map< String, Set<String> > sideEffectMap = new HashMap< String, Set<String> >(); //key:(String)generic drug, val:(set<String>) set of side effects
	
	private Set<String> setionsToIgnore = new HashSet<String>(); //sections not to use for side effect extraction
	
	//side effect indication keywords
	private List<String> causeVerb = new ArrayList<String>();
	private List<String> causeWord1 = new ArrayList<String>();
	private List<String> causeWord2 = new ArrayList<String>();
	private List<String> discontVerb = new ArrayList<String>();
	private List<String> sideEffectWord = new ArrayList<String>();
	private List<String> noteVerb = new ArrayList<String>();
	private List<String> madeVerb = new ArrayList<String>();
	private List<String> afterWord = new ArrayList<String>();

	public void initialize(UimaContext annotCtx)
	throws ResourceInitializationException
	{ 
		super.initialize(annotCtx);	
		
		//get a side-effect dictionary
		FileResource fResrc = null;
		try {
			fResrc = (FileResource) getContext().getResourceObject("sideEffectTable");	
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
		//set a side-effect dictionary
		setMap(fResrc.getFile(), keyDrugMap, sideEffectMap);
		
		try {			
	        String[] str;
	        
	        //get sections to ignore for side effect extraction
	        str = (String[]) getContext().getConfigParameterValue("sectionsToIgnore");
	        for (int i = 0; i < str.length; i++)
	        	setionsToIgnore.add(str[i]);
	        
	        //get indication words for each pattern matching rules
	        
	        str = (String[]) getContext().getConfigParameterValue("hasPatternOfDrugCauseVerbPse");
	        for (int i = 0; i < str.length; i++)
	        	causeVerb.add(str[i]);
	        
	        str = (String[]) getContext().getConfigParameterValue("hasPatternOfPseDueToDrug");
	        for (int i = 0; i < str.length; i++)
	        	causeWord1.add(str[i]);
	        
	        str = (String[]) getContext().getConfigParameterValue("hasPatternOfDrugDueToPse");
	        for (int i = 0; i < str.length; i++)
	        	causeWord2.add(str[i]);
	        
	        str = (String[]) getContext().getConfigParameterValue("hasPatternOfDiscontDrugBecausePse");
	        for (int i = 0; i < str.length; i++)
	        	discontVerb.add(str[i]);
	        
	        //used in hasWordOfSideEffect() and hasSideEffectAsPse()
	        str = (String[]) getContext().getConfigParameterValue("sideEffectWord");
	        for (int i = 0; i < str.length; i++)
	        	sideEffectWord.add(str[i]);
	        
	        str = (String[]) getContext().getConfigParameterValue("hasPatternOfNotePseWithDrug");
	        for (int i = 0; i < str.length; i++)
	        	noteVerb.add(str[i]);
	        
	        str = (String[]) getContext().getConfigParameterValue("hasPatternOfDrugMadePse");
	        for (int i = 0; i < str.length; i++)
	        	madeVerb.add(str[i]);
	        
	        str = (String[]) getContext().getConfigParameterValue("hasPatternOfPseAfterDrug");
	        for (int i = 0; i < str.length; i++)
	        	afterWord.add(str[i]);	        
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}		
	}

	public void process(JCas jcas)
	throws AnalysisEngineProcessException
	{
		List<PotentialSideEffect> potentialSideEffectLst;
		List<SideEffect> sideEffectLst;

      String docName = DocIdUtil.getDocumentID( jcas );
        System.out.println("---"+docName+" processed---");
        
        //remove duplicated named entities
        //There can be duplicated NEs if there is an overlap between the original and supplemental dictionary
        removeDuplicatedNEs(jcas);
                
        //get potential side effects
        potentialSideEffectLst = getPotentialSideEffects(jcas);
        
        //extract side effects & causative drugs
        sideEffectLst = getSideEffectsWithPrioritizedRule(jcas, potentialSideEffectLst);
                
        //add side effects to JCAS
        annotateSideEffects(jcas, sideEffectLst);
	}

	/**
	 * Remove duplicated NEs from Index (same type and offset)
	 * @param jcas
	 */
	private void removeDuplicatedNEs(JCas jcas) {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
        Iterator neItr= indexes.getAnnotationIndex(IdentifiedAnnotation.type).iterator();
        Set<String> spanSet = new HashSet<String>();
        List<IdentifiedAnnotation> duplicatedNE = new ArrayList<IdentifiedAnnotation>();
        
        while (neItr.hasNext()) {
        	IdentifiedAnnotation nea = (IdentifiedAnnotation) neItr.next();        	
        	String span = Integer.toString(nea.getTypeID())+"|"+
        		Integer.toString(nea.getBegin())+"|"+Integer.toString(nea.getEnd());
        	if(spanSet.contains(span)) {
        		duplicatedNE.add(nea);
        	}
        	spanSet.add(span);
        }
        
        for(IdentifiedAnnotation ne : duplicatedNE) {
        	ne.removeFromIndexes();
        }
	}
	
	/**
	 * Return List of NEs (signs/symptoms & diseases/disorders) if they are NOT negated
	 * -- ignore sections in setionsToIgnore
	 * -- only get the longest NE if it contains the shorter
     * 	  (eg. "chest pain", "pain" - select only "chest pain")
	 * @param jcas
	 * @return List of non-negated signs/symptoms & diseases/disorders in the document 
	 */
	private List<IdentifiedAnnotation> getSideEffectNEs(JCas jcas) {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
        Iterator neItr= indexes.getAnnotationIndex(IdentifiedAnnotation.type).iterator();
        List<IdentifiedAnnotation> l = new ArrayList<IdentifiedAnnotation>();
        
        while (neItr.hasNext()) {
        	IdentifiedAnnotation nea = (IdentifiedAnnotation) neItr.next();
        	if(setionsToIgnore.contains(nea.getSegmentID())) continue;
        	if(nea.getPolarity()==-1) continue; //negated
        	       	
        	boolean addFlag = true;
        	        	
        	if(nea.getTypeID()==3 || nea.getTypeID()==2) { //signs/symptoms or diseases/disorders
        		//add the longest only (also remove the duplicate)
        		if(l.size()==0) l.add(nea);
        		else {        			
	        		for(int i=0; i<l.size(); i++) {
	        			IdentifiedAnnotation x = (IdentifiedAnnotation) l.get(i);
	        			int j = SEUtil.contains(nea.getBegin(), nea.getEnd(), 
	        						x.getBegin(), x.getEnd());
	        			if(j==1) {
	        				l.remove(i);
	        				i--;
	        			}
	        			else if(j==2) { 
	        				addFlag = false; 
	        				break;
	        			}
	        		}	        		
	        		if(addFlag)
	        			l.add(nea);       			
        		}       		
        	}
        } 
        
        return l;
	}
	
	/**
	 * Return List of PotentialSideEffect classes 
	 * - get NEs from getSideEffectNEs()
	 * - drug must appear in the same or previous sentence 
	 *   - but in Allergy section drug must appear in the same sentence 
	 *   - don't include the sentence in different line
	 *
	 * @param jcas
	 * @return List of PotentialSideEffect
	 */
	private List<PotentialSideEffect> getPotentialSideEffects(JCas jcas) {		
        List<IdentifiedAnnotation> l = getSideEffectNEs(jcas);        
        List<PotentialSideEffect> ll = new ArrayList<PotentialSideEffect>();
        
        for(int i=0; i<l.size(); i++) {
        	IdentifiedAnnotation ne = (IdentifiedAnnotation) l.get(i);
    		int[] senSpan = SEUtil.getSentenceSpanContainingGivenSpan(jcas, ne.getBegin(), ne.getEnd());
    		String sentence = SEUtil.getSentenceTextContainingGivenSpan(jcas, ne.getBegin(), ne.getEnd()).trim();
    		boolean foundDrug = false;
    		
    		//check the same sentence if it contains drug
    		Iterator neIter = FSUtil.getAnnotationsIteratorInSpan(jcas, IdentifiedAnnotation.type, senSpan[0], senSpan[1]+1);
    		while(neIter.hasNext()) {
    			IdentifiedAnnotation n = (IdentifiedAnnotation) neIter.next();
    			if(n.getTypeID()==1) { //drug    				
    				PotentialSideEffect pse = new PotentialSideEffect();
        			pse.ne = ne;
        			pse.sentence = sentence;
        			pse.senBegin = senSpan[0];
        			pse.senEnd = senSpan[1];
        			pse.drug = n;
        			ll.add(pse);
    				foundDrug = true;
    			}
    		}
    		
    		// if drug is not found in the same sentence, check the previous sentence
    		if(!foundDrug && !ne.getSegmentID().equals("20105")) {
    			int num = SEUtil.getSentenceNumContainingGivenSpan(jcas, ne.getBegin(), ne.getEnd());
    			num = (num>0) ? num-1 : num;    			
    			int [] previousSenSpan = SEUtil.getSentenceSpanOfGivenSentenceNum(jcas, num);
    			
    			//only if they are in the same line
    			if(SEUtil.isSpanInSameLine(jcas, previousSenSpan[0], senSpan[1])) {
    				neIter = FSUtil.getAnnotationsIteratorInSpan(jcas, IdentifiedAnnotation.type, previousSenSpan[0], previousSenSpan[1]+1);
    				while(neIter.hasNext()) {
    					IdentifiedAnnotation n = (IdentifiedAnnotation) neIter.next();
    					if(n.getTypeID()==1) { //drug
    						PotentialSideEffect pse = new PotentialSideEffect();
    						pse.ne = ne;
    						pse.sentence = SEUtil.getSentenceTextContainingGivenSpan(
    								jcas, n.getBegin(), n.getEnd()) 
    								+ " " + sentence;
    						pse.senBegin = previousSenSpan[0];
    						pse.senEnd = senSpan[1];
    						pse.drug = n;
    						ll.add(pse);
    					}
    				}
    			}
    		}   			
        }
        	
        return ll;
	}
			
	/**
	 * Return a list of SideEffect found by rules. Rules are applied in order of reliability.
	 * Assigning se.rule is optional (it is just to check which rule is used for SE extraction) 
	 * 
	 * @param jcas
	 * @param pseLst list of PotentialSideEffect classes
	 * @return List of SideEffect classes found by rules
	 */
	private List<SideEffect> getSideEffectsWithPrioritizedRule(JCas jcas, List<PotentialSideEffect> pseLst) {
		List<SideEffect> seLst = new ArrayList<SideEffect>();
		
		for(PotentialSideEffect pse : pseLst) {
			String input = getRegexInput(jcas, pse);
			SideEffect se = new SideEffect(); 
			
			if(isInAllergySection(jcas, pse)) {
				se.pse = pse;
				se.rule = "isInAllergySection";
				seLst.add(se);	
			}
			else if(hasPatternOfDrugDueToPse(jcas, pse, input)) { 
				se.pse = pse;
				se.rule = "hasPatternOfDrugDueToPse";
				seLst.add(se);	
			}
			else if(hasPatternOfDrugCausePse(jcas, pse, input)) {
				se.pse = pse;
				se.rule = "hasPatternOfDrugCausePse";
				seLst.add(se);	
			}
			else if(hasPatternOfDrugMadePse(jcas, pse, input)) {
				se.pse = pse;
				se.rule = "hasPatternOfDrugMadePse";
				seLst.add(se);	
			}
			else if(hasWordOfSideEffect(jcas, pse, input)) {
				se.pse = pse;
				se.rule = "hasWordOfSideEffect";
				seLst.add(se);	
			}
			else if(hasPatternOfPseDueToDrug(jcas, pse, input)) {
				se.pse = pse;
				se.rule = "hasPatternOfPseDueToDrug";
				seLst.add(se);
			}
			else if(hasSideEffectAsPse(jcas, pse, input)) {
				se.pse = pse;
				se.rule = "hasSideEffectAsPse";
				seLst.add(se);
			}
			else if(hasPatternOfNotePseWithDrug(jcas, pse, input)) {
				se.pse = pse;
				se.rule = "hasPatternOfNotePseWithDrug";
				seLst.add(se);
			}
			else if(hasPatternOfDiscontDrugBecausePse(jcas, pse, input)) {
				se.pse = pse;
				se.rule = "hasPatternOfDiscontDrugBecausePse";
				seLst.add(se);
			}
			else if(hasPatternOfDrugDiscontBecausePse(jcas, pse, input)) {
				se.pse = pse;
				se.rule = "hasPatternOfDrugDiscontBecausePse";
				seLst.add(se);
			}
			else if(hasPatternOfPseAfterDrug(jcas, pse, input)) { 
				se.pse = pse;
				se.rule = "hasPatternOfPseAfterDrug";
				seLst.add(se);
			}
			else if(isInParenthesis(jcas, pse, input)) { 
				se.pse = pse;
				se.rule = "isInParenthesis";
				seLst.add(se);
			}
			else if(isInDictionary(jcas, pse)) {
				se.pse = pse;
				se.rule = "isInDictionary";
				seLst.add(se);
			}
		}
					
		return seLst;		
	}
		
	/**
	 * Add side effect components to CAS
	 * @param jcas
	 * @param seLst List of SideEffect classes found by the rules
	 */
	private void annotateSideEffects(JCas jcas, List<SideEffect> seLst) {
		for(int i=0; i<seLst.size(); i++) {
			SideEffect se = (SideEffect) seLst.get(i);
		
			SideEffectAnnotation sea = new SideEffectAnnotation(jcas);
			sea.setSideEffect(se.pse.ne);
			if(se.pse.ne!=null) {
				sea.setBegin(se.pse.ne.getBegin());
				sea.setEnd(se.pse.ne.getEnd());
			}
			else {
				sea.setBegin(se.pse.senBegin);
				sea.setEnd(se.pse.senEnd);
			}
			sea.setDrug(se.pse.drug);
						
			SESentence ses = new SESentence(jcas);
			ses.setBegin(se.pse.senBegin);
			ses.setEnd(se.pse.senEnd);
			sea.setSentence(ses);
			
			sea.addToIndexes();
		}
	}
		
	/**
	 * set keyDrugMap and sideEffectMap
	 * -- skip line starting with "//"
	 * @param file format:genericDrug|brandDrug1, brandDrug2,...|se1, se2,...
	 * @param keyMap keyDrugMap
	 * @param seMap sideEffectMap
	 */
	private void setMap(File file, Map<String, String> keyMap, Map< String, Set<String> > seMap) {
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader(file));
			String line="";
			while((line=fileReader.readLine()) != null)
			{
	    		if(line.startsWith("//")) continue;
	    		line = line.toLowerCase();
	    		String[] str = line.split("\\|");
	    		String genericDrug = str[0].trim();
	    		String[] brandDrugs = str[1].split(",");
	    		String[] sideEffects = str[2].split(",");
	    		Set<String> seSet = new HashSet<String>();

	    		keyMap.put(genericDrug, genericDrug);	    		
	    		for(int i=0; i<brandDrugs.length; i++) {
	    			keyMap.put(brandDrugs[i].trim(), genericDrug);
	    		}
	    		for(int i=0; i<sideEffects.length; i++) {
	    			seSet.add(sideEffects[i].trim());
	    		}
	    		seMap.put(genericDrug, seSet);
			}			
			fileReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
		
	//*---------------------------------------------------------------------------
	//* rules to extract side effect and causative drug pairs
	//*---------------------------------------------------------------------------
	/**
	 * Return true if pse+...DueToWord...drug+ 
	 * - drug must NOT be negated
	 * - only consecutive drug(s) are permitted for multiple causing drugs 
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match 
	 * @return true if finds a given pattern, else false 
	 */
	private boolean hasPatternOfPseDueToDrug(JCas jcas, PotentialSideEffect pse, String input) {
		//false if drug is negated
		if(pse.drug.getPolarity()==-1) return false;

		//false if not satisfied a given pattern
		PatternMatch pm = new PatternMatch("(<@PSE>).*(KW).*(<@DRUG>)", input, causeWord1);
		if(!pm.mat.find()) return false;
		
		//false if there is DRUG between @PSE and KW
		if(pm.isDrugBetween(pm.mat.end(1), pm.mat.start(2))) return false;
		
		//false if there is PSE between KW and @DRUG
		if(pm.isPseBetween(pm.mat.end(2), pm.mat.start(3))) return false;

		//if exist another DRUG(s) between KW and @DRUG (i.e., KW...DRUG...@DRUG)
		//only "and" "or" "," is permitted between DRUG and @DRUG 
		int[] drugSpan={-1, -1};
		if(pm.isDrugBetween(pm.mat.end(2), pm.mat.start(3), drugSpan)) {
			if(pm.isDistantBetween(drugSpan[1], pm.mat.start(3)))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Return true if drug+...DueToWord...pse+
	 *
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match
	 * @return true if finds a given pattern, else false 
	 */
	private boolean hasPatternOfDrugDueToPse(JCas jcas, PotentialSideEffect pse, String input) {
		//false if not satisfied a given pattern
		PatternMatch pm = new PatternMatch("(<@DRUG>).*(KW).*(<@PSE>)", input, causeWord2);
		if(!pm.mat.find()) return false;
		
		//false if there is PSE between @DRUG and KW
		if(pm.isPseBetween(pm.mat.end(1), pm.mat.start(2))) return false;
		
		//false if there is DRUG between KW and @PSE
		if(pm.isDrugBetween(pm.mat.end(2), pm.mat.start(3))) return false;

		//if exist another DRUG(s) between @DRUG and KW (i.e., @DRUG...DRUG...KW)
		//only "and" "or" "," is permitted between @DRUG and DRUG
		if(pm.isDrugBetween(pm.mat.end(1), pm.mat.start(2))) {
			if(pm.isDistantBetween(pm.mat.end(1), pm.mat.start(2)))
				return false;
		}
		
		return true;
	}

	/**
	 * Return true if discontVerb...DRUG+...because|after...PSE+
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match 
	 * @return true if finds a given pattern, else false 
	 */
	private boolean hasPatternOfDiscontDrugBecausePse(JCas jcas, PotentialSideEffect pse, String input) {
		PatternMatch pm = new PatternMatch("(KW).*(<@DRUG>).*(because|after).*(<@PSE>)", input, discontVerb);
		if(!pm.mat.find()) return false;
		
		if(pm.isPseBetween(pm.mat.end(1), pm.mat.start(2))) return false;
		if(pm.isPseBetween(pm.mat.end(2), pm.mat.start(3))) return false;
		if(pm.isDrugBetween(pm.mat.end(3), pm.mat.start(4))) return false;
		int[] drugSpan={-1, -1};
		if(pm.isDrugBetween(pm.mat.end(1), pm.mat.start(2), drugSpan)) {
			if(pm.isDistantBetween(drugSpan[1], pm.mat.start(2)))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Return true if DRUG+***discontVerb...because|after...PSE+ 
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match 
	 * @return true if finds a given pattern, else false 
	 */
	private boolean hasPatternOfDrugDiscontBecausePse(JCas jcas, PotentialSideEffect pse, String input) {
		PatternMatch pm = new PatternMatch("(<@DRUG>).*(KW).*(because|after).*(<@PSE>)", input, discontVerb);
		if(!pm.mat.find()) return false;
		
		if(pm.isPseBetween(pm.mat.end(2), pm.mat.start(3))) return false;
		if(pm.isDrugBetween(pm.mat.end(2), pm.mat.start(3))) return false;
		if(pm.isDrugBetween(pm.mat.end(3), pm.mat.start(4))) return false;
		int[] drugSpan={-1, -1};
		if(pm.isDrugBetween(pm.mat.end(1), pm.mat.start(2), drugSpan)) {
			if(pm.isDistantBetween(pm.mat.end(1), drugSpan[0]))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Return true if noteVerb...PSE+...with...Drug+ 
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match
	 * @return true if finds a given pattern, else false 
	 */
	private boolean hasPatternOfNotePseWithDrug(JCas jcas, PotentialSideEffect pse, String input) {
		PatternMatch pm = new PatternMatch("(KW).*(<@PSE>).*(with).*(<@DRUG>)", input, noteVerb);
		if(!pm.mat.find()) return false;
		
		if(pm.isDrugBetween(pm.mat.end(1), pm.mat.start(2))) return false;
		if(pm.isDrugBetween(pm.mat.end(2), pm.mat.start(3))) return false;
		if(pm.isPseBetween(pm.mat.end(3), pm.mat.start(4))) return false;
		int[] drugSpan={-1, -1};
		if(pm.isDrugBetween(pm.mat.end(3), pm.mat.start(4), drugSpan)) {
			if(pm.isDistantBetween(drugSpan[1], pm.mat.start(4)))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Return true if Drug+***causeVerb...Pse+
	 * -- allow other than drug NE between Drug and causeVerb
	 *    (eg: Paxil was started for anxiety but caused a rash and was discontinued.)
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match
	 * @return true if finds a given pattern, else false 
	 */
	private boolean hasPatternOfDrugCausePse(JCas jcas, PotentialSideEffect pse, String input) {
		PatternMatch pm = new PatternMatch("(<@DRUG>).*(KW).*(<@PSE>)", input, causeVerb);
		if(!pm.mat.find()) return false;
		
		if(pm.isDrugBetween(pm.mat.end(2), pm.mat.start(3))) return false;
		int[] drugSpan={-1, -1};
		if(pm.isDrugBetween(pm.mat.end(1), pm.mat.start(2), drugSpan)) {
			if(pm.isDistantBetween(pm.mat.end(1), drugSpan[0]))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Return true if Drug+***madeVerb him/her Pse+
	 * -- allow other than drug NE between Drug and madeVerb
	 *    (eg: Paxil was started for anxiety but made him a rash.)
	 *    
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match 
	 * @return true if finds a given pattern, else false 
	 */
	private boolean hasPatternOfDrugMadePse(JCas jcas, PotentialSideEffect pse, String input) {
		PatternMatch pm = new PatternMatch("(<@DRUG>).*(KW)\\s(him|her)\\s(<@PSE>)", input, madeVerb);
		if(!pm.mat.find()) return false;
		
		int[] drugSpan={-1, -1};
		if(pm.isDrugBetween(pm.mat.end(1), pm.mat.start(2), drugSpan)) {
			if(pm.isDistantBetween(pm.mat.end(1), drugSpan[0]))
				return false;
		}
		
		return true;	
	}
	
	/** 
	 * Return true if pse is NOT sideEffectWord and the pse.sentence contains sideEffectWord 
	 * - this is a naive rule
	 * - sideEffectWord must not be negated
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match  
	 * @return true if finds a given pattern, else false 
	 */	
	private boolean hasWordOfSideEffect(JCas jcas, PotentialSideEffect pse, String input) {		
		//if pse is one of sideEffectWord return false
		String pseStr = pse.ne.getCoveredText().replace('-',' ').toLowerCase().trim();
		if(sideEffectWord.contains(pseStr)) return false;
								
		//if there is no sideEffectWord in the sentence, return false
		String pseSen = pse.sentence.replace('-', ' ').toLowerCase().trim();
		PatternMatch pm = new PatternMatch("(KW)", pseSen, sideEffectWord);
		if(!pm.mat.find()) return false;
		
		//if sideEffectWord is negated return false
		Iterator neIter = FSUtil.getAnnotationsIteratorInSpan(jcas, IdentifiedAnnotation.type, pse.senBegin, pse.senEnd+1);
		while(neIter.hasNext()) {
			IdentifiedAnnotation ne = (IdentifiedAnnotation) neIter.next();
			if(ne.getCoveredText().replace('-',' ').toLowerCase().trim()
					.indexOf(pm.mat.group(1))!=-1 && ne.getPolarity()==-1) 
				return false;			
		}
				
		return true;
	}
	
	/**
	 * Return true if pse is sideEffectWord and there is no other pse(s) in the pse.sentence 
	 * but if the pse sentence contains certain word, return false
	 * - this is a naive rule 
	 * - sideEffectWord must not be negated
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match
	 * @return true if finds a given pattern, else false 
	 */	
	private boolean hasSideEffectAsPse(JCas jcas, PotentialSideEffect pse, String input) {
		//if the pse sentence contains certain word, return false
		if(input.matches(".*(dicussed|concerned).*")) return false;
				
		//if pse!=sideEffectWord, return false
		String str = pse.ne.getCoveredText().replace('-', ' ').toLowerCase().trim();
		if(!sideEffectWord.contains(str)) return false;
		
		//if there is PSE, return false
		if(input.matches(".*(<PSE>).*")) return false;
				
		return true;
	}
	
	/**
	 * Return true if pse+...after taking/after starting...drug+ 
	 * but if the pse sentence contains certain word, return false
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match
	 * @return true if finds a given pattern, else false 
	 */	
	private boolean hasPatternOfPseAfterDrug(JCas jcas, PotentialSideEffect pse, String input) {
		//if the pse sentence contains certain word, return false
		if(input.matches(".*(check).*")) return false;
		
		PatternMatch pm = new PatternMatch("(<@PSE>).*(KW).*(<@DRUG>)", input, afterWord);
		if(!pm.mat.find()) return false;
		
		if(pm.isDrugBetween(pm.mat.end(1), pm.mat.start(2))) return false;
		if(pm.isPseBetween(pm.mat.end(2), pm.mat.start(3))) return false;
		int[] drugSpan={-1, -1};
		if(pm.isDrugBetween(pm.mat.end(2), pm.mat.start(3), drugSpan)) {
			if(pm.isDistantBetween(drugSpan[1], pm.mat.start(3)))
				return false;
		}
		
		return true;
	}
			
	/**
	 * This rule is customized in the local document. May require to adjust for different allergy patterns!!
	 * Return true if pse is in Medication subsection in Allergy section. 
	 * A drug in the pse.sentence is assigned as causing drug
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @return true if satisfies given conditions, else false 
	 */
	private boolean isInAllergySection(JCas jcas, PotentialSideEffect pse) {
		if(!pse.ne.getSegmentID().equals("20105")) return false;
		
		//check if pse is in Medication subsection
		boolean inMedication = false;
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
        Iterator saIter= indexes.getAnnotationIndex(Segment.type).iterator();
        while(saIter.hasNext()) {
        	Segment sa = (Segment) saIter.next();
        	if(sa.getBegin()<pse.ne.getBegin() && sa.getEnd()>pse.ne.getEnd()) {
        		Iterator ssIter = FSUtil.getAnnotationsIteratorInSpan(jcas, SubSectionAnnotation.type, sa.getBegin(), sa.getEnd());
        		while(ssIter.hasNext()) {
        			SubSectionAnnotation ss = (SubSectionAnnotation) ssIter.next();
        			//if(ss.getCoveredText().replaceAll(":","").trim().equalsIgnoreCase("medication")) 
            		if(ss.getCoveredText().toLowerCase().trim().startsWith("medication")) 
        				if(ss.getSubSectionHeaderBegin()<pse.ne.getBegin() &&
        						ss.getSubSectionBodyEnd()>pse.ne.getEnd()) {
        					inMedication = true;
        					break;
        				}
        		}
        	}
        }	
        if(!inMedication) return false;
        	
		return true;
	}

	/**
	 * This rule is customized in the local document. May require to adjust for different documents!!
	 * If pse is right after drug in the parenthesis 
	 * with certain words (but not with "for" or "treated with")
	 * or with the pse sentence containing "trial*",
	 * then return true 
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @param input modified SE sentence used for regular expression match 
	 * @return true if satisfies given conditions, else false 
	 */
	private boolean isInParenthesis(JCas jcas, PotentialSideEffect pse, String input) {
		//if pse is not in the parenthesis return false		
		PatternMatch pm = new PatternMatch("(<@DRUG>)\\s(\\(.*<@PSE>.*\\))", input); 
		if(!pm.mat.find()) return false;
		
		//if "for" or "treated with" is also in the parenthesis, return false
		if(pm.mat.group(2).matches("\\(.*((for)|(treated with)).*\\)")) return false; 

		//if there is certain words in the parenthesis, return true
		if(pm.mat.group(2).matches("\\(.*(made|got).*\\)")) return true; 

		//if the pse sentence does not include "trial", return false
		if(input.matches(".*trial.*")) return true;

		return false;
	}
	
	/**
	 * Return true if pse belongs to side effects in the side-effect dictionary
	 * 
	 * @param jcas
	 * @param pse potential side effect
	 * @return true if satisfies given conditions, else false 
	 */
	private boolean isInDictionary(JCas jcas, PotentialSideEffect pse) {
		String drug = pse.drug.getCoveredText().toLowerCase().trim();
		
		if(keyDrugMap.containsKey(drug)) {
			Set<String> seSet = sideEffectMap.get(keyDrugMap.get(drug));
			String pseStr = pse.ne.getCoveredText().toLowerCase().trim();
			//exact matching
			if(seSet.contains(pseStr)) return true;
		}
		
		return false;
	}
	
	//-------------------------------------------------------------------
	/**
	 * Return a string to be used for regular expression pattern matching
	 * Replace a target sign/symptom or disorder/diseas with <@PSE>,
	 * a target drug with <@DRUG>, and the other pse and drug with <PSE> and <DRUG> respectively, 
	 * and lower cased the rest text. 
	 *  
	 *	eg) target PSE: upset stomach, target drug: Aspirin
	 * 	    Aspirin and Tylenol cause upset stomach -> <@DRUG> and <DRUG> cause <@PSE>
	 * 
	 * @param jcas
	 * @param pse instance of PotentialSideEffect
	 * @return string that is used for regular expression matching  
	 */
	private String getRegexInput(JCas jcas, PotentialSideEffect pse) {		
		String str = pse.sentence.toLowerCase();
		Iterator neIter = FSUtil.getAnnotationsIteratorInSpan(jcas, IdentifiedAnnotation.type, pse.senBegin, pse.senEnd+1);
		// NEs are stored in CAS in order of offsets so using replaceFirst() works
		while(neIter.hasNext()) {
			IdentifiedAnnotation nea = (IdentifiedAnnotation) neIter.next();
			if(nea.getTypeID()==1) {
				String drug="";
				if(nea.getBegin()==pse.drug.getBegin() && nea.getEnd()==pse.drug.getEnd())
					drug = "<@DRUG>";
				else 
					drug = "<DRUG>";
				//some named entity contain special char used in RegEx (eg, ')')
				str = str.replaceFirst(
						nea.getCoveredText().replaceAll("[\\<\\(\\[\\{\\\\^\\-\\=\\$\\!\\|\\]\\}\\)\\?\\*\\+\\.\\>]", "").toLowerCase(),drug);
			}
			else if(nea.getTypeID()==2 || nea.getTypeID()==3) {
				String ps="";
				if(nea.getBegin()==pse.ne.getBegin() && nea.getEnd()==pse.ne.getEnd())
					ps = "<@PSE>";
				else 
					ps = "<PSE>";
				str = str.replaceFirst(
						nea.getCoveredText().replaceAll("[\\<\\(\\[\\{\\\\^\\-\\=\\$\\!\\|\\]\\}\\)\\?\\*\\+\\.\\>]", "").toLowerCase(),ps);
			}			
		}
				
		return str.trim();
	}
}
