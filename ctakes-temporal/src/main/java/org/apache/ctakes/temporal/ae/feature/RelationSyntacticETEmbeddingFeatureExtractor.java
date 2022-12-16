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
package org.apache.ctakes.temporal.ae.feature;

import org.apache.ctakes.constituency.parser.treekernel.TreeExtractor;
import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Word embedding based features.
 */
public class RelationSyntacticETEmbeddingFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation, IdentifiedAnnotation> {

	private int numberOfDimensions;
	private WordEmbeddings paths = null;

	public RelationSyntacticETEmbeddingFeatureExtractor(String vecFile) throws
	CleartkExtractorException {
		try {
			paths =
					WordVectorReader.getEmbeddings(FileLocator.getAsStream(vecFile));
		} catch (IOException e) {
			e.printStackTrace();
			throw new CleartkExtractorException(e);
		}
		numberOfDimensions = paths.getDimensionality();
	}

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {

		List<Feature> features = new ArrayList<>();

		//get the PET tree between arg1 and arg2:
		// first get the root and print it out...
		TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(jCas, AnnotationTreeUtils.getAnnotationTree(jCas, arg1));

		if(root == null){
			return features; 
		}

		TreebankNode t1 = AnnotationTreeUtils.annotationNode(jCas, arg1);
		TreebankNode t2 = AnnotationTreeUtils.annotationNode(jCas, arg2);

		if ( t1 == null || t2 == null ) {
			return features;
		}
		//		addOtherTimes(jcas,root, arg1, arg2);

		// words between argument features
		List<String> pathsBetweenArgs = new ArrayList<>();

		//		SimpleTree tree = null;
		if(t1.getBegin() <= t2.getBegin() && t1.getEnd() >= t2.getEnd()){
			// t1 encloses t2
			//			tree = TreeExtractor.getSimpleClone(t1);
			pathsBetweenArgs.add(getPathBetweenNodes(t2, t1,""));
		}else if(t2.getBegin() <= t1.getBegin() && t2.getEnd() >= t1.getEnd()){
			// t2 encloses t1
			//			tree = TreeExtractor.getSimpleClone(t2);
			pathsBetweenArgs.add(getPathBetweenNodes(t1, t2,""));
		}else{
			//			tree = TreeExtractor.extractPathEnclosedTree(t1, t2, jCas);
			TreebankNode lca = TreeExtractor.getLCA(t1, t2);
			pathsBetweenArgs.add(getPathBetweenNodes(t1, lca,""));
			pathsBetweenArgs.add(getPathBetweenNodes(t2, lca,""));
		}

		List<Double> sum = new ArrayList<>(Collections.nCopies(numberOfDimensions, 0.0));
		for(String path : pathsBetweenArgs) {
			WordVector wordVector;
			if(paths.containsKey(path)) {
				wordVector = paths.getVector(path);
			} else {
				while(!paths.containsKey(path)){
					String trimmedPath = removeTail(path);
					if(trimmedPath==null){
						break;
					}
					path = trimmedPath;
				}
				if(paths.containsKey(path)){
					wordVector = paths.getVector(path);
				}else{
					wordVector = paths.getVector("<unk>");
				}
			}
			sum = addVectors(sum, wordVector);      
		}

		for(int dim = 0; dim < numberOfDimensions; dim++) {
			String featureName = String.format("syntactic_average_dim_%d", dim);
			features.add(new Feature(featureName, sum.get(dim) / pathsBetweenArgs.size()));
		}

		return features;
	}

	private static String removeTail(String path) {
		int dashIdx = path.lastIndexOf("-");
		if(dashIdx>0){
			path = path.substring(0, dashIdx);
			return path;
		}
		return null;
	}

	private String getPathBetweenNodes(TreebankNode child, TreebankNode ancestor, String path) {
		TreebankNode father = child.getParent();
		if("".equals(path)){
			path = child.getNodeType();
		}else{
			path = child.getNodeType()+"-"+path;
		}
		if(father == null){
			return path;
		}else if(father == ancestor){
			path = father.getNodeType() + "-" + path;
			return path;
		}
		return getPathBetweenNodes(father, ancestor, path);
	}


	/**
	 * Compute cosine similarity between two vectors.
	 */
	public double computeCosineSimilarity(WordVector vector1, WordVector vector2) {

		double dotProduct = 0.0;
		double norm1 = 0.01;
		double norm2 = 0.01;

		for (int dim = 0; dim < numberOfDimensions; dim++) {
			dotProduct = dotProduct + vector1.getValue(dim) * vector2.getValue(dim);
			norm1 = norm1 + Math.pow(vector1.getValue(dim), 2);
			norm2 = norm2 + Math.pow(vector2.getValue(dim), 2);
		}

		return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
	}

	public double computeCosineSimilarity(List<Double> vector1, List<Double> vector2) {

		double dotProduct = 0.0;
		double norm1 = 0.01;
		double norm2 = 0.01;

		for (int dim = 0; dim < numberOfDimensions; dim++) {
			dotProduct = dotProduct + vector1.get(dim) * vector2.get(dim);
			norm1 = norm1 + Math.pow(vector1.get(dim), 2);
			norm2 = norm2 + Math.pow(vector2.get(dim), 2);
		}

		return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
	}

	/**
	 * Add two vectors. Return the sum vector.
	 */
	public List<Double> addVectors(List<Double> vector1, WordVector vector2) {

		List<Double> sum = new ArrayList<>();
		for(int dim = 0; dim < numberOfDimensions; dim++) {
			sum.add(vector1.get(dim) + vector2.getValue(dim));
		}

		return sum;
	}
}
