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
package org.apache.ctakes.ytex.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Data structure populated by AbstractBagOfWordsExporter that has all the
 * instance attributes needed for exporting to various formats.
 * 
 * @author vijay
 * 
 */
public class BagOfWordsData {
	/**
	 * should we perform tf-idf normalization?
	 */
	boolean tfIdf;
	/**
	 * Map of instance id to class label
	 */
	Map<Integer, String> documentClasses = new HashMap<Integer, String>();
	/**
	 * class labels
	 */
	SortedSet<String> classes = new TreeSet<String>();
	/**
	 * numeric attribute labels
	 */
	SortedSet<String> numericWords = new TreeSet<String>();
	/**
	 * map if instance id to map of attribute name - value pairs
	 */
	Map<Integer, SortedMap<String, Double>> instanceNumericWords = new HashMap<Integer, SortedMap<String, Double>>();
	/**
	 * instance nominal attribute values
	 */
	Map<Integer, SortedMap<String, String>> instanceNominalWords = new HashMap<Integer, SortedMap<String, String>>();
	/**
	 * nominal attribute names and values
	 */
	SortedMap<String, SortedSet<String>> nominalWordValueMap = new TreeMap<String, SortedSet<String>>();
	/**
	 * for tf-idf, length of each instance
	 */
	Map<Integer, Integer> docLengthMap = new HashMap<Integer, Integer>();
	/**
	 * for tf-idf, term-document count map
	 */
	Map<String, Integer> idfMap = new HashMap<String, Integer>();

	public Map<Integer, String> getDocumentClasses() {
		return documentClasses;
	}

	public void setDocumentClasses(Map<Integer, String> documentClasses) {
		this.documentClasses = documentClasses;
	}

	public SortedSet<String> getClasses() {
		return classes;
	}

	public void setClasses(SortedSet<String> classes) {
		this.classes = classes;
	}

	public SortedSet<String> getNumericWords() {
		return numericWords;
	}

	public void setNumericWords(SortedSet<String> numericWords) {
		this.numericWords = numericWords;
	}

	public Map<Integer, SortedMap<String, Double>> getInstanceNumericWords() {
		return instanceNumericWords;
	}

	public void setInstanceNumericWords(
			Map<Integer, SortedMap<String, Double>> instanceNumericWords) {
		this.instanceNumericWords = instanceNumericWords;
	}

	public Map<Integer, SortedMap<String, String>> getInstanceNominalWords() {
		return instanceNominalWords;
	}

	public void setInstanceNominalWords(
			Map<Integer, SortedMap<String, String>> instanceNominalWords) {
		this.instanceNominalWords = instanceNominalWords;
	}

	public SortedMap<String, SortedSet<String>> getNominalWordValueMap() {
		return nominalWordValueMap;
	}

	public void setNominalWordValueMap(
			SortedMap<String, SortedSet<String>> nominalWordValueMap) {
		this.nominalWordValueMap = nominalWordValueMap;
	}

	public Map<Integer, Integer> getDocLengthMap() {
		return docLengthMap;
	}

	public void setDocLengthMap(Map<Integer, Integer> docLengthMap) {
		this.docLengthMap = docLengthMap;
	}

	public Map<String, Integer> getIdfMap() {
		return idfMap;
	}

	public void setIdfMap(Map<String, Integer> idfMap) {
		this.idfMap = idfMap;
	}

	public boolean isTfIdf() {
		return tfIdf;
	}

	public void setTfIdf(boolean tfIdf) {
		this.tfIdf = tfIdf;
	}

}
