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
 * compute intrinsic LCH as in eqn 28 from
 * http://dx.doi.org/10.1016/j.jbi.2011.03.013
 * 
 * Scale to unit interval
 * 
 * @author vijay
 * 
 */
public class IntrinsicLCHMetric extends BaseSimilarityMetric {
	double logMaxIC2 = 0d;

	public IntrinsicLCHMetric(ConceptSimilarityService simSvc, Double maxIC) {
		super(simSvc);
		if (maxIC != null)
			this.logMaxIC2 = Math.log(2 * maxIC.doubleValue()) + 1d;
	}

	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {
		double sim = 0d;
		if (logMaxIC2 != 0d) {
			double ic1 = simSvc.getIC(concept1, true);
			double ic2 = simSvc.getIC(concept2, true);
			double lcsIC = initLcsIC(concept1, concept2, conceptFilter,
					simInfo, true);
			sim = 1 - (Math.log(ic1 + ic2 - 2 * (lcsIC) + 1) / logMaxIC2);

		}
		return sim;
	}

}
