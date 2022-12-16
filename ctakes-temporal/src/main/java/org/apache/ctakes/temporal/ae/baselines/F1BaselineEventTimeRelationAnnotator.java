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
package org.apache.ctakes.temporal.ae.baselines;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

@PipeBitInfo(
		name = "E-T Baseline Scorer",
		description = "Prints Event - Time Scores ...",
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.EVENT, PipeBitInfo.TypeProduct.TIMEX }
)
public class F1BaselineEventTimeRelationAnnotator extends
		RelationExtractorAnnotator {

	  public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
		      throws ResourceInitializationException {
		    return AnalysisEngineFactory.createEngineDescription(
		        F1BaselineEventTimeRelationAnnotator.class,
		        CleartkAnnotator.PARAM_IS_TRAINING,
		        false,
		        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
		        new File(modelDirectory, "model.jar"));
		  }

	@Override
	protected Class<? extends Annotation> getCoveringClass() {
		return Sentence.class;
	}

	@Override
	protected List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas jCas, Annotation sentence) {
    	List<EventMention> events = JCasUtil.selectCovered(jCas, EventMention.class, sentence);
	    	    
	    if(events.size() == 0) {
	      return Lists.newArrayList();
	    }

	    // compute token distance for each time-event pair
	    HashMap<IdentifiedAnnotationPair, Integer> distanceLookup = new HashMap<IdentifiedAnnotationPair, Integer>();
	    for (EventMention event : events) {
	    	if (event.getClass().equals(EventMention.class)) {
	    		// get only times preceding this event:
	    		List<TimeMention> times = JCasUtil.selectCovered(jCas, TimeMention.class, sentence.getBegin(), event.getBegin());
	    		if(times.size() == 0) continue;
	    		for (TimeMention time : times) {
	    			// ignore subclasses like Procedure and Disease/Disorder
	    			IdentifiedAnnotationPair pair = new IdentifiedAnnotationPair(time, event);
	    			List<BaseToken> baseTokens = JCasUtil.selectBetween(jCas, BaseToken.class, pair.getArg1(), pair.getArg2());
	    			int distance = baseTokens.size();
	    			distanceLookup.put(pair, distance);
	    		}
	    	}

	    }
	    
	    if(distanceLookup.size() == 0) return Lists.newArrayList();
	    
	    // find the pair where the distance between entities is the smallest and return it
	    List<IdentifiedAnnotationPair> rankedPairs = new ArrayList<IdentifiedAnnotationPair>(distanceLookup.keySet());
	    Function<IdentifiedAnnotationPair, Integer> getValue = Functions.forMap(distanceLookup);
	    Collections.sort(rankedPairs, Ordering.natural().onResultOf(getValue));
	    
	    List<IdentifiedAnnotationPair> results = new ArrayList<IdentifiedAnnotationPair>();
	    Set<EventMention> relTimes = new HashSet<EventMention>();
	    for(IdentifiedAnnotationPair result : rankedPairs){
	    	if(!relTimes.contains(result.getArg2())){
	    		relTimes.add((EventMention)result.getArg2());
	    		results.add(result);
	    		Sentence sent = JCasUtil.selectCovering(jCas, Sentence.class, result.getArg1().getBegin(), result.getArg1().getEnd()).get(0); 
	    		System.out.println(sent.getCoveredText());
	    		System.out.println("Relation found: CONTAINS(" + result.getArg1().getCoveredText() + "," + result.getArg2().getCoveredText() + ")\n");
	    	}
	    }
	    return results;
	}

	  @Override
	  public String classify(List<Feature> features) {
	    return "CONTAINS";
	  }
}
