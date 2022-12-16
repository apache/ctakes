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
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

public class DependencyFeatureExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		ArrayList<Feature> feats = new ArrayList<Feature>();
		EventMention event = null;
		TimeMention time = null;
		
		if(arg1 instanceof EventMention){
			event = (EventMention) arg1;
			time = (TimeMention) arg2;
		}else{
			time = (TimeMention) arg1;
			event = (EventMention) arg2;
		}		
		//1 get covering ConllDependencyNode:
		Map<IdentifiedAnnotation, Collection<ConllDependencyNode>> dependencyCoveringMap =
				JCasUtil.indexCovering(jCas, IdentifiedAnnotation.class, ConllDependencyNode.class);
		Collection<ConllDependencyNode> eventDNodeList = dependencyCoveringMap.get(event);
		Collection<ConllDependencyNode> timeDNodeList = dependencyCoveringMap.get(time);
		
		//2 get dependency relationship
		if (timeDNodeList != null && !timeDNodeList.isEmpty() && eventDNodeList != null && !eventDNodeList.isEmpty()){
			for(ConllDependencyNode timeNode : timeDNodeList) {
				ConllDependencyNode headOfTime = timeNode.getHead();
				for (ConllDependencyNode eventNode : eventDNodeList) {
					if ( timeNode.getPostag() != null && eventNode.getPostag() != null){//make sure the covering nodes are not root sentences
						while( headOfTime != null ){
							if (headOfTime.equals(eventNode)) {
								Feature indicator = new Feature("DependentTo", "DependentTo");
								feats.add(indicator);
								return feats;
//							}else if (headOfTime.getHead() == null){//if one of the node is dependent to the root sentence
//								Feature indicator = new Feature("OpenDependentTo", "OpenDependentTo");
//								feats.add(indicator);
//								return feats;
							}
							headOfTime = headOfTime.getHead();
						}
					}
				}
			}

		}

		Feature indicator = new Feature("NotDependent", "NotDependent");
		feats.add(indicator);
		return feats;
	}

}
