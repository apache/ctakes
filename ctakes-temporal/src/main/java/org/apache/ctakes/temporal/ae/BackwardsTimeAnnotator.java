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

import com.google.common.collect.Lists;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.temporal.ae.feature.ParseSpanFeatureExtractor;
import org.apache.ctakes.temporal.ae.feature.TimeWordTypeExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.chunking.BioChunking;
import org.cleartk.ml.feature.extractor.*;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction.PatternType;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@PipeBitInfo(
      name = "Backward Timex Annotator",
      description = "Annotates absolute time / date Temporal expressions.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
                       PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = { PipeBitInfo.TypeProduct.TIMEX }
)
public class BackwardsTimeAnnotator extends TemporalEntityAnnotator_ImplBase {

   static private final Logger LOGGER = LoggerFactory.getLogger( "BackwardsTimeAnnotator" );

  public static final String PARAM_TIMEX_VIEW = "TimexView";
  @ConfigurationParameter(
      name = PARAM_TIMEX_VIEW,
      mandatory = false,
      description = "View to write timexes to (used for ensemble methods)")
  protected String timexView = CAS.NAME_DEFAULT_SOFA;

  public static AnalysisEngineDescription createDataWriterDescription(
      Class<? extends DataWriter<String>> dataWriterClass, File outputDirectory)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        BackwardsTimeAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        true,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        dataWriterClass,
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outputDirectory);
  }

  public static AnalysisEngineDescription createAnnotatorDescription(
	      String modelPath) throws ResourceInitializationException {
	    return AnalysisEngineFactory.createEngineDescription(
	        BackwardsTimeAnnotator.class,
	        CleartkAnnotator.PARAM_IS_TRAINING,
	        false,
	        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
	        modelPath);
	  }
  
  public static AnalysisEngineDescription createEnsembleDescription(String modelPath,
	      String viewName) throws ResourceInitializationException {
	    return AnalysisEngineFactory.createEngineDescription(
	        BackwardsTimeAnnotator.class,
	        CleartkAnnotator.PARAM_IS_TRAINING,
	        false,
	        BackwardsTimeAnnotator.PARAM_TIMEX_VIEW,
	        viewName,
	        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
	        modelPath);
	  }
  
  /**
   * @deprecated use String path instead of File.
   * ClearTK will automatically Resolve the String to an InputStream.
   * This will allow resources to be read within from a jar as well as File.  
   */
  public static AnalysisEngineDescription createAnnotatorDescription(
      File modelDirectory) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        BackwardsTimeAnnotator.class,
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
        BackwardsTimeAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        BackwardsTimeAnnotator.PARAM_TIMEX_VIEW,
        viewName,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(modelDirectory, "model.jar"));
  }

  protected List<FeatureExtractor1> tokenFeatureExtractors;

  protected List<CleartkExtractor> contextFeatureExtractors;
  
//  protected List<FeatureExtractor1> parseFeatureExtractors;
  protected ParseSpanFeatureExtractor parseExtractor;
  
  private BioChunking<BaseToken, TimeMention> timeChunking;

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
     LOGGER.info( "Initializing ..." );
     try ( DotLogger dotter = new DotLogger() ) {
        super.initialize( context );

        // define chunking
        this.timeChunking = new BioChunking<BaseToken, TimeMention>(BaseToken.class, TimeMention.class, "timeClass");

        CombinedExtractor1<BaseToken> allExtractors = new CombinedExtractor1<>(
              new CoveredTextExtractor<BaseToken>(),
              CharacterCategoryPatternFunction.<BaseToken>createExtractor(PatternType.REPEATS_MERGED),
              CharacterCategoryPatternFunction.<BaseToken>createExtractor(PatternType.ONE_PER_CHAR),
              new TypePathExtractor<>(BaseToken.class, "partOfSpeech"),
              new TimeWordTypeExtractor<BaseToken>());

//    CombinedExtractor1 parseExtractors = new CombinedExtractor(
//        new ParseSpanFeatureExtractor()
//        );
        this.tokenFeatureExtractors = new ArrayList<FeatureExtractor1>();
        this.tokenFeatureExtractors.add(allExtractors);

        this.contextFeatureExtractors = new ArrayList<CleartkExtractor>();
        this.contextFeatureExtractors.add(new CleartkExtractor(
              BaseToken.class,
              allExtractors,
              new Preceding(3),
              new Following(3)));
//    this.parseFeatureExtractors = new ArrayList<ParseSpanFeatureExtractor>();
//    this.parseFeatureExtractors.add(new ParseSpanFeatureExtractor());
        parseExtractor = new ParseSpanFeatureExtractor();
     } catch ( IOException ioE ) {
        throw new ResourceInitializationException( ioE );
     }
