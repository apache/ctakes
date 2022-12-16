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
 * compute Intrinsic path distance. Scale the distance to the unit
 * interval using max IC.
 * 
 * @author vijay
 * 
 */
public class IntrinsicPathMetric extends BaseSimilarityMetric {
	Double maxIC;

	public IntrinsicPathMetric(ConceptSimilarityService simSvc, Double maxIC) {
		super(simSvc);
		this.maxIC = maxIC;
	}

	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {
		if (maxIC == null)
			return 0d;
		double lcsIC = this.initLcsIC(concept1, concept2, conceptFilter, simInfo, true);
		if (lcsIC == 0d)
			return 0d;
		double ic1 = simSvc.getIC(concept1, true);
		double ic2 = simSvc.getIC(concept2, true);
		// scale to unit interval
		return 1d/(ic1 + ic2 - (2 * lcsIC) + 1);
	}

}
