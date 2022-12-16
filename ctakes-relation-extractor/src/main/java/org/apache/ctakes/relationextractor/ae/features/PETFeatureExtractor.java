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
package org.apache.ctakes.relationextractor.ae.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.constituency.parser.treekernel.TreeExtractor;
import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class PETFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	// Returns Path-enclosed trees of Moschitti 2004 (ACL)
	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = new ArrayList<Feature>();
		// first get the root and print it out...
		TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(jcas, AnnotationTreeUtils.getAnnotationTree(jcas, arg1));
		
//		SimpleTree tempClone = TreeExtractor.getSimpleClone(root);
//		features.add(new Feature("TK_FULL", tempClone.toString()));
		TreebankNode t1 = AnnotationTreeUtils.insertAnnotationNode(jcas, root, arg1, "ARG1");
		TreebankNode t2 = AnnotationTreeUtils.insertAnnotationNode(jcas, root, arg2, "ARG2");

		SimpleTree tree = null;
		if(t1.getBegin() <= t2.getBegin() && t1.getEnd() >= t2.getEnd()){
			// t1 encloses t2
			tree = TreeExtractor.getSimpleClone(t1);
		}else if(t2.getBegin() <= t1.getBegin() && t2.getEnd() >= t1.getEnd()){
			// t2 encloses t1
			tree = TreeExtractor.getSimpleClone(t2);
		}else{
			tree = TreeExtractor.extractPathEnclosedTree(t1, t2, jcas);
		}

		features.add(new Feature("TK_PET", tree.toString()));
		return features;
	}

}
