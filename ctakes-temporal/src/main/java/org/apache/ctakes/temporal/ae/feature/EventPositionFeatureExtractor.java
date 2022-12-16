/**
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
//import java.util.logging.Logger;

import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

/**
 * Event postion feature extractor:
 * 1. check if an event in in the first/last 5 sentence
 * 2. if an event is the fisrt/last 3 events in its section
 * 3. if an event is the first/last 5 events in a note
 * @author CH151862
 *
 */
public class EventPositionFeatureExtractor implements FeatureExtractor1 {

	private String name;

	//  private Logger logger = Logger.getLogger(this.getClass().getName());

	public EventPositionFeatureExtractor() {
		super();
		this.name = "EventPosition";

	}

	@Override
	public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
		List<Feature> features = new ArrayList<>();

		//check if it within the last/first 5 sentences
		Collection<Sentence> sentences = JCasUtil.select(view, Sentence.class);
		List<Sentence> sentList = Lists.newArrayList();
		sentList.addAll(sentences);
		int sentSize = sentList.size();
		if( sentSize >= 5){
			if( containEvent(sentList.get(0), sentList.get(4), annotation)){
				Feature feature = new Feature(this.name, "Within_Top_5_sentences");
				features.add(feature);
			}
			if( containEvent(sentList.get(sentSize-5), sentList.get(sentSize-1), annotation)){
				Feature feature = new Feature(this.name, "Within_Last_5_sentences");
				features.add(feature);
			}
		}

		//check if it within the last/first 5 events
		Collection<EventMention> events = JCasUtil.select(view, EventMention.class);
		List<EventMention> eventList = Lists.newArrayList();
		//filter events
		for(EventMention event : events){
			// filter out ctakes events
			if(event.getClass().equals(EventMention.class)){
				eventList.add(event);
			}
		}
		int eventSize = eventList.size();
		if(eventSize >= 5){
			if( containEvent(eventList.get(0), eventList.get(4), annotation)){
				Feature feature = new Feature(this.name, "Within_Top_5_events");
				features.add(feature);
			}
			if( containEvent(eventList.get(eventSize-5), eventList.get(eventSize-1), annotation)){
				Feature feature = new Feature(this.name, "Within_Last_5_events");
				features.add(feature);
			}
		}

		//get covering segment:
		Map<EventMention, Collection<Segment>> coveringMap =
				JCasUtil.indexCovering(view, EventMention.class, Segment.class);
		EventMention targetTokenAnnotation = (EventMention)annotation;
		Collection<Segment> segList = coveringMap.get(targetTokenAnnotation);

		//if an event is the first/last 3 events in its section
		for(Segment seg : segList) {
			String segname = seg.getId();
			if (!segname.equals("SIMPLE_SEGMENT")){//remove simple segment
				List<EventMention> segEvents = JCasUtil.selectCovered(view, EventMention.class, seg);
				List<EventMention> realEvents = new ArrayList<>();
				//filtering events
				for(EventMention event : segEvents){
					// filter out ctakes events
					if(event.getClass().equals(EventMention.class)){
						realEvents.add(event);
					}
				}
				segEvents = realEvents;
				int segEventSize = segEvents.size();
				if(segEventSize >= 3){
					if( containEvent(segEvents.get(0), segEvents.get(2), annotation)){
						Feature feature = new Feature(this.name, "Within_Top_3_events_of"+segname);
						features.add(feature);
					}
					if( containEvent(segEvents.get(segEventSize-3), segEvents.get(segEventSize-1), annotation)){
						Feature feature = new Feature(this.name, "Within_Last_3_events_of"+segname);
						features.add(feature);
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
