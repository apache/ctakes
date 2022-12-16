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

public class WuPalmerMetric extends BaseSimilarityMetric {
	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {
		initLCSes(concept1, concept2, simInfo);
		if (simInfo.getLcses().size() > 0) {
			int lcsDepth = 0;
			for (String lcs : simInfo.getLcses()) {
				int d = simSvc.getDepth(lcs);
				if (d > lcsDepth)
					lcsDepth = d;
			}
			double lcsDepth2 = (double) (lcsDepth * 2);
			return lcsDepth2 / (lcsDepth2 + (double) (simInfo.getLcsDist()-1));
		}
		return 0d;
	}

	public WuPalmerMetric(ConceptSimilarityService simSvc) {
		super(simSvc);
	}

}
