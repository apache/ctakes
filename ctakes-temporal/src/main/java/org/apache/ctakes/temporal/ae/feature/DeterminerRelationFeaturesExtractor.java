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
package org.apache.ctakes.temporal.ae.feature;

import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;
/**
 * extract the nearby nearest time, date information for any event 
 * @author CH151862
 *
 */
public class DeterminerRelationFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	private String name="Determiner";

	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = Lists.newArrayList();

		List<IdentifiedAnnotation> arguments = Lists.newArrayList();
		arguments.add(arg1);
		arguments.add(arg2);
		
		for(IdentifiedAnnotation arg: arguments){
			List<WordToken> tokens = JCasUtil.selectCovered(jCas, WordToken.class, arg);
			for(WordToken word : tokens){
				String pos = word.getPartOfSpeech();
				if(pos.startsWith("DT")||pos.startsWith("PRP$")){
//					features.add(new Feature(this.name, "contains_DT"));
					features.add(new Feature(this.name, pos));
					break;
				}
			}
		}

		return features;
	}
}
