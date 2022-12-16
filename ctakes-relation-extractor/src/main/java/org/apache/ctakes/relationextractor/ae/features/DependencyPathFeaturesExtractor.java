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
import java.util.LinkedList;
import java.util.List;

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class DependencyPathFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {
	
	
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		
		List<Feature> features = new ArrayList<Feature>();
		
	    ConllDependencyNode node1 = DependencyParseUtils.findAnnotationHead(jCas, arg1);
	    ConllDependencyNode node2 = DependencyParseUtils.findAnnotationHead(jCas, arg2);
	    if (node1 == null || node2 == null) { return features; }
	    
	    List<LinkedList<ConllDependencyNode>> paths = DependencyParseUtils.getPathsToCommonAncestor(node1, node2);
	    LinkedList<ConllDependencyNode> path1 = paths.get(0);
	    LinkedList<ConllDependencyNode> path2 = paths.get(1);
	    
	    features.add(new Feature("DEPENDENCY_PATH_MEAN_DISTANCE_TO_COMMON_ANCESTOR", (path1.size() + path2.size()) / 2.0));
	    features.add(new Feature("DEPENDENCY_PATH_MAX_DISTANCE_TO_COMMON_ANCESTOR", Math.max(path1.size(), path2.size())));
	    features.add(new Feature("DEPENDENCY_PATH_MIN_DISTANCE_TO_COMMON_ANCESTOR", Math.min(path1.size(), path2.size())));
	    
	    LinkedList<ConllDependencyNode> node1ToNode2Path = DependencyParseUtils.getPathBetweenNodes(node1, node2);
	    features.add(new Feature("DEPENDENCY_PATH", DependencyParseUtils.pathToString(node1ToNode2Path)));
	    
	    return features;
	}

}
