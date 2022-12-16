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
package org.apache.ctakes.ytex.ws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;
import javax.ws.rs.WebApplicationException;

import org.apache.ctakes.ytex.kernel.metric.ConceptPair;
import org.apache.ctakes.ytex.kernel.metric.ConceptPairSimilarity;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService;
import org.apache.ctakes.ytex.kernel.metric.LCSPath;
import org.apache.ctakes.ytex.kernel.metric.SimilarityInfo;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import org.apache.ctakes.ytex.web.search.SemanticSimRegistryBean;

@WebService(endpointInterface = "org.apache.ctakes.ytex.ws.ConceptSimilarityWebService")
public class ConceptSimilarityWebServiceImpl implements
		ConceptSimilarityWebService, ConceptSimilarityRestService {
	public SemanticSimRegistryBean getSemanticSimRegistryBean() {
		return semanticSimRegistryBean;
	}

	public void setSemanticSimRegistryBean(
			SemanticSimRegistryBean semanticSimRegistryBean) {
		this.semanticSimRegistryBean = semanticSimRegistryBean;
	}

	SemanticSimRegistryBean semanticSimRegistryBean;

	public ConceptPairSimilarity similarity(String conceptGraph,
			String concept1, String concept2, String metrics, String lcs) {
		ConceptSimilarityService s = getConceptSimilarityService(conceptGraph);
		if (s == null)
			return null;
		List<SimilarityMetricEnum> metricList = this
				.metricArrayToList(metrics.split(","));
		if (metricList.size() == 0)
			return null;
		return s.similarity(metricList, concept1, concept2, null,
				"true".equalsIgnoreCase(lcs));
	}

	private ConceptSimilarityService getConceptSimilarityService(
			String conceptGraph) {
		String conceptGraphName = conceptGraph;
		if (conceptGraphName == null || conceptGraphName.length() == 0)
			conceptGraphName = semanticSimRegistryBean
					.getDefaultConceptGraphName();
		ConceptSimilarityService s = semanticSimRegistryBean
				.getSemanticSimServiceMap().get(conceptGraphName)
				.getConceptSimilarityService();
		return s;
	}

	public List<ConceptPairSimilarity> similarities(String conceptGraph,
			ConceptPair[] conceptPairs, String[] metrics, boolean lcs) {
		ConceptSimilarityService s = this
				.getConceptSimilarityService(conceptGraph);
		List<SimilarityMetricEnum> metricList = metricArrayToList(metrics);
		List<ConceptPair> conceptPairList = Arrays.asList(conceptPairs);
		return s.similarity(conceptPairList, metricList, null, lcs);
	}

	private List<SimilarityMetricEnum> metricArrayToList(String[] metrics) {
		List<SimilarityMetricEnum> metricIndexMap = new ArrayList<SimilarityMetricEnum>();
		for (String metric : metrics) {
			SimilarityMetricEnum m = SimilarityMetricEnum.valueOf(metric);
			if (m != null)
				metricIndexMap.add(m);
		}
		return metricIndexMap;
	}

	public SimServiceInfo getDefaultConceptGraph() {
		String conceptGraph = this.semanticSimRegistryBean
				.getDefaultConceptGraphName();
		if (conceptGraph != null)
			return new SimServiceInfo(conceptGraph, semanticSimRegistryBean
					.getSemanticSimServiceMap().get(conceptGraph)
					.getDescription());
		else
			return null;
	}

	public List<SimServiceInfo> getConceptGraphs() {
		List<SimServiceInfo> cgs = new ArrayList<SimServiceInfo>(
				semanticSimRegistryBean.getSemanticSimDescriptionMap().size());
		for (Map.Entry<String, String> entry : semanticSimRegistryBean
				.getSemanticSimDescriptionMap().entrySet()) {
			cgs.add(new SimServiceInfo(entry.getValue(), entry.getKey()));
		}
		return cgs;
	}

}
