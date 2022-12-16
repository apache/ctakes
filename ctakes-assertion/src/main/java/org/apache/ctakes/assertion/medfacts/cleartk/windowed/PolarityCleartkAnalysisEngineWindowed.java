/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.assertion.medfacts.cleartk.windowed;

import org.apache.ctakes.assertion.attributes.features.selection.Chi2FeatureSelection;
import org.apache.ctakes.assertion.attributes.features.selection.FeatureSelection;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.AboveLeftFragmentExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.AssertionAboveLeftTreeExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor.WindowedAssertionDependencyTreeExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor.WindowedContextWordWindowExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor.WindowedNegationDependencyFeatureExtractor;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;


@PipeBitInfo(
      name = "Negation Annotator (ClearTK)",
      description = "Annotates negation property.",
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class PolarityCleartkAnalysisEngineWindowed extends WindowedAssertionCleartkAnalysisEngine {

   public static final String NEGATED = "NEGATED";
   public static final String NOT_NEGATED = "NOT_NEGATED";


   @Override
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      probabilityOfKeepingADefaultExample = 1.0; //0.1;

      if ( this.entityFeatureExtractors == null ) {
         this.entityFeatureExtractors = new ArrayList<>();
      }

      if ( featConfig == FEATURE_CONFIG.NO_TOK ) {
         this.tokenCleartkExtractors = new ArrayList<>();
      }

      // polarity keyword list:
      if ( featConfig != FEATURE_CONFIG.NO_SEM ) {
         this.entityFeatureExtractors.add( new WindowedContextWordWindowExtractor( "org/apache/ctakes/assertion/models/polarity.txt" ) );
      }

      // stk frags feature:
      if ( featConfig == FEATURE_CONFIG.STK_FRAGS || featConfig == FEATURE_CONFIG.ALL_SYN ||
           featConfig == FEATURE_CONFIG.NO_TOK ) {
//		  this.entityFeatureExtractors.add(new AboveLeftFragmentExtractor("AL_Polarity","org/apache/ctakes/assertion/models/jbi_paper_polarity_sems_frags.txt"));
         this.entityFeatureExtractors.add( new AboveLeftFragmentExtractor( "AL_Polarity", "org/apache/ctakes/assertion/models/sharpPolarityFrags.txt" ) );
//		  this.entityFeatureExtractors.add(new ConceptModifierPETFragmentExtractor("NegRel", "org/apache/ctakes/assertion/models/polarityRelnFragsStrat.txt"));
      }

      if ( featConfig == FEATURE_CONFIG.PTK_FRAGS || featConfig == FEATURE_CONFIG.DEP_REGEX_FRAGS ||
           featConfig == FEATURE_CONFIG.ALL_SYN ) {
//	     ptk frags feature:
//		  this.entityFeatureExtractors.add(new DependencyWordsFragmentExtractor("DW_Polarity", "org/apache/ctakes/assertion/models/jbi_paper_polarity_dw_frags.txt"));
      }

      if ( featConfig == FEATURE_CONFIG.DEP_REGEX || featConfig == FEATURE_CONFIG.DEP_REGEX_FRAGS ||
           featConfig == FEATURE_CONFIG.ALL_SYN || featConfig == FEATURE_CONFIG.NO_TOK ) {
         // dep regex feature:
         this.entityFeatureExtractors.add( new WindowedNegationDependencyFeatureExtractor() );
      }

      if ( featConfig == FEATURE_CONFIG.STK ) {
         // stk constituency feature:
         this.entityTreeExtractors.add( new AssertionAboveLeftTreeExtractor() );
      }

      if ( featConfig == FEATURE_CONFIG.PTK ) {
         // ptk dependency feature:
         this.entityTreeExtractors.add( new WindowedAssertionDependencyTreeExtractor() );
      }

      // srl & non-effective stk frags feature:
//  this.entityFeatureExtractors.add(new SRLFeatureExtractor());
//  this.entityFeatureExtractors.add(new AboveRightFragmentExtractor("AR_Polarity","org/apache/ctakes/assertion/models/sharpArPolarityFrags.txt"));
      initializeFeatureSelection();
   }

   @Override
   public void setClassLabel( IdentifiedAnnotation entityOrEventMention, Instance<String> instance )
         throws AnalysisEngineProcessException {
      if ( this.isTraining() ) {
         String polarity = (entityOrEventMention.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT) ? NEGATED
                                                                                                      : NOT_NEGATED; // "negated" : "present";
         this.lastLabel = polarity;
         // downsampling. initialize probabilityOfKeepingADefaultExample to 1.0 for no downsampling
         if ( NEGATED.equals( polarity ) ) {
            logger.debug( "TRAINING: " + polarity );
         }
         if ( NOT_NEGATED.equals( polarity )
              && coin.nextDouble() >= this.probabilityOfKeepingADefaultExample ) {
            return;
         }
         instance.setOutcome( polarity );
//	        this.dataWriter.write(instance);
      } else {
         String label = this.classifier.classify( instance.getFeatures() );
         this.lastLabel = label;
         int polarity = CONST.NE_POLARITY_NEGATION_ABSENT;
         if ( NOT_NEGATED.equals( label ) ) {
            polarity = CONST.NE_POLARITY_NEGATION_ABSENT;
         } else if ( NEGATED.equals( label ) ) {
            polarity = CONST.NE_POLARITY_NEGATION_PRESENT;
            logger.debug( String.format( "DECODING/EVAL: %s//%s [%d-%d] (%s)", label, polarity, entityOrEventMention.getBegin(), entityOrEventMention
                  .getEnd(), entityOrEventMention.getClass().getName() ) );
         }
         entityOrEventMention.setPolarity( polarity );
      }
   }

   public static FeatureSelection<String> createFeatureSelection( double threshold ) {
      return new Chi2FeatureSelection<String>( WindowedAssertionCleartkAnalysisEngine.FEATURE_SELECTION_NAME, threshold, false );
      //		  return new MutualInformationFeatureSelection<String>(AssertionCleartkAnalysisEngine.FEATURE_SELECTION_NAME);
   }

   public static URI createFeatureSelectionURI( File outputDirectoryName ) {
      return new File( outputDirectoryName, FEATURE_SELECTION_NAME + "_Chi2_extractor.dat" ).toURI();
   }

   @Override
   protected void initializeFeatureSelection() throws ResourceInitializationException {
      if ( featureSelectionThreshold == 0 ) {
         this.featureSelection = null;
      } else {
         this.featureSelection = this.createFeatureSelection( this.featureSelectionThreshold );

//	    	if ( (new File(this.featureSelectionURI)).exists() ) {
//	    		try {
//	    			this.featureSelection.load(this.featureSelectionURI);
//	    		} catch (IOException e) {
//	    			throw new ResourceInitializationException(e);
//	    		}
//	    	}
      }
   }

   public static AnalysisEngineDescription createAnnotatorDescription( String modelPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( PolarityCleartkAnalysisEngineWindowed.class,
            WindowedAssertionCleartkAnalysisEngine.PARAM_FEATURE_CONFIG,
            FEATURE_CONFIG.ALL_SYN,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            modelPath );
   }

   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return createAnnotatorDescription( "/org/apache/ctakes/assertion/models/polarity/sharpi2b2mipacqnegex/model.jar" );
   }
}
