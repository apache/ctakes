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
package org.apache.ctakes.ytex.kernel.evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.metric.ConceptPairSimilarity;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import org.springframework.beans.factory.InitializingBean;


public class SemanticSimKernel extends CacheKernel implements InitializingBean {
	private static final Log log = LogFactory.getLog(LinKernel.class);
	private Map<String, Double> conceptFilter = null;
	private ConceptSimilarityService conceptSimilarityService;
	private double cutoff = 0;
	private String label = null;
	private String metricNames;
	private List<SimilarityMetricEnum> metrics;
	private Integer rankCutoff = null;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		this.initializeConceptFilter();
	}

	/**
	 * override CacheKernel - don't bother caching evaluation if the concepts
	 * are not in the conceptFilter, or if they are identical.
	 */
	@Override
	public double evaluate(Object o1, Object o2) {
		String c1 = (String) o1;
		String c2 = (String) o2;
		double d = 0;
		if (c1 != null && c2 != null) {
			if (c1.equals(c2)) {
				d = 1d;
			} else if (this.conceptFilter == null
					|| (conceptFilter.containsKey((String) o1) && conceptFilter
							.containsKey((String) o2))) {
				d = super.evaluate(o1, o2);
			}
		}
		return d;
	}

	public ConceptSimilarityService getConceptSimilarityService() {
		return conceptSimilarityService;
	}

	public double getCutoff() {
		return cutoff;
	}

	public String getLabel() {
		return label;
	}

	public String getMetricNames() {
		return metricNames;
	}

	public Integer getRankCutoff() {
		return rankCutoff;
	}

	protected void initializeConceptFilter() {
		if (rankCutoff != null) {
			conceptFilter = new HashMap<String, Double>();
			cutoff = conceptSimilarityService.loadConceptFilter(label,
					rankCutoff, conceptFilter);
			if (conceptFilter.isEmpty()) {
				log.warn("no concepts that matched the threshold for supervised semantic similarity. label="
						+ label + ", rankCutoff=" + rankCutoff);
			}
		}
	}

	/**
	 * return the product of all the similarity metrics
	 */
	@Override
	public double innerEvaluate(Object o1, Object o2) {
		double d = 0;
		String c1 = (String) o1;
		String c2 = (String) o2;
		if (c1 != null && c2 != null) {
			if (c1.equals(c2)) {
				d = 1;
			} else {
				d = 1;
				ConceptPairSimilarity csim = conceptSimilarityService
						.similarity(metrics, c1, c2, conceptFilter, false);
				for (Double simVal : csim.getSimilarities()) {
					d *= simVal;
				}
			}
		}
		return d;
	}

	public void setConceptSimilarityService(
			ConceptSimilarityService conceptSimilarityService) {
		this.conceptSimilarityService = conceptSimilarityService;
	}

	public void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setMetricNames(String metricNames) {
		this.metricNames = metricNames;
		this.metrics = new ArrayList<SimilarityMetricEnum>();
		for(String metricName : metricNames.split(",")) {
			SimilarityMetricEnum s = SimilarityMetricEnum.valueOf(metricName);
			if(s == null) {
				throw new RuntimeException("invalid metric name: " + metricName);
			}
			metrics.add(s);
		}
	}

	public void setRankCutoff(Integer rankCutoff) {
		this.rankCutoff = rankCutoff;
	}

}
