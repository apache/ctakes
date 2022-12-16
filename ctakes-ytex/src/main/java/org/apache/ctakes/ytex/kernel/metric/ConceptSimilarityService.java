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
package org.apache.ctakes.ytex.kernel.metric;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.ytex.kernel.model.ConceptGraph;


public interface ConceptSimilarityService {

	public enum SimilarityMetricEnum {
		LCH(false, false), INTRINSIC_LCH(true, false), LIN(false, true), INTRINSIC_LIN(
				true, false), PATH(false, false), INTRINSIC_PATH(true, false), JACCARD(
				true, false), SOKAL(true, false), RADA(false, false), INTRINSIC_RADA(
				true, false), WUPALMER(false, false), PAGERANK(false, false);
		boolean intrinsicIC = false;
		boolean corpusIC = false;

		/**
		 * is this measure taxonomy based?
		 * 
		 * @return
		 */
		public boolean isTaxonomy() {
			return !intrinsicIC && !corpusIC;
		}

		/**
		 * is this measure based on intrinsic IC?
		 * 
		 * @return
		 */
		public boolean isIntrinsicIC() {
			return intrinsicIC;
		}

		/**
		 * is this measure based on corpus IC?
		 * 
		 * @return
		 */
		public boolean isCorpusIC() {
			return corpusIC;
		}

		SimilarityMetricEnum(boolean intrinsicIC, boolean corpusIC) {
			this.intrinsicIC = intrinsicIC;
			this.corpusIC = corpusIC;
		}
	}

	public String getConceptGraphName();

	// public abstract double lch(String concept1, String concept2);

	// public abstract double lin(String concept1, String concept2);

	public int lcs(String concept1, String concept2, List<LCSPath> lcsPath);

	public abstract ConceptGraph getConceptGraph();

	/**
	 * cui - tui map. tuis are bitsets, indices correspond to tuis in
	 * {@link #getTuiList()}
	 * 
	 * @return
	 */
	public abstract Map<String, BitSet> getCuiTuiMap();

	// /**
	// * supervised lin measure.
	// *
	// * @param concept1
	// * @param concept2
	// * @param conceptFilter
	// * map of concept id to imputed infogain. if the concept isn't in
	// * this map, the concepts won't be compared. null for
	// * unsupervised lin.
	// * @return
	// */
	// public abstract double filteredLin(String concept1, String concept2,
	// Map<String, Double> conceptFilter);

	/**
	 * list of tuis that corresponds to bitset indices
	 * 
	 * @return
	 */
	public abstract List<String> getTuiList();

	/**
	 * For the given label and cutoff, get the corresponding concepts whose
	 * propagated ig meets the threshold. Used by lin kernel to find concepts
	 * that actually have a non-trivial similarity
	 * 
	 * @param label
	 *            label
	 * @param rankCutoff
	 *            cutoff
	 * @param conceptFilter
	 *            set to fill with concepts
	 * @return double minimum evaluation
	 */
	public abstract double loadConceptFilter(String label, int rankCutoff,
			Map<String, Double> conceptFilter);

	/**
	 * get the lcs(s) for the specified concepts
	 * 
	 * @param concept1
	 *            required
	 * @param concept2
	 *            required
	 * @param lcses
	 *            required - will be filled with the lcs(s).
	 * @param lcsPathMap
	 *            optional - will be filled with lcs and paths through the
	 *            lcses.
	 * @return distance of path through lcs
	 */
	public int getLCS(String concept1, String concept2, Set<String> lcses,
			List<LCSPath> lcsPaths);

	/**
	 * get the best lcs
	 * 
	 * @param lcses
	 *            set of lcses
	 * @param intrinsicIC
	 *            should the intrinsic ic be used? false - use corpus-based ic.
	 *            For multiple lcses not using concept filter, use the lcs with
	 *            the lowest infocontent
	 * @param conceptFilter
	 *            limit to lcses in the concept filter. The lcs with the highest
	 *            value will be used.
	 * @return array with 2 entries. Entry 1 - lcs (String). Entry 2 -
	 *         infocontent (double). Null if no lcses are in the concept filter.
	 */
	public Object[] getBestLCS(Set<String> lcses, boolean intrinsicIC,
			Map<String, Double> conceptFilter);

	public abstract double getIC(String concept, boolean intrinsicICMap);

	/**
	 * compute similarity for a pair of concepts
	 * 
	 * @param metrics
	 *            required, similarity metrics to compute
	 * @param concept1
	 *            required
	 * @param concept2
	 *            required
	 * @param conceptFilter
	 *            optional - only lcs's in this set will be used.
	 * @param simInfo
	 *            optional - pass this to get information on lcs. Instantiate
	 *            the lcsPathMap to get paths through lcs
	 * @return similarities
	 */
	public abstract ConceptPairSimilarity similarity(
			List<SimilarityMetricEnum> metrics, String concept1,
			String concept2, Map<String, Double> conceptFilter, boolean lcs);

	/**
	 * compute similarity for a list of concept pairs
	 * 
	 * @param conceptPairs
	 *            required, concept pairs for which similarity should be
	 *            computed
	 * @param metrics
	 *            required, similarity metrics to compute
	 * @param conceptFilter
	 *            optional - only lcs's in this set will be used.
	 * @param simInfos
	 *            optional - if provided, this list will be filled with the
	 *            similarity info for each concept pair.
	 * @return similarities
	 */
	public List<ConceptPairSimilarity> similarity(
			List<ConceptPair> conceptPairs, List<SimilarityMetricEnum> metrics,
			Map<String, Double> conceptFilter, boolean lcs);

	public abstract int getDepth(String concept);
}