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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.util.ViewUriUtil;

import java.util.ArrayList;
import java.util.List;
/**
 * Given a pair of arguments, if any one of them is an event, get the related event-sectiontime as features 
 * @author CH151862
 *
 */
public class EventTimeRelationFeatureExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	private String cachedDocID = null;
	private Multimap<EventMention, String> eventSectionTimeRelationLookup;
	
	@SuppressWarnings("null")
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		
		String docId=null;
		try{
			docId = ViewUriUtil.getURI(jCas).toString();// get docID
		}catch(Exception e){
         docId = DocIdUtil.getDocumentID( jCas );
		}
		if(!docId.equals(cachedDocID)){
			// rebuild cache
			cachedDocID = docId;
			rebuildCache(jCas);
		}
		
		ArrayList<Feature> feats = new ArrayList<>();
		
		List<EventMention> events = Lists.newArrayList();
		if(arg1 instanceof EventMention) events.add((EventMention)arg1);
		if(arg2 instanceof EventMention) events.add((EventMention)arg2);
		for(EventMention event: events){
			for(String value : eventSectionTimeRelationLookup.get(event)){
				feats.add(new Feature("hasEventTimeRelation_", value));
			}
		}

		return feats;
	}

	private void rebuildCache(JCas jCas){
		//get admission Time:
		TimeMention admissionTime = null;
		//may need better way to identify Discharge Time other than relative span information:
		for (TimeMention time : JCasUtil.selectCovered(jCas, TimeMention.class, 15, 30)) {
			if(time.getTimeClass() != null && time.getTimeClass().equals("DATE")){
				admissionTime = time;
				break;
			}
		}
		//get discharge Time id: T1:
		TimeMention dischargeTime = null;
		//may need better way to identify Discharge Time other than relative span information:
		for (TimeMention time : JCasUtil.selectCovered(jCas, TimeMention.class, 40, 60)) {
			if(time.getTimeClass() != null && time.getTimeClass().equals("DATE")){
				dischargeTime = time;
				break;
			}
		}
		
		eventSectionTimeRelationLookup = HashMultimap.create();
		for (TemporalTextRelation relation : JCasUtil.select(jCas, TemporalTextRelation.class)) {
//			Annotation potentialSectTime = relation.getArg2().getArgument();
//			String relationTo = "";
//			if(potentialSectTime==admissionTime){
//				relationTo="admissionTime";
//			}else if(potentialSectTime == dischargeTime){
//				relationTo="dischargeTime";
//			}
			Annotation arg1 = relation.getArg1().getArgument();
			Annotation arg2 = relation.getArg2().getArgument();
			Annotation event = null;
			TimeMention time  = null;
			if( arg1 instanceof EventMention && arg2 instanceof TimeMention){
				event = arg1;
				time  = (TimeMention)arg2;
			}else if(arg2 instanceof EventMention && arg1 instanceof TimeMention){
				event = arg2;
				time  = (TimeMention)arg1;
			}
			if(event != null && time !=null){
				if(time == admissionTime){
					eventSectionTimeRelationLookup.put((EventMention)event, "admissionTime_"+relation.getCategory());
				}else if(time == dischargeTime){
					eventSectionTimeRelationLookup.put((EventMention)event, "dischargeTime_"+relation.getCategory());
				}else{
					eventSectionTimeRelationLookup.put((EventMention)event, time.getTimeClass()+"_"+relation.getCategory());
				}
//				Annotation potentialEvent = relation.getArg1().getArgument();
//				if(potentialEvent instanceof EventMention){
//					eventSectionTimeRelationLookup.put((EventMention)potentialEvent, relationTo+"_"+relation.getCategory());
//				}
			} 
		}

	}
}
