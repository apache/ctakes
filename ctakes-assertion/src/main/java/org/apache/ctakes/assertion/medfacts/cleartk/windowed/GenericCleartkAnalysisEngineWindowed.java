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
import org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor.WindowedContextWordWindowExtractor;
import org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor.WindowedGenericFeaturesExtractor;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
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
      name = "Generic Status ClearTK Annotator",
      description = "Annotates the Generic status for Identified Annotations.",
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class GenericCleartkAnalysisEngineWindowed extends
                                                  WindowedAssertionCleartkAnalysisEngine {

   boolean USE_DEFAULT_EXTRACTORS = false;

   @Override
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      probabilityOfKeepingADefaultExample = 0.5;

      initialize_generic_extractor();
      initializeFeatureSelection();
   }

   private void initialize_generic_extractor() {
      if ( this.entityFeatureExtractors == null ) {
         this.entityFeatureExtractors = new ArrayList<>();
      }
//		this.entityFeatureExtractors.add(new ContextWordWindowExtractor("org/apache/ctakes/assertion/models/generic.txt"));
      this.entityFeatureExtractors.add( new WindowedContextWordWindowExtractor( "org/apache/ctakes/assertion/models/generic.txt" ) );
      this.entityFeatureExtractors.add( new WindowedGenericFeaturesExtractor() );
   }

   @Override
   public void setClassLabel( IdentifiedAnnotation entityOrEventMention,
                              Instance<String> instance ) throws AnalysisEngineProcessException {
      if ( this.isTraining() ) {
         boolean generic = entityOrEventMention.getGeneric();

         // downsampling. initialize probabilityOfKeepingADefaultExample to 1.0 for no downsampling
         if ( !generic
              && coin.nextDouble() >= this.probabilityOfKeepingADefaultExample ) {
            return;
         }
         instance.setOutcome( "" + generic );
      } else {
         String label = this.classifier.classify( instance.getFeatures() );
         entityOrEventMention.setGeneric( Boolean.parseBoolean( label ) );
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
      return AnalysisEngineFactory.createEngineDescription( GenericCleartkAnalysisEngineWindowed.class,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            modelPath );
   }

   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return createAnnotatorDescription( "/org/apache/ctakes/assertion/models/generic/model.jar" );
   }

}
