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
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;
/**
 * extract the nearby nearest time, date information for any event 
 * @author CH151862
 *
 */
public class EventPositionRelationFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	private String name="EventRelaionPosition";

	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = Lists.newArrayList();

		List<EventMention> events = Lists.newArrayList();
		if(arg1 instanceof EventMention) events.add((EventMention)arg1);
		if(arg2 instanceof EventMention) events.add((EventMention)arg2);
		
		Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
		List<Sentence> sentList = Lists.newArrayList();
		sentList.addAll(sentences);
		int sentSize = sentList.size();
		
		//get covering segment:
		Map<EventMention, Collection<Segment>> coveringMap =
				JCasUtil.indexCovering(jCas, EventMention.class, Segment.class);
		
		Collection<EventMention> allevents = JCasUtil.select(jCas, EventMention.class);
		List<EventMention> eventList = Lists.newArrayList();
		//filter events
		for(EventMention eventa : allevents){
			// filter out ctakes events
			if(eventa.getClass().equals(EventMention.class)){
				eventList.add(eventa);
			}
		}
		int eventSize = eventList.size();

		for(EventMention event: events){
			//check if it within the last/first 5 sentences			
			if( sentSize >= 5){
				if( containEvent(sentList.get(0), sentList.get(4), event)){
					Feature feature = new Feature(this.name, "Within_Top_5_sentences");
					features.add(feature);
				}
				if( containEvent(sentList.get(sentSize-5), sentList.get(sentSize-1), event)){
					Feature feature = new Feature(this.name, "Within_Last_5_sentences");
					features.add(feature);
				}
			}

			//check if it within the last/first 5 events
			if(eventSize >= 5){
				if( containEvent(eventList.get(0), eventList.get(4), event)){
					Feature feature = new Feature(this.name, "Within_Top_5_events");
					features.add(feature);
				}
				if( containEvent(eventList.get(eventSize-5), eventList.get(eventSize-1), event)){
					Feature feature = new Feature(this.name, "Within_Last_5_events");
					features.add(feature);
				}
			}

			
			Collection<Segment> segList = coveringMap.get(event);

			//if an event is the first/last 3 events in its section
			for(Segment seg : segList) {
				String segname = seg.getId();
				if (!segname.equals("SIMPLE_SEGMENT")){//remove simple segment
					List<EventMention> segEvents = JCasUtil.selectCovered(jCas, EventMention.class, seg);
					List<EventMention> realEvents = new ArrayList<>();
					//filtering events
					for(EventMention segevent : segEvents){
						// filter out ctakes events
						if(segevent.getClass().equals(EventMention.class)){
							realEvents.add(segevent);
						}
					}
					segEvents = realEvents;
					int segEventSize = segEvents.size();
					if(segEventSize >= 3){
						if( containEvent(segEvents.get(0), segEvents.get(2), event)){
							Feature feature = new Feature(this.name, "Within_Top_3_events_of"+segname);
							features.add(feature);
						}
						if( containEvent(segEvents.get(segEventSize-3), segEvents.get(segEventSize-1), event)){
							Feature feature = new Feature(this.name, "Within_Last_3_events_of"+segname);
							features.add(feature);
						}
					}
				}
			}
		}

		return features;
	}

	private static boolean containEvent(Annotation arg1, Annotation arg2,
			Annotation target) {
		if(target.getBegin()>=arg1.getBegin() && target.getEnd()<= arg2.getEnd())
			return true;
		return false;
	}
}
