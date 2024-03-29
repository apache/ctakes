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

import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Features that capture punctuation marks between the two arguments.
 */
public class PunctuationFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {

  	List<Feature> features = new ArrayList<Feature>();
  	
  	// entity1 ... entity2 scenario
  	if(arg1.getEnd() < arg2.getBegin()) {
  		for(PunctuationToken token : JCasUtil.selectCovered(jCas, PunctuationToken.class, arg1.getEnd(), arg2.getBegin())) {
  			features.add(new Feature("arg1_punctuation_arg2", token.getCoveredText()));
  			break;
  		}
  	}
  	
  	// entity2 ... entity1 scenario
  	if(arg2.getEnd() < arg1.getBegin()) {
  		for(PunctuationToken token : JCasUtil.selectCovered(jCas, PunctuationToken.class, arg2.getEnd(), arg1.getBegin())) {
  			features.add(new Feature("arg2_punctuation_arg1", token.getCoveredText()));
  			break;
  		}
  	}
  	
    return features;
  }

}
