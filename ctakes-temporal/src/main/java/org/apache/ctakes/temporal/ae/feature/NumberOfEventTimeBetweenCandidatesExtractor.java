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
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

/**
 * Count the number of EventMention and TimeMention in between two arguments as features
 * @author CH151862
 *
 */
public class NumberOfEventTimeBetweenCandidatesExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	@SuppressWarnings("null")
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		ArrayList<Feature> feats = new ArrayList<>();
		
		//suppose arg1 is before arg2
		int begin = arg1.getEnd();
		int end   = arg2.getBegin();
		
		//if arg1 is after arg2
		if (begin > end ){
			begin = arg2.getEnd();
			end   = arg1.getBegin();
		}
		
		if(begin > end){
			return feats;
		}
		
		int eventsInBetween = 0;
		int timesInBetween  = 0;
		int wordsInBetween  = 0;
		int puncsInBetween  = 0;
//		int numsInBetween   = 0;
//		int newlineInBetween= 0;
		
		List<EventMention> events = JCasUtil.selectCovered(jCas, EventMention.class, begin, end);
		List<TimeMention> times   = JCasUtil.selectCovered(jCas, TimeMention.class, begin, end);
		List<WordToken> words 	  = JCasUtil.selectCovered(jCas, WordToken.class, begin, end);
		List<PunctuationToken>punc= JCasUtil.selectCovered(jCas, PunctuationToken.class, begin, end);
//		List<NumToken> numTokens  = JCasUtil.selectCovered(jCas, NumToken.class, begin, end);
//		List<NewlineToken> newline= JCasUtil.selectCovered(jCas, NewlineToken.class, begin, end);
		
		//filter out ctakes events
//		List<EventMention> realEvents = new ArrayList<>();
//		for(EventMention event : events){
//			// filter out ctakes events
//			if(event.getClass().equals(EventMention.class)){
//				realEvents.add(event);
//			}
//		}
//		events = realEvents;
		eventsInBetween = events==null? 0: events.size();
//		if(eventsInBetween > 10) eventsInBetween = 10;
		timesInBetween  = times==null? 0: times.size();
		wordsInBetween  = words==null? 0 : words.size();
		if(wordsInBetween > 20) wordsInBetween = 20;
		puncsInBetween   = punc == null? 0 : punc.size();
		if(puncsInBetween > 5) puncsInBetween = 5;
//		numsInBetween   = numTokens == null? 0 : numTokens.size();
//		newlineInBetween= newline == null? 0 : newline.size();
		
		
		feats.add(new Feature("NumOfEvents_InBetween", eventsInBetween));
		feats.add(new Feature("NumOfTimes_InBetween", timesInBetween));
		feats.add(new Feature("NumOfEventsAndTimes_InBetween", timesInBetween+eventsInBetween));
		feats.add(new Feature("NumOfWords_InBetween", wordsInBetween));
		feats.add(new Feature("NumOfPunctuation_InBetween", puncsInBetween));
//		feats.add(new Feature("NumOfNumberToken_InBetween", numsInBetween));
//		feats.add(new Feature("NumOfNewLine_InBetween", newlineInBetween));
		
		return feats;
	}

}