//     LOGGER.info( "Finished." );
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    LOGGER.info( "Finding Times ..." );
    super.process( jCas );
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process(JCas jCas, Segment segment) throws AnalysisEngineProcessException {
    // classify tokens within each sentence
    for (Sentence sentence : JCasUtil.selectCovered(jCas, Sentence.class, segment)) {
      List<BaseToken> tokens = JCasUtil.selectCovered(jCas, BaseToken.class, sentence);
      
      // during training, the list of all outcomes for the tokens
      List<String> outcomes;
      if (this.isTraining()) {
        List<TimeMention> times = JCasUtil.selectCovered(jCas, TimeMention.class, sentence);
        outcomes = this.timeChunking.createOutcomes(jCas, tokens, times);
        outcomes = Lists.reverse(outcomes);
      }
      // during prediction, the list of outcomes predicted so far
      else {
        outcomes = new ArrayList<String>();
      }

      tokens = Lists.reverse(tokens);

      // extract features for all tokens
      int tokenIndex = -1;
      for (BaseToken token : tokens) {
        ++tokenIndex;

        List<Feature> features = new ArrayList<Feature>();
        // features from token attributes
        for (FeatureExtractor1 extractor : this.tokenFeatureExtractors) {
          features.addAll(extractor.extract(jCas, token));
        }
        // features from surrounding tokens
        for (CleartkExtractor extractor : this.contextFeatureExtractors) {
          features.addAll(extractor.extractWithin(jCas, token, sentence));
        }
        // features from previous classifications
        int nPreviousClassifications = 2;
        for (int i = nPreviousClassifications; i > 0; --i) {
          int index = tokenIndex - i;
          String previousOutcome = index < 0 ? "O" : outcomes.get(index);
          features.add(new Feature("PreviousOutcome_" + i, previousOutcome));
        }
        // features from dominating parse tree
//        for(FeatureExtractor1 extractor : this.parseFeatureExtractors){
        BaseToken startToken = token;
        for(int i = tokenIndex-1; i >= 0; --i){
          String outcome = outcomes.get(i);
          if(outcome.equals("O")){
            break;
          }
          startToken = tokens.get(i);
        }
        features.addAll(parseExtractor.extract(jCas, startToken.getBegin(), token.getEnd()));
//        }
        // if training, write to data file
        if (this.isTraining()) {
          String outcome = outcomes.get(tokenIndex);
          this.dataWriter.write(new Instance<String>(outcome, features));
        }

        // if predicting, add prediction to outcomes
        else {
          outcomes.add(this.classifier.classify(features));
        }
      }

      // during prediction, convert chunk labels to times and add them to the CAS
      if (!this.isTraining()) {
        tokens = Lists.reverse(tokens);
        outcomes = Lists.reverse(outcomes);
        JCas timexCas;
        try{
          timexCas = jCas.getView(timexView);
        }catch(CASException e){
          throw new AnalysisEngineProcessException(e);
        }
        this.timeChunking.createChunks(timexCas, tokens, outcomes);
      }
    }
  }

}
