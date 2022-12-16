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
package org.apache.ctakes.relationextractor.pipelines;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.constituency.parser.treekernel.TreeExtractor;
import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;

/*
 * This class is for writing trees to a file which are then used by a tree-kernel
 * SVM for training/model building.  
 * 
 * @author Tim Miller (timothy.miller@childrens.harvard.edu)
 */
public class TreeFeatureConsumer extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
	private static final String NO_RELATION_CATEGORY = "-NONE-";
	public static final String PARAM_OUTFILE = "outputFilename";
	public static final String PARAM_CLASSIFY_BOTH_DIRECTIONS = "ClassifyBothDirections";
	private PrintWriter out = null;
	PrintWriter rels = null;
	PrintWriter trees = null;
	int docNum = 0;
	int lineNum = 0;
	
	@ConfigurationParameter(
			name = PARAM_OUTFILE,
			mandatory = true,
			description = "The file of tree examples in svm-light-tk format.")
			private String outputFile;

	protected final boolean classifyBothDirections = false;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			out = new PrintWriter(outputFile);
			String relFilename = (new File(outputFile)).getParent() + "/rels.txt";
			rels = new PrintWriter(relFilename);
			String treeFilename = (new File(outputFile)).getParent()+ "/wholetrees.txt";
			trees = new PrintWriter(treeFilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new ResourceInitializationException();
		}
	}
	
	@Override
	public void process(JCas jcas) {
		// lookup from pair of annotations to binary text relation
		// note: assumes that there will be at most one relation per pair
		JCas goldView = null;
		try {
			goldView = jcas.getView("GoldView");
		} catch (CASException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<List<Annotation>, BinaryTextRelation> relationLookup;
		relationLookup = new HashMap<List<Annotation>, BinaryTextRelation>();
		for (BinaryTextRelation relation: JCasUtil.select(goldView, BinaryTextRelation.class)) {
			  Annotation arg1, arg2;
			  if (relation.getArg1().getRole().equals("Argument")) {
				  arg1 = relation.getArg1().getArgument();
				  arg2 = relation.getArg2().getArgument();
			  } else {
				  arg2 = relation.getArg1().getArgument();
				  arg1 = relation.getArg2().getArgument();
			  }
//			Annotation arg1 = relation.getArg1().getArgument();
//			Annotation arg2 = relation.getArg2().getArgument();
			List<Annotation> key = new ArrayList<Annotation>(Arrays.asList(arg1, arg2));
			relationLookup.put(key, relation);
		}

		// walk through each sentence in the text
		for (Sentence sentence : JCasUtil.select(jcas, Sentence.class)) {

			// collect all possible relation arguments from the sentence
			List<EntityMention> args = JCasUtil.selectCovered(
					goldView,
					EntityMention.class,
					sentence);

			// walk through the pairs
			for (int i = 0; i < args.size(); ++i) {
				EntityMention arg1 = args.get(i);
				int jStart = this.classifyBothDirections ? 0 : i + 1;
				for (int j = jStart; j < args.size(); ++j) {
					EntityMention arg2 = args.get(j);
					// skip identical entity mentions and mentions with identical spans
					if (i == j || (arg1.getBegin() == arg2.getBegin() && arg1.getEnd() == arg2.getEnd())) {
						continue;
					}
					// load the relation label from the CAS (if there is one)
					List<Annotation> key = new ArrayList<Annotation>(Arrays.asList(arg1, arg2));
					String category;
					if (relationLookup.containsKey(key)) {
						BinaryTextRelation relation = relationLookup.get(key);
						category = relation.getCategory();
					} else {
						// first check for opposite direction
						key = new ArrayList<Annotation>(Arrays.asList(arg2,arg1));
						if(relationLookup.containsKey(key)){
							// reverse direction
							BinaryTextRelation relation = relationLookup.get(key);
							category = relation.getCategory() + "-1";
						}else{
							category = NO_RELATION_CATEGORY;
						}
					}
					
					// first get the root and print it out...
					TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(jcas, AnnotationTreeUtils.getAnnotationTree(jcas, arg1));
					SimpleTree tempClone = TreeExtractor.getSimpleClone(root);
					trees.println(tempClone.toString());
//					TopTreebankNode t2 = AnnotationTreeUtils.getTreeCopy(AnnotationTreeUtils.getAnnotationTree(jcas, arg2));
					
					if(root == null){
						System.err.println("Root is null!");
					}
					TreebankNode t1 = AnnotationTreeUtils.insertAnnotationNode(jcas, root, arg1, "ARG1");
					TreebankNode t2 = AnnotationTreeUtils.insertAnnotationNode(jcas, root, arg2, "ARG2");
					String relationString = category + "(\"" + arg1.getCoveredText() + "\", \"" + arg2.getCoveredText() + "\")";
					rels.print("\t");
					rels.print(lineNum);
					rels.print("\t");
					rels.println(relationString);
					rels.flush();
					lineNum++;
//					root.setNodeType(category);
//					out.println(TreeExtractor.getSimpleClone(root));
//					out.flush();
					
					
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
					if(category.equals("location_of-1")){
						out.print("+1 |BT| ");
					}else{
						out.print("-1 |BT| ");
					}
					TreeExtractor.lowercaseWords(tree);
					out.print(tree.toString());
					out.println(" |ET|");
//					root.setNodeType("TOP");
					out.flush();
				}
			}
		}
	}
	
	@Override
	public void collectionProcessComplete()
			throws AnalysisEngineProcessException {
		// TODO Auto-generated method stub
		super.collectionProcessComplete();
		out.close();
		rels.close();
		trees.close();
	}
	
	public static void main(String[] args){
		if(args.length < 2){
			System.err.println("Usage: TreeFeatureConsumer <training data> <output file>");
			System.exit(-1);
		}
		File trainDir = new File(args[0]);
		if(!trainDir.isDirectory()){
			System.err.println("First arg should be a directory! (full of xmi files)");
			System.exit(-1);
		}
		File[] files = trainDir.listFiles();
		String[] paths = new String[files.length];
		for(int i = 0; i < files.length; i++){
			paths[i] = files[i].getAbsolutePath();
		}
//		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath("../common-type-system/desc/common_type_system.xml");
		try {
			CollectionReader xmiReader = CollectionReaderFactory.createReader(XMIReader.class, XMIReader.PARAM_FILES, paths);
			AnalysisEngine treeConsumer = AnalysisEngineFactory.createEngine(TreeFeatureConsumer.class, 
													TreeFeatureConsumer.PARAM_OUTFILE, args[1], 
													TreeFeatureConsumer.PARAM_CLASSIFY_BOTH_DIRECTIONS, true);
			SimplePipeline.runPipeline(xmiReader, treeConsumer);
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		} catch (UIMAException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
