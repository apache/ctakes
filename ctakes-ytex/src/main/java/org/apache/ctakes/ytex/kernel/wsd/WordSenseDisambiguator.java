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
package org.apache.ctakes.ytex.kernel.wsd;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;


public interface WordSenseDisambiguator {

	public abstract String disambiguate(List<Set<String>> sentenceConcepts,
			int index, Set<String> contextConcepts, int windowSize,
			SimilarityMetricEnum metric, Map<String, Double> scoreMap);

	/**
	 * Disambiguate a named entity.
	 * 
	 * @param sentenceConcepts
	 *            named entities from the document, represented as list of
	 *            sets of concept ids
	 * @param index
	 *            index of target named entity to disambiguate
	 * @param contextConcepts
	 *            context concepts, e.g. from title
	 * @param windowSize
	 *            number of named entities on either side of target to use for
	 *            disambiguation
	 * @param metric
	 *            metric to use
	 * @param scoreMap
	 *            optional to get the scores assigned to each concept
	 * @param weighted
	 *            to weight context concepts by frequency
	 * @return highest scoring concept, or null if none of the target concepts
	 *         are in the concept graph, or if all the target concepts have the
	 *         same score
	 */
	String disambiguate(List<Set<String>> sentenceConcepts, int index,
			Set<String> contextConcepts, int windowSize,
			SimilarityMetricEnum metric, Map<String, Double> scoreMap,
			boolean weighted);

}