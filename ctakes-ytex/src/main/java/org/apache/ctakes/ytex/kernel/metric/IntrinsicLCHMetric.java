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
 * This version is NOT scaled to the unit metric
 * 
 * @author vijay
 * 
 */
public class IntrinsicLCHMetric extends BaseSimilarityMetric {
	
	double maxIC2 = 0d;

	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {
		
		if (maxIC2 != 0d) {
			
			double ic1 = simSvc.getIC(concept1, true);
			double ic2 = simSvc.getIC(concept2, true);
			double lcsIC = initLcsIC(concept1, concept2, conceptFilter,
					simInfo, true);
			
			// Compute the Intrinsic LCH metric
			double sim = Math.log( (ic1 + ic2 - (2d * lcsIC) + 1d) / maxIC2) * -1.0d;
			return sim;
			
		}
		return 0d;		
		
	}
	
	public IntrinsicLCHMetric(ConceptSimilarityService simSvc, Double maxIC) {
		super(simSvc);
		if (maxIC != null) {
			// Compute the denominator of the Intrinsic LCH metric
			this.maxIC2 = 2.0d * maxIC.doubleValue();
		}
	}



}
