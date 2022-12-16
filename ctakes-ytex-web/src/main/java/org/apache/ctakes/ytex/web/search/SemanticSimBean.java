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
package org.apache.ctakes.ytex.web.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.ctakes.ytex.kernel.metric.ConceptPair;
import org.apache.ctakes.ytex.kernel.metric.ConceptPairSimilarity;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import org.apache.ctakes.ytex.kernel.metric.LCSPath;
import org.apache.ctakes.ytex.kernel.metric.SimilarityInfo;

public class SemanticSimBean implements Serializable {
	public static class SimilarityEntry implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ConceptPair conceptPair;
		private Map<SimilarityMetricEnum, Double> similarityMap;

		public ConceptPair getConceptPair() {
			return conceptPair;
		}

		public Map<SimilarityMetricEnum, Double> getSimilarityMap() {
			return similarityMap;
		}

		public void setConceptPair(ConceptPair conceptPair) {
			this.conceptPair = conceptPair;
		}

		public void setSimilarityMap(
				Map<SimilarityMetricEnum, Double> similarityMap) {
			this.similarityMap = similarityMap;
		}

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	transient ConceptLookupBean concept1;
	transient ConceptLookupBean concept2;
	transient String conceptGraphName;
	transient String conceptPairText;
	transient String corpusLcsTerm;
	transient String exportType;

	transient String intrinsicLcsTerm;
	transient Map<String, String> lcsPathMap = new TreeMap<String, String>();
	transient List<SimilarityMetricEnum> metrics = new ArrayList<SimilarityMetricEnum>();
	transient String[] metricSelectCorpusIC;
	transient String[] metricSelectIntrinsicIC;
	transient String[] metricSelectTaxonomy;

	transient SemanticSimRegistryBean semanticSimRegistryBean;

	/**
	 * when doing similarity for multiple concept pairs
	 */
	transient List<SimilarityEntry> similarityList;

	transient Map<SimilarityMetricEnum, Double> similarityMap = new HashMap<SimilarityMetricEnum, Double>();

	/**
	 * when doing similarity for a single concept pair
	 */
	transient SimilarityInfo simInfo = new SimilarityInfo();
	public SemanticSimBean() {
	}
	public ConceptLookupBean getConcept1() {
		return concept1;
	}

	public ConceptLookupBean getConcept2() {
		return concept2;
	}

	public String getConceptGraphName() {
		return conceptGraphName;
	}

	public String getConceptPairText() {
		return conceptPairText;
	}

	public String getCorpusLcsTerm() {
		return corpusLcsTerm;
	}

	public String getExportType() {
		return exportType;
	}

	public String getIntrinsicLcsTerm() {
		return intrinsicLcsTerm;
	}

	public Map<String, String> getLcsPathMap() {
		return lcsPathMap;
	}

	public List<SimilarityMetricEnum> getMetrics() {
		return metrics;
	}

	public String[] getMetricSelectCorpusIC() {
		return metricSelectCorpusIC;
	}

	public String[] getMetricSelectIntrinsicIC() {
		return metricSelectIntrinsicIC;
	}

	public String[] getMetricSelectTaxonomy() {
		return metricSelectTaxonomy;
	}

	public SemanticSimRegistryBean getSemanticSimRegistryBean() {
		return semanticSimRegistryBean;
	}

	public List<SimilarityEntry> getSimilarityList() {
		return similarityList;
	}

	public Map<SimilarityMetricEnum, Double> getSimilarityMap() {
		return similarityMap;
	}

	public SimilarityInfo getSimInfo() {
		return simInfo;
	}

	private void initMetrics() {
		this.metrics.clear();
		for (String metric : this.metricSelectTaxonomy) {
			metrics.add(SimilarityMetricEnum.valueOf(metric));
		}
		for (String metric : this.metricSelectCorpusIC) {
			metrics.add(SimilarityMetricEnum.valueOf(metric));
		}
		for (String metric : this.metricSelectIntrinsicIC) {
			metrics.add(SimilarityMetricEnum.valueOf(metric));
		}
	}

	private void reset() {
		ConceptSearchService searchSvc = this.getSemanticSimRegistryBean()
				.getSemanticSimServiceMap().get(this.conceptGraphName)
				.getConceptSearchService();
		concept1.reset();
		concept1.setUmlsFirstWordService(searchSvc);
		concept2.reset();
		concept2.setUmlsFirstWordService(searchSvc);
		this.simInfo = null;
		this.intrinsicLcsTerm = null;
		this.corpusLcsTerm = null;
		this.metrics.clear();
		if (this.similarityMap != null)
			this.similarityMap.clear();
	}

	public void resetListen(ActionEvent event) {
		reset();
	}

	public void setConcept1(ConceptLookupBean concept1) {
		this.concept1 = concept1;
	}

	public void setConcept2(ConceptLookupBean concept2) {
		this.concept2 = concept2;
	}

	public void setConceptGraphName(String conceptGraphName) {
		this.conceptGraphName = conceptGraphName;
	}

	public void setConceptPairText(String conceptPairText) {
		this.conceptPairText = conceptPairText;
	}

	public void setCorpusLcsTerm(String corpusLcsTerm) {
		this.corpusLcsTerm = corpusLcsTerm;
	}

	public void setExportType(String exportType) {
		this.exportType = exportType;
	}

	public void setIntrinsicLcsTerm(String intrinsicLcsTerm) {
		this.intrinsicLcsTerm = intrinsicLcsTerm;
	}

