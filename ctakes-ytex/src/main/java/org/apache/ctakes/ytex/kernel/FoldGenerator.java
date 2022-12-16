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

import java.util.SortedMap;

public interface FoldGenerator {

	/**
	 * Generate cross validation folds, store in database.
	 * 
	 * @param corpusName
	 *            class label
	 * @param query
	 *            query to get instance id - label - class triples
	 * @param nFolds
	 *            number of folds to generate
	 * @param nMinPerClass
	 *            minimum number of instances of each class per fold
	 * @param nSeed
	 *            random number seed; if null will be set currentTime in millis
	 * @param nRuns
	 *            number of runs
	 */
	public abstract void generateRuns(String corpusName, String splitName,
			String query, int nFolds, int nMinPerClass, Integer nSeed, int nRuns);

	/**
	 * Generate cross validation folds, don't store in database.
	 * 
	 * @param labelToInstanceMap
	 *            an instance class map without folds @see
	 *            {@link InstanceData#labelToInstanceMap}
	 * @param nFolds
	 *            number of folds
	 * @param nMinPerClass
	 *            minimum instance per class
	 * @param nSeed
	 *            random seed default to System.currentTimeMillis()
	 * @param nRuns
	 *            number of runs
	 * @param foldMap
	 *            same structure as labelToInstanceMap, but with folds
	 */
	public SortedMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> generateRuns(
			SortedMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> labelToInstanceMap,
			int nFolds,
			int nMinPerClass,
			Integer nSeed,
			int nRuns);
}