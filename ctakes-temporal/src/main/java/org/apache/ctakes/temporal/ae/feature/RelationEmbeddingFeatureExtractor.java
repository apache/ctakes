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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.distsem.WordEmbeddings;
import org.apache.ctakes.utils.distsem.WordVector;
import org.apache.ctakes.utils.distsem.WordVectorReader;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;

/**
 * Word embedding based features.
 */
public class RelationEmbeddingFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation, IdentifiedAnnotation> {

	private int numberOfDimensions;
	private WordEmbeddings words = null;

	public RelationEmbeddingFeatureExtractor(String vecFile) throws
	CleartkExtractorException {
		try {
			words =
					WordVectorReader.getEmbeddings(FileLocator.getAsStream(vecFile));
		} catch (IOException e) {
			e.printStackTrace();
			throw new CleartkExtractorException(e);
		}
		numberOfDimensions = words.getDimensionality();
	}

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {

		List<Feature> features = new ArrayList<>();

		List<WordToken> preWords = null;
		List<WordToken> afterWords = null;
		List<WordToken> wordsOfArgs1 = null;
		List<WordToken> wordsOfArgs2 = null;
		if(arg1.getBegin() < arg2.getBegin()){//if arg1 before arg2
			preWords = JCasUtil.selectPreceding(jCas, WordToken.class, arg1, 2);
			afterWords = JCasUtil.selectFollowing(jCas, WordToken.class, arg2, 2);
			wordsOfArgs1 = JCasUtil.selectCovered(jCas, WordToken.class, arg1);
			wordsOfArgs2 = JCasUtil.selectCovered(jCas, WordToken.class, arg2); 
		}else{
			preWords = JCasUtil.selectPreceding(jCas, WordToken.class, arg2, 2);
			afterWords = JCasUtil.selectFollowing(jCas, WordToken.class, arg1, 2);
			wordsOfArgs1 = JCasUtil.selectCovered(jCas, WordToken.class, arg2);
			wordsOfArgs2 = JCasUtil.selectCovered(jCas, WordToken.class, arg1);
		}
		
		//get the 2 words before the first argument
		List<Double> sum = getSumVector(preWords);
		features = addFeatures(features, sum, preWords.size(), "pre");		

		// head words of the first argument features
		sum = getSumVector(wordsOfArgs1);
		features = addFeatures(features, sum, wordsOfArgs1.size(), "arg1");

		// words between argument features
		List<WordToken> wordsBetweenArgs = new ArrayList<>(JCasUtil.selectBetween(jCas, WordToken.class, arg1, arg2));
		sum = getSumVector(wordsBetweenArgs);
		features = addFeatures(features, sum, wordsBetweenArgs.size(), "inBetween");
		
		// head words of the 2nd argument features 
		sum = getSumVector(wordsOfArgs2);
		features = addFeatures(features, sum, wordsOfArgs2.size(), "arg2");
		
		//get the 2 words after the 2nd argument
		sum = getSumVector(afterWords);
		features = addFeatures(features, sum, afterWords.size(), "after");

		return features;
	}

	private List<Feature> addFeatures(List<Feature> features, List<Double> sum, int size, String field) {
		if(size == 0){
			for(int dim = 0; dim < numberOfDimensions; dim++) {
				String featureName = String.format(field+"_dim_%d", dim);
				features.add(new Feature(featureName, sum.get(dim)));
			}
		}else{
			for(int dim = 0; dim < numberOfDimensions; dim++) {
				String featureName = String.format(field+"_dim_%d", dim);
				features.add(new Feature(featureName, sum.get(dim) / size));
			}
		}
		return features;
	}

	private List<Double> getSumVector(List<WordToken> wordsInCheck) {
		List<Double> sum = new ArrayList<>(Collections.nCopies(numberOfDimensions, 0.0));
		for(WordToken wordToken : wordsInCheck) {
			WordVector wordVector;
			if(words.containsKey(wordToken.getCoveredText().toLowerCase())) {
				wordVector = words.getVector(wordToken.getCoveredText().toLowerCase());
			} else {
				wordVector = words.getVector("and");
			}
			sum = addVectors(sum, wordVector);      
		}
		return sum;
	}

	/**
	private List<Double> getGroupVector(List<WordToken> wordsOfArgs) {
		List<Double> argVec = new ArrayList<>(Collections.nCopies(numberOfDimensions, 0.0));

		for(WordToken wordToken : wordsOfArgs) {
			WordVector wordVector;
			if(words.containsKey(wordToken.getCoveredText().toLowerCase())) {
				wordVector = words.getVector(wordToken.getCoveredText().toLowerCase());
			} else {
				wordVector = words.getVector("and");
			}
			argVec = addVectors(argVec, wordVector);    
		}
		int numOfWords = wordsOfArgs.size();
		if( numOfWords > 1){
			for(int dim = 0; dim < numberOfDimensions; dim++) {
				argVec.set(dim, argVec.get(dim) /numOfWords) ;
			}
		}
		return argVec;
	}*/

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
