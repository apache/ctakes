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

import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

public class TreeHeightBaseline extends RelationExtractorAnnotator {

	  public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
		      throws ResourceInitializationException {
		    return AnalysisEngineFactory.createEngineDescription(
		        TreeHeightBaseline.class,
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
	public String classify(List<Feature> features) {
		return "CONTAINS";
	}
	
	@Override
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
			throws ResourceInitializationException {
		return new ArrayList<>();
	}

	@Override
	protected List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas jCas, Annotation sentence) {
	    List<IdentifiedAnnotationPair> results = new ArrayList<>();

	    // get all event mentions in the sentence
	    List<EventMention> events = JCasUtil.selectCovered(jCas, EventMention.class, sentence);
	    
	    // filter out all the ctakes-generated events with more specific types
	    List<EventMention> realEvents = new ArrayList<>();
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
	    
	    // for each event, cmopute the tree distance to the root, and call the closest to the root the anchor.
	    int minDepth = Integer.MAX_VALUE;
	    EventMention anchorMention = null;
	    for(EventMention event : events){
	    	TreebankNode node = AnnotationTreeUtils.annotationNode(jCas, event);
	    	int depth = 0;
	    	while(node.getParent() != null){
	    		depth++;
	    		node = node.getParent();
	    	}
	    	if(depth < minDepth){
	    		minDepth = depth;
	    		anchorMention = event;
	    	}
	    }
	    
	    // now that we have the anchor, connect every other mention to it:
	    for(EventMention event : events){
	    	if(event != anchorMention){
	    		results.add(new IdentifiedAnnotationPair(anchorMention, event));
	    	}
	    }
	    
	    return results;
	}

}
