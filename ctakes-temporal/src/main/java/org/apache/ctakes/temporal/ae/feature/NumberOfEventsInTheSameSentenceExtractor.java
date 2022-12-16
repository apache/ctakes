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

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;
/**
 * Count the number of EventMentions in the same sentence as the two arguments, used for features for within sentence event-event relation discovery
 * @author CH151862
 *
 */
public class NumberOfEventsInTheSameSentenceExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	@SuppressWarnings("null")
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		ArrayList<Feature> feats = new ArrayList<>();

		//get the sentence that covers the first argument, suppose arg1 and arg2 are within the same sentence
		Map<IdentifiedAnnotation, Collection<Sentence>> coveringMap =
				JCasUtil.indexCovering(jCas, IdentifiedAnnotation.class, Sentence.class);
		Collection<Sentence> sentList = coveringMap.get(arg1);
		if (sentList == null && sentList.isEmpty()) return feats;

		for(Sentence sent : sentList) {
			List<EventMention> events = JCasUtil.selectCovered(jCas, EventMention.class, sent);

			//filter out ctakes events:
			List<EventMention> realEvents = Lists.newArrayList();
			for( EventMention event : events){
				if(event.getClass().equals(EventMention.class)){
					realEvents.add(event);
				}
			}
			events = realEvents;

			int eventsNum = events==null? 0: events.size();
			
			//find the two arguments indices in the event list
			int arg1Index = -1;
			int arg2Index = -1;
			for(int i=0; i< eventsNum; i++){
				EventMention currentEvent = events.get(i);
				if(hasOverlappingSpan(currentEvent,arg1)){
					arg1Index = i;
				}
				if(hasOverlappingSpan(currentEvent,arg2)){
					arg2Index = i;
				}
				if(arg1Index!=-1 && arg2Index!=-1){
					break;
				}
			}
//			feats.add(new Feature("NumOfEvents_InTheSentence", eventsNum)); //tried this feature, but it is not helpful.
			if(arg1Index!=-1 && arg2Index!=-1){
//				feats.add(new Feature("arg1_index", arg1Index));
//				feats.add(new Feature("arg2_index", arg2Index)); //tried these two features, but not helpful
//				if(arg1Index == 0 || arg2Index == 0){
//					feats.add(new Feature("EventPair_", "ContainsTheFirstEvent"));
//				}// tried this feature, but not helpful
//				if(arg1Index == eventsNum-1 || arg2Index == eventsNum-1){
//					feats.add(new Feature("EventPair_", "ContainsTheLastEvent"));
//				}// tired, not helpful
				int dis = Math.abs(arg1Index-arg2Index);
				feats.add(new Feature("EventPair_Distance", dis));
				if(dis == eventsNum-1){
					feats.add(new Feature("EventPair_", "Major"));
				}
				if(dis==1){
					feats.add(new Feature("EventPair_", "Consecutive"));
				}
			}else{
				System.out.println("Same sentence events matching error.");
			}
		}
		return feats;
	}

	private static boolean hasOverlappingSpan(EventMention cevent,
			IdentifiedAnnotation arg) {
		if(cevent.getBegin()==arg.getBegin() || arg.getEnd()==cevent.getEnd()){
			return true;
		}else if(arg.getBegin()<=cevent.getBegin() && cevent.getEnd()>=arg.getEnd()){
			return true;
		}else if(arg.getBegin()>=cevent.getBegin() && cevent.getEnd()<=arg.getEnd()){ //if argument cover current (gold) event mention.
			return true;
		}
		return false;
	}

}
