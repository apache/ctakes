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
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

public class SectionHeaderRelationExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>{

	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<>();

		//find event
		EventMention eventA = null;
		EventMention eventB = null;
		if(arg1 instanceof EventMention){
			eventA = (EventMention) arg1;
		}
		if(arg2 instanceof EventMention){
			eventB = (EventMention) arg2;
		}
		
		if(eventA==null && eventB==null){
			return feats;
		}

		//get covering segment set:
		Map<EventMention, Collection<Segment>> coveringMap =
				JCasUtil.indexCovering(jcas, EventMention.class, Segment.class);
		List<Segment> segListA = Lists.newArrayList();
		List<Segment> segListB = Lists.newArrayList();
		if(eventA != null){
			for ( Segment seg : coveringMap.get(eventA)){
				if (!seg.getId().equals("SIMPLE_SEGMENT")){//remove simple segment
					segListA.add(seg);
				}
			}
		}
		if(eventB != null){
			for ( Segment seg : coveringMap.get(eventB)){
				if (!seg.getId().equals("SIMPLE_SEGMENT")){//remove simple segment
					segListB.add(seg);
				}
			}
		}
		
		//get segment id
		List<String> segANames = Lists.newArrayList();
		List<String> segBNames = Lists.newArrayList();
		for(Segment seg : segListA) {
			String segname = seg.getId();
			Feature feature = new Feature("SegmentID_arg1", segname);
			feats.add(feature);
			segANames.add(segname);
		}
		for(Segment seg : segListB) {
			String segname = seg.getId();
			Feature feature = new Feature("SegmentID_arg2", segname);
			feats.add(feature);
			segBNames.add(segname);
		}
		for(String segA : segANames){
			for(String segB : segBNames){
				if(segA.equals(segB)){
					Feature feature = new Feature("InTheSameSegment_", segA);
					feats.add(feature);
				}
			}
		}
		return feats;
	}


}
