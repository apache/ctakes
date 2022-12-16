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
import org.apache.ctakes.typesystem.type.textsem.FractionAnnotation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MeasurementAnnotation;
import org.apache.ctakes.typesystem.type.textsem.RangeAnnotation;
import org.apache.ctakes.typesystem.type.textsem.RomanNumeralAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

public class SpecialAnnotationRelationExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>{

	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<>();
		EventMention eventA = null;
		EventMention eventB = null;

		//find event
		if(arg1 instanceof EventMention){
			eventA = (EventMention) arg1;
		}else if(arg2 instanceof EventMention){
			eventB = (EventMention) arg2;
		}else{
			return feats;
		}

		//1 get covering sentence:
		Map<EventMention, Collection<Sentence>> coveringMap =
				JCasUtil.indexCovering(jcas, EventMention.class, Sentence.class);

		Sentence sentenceA = getCoveringSentence(eventA, coveringMap);
		Sentence sentenceB = getCoveringSentence(eventB, coveringMap);

		//get all special annotation that is in sentence A
		List<RangeAnnotation> rangeInA = new ArrayList<>();
		List<FractionAnnotation> fracInA = new ArrayList<>();
		List<RomanNumeralAnnotation> romanInA = new ArrayList<>();
		List<MeasurementAnnotation> measureInA = new ArrayList<>();
		if(sentenceA != null){
			rangeInA.addAll(JCasUtil.selectCovered(jcas, RangeAnnotation.class, sentenceA));
			if(!rangeInA.isEmpty()) feats.add(new Feature("arg1_has_nearby_", "RangeAnnotation"));
			fracInA.addAll(JCasUtil.selectCovered(jcas, FractionAnnotation.class, sentenceA));
			if(!fracInA.isEmpty()) feats.add(new Feature("arg1_has_nearby_", "FractionAnnotation"));
			romanInA.addAll(JCasUtil.selectCovered(jcas, RomanNumeralAnnotation.class, sentenceA));
			if(!romanInA.isEmpty()) feats.add(new Feature("arg1_has_nearby_", "RomanNumeralAnnotation"));
			measureInA.addAll(JCasUtil.selectCovered(jcas, MeasurementAnnotation.class, sentenceA));
			if(!measureInA.isEmpty()) feats.add(new Feature("arg1_has_nearby_", "MeasurementAnnotation"));
		}

		//get all special annotation that is in sentence B
		List<RangeAnnotation> rangeInB = new ArrayList<>();
		List<FractionAnnotation> fracInB = new ArrayList<>();
		List<RomanNumeralAnnotation> romanInB = new ArrayList<>();
		List<MeasurementAnnotation> measureInB = new ArrayList<>();
		if(sentenceB != null){
			rangeInB.addAll(JCasUtil.selectCovered(jcas, RangeAnnotation.class, sentenceB));
			if(!rangeInB.isEmpty()) feats.add(new Feature("arg2_has_nearby_", "RangeAnnotation"));
			fracInB.addAll(JCasUtil.selectCovered(jcas, FractionAnnotation.class, sentenceB));
			if(!fracInB.isEmpty()) feats.add(new Feature("arg2_has_nearby_", "FractionAnnotation"));
			romanInB.addAll(JCasUtil.selectCovered(jcas, RomanNumeralAnnotation.class, sentenceB));
			if(!romanInB.isEmpty()) feats.add(new Feature("arg2_has_nearby_", "RomanNumeralAnnotation"));
			measureInB.addAll(JCasUtil.selectCovered(jcas, MeasurementAnnotation.class, sentenceB));
			if(!measureInB.isEmpty()) feats.add(new Feature("arg2_has_nearby_", "MeasurementAnnotation"));
		}

		//check if annotation matches
		if(sentenceA != null && sentenceB != null && sentenceA != sentenceB){
			range:
			for(RangeAnnotation rangeA: rangeInA){
				for(RangeAnnotation rangeB: rangeInB){
					if(rangeA.getCoveredText().equalsIgnoreCase(rangeB.getCoveredText())){
						feats.add(new Feature("shareCommonRange_", rangeB.getCoveredText().toLowerCase()));
						break range;
					}
				}
			}

		    fraction:
			for(FractionAnnotation fracA: fracInA){
				for(FractionAnnotation fracB: fracInB){
					if(fracA.getCoveredText().equalsIgnoreCase(fracB.getCoveredText())){
						feats.add(new Feature("shareCommonFraction_", fracB.getCoveredText().toLowerCase()));
						break fraction;
					}
				}
			}

		    number:
			for(RomanNumeralAnnotation numA: romanInA){
				for(RomanNumeralAnnotation numB: romanInB){
					if(numA.getCoveredText().equalsIgnoreCase(numB.getCoveredText())){
						feats.add(new Feature("shareCommonFraction_", numB.getCoveredText().toLowerCase()));
						break number;
					}
				}
			}
			
			measure:
			for(MeasurementAnnotation meaA: measureInA){
				for(MeasurementAnnotation meaB: measureInB){
					if(meaA.getCoveredText().equalsIgnoreCase(meaB.getCoveredText())){
						feats.add(new Feature("shareCommonFraction_", meaB.getCoveredText().toLowerCase()));
						break measure;
					}
				}
			}
		}



		return feats;
	}

	private static Sentence getCoveringSentence(EventMention event,
			Map<EventMention, Collection<Sentence>> coveringMap) {
		List<Sentence> sentList = new ArrayList<>();
		if(event != null){
			sentList.addAll(coveringMap.get(event));			
			if(!sentList.isEmpty()){
				return(sentList.get(0));
			}
		}
		return null;
	}


}
