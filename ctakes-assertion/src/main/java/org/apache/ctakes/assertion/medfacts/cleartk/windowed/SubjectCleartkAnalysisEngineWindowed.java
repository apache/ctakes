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

import org.apache.ctakes.assertion.attributes.features.SubjectFeaturesExtractor;
import org.apache.ctakes.assertion.attributes.features.selection.Chi2FeatureSelection;
import org.apache.ctakes.assertion.attributes.features.selection.FeatureSelection;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Level;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.net.URI;

public class SubjectCleartkAnalysisEngineWindowed extends
                                                  WindowedAssertionCleartkAnalysisEngine {

   boolean USE_DEFAULT_EXTRACTORS = false;

   @Override
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      probabilityOfKeepingADefaultExample = 1.0;

      if ( this.isTraining() && this.goldViewName == null ) {
         throw new IllegalArgumentException( PARAM_GOLD_VIEW_NAME + " must be defined during training" );
      }

      initialize_subject_extractor();
      initializeFeatureSelection();

   }


   private void initialize_subject_extractor() {
      this.entityFeatureExtractors.add( new SubjectFeaturesExtractor() );
   }

   @Override
   public void setClassLabel( IdentifiedAnnotation entityOrEventMention,
                              Instance<String> instance ) throws AnalysisEngineProcessException {
      if ( this.isTraining() ) {
         String subj = entityOrEventMention.getSubject();

         // downsampling. initialize probabilityOfKeepingADefaultExample to 1.0 for no downsampling
         if ( "patient".equals( subj )
              && coin.nextDouble() >= this.probabilityOfKeepingADefaultExample ) {
            return;
         }
         instance.setOutcome( subj );
         logger.log( Level.DEBUG, String.format( "[%s] expected: ''; actual: ''; features: %s",
               this.getClass().getSimpleName(),
               instance.toString()
         ) );
      } else {
         String label = this.classifier.classify( instance.getFeatures() );
         entityOrEventMention.setSubject( label );
         logger.log( Level.DEBUG,
               "SUBJECT is being set on an IdentifiedAnnotation: " + label + " " + entityOrEventMention.getSubject() );
      }
   }

   public static FeatureSelection<String> createFeatureSelection( double threshold ) {
      return new Chi2FeatureSelection<>( WindowedAssertionCleartkAnalysisEngine.FEATURE_SELECTION_NAME, threshold, false );
   }

   public static URI createFeatureSelectionURI( File outputDirectoryName ) {
      return new File( outputDirectoryName, FEATURE_SELECTION_NAME + "_Chi2_extractor.dat" ).toURI();
   }

   @Override
   protected void initializeFeatureSelection() throws ResourceInitializationException {
      if ( featureSelectionThreshold == 0 ) {
         this.featureSelection = null;
      } else {
         this.featureSelection = createFeatureSelection( this.featureSelectionThreshold );
      }
   }

   public static AnalysisEngineDescription createAnnotatorDescription( String modelPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( SubjectCleartkAnalysisEngineWindowed.class,
            WindowedAssertionCleartkAnalysisEngine.PARAM_FEATURE_CONFIG,
            FEATURE_CONFIG.DEP_REGEX,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            modelPath );
   }

   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return createAnnotatorDescription( "/org/apache/ctakes/assertion/models/subject/model.jar" );
   }

}
