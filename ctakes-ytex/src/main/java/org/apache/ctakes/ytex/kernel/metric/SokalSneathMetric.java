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
 * Sokal and Sneath metric as in eqn 18 from
 * http://dx.doi.org/10.1016/j.jbi.2011.03.013
 * 
 * @author vijay
 * 
 */
public class SokalSneathMetric extends BaseSimilarityMetric {

	public SokalSneathMetric(ConceptSimilarityService simSvc) {
		super(simSvc);
	}

	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {
		double lcsIC = this.initLcsIC(concept1, concept2, conceptFilter,
				simInfo, true);
		if (lcsIC == 0d)
			return 0d;
		double ic1 = simSvc.getIC(concept1, true);
		double ic2 = simSvc.getIC(concept2, true);
		return lcsIC / (2 * (ic1 + ic2) - 3 * lcsIC);
	}

}
