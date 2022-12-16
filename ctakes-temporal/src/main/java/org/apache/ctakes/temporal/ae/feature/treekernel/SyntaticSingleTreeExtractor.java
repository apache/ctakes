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
package org.apache.ctakes.temporal.ae.feature.treekernel;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.constituency.parser.treekernel.TreeExtractor;
import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.TreeFeature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
/**
 * Given a focused annotation, get the whole sentence-level parse tree that cover this annotation.
 * @author CH151862
 *
 */
public class SyntaticSingleTreeExtractor implements FeatureExtractor1 {

	public static final String FEAT_NAME = "TK_SingleT";

	@Override
	public List<Feature> extract(JCas view, Annotation focusAnnotation)
			throws CleartkExtractorException {
		List<Feature> features = new ArrayList<Feature>();
		// first get the root and print it out...
		TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(view, AnnotationTreeUtils.getAnnotationTree(view, focusAnnotation));

		if(root == null){
			SimpleTree fakeTree = new SimpleTree("(S (NN null))");
			features.add(new TreeFeature(FEAT_NAME, fakeTree.toString()));
			return features;
		}


		String etype="";
		String eventModality="";

		if(focusAnnotation instanceof EventMention){
			eventModality = ((EventMention)focusAnnotation).getEvent().getProperties().getContextualModality();
			etype = "EVENT-"+eventModality;
			AnnotationTreeUtils.insertAnnotationNode(view, root, focusAnnotation, etype);
		}
	
		SimpleTree tree = null;
		tree = TreeExtractor.getSimpleClone(root);

		TemporalPETExtractor.moveTimexDownToNP(tree);

		features.add(new TreeFeature(FEAT_NAME, tree.toString()));
		return features;
	}

}
