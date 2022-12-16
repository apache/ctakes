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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.data.analysis.Utils;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

/**
 * Word embedding based features.
 * OOV words are handled by the average vector which should be 
 * included with the rest of the vectors and indexed as "oov".
 */
public class EmbeddingFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation, IdentifiedAnnotation> {

  private int numberOfDimensions;
  private Map<String, List<Double>> wordVectors;

  public EmbeddingFeatureExtractor(Map<String, List<Double>> wordVectors) {
    this.wordVectors = wordVectors;
    numberOfDimensions = this.wordVectors.get("oov").size();
  }

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {

    List<Feature> features = new ArrayList<>();

    String arg1LastWord = Utils.getLastWord(jCas, arg1).toLowerCase();
    String arg2LastWord = Utils.getLastWord(jCas, arg2).toLowerCase();

    List<Double> arg1Vector;
    if(wordVectors.containsKey(arg1LastWord)) {
      arg1Vector = wordVectors.get(arg1LastWord);
    } else {
      arg1Vector = wordVectors.get("oov");
    }
    List<Double> arg2Vector;
    if(wordVectors.containsKey(arg2LastWord)) {
      arg2Vector = wordVectors.get(arg2LastWord);
    } else {
      arg2Vector = wordVectors.get("oov");
    }
    
    // head word feataures
    for(int dim = 0; dim < numberOfDimensions; dim++) {
      String featureName = String.format("arg1_dim_%d", dim);
      features.add(new Feature(featureName, arg1Vector.get(dim)));
    }
    for(int dim = 0; dim < numberOfDimensions; dim++) {
      String featureName = String.format("arg2_dim_%d", dim);
      features.add(new Feature(featureName, arg2Vector.get(dim)));
    }    

    // head word similarity features
    double similarity = computeCosineSimilarity(arg1Vector, arg2Vector); 
    features.add(new Feature("arg_cos_sim", similarity));
    
    // words between argument features
    List<WordToken> wordsBetweenArgs = JCasUtil.selectBetween(jCas, WordToken.class, arg1, arg2);
    if(wordsBetweenArgs.size() < 1) {
      return features;  
    }
    
    List<Double> sum = new ArrayList<>(Collections.nCopies(numberOfDimensions, 0.0));
    for(WordToken wordToken : wordsBetweenArgs) {
      List<Double> wordVector;
      if(wordVectors.containsKey(wordToken.getCoveredText().toLowerCase())) {
        wordVector = wordVectors.get(wordToken.getCoveredText().toLowerCase());
      } else {
        wordVector = wordVectors.get("oov");
      }
      sum = addVectors(sum, wordVector);      
    }

    for(int dim = 0; dim < numberOfDimensions; dim++) {
      String featureName = String.format("average_dim_%d", dim);
      features.add(new Feature(featureName, sum.get(dim) / wordsBetweenArgs.size()));
    }

    return features;
  }

  /**
   * Compute cosine similarity between two vectors.
   */
  public double computeCosineSimilarity(List<Double> vector1, List<Double> vector2) {

    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;

    for (int dim = 0; dim < numberOfDimensions; dim++) {
      dotProduct = dotProduct + vector1.get(dim) * vector2.get(dim);
      norm1 = norm1 + Math.pow(vector1.get(dim), 2);
      norm2 = norm2 + Math.pow(vector2.get(dim), 2);
    }

    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
  }
  
  /**
   * Add two vectors. Return the sum vector.
   */
  public List<Double> addVectors(List<Double> vector1, List<Double> vector2) {
    
    List<Double> sum = new ArrayList<>();
    for(int dim = 0; dim < numberOfDimensions; dim++) {
      sum.add(vector1.get(dim) + vector2.get(dim));
    }
    
    return sum;
  }
}
