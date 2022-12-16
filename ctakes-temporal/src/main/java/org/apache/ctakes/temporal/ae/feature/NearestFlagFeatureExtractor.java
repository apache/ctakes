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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

public class NearestFlagFeatureExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		ArrayList<Feature> feats = new ArrayList<Feature>();
		EventMention event = null;
		TimeMention time = null;
		
		// swap the order if necessary:
		if(arg2.getBegin() <= arg1.getBegin() && arg2.getEnd() <= arg1.getEnd()){
			IdentifiedAnnotation temp = arg1;
			arg1 = arg2;
			arg2 = temp;
		}

		if(arg1 instanceof EventMention){
			event = (EventMention) arg1;
			time = (TimeMention) arg2;
		}else{
			time = (TimeMention) arg1;
			event = (EventMention) arg2;
		}
		
		//1 get covering sentence:
		Map<EventMention, Collection<Sentence>> eventCoveringMap =
				JCasUtil.indexCovering(jCas, EventMention.class, Sentence.class);
		Collection<Sentence> eventSentList = eventCoveringMap.get(event);
		Map<TimeMention, Collection<Sentence>> timeCoveringMap =
				JCasUtil.indexCovering(jCas, TimeMention.class, Sentence.class);
		Collection<Sentence> timeSentList = timeCoveringMap.get(time);

		//2 get TimeX
		Map<Integer, TimeMention> timeDistMap = null;
		Map<Integer, EventMention> eventDistMap = null;
		
		if (eventSentList != null && !eventSentList.isEmpty()){
			timeDistMap = new TreeMap<Integer, TimeMention>();

			for(Sentence sent : eventSentList) {
				for (TimeMention atime : JCasUtil.selectCovered(jCas, TimeMention.class, sent)) {
					timeDistMap.put(Math.abs(atime.getBegin() - event.getBegin()), atime);
				}
			}

			//get the closest Time Expression feature
			for (Map.Entry<Integer, TimeMention> entry : timeDistMap.entrySet()) {
				if( entry.getValue().equals(time)){
					Feature indicator = new Feature("ClosestPair", "ClosestPair");
					feats.add(indicator);
					return feats;
				}
				break;
			}
		}

		if (timeSentList != null && !timeSentList.isEmpty()){
			eventDistMap = new TreeMap<Integer, EventMention>();

			for(Sentence sent : timeSentList) {
				for (EventMention aEvent : JCasUtil.selectCovered(jCas, EventMention.class, sent)) {
					eventDistMap.put(Math.abs(aEvent.getBegin() - time.getBegin()), aEvent);
				}
			}

			//get the closest Event feature
			for (Map.Entry<Integer, EventMention> entry : eventDistMap.entrySet()) {
				if( entry.getValue().equals(event)){
					Feature indicator = new Feature("ClosestPair", "ClosestPair");
					feats.add(indicator);
				}
				break;
			}
		}

		return feats;
	}

}
