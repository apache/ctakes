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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.apache.ctakes.constituency.parser.treekernel.TreeExtractor;
import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.FragmentUtils;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

/* 
 * This class implements a ClearTK feature extractor for tree kernel fragment features
 * as derived using the flink toolkit (http://danielepighin.net/cms/software/flink).
 * Model location is hardcoded as of right now.
 * TODO: Parameterize & unstaticize this so that, e.g., multiple projects could use this feature if necessary.
 */
public class TreeFragmentFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	static HashSet<SimpleTree> frags = null;

	public TreeFragmentFeatureExtractor(){
		if(frags == null) initializeFrags();
	}


	private static void initializeFrags(){
		frags = new HashSet<SimpleTree>();
		try{
//			File fragsFile = FileLocator.locateFile("resources/frags_args.txt");
			File fragsFile = FileLocator.getFile("org/apache/ctakes/relationextractor/frags_nolex_args.txt");
			Scanner scanner = new Scanner(fragsFile);
			while(scanner.hasNextLine()){
				frags.add(FragmentUtils.frag2tree(scanner.nextLine().trim()));
			}
		}catch(FileNotFoundException e){
			System.err.println("Missing fragment file!");
		}
	}

	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = new ArrayList<Feature>();
		// first get the root and print it out...
		TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(jcas, AnnotationTreeUtils.getAnnotationTree(jcas, arg1));
//		SimpleTree tempClone = TreeExtractor.getSimpleClone(root);
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

		for(SimpleTree frag : frags){
			if(TreeUtils.containsIgnoreCase(tree, frag)){
				features.add(new Feature("TK_" + frag.toString()));
				//			}else{
				//				features.add(new Feature(frag.toString(), false));
			}
		}

		return features;
	}

}
