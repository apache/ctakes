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
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.TreeFeature;

public class TemporalFTExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	public static final String FEAT_NAME = "TK_FT";
	
	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = new ArrayList<Feature>();
		// first get the root and print it out...
		TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(jcas, AnnotationTreeUtils.getAnnotationTree(jcas, arg1));
		
		if(root == null){
			SimpleTree fakeTree = new SimpleTree("(S (NN null))");
			features.add(new TreeFeature(FEAT_NAME, fakeTree.toString()));
			return features;
		}
		
		// swap the order if necessary:
		if(arg2.getBegin() <= arg1.getBegin() && arg2.getEnd() <= arg1.getEnd()){
			IdentifiedAnnotation temp = arg1;
			arg1 = arg2;
			arg2 = temp;
		}
		
		String a1type="", a2type="";
		String eventModality="";
		String timeClass;
		
		if(arg1 instanceof EventMention){
			eventModality = ((EventMention)arg1).getEvent().getProperties().getContextualModality();
			a1type = "EVENT-"+eventModality;
		}else if(arg1 instanceof TimeMention){
			timeClass = ((TimeMention)arg1).getTimeClass();
			a1type = "TIMEX-"+timeClass;
		}
		
		if(arg2 instanceof EventMention){
      eventModality = ((EventMention)arg2).getEvent().getProperties().getContextualModality();
      a2type = "EVENT-"+eventModality;		  
		}else if(arg2 instanceof TimeMention){
      timeClass = ((TimeMention)arg2).getTimeClass();
      a2type = "TIMEX-"+timeClass;		  
		}
		
		TreebankNode t1 = AnnotationTreeUtils.insertAnnotationNode(jcas, root, arg1, "ARG1-"+a1type);
		TreebankNode t2 = AnnotationTreeUtils.insertAnnotationNode(jcas, root, arg2, "ARG2-"+a2type);

//		addOtherTimes(jcas,root, arg1, arg2);
				
		SimpleTree tree = null;
		if(t1.getBegin() <= t2.getBegin() && t1.getEnd() >= t2.getEnd()){
			// t1 encloses t2
			tree = TreeExtractor.getSimpleClone(t1);
		}else if(t2.getBegin() <= t1.getBegin() && t2.getEnd() >= t1.getEnd()){
			// t2 encloses t1
			tree = TreeExtractor.getSimpleClone(t2);
		}else{
//			tree = TreeExtractor.extractPathEnclosedTree(t1, t2, jcas);
			tree = TreeExtractor.getSimpleClone(root);
		}

		TemporalPETExtractor.moveTimexDownToNP(tree);
		
		features.add(new TreeFeature(FEAT_NAME, tree.toString()));
		return features;
	}

}
