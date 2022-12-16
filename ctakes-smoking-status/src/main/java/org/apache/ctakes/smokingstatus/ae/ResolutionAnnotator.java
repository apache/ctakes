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
package org.apache.ctakes.smokingstatus.ae;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import org.apache.ctakes.smokingstatus.type.NonSmokerNamedEntityAnnotation;
import org.apache.ctakes.smokingstatus.type.SmokerNamedEntityAnnotation;

import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.smokingstatus.Const;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.smokingstatus.type.libsvm.NominalAttributeValue;

/**
 * Resolves the data produced by the KU classifier, negation detection, and PCS
 * classifier into a single smoking status value for the given sentence. The old
 * NominalAttributeValue objects are removed and replaced with a single
 * NominalAttributeValue object that represents the final classification.
 * 
 * @author Mayo Clinic
 * 
 */
public class ResolutionAnnotator 
{
    Set<String> conWords; //contradiction words for negation -- if this word appears in sentence do not negate
	// LOG4J logger based on class name
	public Logger iv_logger = Logger.getLogger(getClass().getName());

	public void initialize(UimaContext aContext)
	throws AnnotatorConfigurationException, AnnotatorInitializationException
	{
		conWords = new HashSet<String>();
		
		try
		{
			//String conWordsFileName = (String) aContext.getConfigParameterValue("ConWordsFile");
			//conWords = readLinesFromFile(FileLocator.locateFile(conWordsFileName.replaceAll(apiMacroHome, ".")).getAbsolutePath());
			
			FileResource fResrc = (FileResource) aContext.getResourceObject("negationContradictionWordsKey");
			File conWordsFile = fResrc.getFile();
			conWords = readLinesFromFile(conWordsFile.getAbsolutePath());
			
		}
		catch (Exception ace)
		{
			throw new AnnotatorConfigurationException(ace);
		}
	}
	
    public void process(JCas jcas)
    	throws AnnotatorProcessException
    {        
        // iterate over the NominalAttributeValue objects in the CAS
        // figure out the KU and PCS classification values
        String kuClassification = null;
        String pcsClassification = null;
        Iterator<?> navItr = jcas.getJFSIndexRepository().getAnnotationIndex(
                NominalAttributeValue.type).iterator();
		String navName = null;
        
        List<NominalAttributeValue> removalList = new ArrayList<NominalAttributeValue>();
        while (navItr.hasNext())
        {
            NominalAttributeValue nav = (NominalAttributeValue) navItr.next();

            String nVal = nav.getNominalValue();

            if (nVal.equals(Const.CLASS_KNOWN)
                    || nVal.equals(Const.CLASS_UNKNOWN))
            {
                kuClassification = nVal;
                navName = nav.getAttributeName();
            } else if (nVal.equals(Const.CLASS_CURR_SMOKER)
                    || nVal.equals(Const.CLASS_PAST_SMOKER)
                    || nVal.equals(Const.CLASS_SMOKER)) 
            {
                pcsClassification = nVal;
                navName = nav.getAttributeName();
            } else
            {
                throw new AnnotatorProcessException(new Exception(
                        "Nominal value not part of " + Const.class + ": "
                                + nVal));
            }
            removalList.add(nav);
        }

        // remove old NominalAttributeValue objects from CAS
        Iterator<NominalAttributeValue> removalItr = removalList.iterator();
        while (removalItr.hasNext())
        {
            TOP top = (TOP) removalItr.next();
            top.removeFromIndexes();
        }
      
        /**
         * 
         * This is to deal with cases like "nonsmoker" and "non-smoker"
         * There are two dictionaries: smoker.dictionary and nonsmoker.dictionary
         * and two NameEntities: SmokerNamedEntityAnnotation and NonSmokerNamedEntityAnnotation 
         * Each includes smoker or nonsmoker keywords respectively
         * Configuration file and dictionary are set up in Resources in DitionaryLookupAnnotator.xml
         */
      //Smoker or Nonsmoker NamedEntityAnnotation are created only if the sentence include 
      //smoker or nonsmoker keywords
      int negCnt = getSmokerNegatedCount(jcas);      
      int nonsmokerCnt = getNonSmokerNegatedCount(jcas);
      int negConCnt = getNegConCount(jcas);
      String finalClassification = null;
                            			
      /**
        * 12/04/08
        * Originally each roundtrip would have processed just one sentence
        * Now, we process the complete doc
        *
        * 1/22/09 REVERTING TO ORIGINAL CODE as classifier need to just one sentence in the cas
        */

        if (kuClassification.equals(Const.CLASS_UNKNOWN))
        {
            finalClassification = kuClassification;
        } else
        {
          if ( (negCnt>0 && negConCnt==0) || nonsmokerCnt>0 ) 
            {
                finalClassification = Const.CLASS_NON_SMOKER;
            } else
            {
                finalClassification = pcsClassification;
            }
        }

        //---check sentence-level classification 
		if (iv_logger.isInfoEnabled())
		 if(finalClassification!=Const.CLASS_UNKNOWN) {
        	Iterator senIter = jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type).iterator();		
        	while(senIter.hasNext()) {
        		Sentence sen = (Sentence) senIter.next();
        		iv_logger.info("|"+sen.getCoveredText() + "|" + finalClassification + "|" + negCnt);
        	}
        }
        //---
      
