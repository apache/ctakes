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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.JCas;

import org.apache.ctakes.sideeffect.type.PSESentenceFeature;
import org.apache.ctakes.sideeffect.type.SESentence;
import org.apache.ctakes.sideeffect.type.SideEffectAnnotation;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Classify side effect sentences and add them to SESentence
 * @author Mayo Clinic
 *
 */

public class SESentenceClassifierAnnotator extends JCasAnnotator_ImplBase {

	private svm_model model; //trained libsvm model
	private Map<String, String> feaMap; //key:nominal values, values: converted value

	@Override
	public void initialize(UimaContext uimaContext) throws ResourceInitializationException
	{ 
		super.initialize(uimaContext);
		  		
		try {
			String pathOfTrainedModel = (String) getContext().getConfigParameterValue("PathOfModel");
      		model = svm.svm_load_model(pathOfTrainedModel);
	  	}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//mapping nominal values to integer 
		//why convert to integer? tried binary values but didn't performed well
		feaMap = new HashMap<String, String>();
		feaMap.put("nul", "0"); // both side effect keyword and location features
		feaMap.put("pre", "1"); // side effect keyword feature
		feaMap.put("bet", "1"); // location feature
		feaMap.put("bpd", "2"); // location feature
		feaMap.put("bdp", "3"); // location feature
		feaMap.put("bap", "4"); // location feature
		feaMap.put("bep", "5"); // location feature
		feaMap.put("afp", "6"); // location feature
		feaMap.put("any", "7"); // location feature
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		Iterator psfIter = indexes.getAnnotationIndex(PSESentenceFeature.type).iterator();	
		Set<String> seSenSpans_ML = new HashSet<String>();
		
		//get side-effect sentence spans found by ML (livSVM)
		while(psfIter.hasNext()) {
			PSESentenceFeature psf = (PSESentenceFeature) psfIter.next();
			
			//classify SE sentences based on the trained libSVM model
	    	svm_node[] x = new svm_node[psf.getFeatures().size()];
			for(int j=0;j<psf.getFeatures().size();j++)
			{
				x[j] = new svm_node();
				x[j].index = j+1;
				String nomFea = psf.getFeatures(j);
				x[j].value = Double.parseDouble(feaMap.get(nomFea));
			}
			
			double clsLabel; //0:non-SE sentence , 1:SE sentence 
			clsLabel = svm.svm_predict(model,x);
			
			//add side-effect sentence span to the Set
			if(clsLabel==1) {							
				String span = Integer.toString(psf.getPseSen().getBegin()) + "|" 
								+ Integer.toString(psf.getPseSen().getEnd());
				seSenSpans_ML.add(span);
			}	
		}
		
		//get side-effect sentence spans found by rules (SideEffectAnnotator)
		Set<String> seSenSpans_rule = new HashSet<String>();
		Iterator seIter = indexes.getAnnotationIndex(SideEffectAnnotation.type).iterator();
		
	    while(seIter.hasNext()) {
	    	SideEffectAnnotation se = (SideEffectAnnotation) seIter.next();	    	
	    	String span = Integer.toString(se.getSentence().getBegin()) + "|" 
	    					+ Integer.toString(se.getSentence().getEnd());
	    	seSenSpans_rule.add(span);
	    }
		
	    //annotate SE sentences (add them to SESentence)
	    Set<String> seSenSpans = new HashSet<String>(seSenSpans_ML);
	    seSenSpans.addAll(seSenSpans_rule); //union of ML and rule

	    for(String s : seSenSpans) {
	    	String[] stk = s.split("\\|");
	    	int begin = Integer.parseInt(stk[0]);
	    	int end = Integer.parseInt(stk[1]);
			SESentence ses = new SESentence(aJCas);
			ses.setBegin(begin);
			ses.setEnd(end);
			ses.addToIndexes();
	    }
	}
}
