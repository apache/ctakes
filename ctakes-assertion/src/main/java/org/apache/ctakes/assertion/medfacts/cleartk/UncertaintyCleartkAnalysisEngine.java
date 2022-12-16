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
package org.apache.ctakes.assertion.medfacts.cleartk;

import static org.apache.ctakes.assertion.medfacts.cleartk.AssertionCleartkAnalysisEngine.FEATURE_CONFIG.ALL_SYN;
import static org.apache.ctakes.assertion.medfacts.cleartk.AssertionCleartkAnalysisEngine.FEATURE_CONFIG.PTK;
import static org.apache.ctakes.assertion.medfacts.cleartk.AssertionCleartkAnalysisEngine.FEATURE_CONFIG.PTK_FRAGS;
import static org.apache.ctakes.assertion.medfacts.cleartk.AssertionCleartkAnalysisEngine.FEATURE_CONFIG.STK;
import static org.apache.ctakes.assertion.medfacts.cleartk.AssertionCleartkAnalysisEngine.FEATURE_CONFIG.STK_FRAGS;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import org.apache.ctakes.assertion.attributes.features.selection.Chi2FeatureSelection;
import org.apache.ctakes.assertion.attributes.features.selection.FeatureSelection;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.AboveLeftFragmentExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.AssertionAboveLeftTreeExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.AssertionDependencyTreeExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.ContextWordWindowExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.DependencyWordsFragmentExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.UncertaintyFeatureExtractor;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

public class UncertaintyCleartkAnalysisEngine extends AssertionCleartkAnalysisEngine {

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		probabilityOfKeepingADefaultExample = 0.25;
		if(this.entityFeatureExtractors == null){
			this.entityFeatureExtractors = new ArrayList<>();
		}
		this.entityFeatureExtractors.add(new ContextWordWindowExtractor("org/apache/ctakes/assertion/models/uncertainty.txt"));
		this.entityFeatureExtractors.add(new UncertaintyFeatureExtractor());
		// TODO: Uncomment below when good features are found:
//		try {
//      this.entityFeatureExtractors.add(new DependencyPathRegexpFeatureExtractor());
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//      throw new ResourceInitializationException(e);
//    }
		
		if(featConfig == STK_FRAGS){
		  this.entityFeatureExtractors.add(new AboveLeftFragmentExtractor("AL_Unc", "org/apache/ctakes/assertion/models/jbi_paper_unc_seed_frags.txt"));
		}
		
		if(featConfig == PTK_FRAGS){
		  this.entityFeatureExtractors.add(new DependencyWordsFragmentExtractor("DW_Uncertainty", "org/apache/ctakes/assertion/models/jbi_paper_uncertainty_dw_frags.txt"));
		}
		if(featConfig == STK){
		  this.entityTreeExtractors.add(new AssertionAboveLeftTreeExtractor());
		}
		
		if(featConfig == PTK){
		  this.entityTreeExtractors.add(new AssertionDependencyTreeExtractor());
		}

		initializeFeatureSelection();
		
	}

	@Override
	public void setClassLabel(IdentifiedAnnotation entityOrEventMention, Instance<String> instance) throws AnalysisEngineProcessException {
		if (this.isTraining())
	      {
	        String uncertainty = (entityOrEventMention.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT) ? "uncertain" : "certain";

	        // downsampling. initialize probabilityOfKeepingADefaultExample to 1.0 for no downsampling
	        if ("certain".equals(uncertainty) 
	        		&& coin.nextDouble() >= this.probabilityOfKeepingADefaultExample) {
	        	return;
	        }
	        instance.setOutcome(uncertainty);
	      } else
	      {
	        String label = this.classifier.classify(instance.getFeatures());
	        int uncertainty = 0;
	        if (label!= null && label.equals("uncertain"))
	        {
	          uncertainty = CONST.NE_UNCERTAINTY_PRESENT;
	        } else if (label != null && label.equals("certain"))
	        {
	          uncertainty = CONST.NE_UNCERTAINTY_ABSENT;
	        }
	        entityOrEventMention.setUncertainty(uncertainty);
	      }
	}
	
	public static FeatureSelection<String> createFeatureSelection(double threshold) {
		return new Chi2FeatureSelection<>(AssertionCleartkAnalysisEngine.FEATURE_SELECTION_NAME, threshold, false);
	}

	public static URI createFeatureSelectionURI(File outputDirectoryName) {
		return new File(outputDirectoryName, FEATURE_SELECTION_NAME + "_Chi2_extractor.dat").toURI();
	}
	  
	@Override
	protected void initializeFeatureSelection() throws ResourceInitializationException {
	    if (featureSelectionThreshold == 0) {
	    	this.featureSelection = null;
	    } else {
	    	this.featureSelection = createFeatureSelection(this.featureSelectionThreshold);
	    }		
	}
	  
  public static AnalysisEngineDescription createAnnotatorDescription(String modelPath) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(UncertaintyCleartkAnalysisEngine.class,
        AssertionCleartkAnalysisEngine.PARAM_FEATURE_CONFIG,
        AssertionCleartkAnalysisEngine.FEATURE_CONFIG.ALL_SYN,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        modelPath);
  }
  
  public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
    return createAnnotatorDescription("/org/apache/ctakes/assertion/models/uncertainty/model.jar");
  }
}
