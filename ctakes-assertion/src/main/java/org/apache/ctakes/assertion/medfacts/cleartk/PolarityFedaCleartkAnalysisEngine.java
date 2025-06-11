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

import org.apache.ctakes.assertion.attributes.features.selection.Chi2FeatureSelection;
import org.apache.ctakes.assertion.attributes.features.selection.FeatureSelection;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.AboveLeftFragmentExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.ContextWordWindowExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.ExtractorListFeatureFunctionConverter;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.NegationDependencyFeatureExtractor;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;



@PipeBitInfo(
		name = "Negation Annotator (Feda)",
		description = "Annotates negation property.",
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class PolarityFedaCleartkAnalysisEngine extends PolarityCleartkAnalysisEngine {
	static private final Logger LOGGER = LoggerFactory.getLogger( "PolarityFedaCleartkAnalysisEngine" );

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		probabilityOfKeepingADefaultExample = 1.0; //0.1;

		if(this.entityFeatureExtractors == null){
			this.entityFeatureExtractors = new ArrayList<>();
		}
		this.entityFeatureExtractors.add(new NegationDependencyFeatureExtractor());
		this.entityFeatureExtractors.add(new ContextWordWindowExtractor("org/apache/ctakes/assertion/models/polarity.txt"));
		this.entityFeatureExtractors.add(new AboveLeftFragmentExtractor("AL_Polarity","org/apache/ctakes/assertion/models/sharpPolarityFrags.txt"));
		//		this.entityFeatureExtractors.add(new AboveRightFragmentExtractor("AR_Polarity","org/apache/ctakes/assertion/models/sharpArPolarityFrags.txt"));

		initializeDomainAdaptation();
	
		initializeFeatureSelection();
		
	}

	@Override
	public void setClassLabel(IdentifiedAnnotation entityOrEventMention, Instance<String> instance) throws AnalysisEngineProcessException {
	      if (this.isTraining())
	      {
	        String polarity = (entityOrEventMention.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT) ? NEGATED : NOT_NEGATED; // "negated" : "present";
	        this.lastLabel = polarity;
	        // downsampling. initialize probabilityOfKeepingADefaultExample to 1.0 for no downsampling
	        if (NEGATED.equals(polarity))
	        {
	            LOGGER.debug("TRAINING: " + polarity);
	        }
	        if (NOT_NEGATED.equals(polarity) 
	        		&& coin.nextDouble() >= this.probabilityOfKeepingADefaultExample) {
	        	return;
	        }
	        instance.setOutcome(polarity);
//	        this.dataWriter.write(instance);
	      } else
	      {
	        String label = this.classifier.classify(instance.getFeatures());
	        this.lastLabel = label;
	        int polarity = CONST.NE_POLARITY_NEGATION_ABSENT;
	        if (NOT_NEGATED.equals(label))
	        {
	          polarity = CONST.NE_POLARITY_NEGATION_ABSENT;
	        } else if (NEGATED.equals(label))
	        {
	          polarity = CONST.NE_POLARITY_NEGATION_PRESENT;
	          LOGGER.debug(String.format("DECODING/EVAL: %s//%s [%d-%d] (%s)", label, polarity, entityOrEventMention.getBegin(), entityOrEventMention.getEnd(), entityOrEventMention.getClass().getName()));
	        }
	        entityOrEventMention.setPolarity(polarity);
	      }
	}
	public static FeatureSelection<String> createFeatureSelection(double threshold) {
		return new Chi2FeatureSelection<>(AssertionCleartkAnalysisEngine.FEATURE_SELECTION_NAME, threshold, false);
		//		  return new MutualInformationFeatureSelection<String>(AssertionCleartkAnalysisEngine.FEATURE_SELECTION_NAME);
	}

	public static URI createFeatureSelectionURI(File outputDirectoryName) {
		return new File(outputDirectoryName, FEATURE_SELECTION_NAME + "_Chi2_extractor.dat").toURI();
	}

	private void initializeDomainAdaptation() {
		// Do domain adaptation
		featureFunctionExtractors = new ArrayList<>();
		//			FedaFeatureFunction ff = new FedaFeatureFunction(new ArrayList<String>(trainFileToDomain.values()));
		featureFunctionExtractors.addAll(ExtractorListFeatureFunctionConverter.convert(contextFeatureExtractors, ffDomainAdaptor));
		featureFunctionExtractors.addAll(ExtractorListFeatureFunctionConverter.convert(tokenContextFeatureExtractors, ffDomainAdaptor));
		featureFunctionExtractors.addAll(ExtractorListFeatureFunctionConverter.convert(tokenCleartkExtractors, ffDomainAdaptor));
		featureFunctionExtractors.addAll(ExtractorListFeatureFunctionConverter.convert(entityFeatureExtractors, ffDomainAdaptor));
		featureFunctionExtractors.add(new FeatureFunctionExtractor<>(cuePhraseInWindowExtractor, ffDomainAdaptor));
	}
	@Override
	protected void initializeFeatureSelection() throws ResourceInitializationException {
	    if (featureSelectionThreshold == 0) {
	    	this.featureSelection = null;
	    } else {
	    	this.featureSelection = createFeatureSelection(this.featureSelectionThreshold);

//	    	if ( (new File(this.featureSelectionURI)).exists() ) {
//	    		try {
//	    			this.featureSelection.load(this.featureSelectionURI);
//	    		} catch (IOException e) {
//	    			throw new ResourceInitializationException(e);
//	    		}
//	    	}
	    }		
	}
	  
}
