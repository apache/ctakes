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
      name = "E-T Precision TLinker",
      description = "Creates Event - Time relations.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION,
            PipeBitInfo.TypeProduct.EVENT, PipeBitInfo.TypeProduct.TIMEX },
      products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class PrecisionBaselineEventTimeRelationAnnotator extends RelationExtractorAnnotator {

  public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        PrecisionBaselineEventTimeRelationAnnotator.class,
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
  public List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
      JCas jCas,
      Annotation sentence) {
  
    List<EventMention> events = JCasUtil.selectCovered(jCas, EventMention.class, sentence);
    List<TimeMention> times = JCasUtil.selectCovered(jCas, TimeMention.class, sentence);
    
    if(times.size() < 1 || events.size() < 1) {
      return Lists.newArrayList();
    }

    // compute token distance for each time-event pair
    HashMap<IdentifiedAnnotationPair, Integer> distanceLookup = new HashMap<IdentifiedAnnotationPair, Integer>();
    for (EventMention event : events) {
      // ignore subclasses like Procedure and Disease/Disorder
      if (event.getClass().equals(EventMention.class)) {
        for (TimeMention time : times) {
          IdentifiedAnnotationPair pair = new IdentifiedAnnotationPair(time, event);
          List<BaseToken> baseTokens = JCasUtil.selectBetween(jCas, BaseToken.class, pair.getArg1(), pair.getArg2());
          int distance = baseTokens.size();
          distanceLookup.put(pair, distance);
        }
      }
    }

    // find the pair where the distance between entities is the smallest and return it
    List<IdentifiedAnnotationPair> rankedPairs = new ArrayList<IdentifiedAnnotationPair>(distanceLookup.keySet());
    Function<IdentifiedAnnotationPair, Integer> getValue = Functions.forMap(distanceLookup);
    Collections.sort(rankedPairs, Ordering.natural().onResultOf(getValue));

    List<IdentifiedAnnotationPair> results = new ArrayList<IdentifiedAnnotationPair>();
    
    Set<TimeMention> relTimes = new HashSet<TimeMention>();
    for(IdentifiedAnnotationPair result : rankedPairs){
    	if(!relTimes.contains(result.getArg1())){
    		relTimes.add((TimeMention)result.getArg1());
    		results.add(result);
    	}
    }

    return results;
  }
  
  @Override
  public String classify(List<Feature> features) {
    return "CONTAINS";
  }
}
