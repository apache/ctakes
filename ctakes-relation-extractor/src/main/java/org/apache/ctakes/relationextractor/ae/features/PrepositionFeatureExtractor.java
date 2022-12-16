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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

/**
 * Features that indicate whether there is a preposition between the two arguments. 
 * Uses a predifined list of prepositions rather than POS tags.
 * 
 */
public class PrepositionFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {

  	HashSet<String> prepositions = 
  			new HashSet<String>(Arrays.asList("about", "above", "across", "against", "amid", "around", "at", "atop", 
  					"behind", "below", "beneath", "beside", "between", "beyond", "by", "for", "from",
  					"down", "in", "including", "inside", "into", "mid", "near", "of", "off", "on", "onto", "opposite", "out",
  					"outside", "over", "round", "through", "throughout", "to", "under", "underneath", "with", "within", "without"));
  					
  	List<Feature> features = new ArrayList<Feature>();
  	
  	// entity1 ... entity2 scenario
  	if(arg1.getEnd() < arg2.getBegin()) {
  		for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, arg1.getEnd(), arg2.getBegin())) {
  			if(prepositions.contains(token.getCoveredText())) {
  				features.add(new Feature("arg1_preposition_arg2", token.getCoveredText()));
  			}
  		}
  	}
  	
  	// entity2 ... entity1 scenario
  	if(arg2.getEnd() < arg1.getBegin()) {
  		for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, arg2.getEnd(), arg1.getBegin())) {
  			if(prepositions.contains(token.getCoveredText())) {
  				features.add(new Feature("arg2_preposition_arg1", token.getCoveredText()));
  			}
  		}
  	}
  	
    return features;
  }

}
