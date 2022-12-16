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

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.apache.ctakes.ytex.kernel.metric.ConceptPair;
import org.apache.ctakes.ytex.kernel.metric.ConceptPairSimilarity;

@WebService
public interface ConceptSimilarityWebService {
	public SimServiceInfo getDefaultConceptGraph();

	public List<SimServiceInfo> getConceptGraphs();

	/**
	 * compute similarity for a list of concept pairs
	 * 
	 * @param conceptGraphName optional
	 * @param conceptPairs
	 *            required, concept pairs for which similarity should be
	 *            computed
	 * @param metrics
	 *            required, similarity metrics to compute
	 * @param lcs
	 *            optional - if true, fill in the lcs paths for each concept pair.
	 * @return similarities
	 */
	@WebMethod
	public List<ConceptPairSimilarity> similarities(String conceptGraph,
			ConceptPair[] conceptPairs, String[] metrics, boolean lcs);
}
