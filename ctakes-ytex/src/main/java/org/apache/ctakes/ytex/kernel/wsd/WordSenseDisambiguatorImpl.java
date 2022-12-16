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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.ctakes.ytex.kernel.metric.ConceptPairSimilarity;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import org.apache.ctakes.ytex.kernel.model.ConcRel;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;

public class WordSenseDisambiguatorImpl implements WordSenseDisambiguator {
	ConceptSimilarityService conceptSimilarityService;

	public ConceptSimilarityService getConceptSimilarityService() {
		return conceptSimilarityService;
	}

	public void setConceptSimilarityService(
			ConceptSimilarityService conceptSimilarityService) {
		this.conceptSimilarityService = conceptSimilarityService;
	}

	@Override
	public String disambiguate(List<Set<String>> sentenceConcepts, int index,
			Set<String> contextConcepts, int windowSize,
			SimilarityMetricEnum metric, Map<String, Double> scoreMap) {
		return disambiguate(sentenceConcepts, index, contextConcepts,
				windowSize, metric, scoreMap, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.kernel.wsd.WordSenseDisambiguator#disambiguate
	 * (java.util.List, int, java.util.Set, int,
	 * org.apache.ctakes.ytex.kernel.ConceptSimilarityService
	 * .SimilarityMetricEnum, java.util.Map)
	 */
	@Override
	public String disambiguate(List<Set<String>> sentenceConcepts, int index,
			Set<String> contextConcepts, int windowSize,
			SimilarityMetricEnum metric, Map<String, Double> scoreMap,
			boolean weighted) {
		// get the candidate concepts that we want to disambiguate
		Set<String> candidateConcepts = sentenceConcepts.get(index);
		if (candidateConcepts.size() == 1)
			return candidateConcepts.iterator().next();
		// allocate set to hold all the concepts to compare to
		Map<String, Integer> windowContextConcepts = new HashMap<String, Integer>();
		// add context concepts (e.g. title concepts)
		if (contextConcepts != null) {
			addConcepts(windowContextConcepts, contextConcepts);
		}
		// add windowSize concepts from the sentence
		// get left, then right concepts
		// case 1 - enough tokens on both sides
		int indexLeftStart = index - windowSize - 1;
		int indexRightStart = index + windowSize + 1;
		if (indexLeftStart < 0) {
			// case 2 - not enough tokens on left
			indexRightStart += (-1 * indexLeftStart);
			indexLeftStart = 0;
		} else if (indexRightStart >= sentenceConcepts.size()) {
			// case 3 - not enough tokens on right
			indexLeftStart -= indexRightStart - sentenceConcepts.size() - 1;
			indexRightStart = sentenceConcepts.size() - 1;
		}
		// make sure the range is in bounds
		if (indexLeftStart < 0)
			indexLeftStart = 0;
		if (indexRightStart >= sentenceConcepts.size())
			indexRightStart = sentenceConcepts.size() - 1;
		// add the concepts in the ranges
		if (indexLeftStart < index) {
			for (Set<String> cs : sentenceConcepts.subList(indexLeftStart,
					index)) {
				addConcepts(windowContextConcepts, cs);
			}
		}
		if (indexRightStart > index) {
			for (Set<String> cs : sentenceConcepts.subList(index + 1,
					indexRightStart + 1)) {
				addConcepts(windowContextConcepts, cs);
			}
		}
		// allocate map to hold scores
		TreeMultimap<Double, String> scoreConceptMap = TreeMultimap.create();
		for (String c : candidateConcepts) {
			scoreConceptMap
					.put(scoreConcept(c, windowContextConcepts, metric,
							weighted), c);
		}
		// if scoreMap is not null, fill it in with the concept scores - invert
		// scoreConceptMap
		boolean bNonZero = false;
		if (scoreMap != null) {
			for (Map.Entry<Double, String> scoreConcept : scoreConceptMap
					.entries()) {
				scoreMap.put(scoreConcept.getValue(), scoreConcept.getKey());
			}
		}
		SortedSet<String> bestConcepts = scoreConceptMap.get(scoreConceptMap
				.keySet().last());
		String bestConcept = null;
		if (bestConcepts.size() == 1) {
			// only 1 concept with high score
			bestConcept = bestConcepts.iterator().next();
		} else if (bestConcepts.size() == candidateConcepts.size()) {
			// all concepts have same score
			bestConcept = null;
		} else {
			// multiple best candidates - pick concept with lowest ic - most
			// general concept
			double ic = 1e6;
			Map<String, ConcRel> conceptMap = this
					.getConceptSimilarityService().getConceptGraph()
					.getConceptMap();
			for (String c : bestConcepts) {
				ConcRel cr = conceptMap.get(c);
				if (cr != null && cr.getIntrinsicInfoContent() < ic) {
					ic = cr.getIntrinsicInfoContent();
					bestConcept = c;
				}
			}
		}
		// get the best scoring concept
		return bestConcept;
	}

	private void addConcepts(Map<String, Integer> windowContextConcepts,
			Set<String> contextConcepts) {
		for (String c : contextConcepts) {
			Integer cn = windowContextConcepts.get(c);
			if (cn != null) {
				windowContextConcepts.put(c, cn + 1);
			} else {
				windowContextConcepts.put(c, 1);
			}
		}
	}

	private double scoreConcept(String concept,
			Map<String, Integer> windowContextConcepts,
			SimilarityMetricEnum metric, boolean weighted) {
		List<SimilarityMetricEnum> metrics = Arrays.asList(metric);
		double score = 0d;
		for (Map.Entry<String, Integer> windowConcept : windowContextConcepts
				.entrySet()) {
			ConceptPairSimilarity csim = conceptSimilarityService.similarity(
					metrics, concept, windowConcept.getKey(), null, false);
			if (weighted)
				score += csim.getSimilarities().get(0)
						* windowConcept.getValue().doubleValue();
			else
				score += csim.getSimilarities().get(0);
		}
		return score;
	}

}