	public void setLcsPathMap(Map<String, String> lcsPathMap) {
		this.lcsPathMap = lcsPathMap;
	}

	public void setMetrics(List<SimilarityMetricEnum> metrics) {
		this.metrics = metrics;
	}

	public void setMetricSelectCorpusIC(String[] metricSelectCorpusIC) {
		this.metricSelectCorpusIC = metricSelectCorpusIC;
	}

	public void setMetricSelectIntrinsicIC(String[] metricSelectIntrinsicIC) {
		this.metricSelectIntrinsicIC = metricSelectIntrinsicIC;
	}

	public void setMetricSelectTaxonomy(String[] metricSelectTaxonomy) {
		this.metricSelectTaxonomy = metricSelectTaxonomy;
	}

	public void setSemanticSimRegistryBean(
			SemanticSimRegistryBean semanticSimRegistryBean) {
		this.semanticSimRegistryBean = semanticSimRegistryBean;
		if (semanticSimRegistryBean != null)
			this.conceptGraphName = semanticSimRegistryBean
					.getDefaultConceptGraphName();
	}

	public void setSimilarityList(List<SimilarityEntry> similarityList) {
		this.similarityList = similarityList;
	}

	public void setSimilarityMap(Map<SimilarityMetricEnum, Double> similarityMap) {
		this.similarityMap = similarityMap;
	}

	public void setSimInfo(SimilarityInfo simInfo) {
		this.simInfo = simInfo;
	}

	/**
	 * handle submit
	 * 
	 * @param event
	 */
	public void simListen(ActionEvent event) {
		if (this.concept1.getCurrentCUI() != null
				&& this.concept2.getCurrentCUI() != null) {
			this.concept1.setSearchCUI(this.concept1.getCurrentCUI());
			this.concept2.setSearchCUI(this.concept2.getCurrentCUI());
			initMetrics();
			ConceptSimilarityService simSvc = this.getSemanticSimRegistryBean()
					.getSemanticSimServiceMap().get(conceptGraphName)
					.getConceptSimilarityService();
			ConceptSearchService searchSvc = this.getSemanticSimRegistryBean()
					.getSemanticSimServiceMap().get(conceptGraphName)
					.getConceptSearchService();
			ConceptPairSimilarity csim = simSvc.similarity(metrics, concept1
					.getSearchCUI().getConceptId(), concept2.getSearchCUI()
					.getConceptId(), null, true);
			this.simInfo = csim.getSimilarityInfo();
			this.similarityMap = toSimMap(csim);
			lcsPathMap.clear();
			if (simInfo.getLcsPaths() != null) {
				for (LCSPath lcsPath : simInfo.getLcsPaths()) {
					String lcs = lcsPath.getLcs();
					lcsPathMap.put(lcs, lcsPath.toString());
				}
			}
			if (simInfo.getCorpusLcs() != null) {
				this.corpusLcsTerm = searchSvc.getTermByConceptId(simInfo
						.getCorpusLcs());
			} else {
				this.corpusLcsTerm = null;
			}
			if (simInfo.getIntrinsicLcs() != null) {
				this.intrinsicLcsTerm = searchSvc.getTermByConceptId(simInfo
						.getIntrinsicLcs());
			} else {
				this.intrinsicLcsTerm = null;
			}
		}
	}

	public void simMultiListen(ActionEvent event) throws IOException {
		this.similarityList = new ArrayList<SimilarityEntry>();
		if (conceptPairText != null) {
			initMetrics();
			// parse concept pairs
			List<ConceptPair> conceptPairs = new ArrayList<ConceptPair>();
			BufferedReader r = new BufferedReader(new StringReader(
					conceptPairText));
			String line = null;
			while ((line = r.readLine()) != null) {
				String concepts[] = line.split(",|\\s");
				if (concepts.length == 2) {
					ConceptPair p = new ConceptPair();
					p.setConcept1(concepts[0]);
					p.setConcept2(concepts[1]);
					conceptPairs.add(p);
				}
			}
			// calculate sim
			ConceptSimilarityService simSvc = this.getSemanticSimRegistryBean()
					.getSemanticSimServiceMap().get(conceptGraphName)
					.getConceptSimilarityService();
			List<ConceptPairSimilarity> similarities = simSvc.similarity(
					conceptPairs, metrics, null, false);
			// load list with results
			for (int i = 0; i < conceptPairs.size(); i++) {
				SimilarityEntry e = new SimilarityEntry();
				e.setConceptPair(conceptPairs.get(i));
				e.setSimilarityMap(this.toSimMap(similarities.get(i)));
				this.similarityList.add(e);
			}
		}

	}

	private Map<SimilarityMetricEnum, Double> toSimMap(
			ConceptPairSimilarity csim) {
		Map<SimilarityMetricEnum, Double> simMap = new TreeMap<SimilarityMetricEnum, Double>();
		for (int i = 0; i < metrics.size(); i++) {
			simMap.put(metrics.get(i), csim.getSimilarities()
					.get(i));
		}
		return simMap;
	}

	/**
	 * handle selecting different concept graph
	 * 
	 * @param event
	 */
	public void updateConceptGraph(ValueChangeEvent event) {
		String newConceptGraphName = (String) event.getNewValue();
		if (!newConceptGraphName.equals(this.conceptGraphName)) {
			this.conceptGraphName = newConceptGraphName;
			this.reset();
		}
	}
}
