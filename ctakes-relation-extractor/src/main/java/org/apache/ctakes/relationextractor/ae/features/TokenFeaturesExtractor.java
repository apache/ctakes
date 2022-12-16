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
package org.apache.ctakes.relationextractor.ae.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bag;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.FirstCovered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.LastCovered;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CombinedExtractor1;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.DistanceExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.NamingExtractor1;

public class TokenFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  private FeatureExtractor1 coveredText = new CoveredTextExtractor();

  /**
   * First word of the mention, last word of the mention, all words of the mention as a bag, the
   * preceding 3 words, the following 3 words
   */
  private FeatureExtractor1 tokenContext = new CleartkExtractor(
      BaseToken.class,
      coveredText,
      new FirstCovered(1),
      new LastCovered(1),
      new Bag(new Covered()),
      new Preceding(3),
      new Following(3));

  /**
   * All extractors for mention 1, with features named to distinguish them from mention 2
   */
  private FeatureExtractor1 mention1FeaturesExtractor = new NamingExtractor1(
      "mention1",
      new CombinedExtractor1(coveredText, tokenContext));

  /**
   * All extractors for mention 2, with features named to distinguish them from mention 1
   */
  private FeatureExtractor1 mention2FeaturesExtractor = new NamingExtractor1(
      "mention2",
      new CombinedExtractor1(coveredText, tokenContext));

  /**
   * First word, last word, and all words between the mentions
   */
  private CleartkExtractor tokensBetween = new CleartkExtractor(
      BaseToken.class,
      new NamingExtractor1("BetweenMentions", coveredText),
      new FirstCovered(1),
      new LastCovered(1),
      new Bag(new Covered()));

  /**
   * Number of words between the mentions
   */
  private DistanceExtractor nTokensBetween = new DistanceExtractor(null, BaseToken.class);

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation mention1, IdentifiedAnnotation mention2)
      throws AnalysisEngineProcessException {
    List<Feature> features = new ArrayList<Feature>();
    Annotation arg1 = mention1;
    Annotation arg2 = mention2;
    
    if(arg1 instanceof EventMention){
      arg1 = getExpandedEvent(jCas, mention1);
      if(arg1 == null) arg1 = mention1;
    }
    
    if(arg2 instanceof EventMention){
      arg2 = getExpandedEvent(jCas, mention2);
      if(arg2 == null) arg2 = mention2;
    }
    
    features.addAll(this.mention1FeaturesExtractor.extract(jCas, arg1));
    features.addAll(this.mention2FeaturesExtractor.extract(jCas, arg2));
    features.addAll(this.tokensBetween.extractBetween(jCas, arg1, arg2));
    features.addAll(this.nTokensBetween.extract(jCas, arg1, arg2));
    return features;
  }

  private static TreebankNode getExpandedEvent(JCas jCas, IdentifiedAnnotation mention){
    // since events are single words, we are at a terminal node:
    List<TerminalTreebankNode> terms = JCasUtil.selectCovered(TerminalTreebankNode.class, mention);
    if(terms == null || terms.size() == 0){
      return null;
    }
    
    TreebankNode coveringNode = AnnotationTreeUtils.annotationNode(jCas, mention);
    if(coveringNode == null) return terms.get(0);
    
    String pos =terms.get(0).getNodeType(); 
    // do not expand Verbs
    if(pos.startsWith("V")) return coveringNode;
    
    if(pos.startsWith("N")){
      // get first NP node:
      while(coveringNode != null && !coveringNode.getNodeType().equals("NP")){
        coveringNode = coveringNode.getParent();
      }
    }else if(pos.startsWith("J")){
      while(coveringNode != null && !coveringNode.getNodeType().equals("ADJP")){
        coveringNode = coveringNode.getParent();
      }
    }
    if(coveringNode == null) coveringNode = terms.get(0);
    return coveringNode;    
  }
}
