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
import java.util.HashSet;
import java.util.List;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

import edu.mit.jwi.IDictionary;

/**
 * Features that are based on WordNet.
 */
public class WordNetFeatureExtractor {

  public static List<Feature> extract(JCas jCas, IdentifiedAnnotation identifiedAnnotation, IDictionary iDictionary) {
  	
    List<Feature> features = new ArrayList<Feature>();
    
    // get token annotations corresponding to this identified annotation
    List<BaseToken> baseTokens = JCasUtil.selectCovered(
        jCas, 
        BaseToken.class, 
        identifiedAnnotation.getBegin(), 
        identifiedAnnotation.getEnd());

    if(baseTokens.size() < 1) {
      return features;
    }
    
    // need text of the token and pos tag to do a wordnet search
    String token = baseTokens.get(0).getCoveredText().toLowerCase();
    String pos = baseTokens.get(0).getPartOfSpeech();

    List<String> stems = WordNetUtils.getStems(token, pos, iDictionary);
    
    if(stems.size() > 0) {
      HashSet<String> hypernyms = WordNetUtils.getHypernyms(iDictionary, stems.get(0), pos, true);
    
      for(String hypernym : hypernyms) {
        features.add(new Feature("wn_hypernym", hypernym));
      }
    }
    
    return features;
  }
}
