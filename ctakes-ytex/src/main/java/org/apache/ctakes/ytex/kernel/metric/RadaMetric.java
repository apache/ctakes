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

import java.util.Map;

/**
 * 1 - path / (2*maxDepth)
 * 
 * @author vijay
 * 
 */
public class RadaMetric extends BaseSimilarityMetric {

	double depthMax = 0d;

	public RadaMetric(ConceptSimilarityService simSvc, Integer depthMax) {
		super(simSvc);
		if (depthMax != null)
			this.depthMax = depthMax.doubleValue();
	}

	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {
		if (depthMax == 0d)
			return 0d;
		this.initLCSes(concept1, concept2, simInfo);
		if (simInfo.getLcsDist() > 0) {
			return 1 - (((double) simInfo.getLcsDist() - 1) / (double) (2 * depthMax));
		} else {
			return 0;
		}
	}

}
