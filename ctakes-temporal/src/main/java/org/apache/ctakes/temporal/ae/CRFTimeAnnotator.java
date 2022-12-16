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

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.feature.ParseSpanFeatureExtractor;
import org.apache.ctakes.temporal.ae.feature.TimeWordTypeExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instances;
import org.cleartk.ml.chunking.BioChunking;
import org.cleartk.ml.feature.extractor.*;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@PipeBitInfo(
      name = "Timex Annotator",
      description = "Annotates absolute time / date Temporal expressions into a View.",
      role = PipeBitInfo.Role.SPECIAL,
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
                       PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = { PipeBitInfo.TypeProduct.TIMEX }
)
public class CRFTimeAnnotator extends TemporalSequenceAnnotator_ImplBase {
  public static final String PARAM_TIMEX_VIEW = "TimexView";
  @ConfigurationParameter(
      name = PARAM_TIMEX_VIEW,
      mandatory = false,
      description = "View to write timexes to (used for ensemble methods)")
  protected String timexView = CAS.NAME_DEFAULT_SOFA;

//  public static AnalysisEngineDescription createDataWriterDescription(
//      Class<? extends DataWriter<String>> dataWriterClass,
//      File outputDirectory) throws ResourceInitializationException {
//    return AnalysisEngineFactory.createEngineDescription(
//        CRFTimeAnnotator.class,
//        CleartkAnnotator.PARAM_IS_TRAINING,
//        true,
//        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
//        dataWriterClass,
//        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
//        outputDirectory);
//  }

  public static AnalysisEngineDescription createAnnotatorDescription(String modelPath)
	      throws ResourceInitializationException {
	    return AnalysisEngineFactory.createEngineDescription(
	        CRFTimeAnnotator.class,
	        CleartkAnnotator.PARAM_IS_TRAINING,
	        false,
	        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
	        modelPath);
	  }
  
  public static AnalysisEngineDescription createEnsembleDescription(String modelPath,
	      String viewName) throws ResourceInitializationException {
	    return AnalysisEngineFactory.createEngineDescription(
	        CRFTimeAnnotator.class,
	        CleartkAnnotator.PARAM_IS_TRAINING,
	        false,
	        CRFTimeAnnotator.PARAM_TIMEX_VIEW,
	        viewName,
	        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
	        modelPath);
	  }
  
  /**
   * @deprecated use String path instead of File.
   * ClearTK will automatically Resolve the String to an InputStream.
   * This will allow resources to be read within from a jar as well as File.  
   */
  public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        CRFTimeAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(modelDirectory, "model.jar"));
  }

  /**
   * @deprecated use String path instead of File.
   * ClearTK will automatically Resolve the String to an InputStream.
   * This will allow resources to be read within from a jar as well as File.  
   */
  public static AnalysisEngineDescription createEnsembleDescription(File modelDirectory,
      String viewName) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        CRFTimeAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        CRFTimeAnnotator.PARAM_TIMEX_VIEW,
        viewName,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(modelDirectory, "model.jar"));
  }

  protected List<FeatureExtractor1> tokenFeatureExtractors;

  protected List<CleartkExtractor> contextFeatureExtractors;
  
//  protected List<FeatureExtractor1> parseFeatureExtractors;
  protected ParseSpanFeatureExtractor parseExtractor;

  private BioChunking<BaseToken, TimeMention> timeChunking;

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);

    // define chunking
    this.timeChunking = new BioChunking<BaseToken, TimeMention>(BaseToken.class, TimeMention.class);
    CombinedExtractor1 allExtractors = new CombinedExtractor1(
        new CoveredTextExtractor(),
//        new CharacterCategoryPatternExtractor(PatternType.REPEATS_MERGED),
//        new CharacterCategoryPatternExtractor(PatternType.ONE_PER_CHAR),
        new TypePathExtractor(BaseToken.class, "partOfSpeech"),
        new TimeWordTypeExtractor());

//    CombinedExtractor1 parseExtractors = new CombinedExtractor(
//        new ParseSpanFeatureExtractor()
//        );
    this.tokenFeatureExtractors = new ArrayList<>();
    this.tokenFeatureExtractors.add(allExtractors);

    this.contextFeatureExtractors = new ArrayList<CleartkExtractor>();
    this.contextFeatureExtractors.add(new CleartkExtractor(
        BaseToken.class,
        allExtractors,
        new Preceding(2),
        new Following(2)));
//    this.parseFeatureExtractors = new ArrayList<ParseSpanFeatureExtractor>();
//    this.parseFeatureExtractors.add(new ParseSpanFeatureExtractor());
    parseExtractor = new ParseSpanFeatureExtractor();
  }
  
  @Override
  public void process(JCas jCas, Segment segment)
      throws AnalysisEngineProcessException {
    // classify tokens within each sentence
    for (Sentence sentence : JCasUtil.selectCovered(jCas, Sentence.class, segment)) {
      List<BaseToken> tokens = JCasUtil.selectCovered(jCas, BaseToken.class, sentence);

      // during training, the list of all outcomes for the tokens
      List<String> outcomes;
      if (this.isTraining()) {
        List<TimeMention> times = JCasUtil.selectCovered(jCas, TimeMention.class, sentence);
        outcomes = this.timeChunking.createOutcomes(jCas, tokens, times); 
      }
      // during prediction, the list of outcomes predicted so far
      else {
        outcomes = new ArrayList<String>();
      }

      // extract features for all tokens
//      int tokenIndex = -1;
      List<List<Feature>> allFeatures = new ArrayList<List<Feature>>();
      for (BaseToken token : tokens) {
//        ++tokenIndex;

        List<Feature> features = new ArrayList<Feature>();
        // features from token attributes
        for (FeatureExtractor1 extractor : this.tokenFeatureExtractors) {
          features.addAll(extractor.extract(jCas, token));
        }
        // features from surrounding tokens
        for (CleartkExtractor extractor : this.contextFeatureExtractors) {
          features.addAll(extractor.extractWithin(jCas, token, sentence));
        }
        // features from dominating parse tree
        // TODO-  think abouot how to incorporate this - fix or delete
//        BaseToken startToken = token;
//        for(int i = tokenIndex-1; i >= 0; --i){
//          String outcome = outcomes.get(i);
//          if(outcome.equals("O")){
//            break;
//          }
//          startToken = tokens.get(i);
//        }
//        TreebankNode preTerm = AnnotationTreeUtils.annotationNode(jCas, token);
        features.addAll(parseExtractor.extract(jCas, token.getBegin(), token.getEnd()));
        //if(preTerm != null && preTerm.getParent() != null){
        //  features.addAll(parseExtractor.extract(jCas, preTerm.getParent().getBegin(), preTerm.getParent().getEnd()));
        //}
        
        // if training, write to data file
//        if (this.isTraining()) {
//          String outcome = outcomes.get(tokenIndex);
          allFeatures.add(features);
//        }

        // if predicting, add prediction to outcomes
//        else {
//        }
      }

      // during prediction, convert chunk labels to times and add them to the CAS
      if (this.isTraining()) {
        this.dataWriter.write(Instances.toInstances(outcomes, allFeatures));        
      }else{
//        outcomes.add(this.classifier.classify(features));
        outcomes = this.classifier.classify(allFeatures);
        JCas timexCas;
        try {
          timexCas = jCas.getView(timexView);
        } catch (CASException e) {
          throw new AnalysisEngineProcessException(e);
        }
        this.timeChunking.createChunks(timexCas, tokens, outcomes);
      }
    }
  }


}
