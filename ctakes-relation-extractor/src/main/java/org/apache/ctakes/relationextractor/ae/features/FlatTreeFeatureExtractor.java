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
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class FlatTreeFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	// Returns flat POS-trees a la Hovy et al 2012 (EACL)
	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = new ArrayList<Feature>();
		
		SimpleTree tree = null;
		tree = new SimpleTree("BOP");
		TreebankNode lca = TreeExtractor.getLCA(AnnotationTreeUtils.annotationNode(jcas, arg1),
												AnnotationTreeUtils.annotationNode(jcas, arg2));
				
		SimpleTree arg1Tree = new SimpleTree("ARG1");
		SimpleTree arg2Tree = new SimpleTree("ARG2");
		
		tree.addChild(arg1Tree);
		List<BaseToken> coveredTokens = JCasUtil.selectCovered(jcas, BaseToken.class, lca);
		for(BaseToken token : coveredTokens){
			// create pre-terminal tree
			SimpleTree tokenTree = new SimpleTree("TOK");
//			tokenTree.addChild(new SimpleTree(token.getCoveredText()));
			tokenTree.addChild(new SimpleTree(token.getPartOfSpeech()));
			
			// do we add this to one of the arg trees or to the root?
			if(token.getEnd() <= arg1.getEnd()){
				arg1Tree.addChild(tokenTree);
			}else if(token.getBegin() >= arg2.getBegin()){
				arg2Tree.addChild(tokenTree);
			}else{
				SimpleTree termTree = new SimpleTree("TERM");
				termTree.addChild(tokenTree);
				tree.addChild(termTree);
			}
		}
		tree.addChild(arg2Tree);
		
		features.add(new Feature("TK_BOP", tree.toString()));
		return features;
	}

}
