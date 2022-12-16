/*
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
package org.apache.ctakes.temporal.ae;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.cleartk.ml.CleartkSequenceAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instances;
import org.cleartk.ml.SequenceDataWriter;
import org.cleartk.ml.chunking.BioChunking;
import org.cleartk.ml.jar.DefaultSequenceDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

@PipeBitInfo(
      name = "Meta Time Annotator",
      description = "...",
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.BASE_TOKEN, PipeBitInfo.TypeProduct.TIMEX }
)
public class MetaTimeAnnotator extends TemporalSequenceAnnotator_ImplBase {

  private BioChunking<BaseToken, TimeMention> timeChunking;

  @SuppressWarnings("unchecked")
  static Class<? extends JCasAnnotator_ImplBase>[] components = new Class[]{ BackwardsTimeAnnotator.class, TimeAnnotator.class, ConstituencyBasedTimeAnnotator.class, CRFTimeAnnotator.class };
  
  public static Class<? extends JCasAnnotator_ImplBase>[] getComponents() {
    return components;
  }

  public static AnalysisEngineDescription getDataWriterDescription(
      Class<? extends SequenceDataWriter<String>> dataWriterClass,
      File directory) throws ResourceInitializationException {
    AggregateBuilder builder = new AggregateBuilder();
    
    for(Class<?> component : components){
      builder.add(AnalysisEngineFactory.createEngineDescription(ViewCreatorAnnotator.class, ViewCreatorAnnotator.PARAM_VIEW_NAME, component.getSimpleName()));
    }
    
    builder.add(TimeAnnotator.createEnsembleDescription(
                      new File(directory, TimeAnnotator.class.getSimpleName()), 
                      TimeAnnotator.class.getSimpleName()));
    builder.add(BackwardsTimeAnnotator.createEnsembleDescription(
                      Paths.get(
                                directory.getAbsolutePath(),
                                BackwardsTimeAnnotator.class.getSimpleName()
                      ).toAbsolutePath().toString(),
                      BackwardsTimeAnnotator.class.getSimpleName()));
    builder.add(ConstituencyBasedTimeAnnotator.createEnsembleDescription(
                      Paths.get(
                              directory.getAbsolutePath(),
                              ConstituencyBasedTimeAnnotator.class.getSimpleName()
                      ).toAbsolutePath().toString(),
                      ConstituencyBasedTimeAnnotator.class.getSimpleName()));
    builder.add(CRFTimeAnnotator.createEnsembleDescription(
                      Paths.get(
                              directory.getAbsolutePath(),
                              CRFTimeAnnotator.class.getSimpleName()
                      ).toAbsolutePath().toString(),
                      CRFTimeAnnotator.class.getSimpleName()));
    
    builder.add(AnalysisEngineFactory.createEngineDescription(MetaTimeAnnotator.class,
          CleartkSequenceAnnotator.PARAM_IS_TRAINING,
          true,
          DefaultSequenceDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
          dataWriterClass,
          DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
          new File(directory, MetaTimeAnnotator.class.getSimpleName())));
    return builder.createAggregateDescription();
  }

  public static AnalysisEngineDescription getAnnotatorDescription(File directory) throws ResourceInitializationException{
    AggregateBuilder builder = new AggregateBuilder();
    
    for(Class<?> component : components){
      builder.add(AnalysisEngineFactory.createEngineDescription(ViewCreatorAnnotator.class, ViewCreatorAnnotator.PARAM_VIEW_NAME, component.getSimpleName()));
    }
    builder.add(TimeAnnotator.createEnsembleDescription(
                      new File(directory, TimeAnnotator.class.getSimpleName()),
                      TimeAnnotator.class.getSimpleName()));
    builder.add(BackwardsTimeAnnotator.createEnsembleDescription(
                      Paths.get(
                              directory.getAbsolutePath(),
                              BackwardsTimeAnnotator.class.getSimpleName()
                      ).toAbsolutePath().toString(),
                      BackwardsTimeAnnotator.class.getSimpleName()));
    builder.add(ConstituencyBasedTimeAnnotator.createEnsembleDescription(
                      Paths.get(
                              directory.getAbsolutePath(),
                              ConstituencyBasedTimeAnnotator.class.getSimpleName()
                      ).toAbsolutePath().toString(),
                      ConstituencyBasedTimeAnnotator.class.getSimpleName()));
    builder.add(CRFTimeAnnotator.createEnsembleDescription(
                      Paths.get(
                              directory.getAbsolutePath(),
                              CRFTimeAnnotator.class.getSimpleName()
                      ).toAbsolutePath().toString(),
                      CRFTimeAnnotator.class.getSimpleName()));
    builder.add(AnalysisEngineFactory.createEngineDescription(
        MetaTimeAnnotator.class,
        CleartkSequenceAnnotator.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(directory, MetaTimeAnnotator.class.getSimpleName() + File.separator + "model.jar")));
    return builder.createAggregateDescription();
  }
  
  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    // define chunking
    this.timeChunking = new BioChunking<>(BaseToken.class, TimeMention.class);
  }
  
  @Override
  public void process(JCas jCas, Segment segment)
      throws AnalysisEngineProcessException {
    // classify tokens within each sentence
    for (Sentence sentence : JCasUtil.selectCovered(jCas, Sentence.class, segment)) {
      List<List<Feature>> sequenceFeatures = new ArrayList<>();
      List<BaseToken> tokens = JCasUtil.selectCovered(jCas, BaseToken.class, sentence);
      // during training, the list of all outcomes for the tokens
      List<String> outcomes;
      
      if (this.isTraining()) {
        List<TimeMention> times = JCasUtil.selectCovered(jCas, TimeMention.class, sentence);
        outcomes = this.timeChunking.createOutcomes(jCas, tokens, times);
      }
      // during prediction, the list of outcomes predicted so far
      else {
        outcomes = new ArrayList<>();
      }
      
      List<List<String>> componentOutcomes = new ArrayList<>();
      for(Class<?> component : components){
        JCas componentView;
        try {
          componentView = jCas.getView(component.getSimpleName());
          CasCopier casCopy = new CasCopier(jCas.getCas(), componentView.getCas());
          org.apache.uima.cas.Feature sofaFeature = jCas.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFA);
          for(BaseToken token: tokens){
            BaseToken fs = (BaseToken) casCopy.copyFs(token);
            fs.setFeatureValue(sofaFeature, componentView.getSofa());
            fs.addToIndexes(componentView);
          }
          List<BaseToken> viewTokens = JCasUtil.selectCovered(componentView, BaseToken.class, sentence.getBegin(), sentence.getEnd());
          List<TimeMention> times = JCasUtil.selectCovered(componentView, TimeMention.class, sentence);
          componentOutcomes.add(this.timeChunking.createOutcomes(componentView, viewTokens, times));
        } catch (CASException e) {
          e.printStackTrace();
          throw new AnalysisEngineProcessException(e);
        }
      }
      
      for(int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++){
        List<Feature> features = new ArrayList<>();
        
        for(int componentNum = 0; componentNum < componentOutcomes.size(); componentNum++){
          String outcome = componentOutcomes.get(componentNum).get(tokenIndex);
          if(tokenIndex > 0){
//            features.add(new Feature("PreviousOutcome", outcomes.get(tokenIndex-1)));
            features.add(new Feature(String.format("Component%d_PreviousLabel", componentNum), componentOutcomes.get(componentNum).get(tokenIndex-1)));
          }
          features.add(new Feature(String.format("Component%d_Label", componentNum), outcome));
          if(tokenIndex < tokens.size() -1){
            features.add(new Feature(String.format("Component%d_NextLabel", componentNum), componentOutcomes.get(componentNum).get(tokenIndex+1)));
          }
//          if(!outcome.equals("O")){
//            features.add(new Feature(String.format("Component%d_IsTime", componentNum)));
//          }
        }
        
//        if (this.isTraining()) {
//          String outcome = outcomes.get(tokenIndex);
//          outcomes.add(outcome);
//          instances.add(new Instance<String>(outcome, features));
//          this.dataWriter.write(new Instance<String>(outcome, features));
//        }
        // if predicting, add prediction to outcomes
//        else {
//          outcomes.add(this.classifier.classify(features));
//        }
        sequenceFeatures.add(features);
      }
      
      if (this.isTraining()) {
        this.dataWriter.write(Instances.toInstances(outcomes, sequenceFeatures));
      }else{
        outcomes = this.classifier.classify(sequenceFeatures);
        this.timeChunking.createChunks(jCas, tokens, outcomes);
      }

    }
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
      if (!THYMEData.SEGMENTS_TO_SKIP.contains(segment.getId())) {
        this.process(jCas, segment);
      }
    }    
  }


}
