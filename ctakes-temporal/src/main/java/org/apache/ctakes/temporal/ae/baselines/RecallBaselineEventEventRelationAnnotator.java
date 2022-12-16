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
import java.util.List;

import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
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

public class RecallBaselineEventEventRelationAnnotator extends
		RelationExtractorAnnotator {
	  public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
		      throws ResourceInitializationException {
		    return AnalysisEngineFactory.createEngineDescription(
		        RecallBaselineEventEventRelationAnnotator.class,
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
	    List<IdentifiedAnnotationPair> results = new ArrayList<IdentifiedAnnotationPair>();
	    // get all event mentions in the sentence
	    List<EventMention> events = JCasUtil.selectCovered(jCas, EventMention.class, sentence);
	    
	    // filter out all the ctakes-generated events with more specific types
	    List<EventMention> realEvents = new ArrayList<EventMention>();
	    for(EventMention event : events){
	    	if(event.getClass().equals(EventMention.class)){
	    		realEvents.add(event);
	    	}
	    }
	    events = realEvents;
	    
	    // if we still have more than 1 we can continue
	    if(events.size() < 2) {
	      return results;
	    }
	    
	    // left-most event is the anchor and we will say all others are contained by it.
	    EventMention anchor = events.get(0);
	    for(int i = 1; i < events.size(); i++){
	          IdentifiedAnnotationPair pair = new IdentifiedAnnotationPair(anchor, events.get(i));
	          results.add(pair);
	    }
	    
	    return results;
	}
	
	/*
	 * For this method, we simply return the positive label "CONTAINS". All of the work in the baseline is
	 * done by getCandidateRelationArgumentPairs().
	 * 
	 * (non-Javadoc)
	 * @see org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator#classify(java.util.List)
	 */
	@Override
	public String classify(List<Feature> features) {
		return "CONTAINS";
	}
}
