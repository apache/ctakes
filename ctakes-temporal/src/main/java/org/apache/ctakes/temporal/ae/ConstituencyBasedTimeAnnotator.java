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

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.feature.TimeWordTypeExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
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
import org.cleartk.ml.feature.extractor.*;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bag;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction.PatternType;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.timeml.util.TimeWordsExtractor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@PipeBitInfo(
      name = "Constituency Timex Filter",
      description = "Creates Timex annotations.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.BASE_TOKEN,
                       PipeBitInfo.TypeProduct.TIMEX }
)
public class ConstituencyBasedTimeAnnotator extends TemporalEntityAnnotator_ImplBase {

  private static final String NON_MENTION = "NON_TIME_MENTION";
  private static final String MENTION = "TIME_MENTION";
  private static Logger logger = Logger.getLogger(ConstituencyBasedTimeAnnotator.class);
  private static final int	SPAN_LIMIT = 12;

  public static final String PARAM_TIMEX_VIEW = "TimexView";
  @ConfigurationParameter(
      name = PARAM_TIMEX_VIEW,
      mandatory = false,
      description = "View to write timexes to (used for ensemble methods)")
  protected String timexView = CAS.NAME_DEFAULT_SOFA;

  public static AnalysisEngineDescription createDataWriterDescription(
      Class<? extends DataWriter<String>> dataWriterClass,
          File outputDirectory) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        ConstituencyBasedTimeAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        true,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        dataWriterClass,
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outputDirectory);
  }

  public static AnalysisEngineDescription createAnnotatorDescription(String modelPath)
	      throws ResourceInitializationException {
	    return AnalysisEngineFactory.createEngineDescription(
	        ConstituencyBasedTimeAnnotator.class,
	        CleartkAnnotator.PARAM_IS_TRAINING,
	        false,
	        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
	        modelPath);
	  }
  
  public static AnalysisEngineDescription createEnsembleDescription(String modelPath,
	      String viewName) throws ResourceInitializationException {
	    return AnalysisEngineFactory.createEngineDescription(
	        ConstituencyBasedTimeAnnotator.class,
	        CleartkAnnotator.PARAM_IS_TRAINING,
	        false,
	        ConstituencyBasedTimeAnnotator.PARAM_TIMEX_VIEW,
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
        ConstituencyBasedTimeAnnotator.class,
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
        ConstituencyBasedTimeAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        ConstituencyBasedTimeAnnotator.PARAM_TIMEX_VIEW,
        viewName,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(modelDirectory, "model.jar"));
  }

  protected List<FeatureExtractor1> featureExtractors;
  
  protected FeatureExtractor1 wordTypeExtractor;
  
  private static final String LOOKUP_PATH = "/org/apache/ctakes/temporal/time_word_types.txt";
  
  private Map<String, String> wordTypes;
  
  @Override
  public void initialize(UimaContext context)
      throws ResourceInitializationException {
    super.initialize(context);

    CombinedExtractor1<BaseToken> charExtractors = new CombinedExtractor1<>(CharacterCategoryPatternFunction.<BaseToken>createExtractor(PatternType.REPEATS_MERGED),
            CharacterCategoryPatternFunction.<BaseToken>createExtractor(PatternType.ONE_PER_CHAR));
    
    this.wordTypes = Maps.newHashMap();
    URL url = TimeWordsExtractor.class.getResource(LOOKUP_PATH);
    try {
      for (String line : Resources.readLines(url, Charsets.US_ASCII)) {
        String[] typeAndWord = line.split("\\s+");
        if (typeAndWord.length != 2) {
          throw new IllegalArgumentException("Expected '<type> <word>', found: " + line);
        }
        this.wordTypes.put(typeAndWord[1], typeAndWord[0]);
      }
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
    
    CombinedExtractor1<BaseToken> allExtractors = new CombinedExtractor1<>(
        new CoveredTextExtractor<BaseToken>(),
//        new TimeWordTypeExtractor(),
        charExtractors,
        new TypePathExtractor<>(BaseToken.class, "partOfSpeech"));
    
    featureExtractors = new ArrayList<FeatureExtractor1>();
//    featureExtractors.add(new CleartkExtractor(BaseToken.class, new CoveredTextExtractor(), new Bag(new Covered())));
    featureExtractors.add(new CleartkExtractor(BaseToken.class, allExtractors, new Bag(new Covered())));
//    featureExtractors.add(charExtractors);
    wordTypeExtractor = new CleartkExtractor(BaseToken.class, new TimeWordTypeExtractor<BaseToken>(), new Bag(new Covered()));
//    featureExtractors.add(new CleartkExtractor(BaseToken.class, new CoveredTextExtractor(), new Bag(new Preceding(1))));
 //   featureExtractors.add(new CleartkExtractor(BaseToken.class, new CoveredTextExtractor(), new Bag(new Following(1))));
    // bag of constituent descendent labels
//    featureExtractors.add(new CleartkExtractor(TreebankNode.class, new TypePathExtractor(TreebankNode.class, "nodeType"), new Bag(new Covered())));
    
  }
  
  @Override
  public void process(JCas jCas, Segment segment)
      throws AnalysisEngineProcessException {

    HashSet<TimeMention> mentions = new HashSet<TimeMention>(JCasUtil.selectCovered(TimeMention.class, segment));
    
    //output the gold time expression's length and real words
//    if(this.isTraining()){
//    	for( TimeMention time: mentions){
//    		int numTokens = JCasUtil.selectCovered(BaseToken.class, time).size();
//    		System.out.println(numTokens + ";" +time.getCoveredText());
//    	}
//    }
	  
    for(TopTreebankNode root : JCasUtil.selectCovered(TopTreebankNode.class, segment)){
      recursivelyProcessNode(jCas, root.getChildren(0), mentions, 0.0);
    }
  }

  private double recursivelyProcessNode(JCas jCas, TreebankNode node, Set<TimeMention> mentions, double parentScore) throws AnalysisEngineProcessException {
    // accumulate features:
    double score=0.0;
    parentScore = 0.0;
    ArrayList<Feature> features = new ArrayList<Feature>();
    String category = NON_MENTION;

    // node-based features
    if(node.getParent().getParent() == null) features.add(new Feature("IS_ROOT"));
    features.add(new Feature("NODE_LABEL", node.getNodeType()));
    features.add(new Feature("PARENT_LABEL", node.getParent().getNodeType()));
    List<BaseToken> coveredTokens = JCasUtil.selectCovered(BaseToken.class, node);
    
    //check span length, check if a small node contains any time word
    int numTokens = coveredTokens.size();
    
    if(node.getLeaf()){
      features.add(new Feature("IS_LEAF"));
      features.addAll(wordTypeExtractor.extract(jCas, node));
    }else{
      StringBuilder buffer = new StringBuilder();
      for(int i = 0; i < node.getChildren().size(); i++){
        buffer.append(node.getChildren(i).getNodeType());
        buffer.append("_");
        features.add(new Feature("CHILD_BAG", node.getChildren(i).getNodeType()));
      }
//      features.add(new Feature("NUM_TOKENS", JCasUtil.selectCovered(BaseToken.class, node).size()));
      features.add(new Feature("PRODUCTION", buffer.toString()));
//      features.add(new Feature("LeftSibling", getSiblingCategory(node, -1)));
//      features.add(new Feature("RightSibling", getSiblingCategory(node, 1)));
    }
    
    // other feature types:
    for(FeatureExtractor1 extractor : featureExtractors){
      features.addAll(extractor.extract(jCas, node));
    }
      
    if(this.isTraining()){
      List<TimeMention> goldMentions = JCasUtil.selectCovered(TimeMention.class, node);
      for(TimeMention mention : goldMentions){
        if(mention.getBegin() == node.getBegin() && mention.getEnd() == node.getEnd()){
          category = MENTION;
          score=1.0;
          mentions.remove(mention);
          if(node.getCoveredText().contains("postoperative")){
            System.out.println("*** Positive Example: ***");
            System.out.println("*** Parent: " + node.getParent().getCoveredText());
            printFeatures(node, features);
          }
        }
      }
      if(numTokens < SPAN_LIMIT){
        this.dataWriter.write(new Instance<String>(category, features));
      }
    }else{
      Map<String,Double> outcomes = this.classifier.score(features);
      score = outcomes.get(MENTION);
      category = this.classifier.classify(features);
      if(category.equals(MENTION)){
        // add to cas
        JCas timexCas;
        try {
          timexCas = jCas.getView(timexView);
        } catch (CASException e) {
          throw new AnalysisEngineProcessException(e);
        }

        TimeMention mention = new TimeMention(timexCas, node.getBegin(), node.getEnd());
        mention.setConfidence((float)score);
        mention.addToIndexes();
      }else{
        score = 1 - score;
      }
    }

    // now do children if not a leaf & not a mention
    if(node.getLeaf() || MENTION.equals(category)) return score;
    
    double highestScore = 0.5;
    TreebankNode highestScoringChild = null;
    
    for(int i = 0; i < node.getChildren().size(); i++){
      TreebankNode child = node.getChildren(i);
      double childScore = recursivelyProcessNode(jCas, child, mentions, Math.max(score, parentScore));
      if(childScore > highestScore){
        highestScoringChild = child;
        highestScore = childScore;
      }
    }
    if(!this.isTraining() && MENTION.equals(category)){
      logger.info(String.format("\nFound mention (%s) with score %f\n\tParent (%s) : %f\n\tBest child (%s) : %f\n", node.getCoveredText(), score, node.getParent().getCoveredText(), parentScore, highestScoringChild == null ? "(none)" : highestScoringChild.getCoveredText(), highestScore));
    }
    return score;
  }
  
  private static void printFeatures(TreebankNode node, List<Feature> features) {
    System.out.println(node.getCoveredText());
    for(Feature feat : features){
      System.out.printf("%s => %s\n", feat.getName(), feat.getValue());
    }    
  }
}