        // add final classification as a new NominalAttributeValue object
        NominalAttributeValue finalNav = new NominalAttributeValue(jcas);
        finalNav.setAttributeName(navName);
        finalNav.setNominalValue(finalClassification);
        finalNav.addToIndexes();
    }
    
	private Set<String> readLinesFromFile(String fileName) throws IOException
	{
		Set<String> returnValues = new HashSet<String>();
		File file = new File(fileName);
	    BufferedReader fileReader = new BufferedReader(new FileReader(file));
		
		String line;
		while((line = fileReader.readLine()) != null)
		{
    		line = line.toLowerCase();
        	returnValues.add(line);
		}
		return returnValues;
	}
	
	private int getSmokerNegatedCount(JCas jcas)
	{
		int negCnt = 0;      
		Iterator<?> neItr= jcas.getJFSIndexRepository().getAnnotationIndex(
				SmokerNamedEntityAnnotation.type).iterator();

		while (neItr.hasNext())
		{
			SmokerNamedEntityAnnotation neAnn = (SmokerNamedEntityAnnotation) neItr.next();
			int certainty = neAnn.getPolarity();
			//TODO: need to re-define this in TypeSystemConst.java and re-release core
//			if (certainty == TypeSystemConst.NE_CERTAINTY_NEGATED)
			if (certainty == -1)
				negCnt++; 
			iv_logger.info("***SmokerNameEntity***" + neAnn.getCoveredText() + " " + negCnt);
		}

		return negCnt;
	}
    
	private int getNonSmokerNegatedCount(JCas jcas)
	{
		int nonSmokerCnt = 0;
		Iterator<?> neItr= jcas.getJFSIndexRepository().getAnnotationIndex(
				NonSmokerNamedEntityAnnotation.type).iterator();

		while (neItr.hasNext())
		{
			NonSmokerNamedEntityAnnotation neAnn = (NonSmokerNamedEntityAnnotation) neItr.next();
			nonSmokerCnt++;
			iv_logger.info("***NonSmokerNameEntity***" + neAnn.getCoveredText() + " " + nonSmokerCnt + " " + neAnn.getPolarity());
		}

		return nonSmokerCnt;
	}
    
	/**
	 * This is to count contradiction words -- if appears do not negate
	 * eg) Tobacco: no quit in 1980 -- "quit" is contradiction words. So do not negate
	 */
    private int getNegConCount(JCas jcas) {
    	int conCnt = 0;
    	Iterator<?> wordTokenItr = jcas.getJFSIndexRepository().getAnnotationIndex(
    			WordToken.type).iterator();

    	while (wordTokenItr.hasNext())
    	{
    		WordToken token = (WordToken) wordTokenItr.next();
    		String tok = token.getCoveredText();

    		if(tok == null) continue;
    		tok = tok.toLowerCase().replaceAll("[\\W]", " ").trim(); 
    		String[] toks = tok.split("\\s");
    		for(int i=0; i<toks.length; i++) 
    			if(conWords.contains(toks[i])) 
    				conCnt++;      	
    	}

    	return conCnt;
    }
    private String apiMacroHome = "\\$main_root"; 
}
 
