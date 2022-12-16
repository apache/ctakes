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
package org.apache.ctakes.relationextractor.ae.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class DependencyTreeFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>{

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		
	    List<Feature> features = new ArrayList<Feature>();
	    features.addAll(extractForNode(jCas, arg1, "MENTION1"));
	    features.addAll(extractForNode(jCas, arg2, "MENTION2"));
	    return features;
	}
	
	public static List<Feature> extractForNode(JCas jCas, IdentifiedAnnotation mention, String ftrPrefix) {
	    List<Feature> features = new ArrayList<Feature>();
	    ConllDependencyNode mentionHeadNode = DependencyParseUtils.findAnnotationHead(jCas, mention);
		
	    if (mentionHeadNode != null) {
	    	ConllDependencyNode dependsOn = mentionHeadNode.getHead();
	    	if (dependsOn != null) {
	    		features.add(new Feature(ftrPrefix + "_DEPENDS_ON_WORD", dependsOn.getCoveredText()));
	    		features.add(new Feature(ftrPrefix + "_DEPENDS_ON_POS", dependsOn.getPostag()));
	    		// Following features come from Zhou et al. 2005
	    		// ET1DW1: combination of the entity type and the dependent word for M1
	    		features.add(new Feature(ftrPrefix + "_TYPE-GOVERNING_WORD", String.format("%d-%s", mention.getTypeID(), dependsOn.getCoveredText())));
	    		// H1DW1: combination of the head word and the dependent word for M1 
	    		features.add(new Feature(ftrPrefix + "_HEAD_WORD-GOVERNING_WORD", String.format("%s-%s", mentionHeadNode.getCoveredText(), dependsOn.getCoveredText())));
	    		features.add(new Feature(ftrPrefix + "_TYPE-GOVERNING_POS", String.format("%d-%s", mention.getTypeID(), dependsOn.getPostag())));
	    		features.add(new Feature(ftrPrefix + "_HEAD_POS-GOVERNING_POS", String.format("%s-%s", mentionHeadNode.getPostag(), dependsOn.getPostag())));
	    	}
	    }
	    return features;
	}
	

}
