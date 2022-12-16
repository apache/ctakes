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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;
/**
 * Check is an timex argument is in the begining/end of a sentence 
 * @author CH151862
 *
 */
public class TimeXPropertyRelationFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	private String name="TimeXProperty";

	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = Lists.newArrayList();

		List<TimeMention> times = Lists.newArrayList();
		if(arg1 instanceof TimeMention) times.add((TimeMention)arg1);
		if(arg2 instanceof TimeMention) times.add((TimeMention)arg2);

		Map<TimeMention, Collection<Sentence>> coveringMap =
				JCasUtil.indexCovering(jCas, TimeMention.class, Sentence.class);

		for(TimeMention time : times){
			Collection<Sentence> sentList = coveringMap.get(time);

			//get time class
			String timeclass = time.getTimeClass();
			if(timeclass != null) features.add(new Feature("TimeClass", timeclass));
			
			//check if it contains only one word
			List<WordToken> timewords = Lists.newArrayList();
			timewords.addAll(JCasUtil.selectCovered(jCas, WordToken.class, time));
			if(timewords.size()==1){
				features.add(new Feature(name, "one_word"));
			}
			
			//get position in sentence
			for(Sentence sent : sentList) {
				if(sent.getBegin()==time.getBegin() && time.getEnd()>=(sent.getEnd()-2)){
					features.add(new Feature(name, "entire_sentence"));
				}else if(sent.getBegin()==time.getBegin()){
					features.add(new Feature(name, "beginning_of_sentence"));
				}else if(time.getEnd()>=(sent.getEnd()-2)){//allow a sentence to have a space and a period.
					features.add(new Feature(name, "end_of_sentence"));
				}
			}
		}

		return features;
	}

}
