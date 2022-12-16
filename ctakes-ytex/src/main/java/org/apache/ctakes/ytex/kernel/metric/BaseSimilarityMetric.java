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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class BaseSimilarityMetric implements SimilarityMetric {

	protected ConceptSimilarityService simSvc;

	public ConceptSimilarityService getConceptSimilarityService() {
		return simSvc;
	}

	public void setConceptSimilarityService(
			ConceptSimilarityService conceptSimilarityService) {
		this.simSvc = conceptSimilarityService;
	}

	/**
	 * compute the lcses and min path distance for the concept pair, if this
	 * hasn't been done already
	 * 
	 * @param concept1
	 * @param concept2
	 * @param simInfo
	 */
	protected void initLCSes(String concept1, String concept2,
			SimilarityInfo simInfo) {
		if (simInfo.getLcsDist() == null) {
			simInfo.setLcsDist(simSvc.getLCS(concept1, concept2,
					simInfo.getLcses(), simInfo.getLcsPaths()));
		}
	}

	/**
	 * get the best lcs and its information content if this hasn't been done
	 * already.
	 * 
	 * @param conceptFilter
	 * @param simInfo
	 * @param intrinsicIC
	 *            set to false for corpus based ic
	 * @return
	 */
	protected double initLcsIC(Map<String, Double> conceptFilter,
			SimilarityInfo simInfo, boolean intrinsicIC) {
		Double lcsIC = intrinsicIC ? simInfo.getIntrinsicLcsIC() : simInfo
				.getCorpusLcsIC();
		if (lcsIC == null) {
			String lcs = null;
			lcsIC = 0d;
			Object[] bestLCSArr = simSvc.getBestLCS(simInfo.getLcses(),
					intrinsicIC, conceptFilter);
			if (bestLCSArr != null) {
				lcs = (String) bestLCSArr[0];
				lcsIC = (Double) bestLCSArr[1];
				if (intrinsicIC) {
					simInfo.setIntrinsicLcs(lcs);
					simInfo.setIntrinsicLcsIC(lcsIC);
				} else {
					simInfo.setCorpusLcs(lcs);
					simInfo.setCorpusLcsIC(lcsIC);
				}
			}
		}
		return lcsIC;
	}

	/**
	 * call initLCSes and initLcsIC
	 * 
	 * @param concept1
	 * @param concept2
	 * @param conceptFilter
	 * @param simInfo
	 * @param intrinsicIC
	 * @return
	 */
	protected double initLcsIC(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo,
			boolean intrinsicIC) {
		this.initLCSes(concept1, concept2, simInfo);
		return this.initLcsIC(conceptFilter, simInfo, intrinsicIC);
	}

	public BaseSimilarityMetric(ConceptSimilarityService simSvc) {
		this.simSvc = simSvc;
	}

}
