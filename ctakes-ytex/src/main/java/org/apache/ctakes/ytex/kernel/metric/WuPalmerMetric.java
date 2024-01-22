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
 * Wu Palmer metric as in https://wn.readthedocs.io/en/latest/api/wn.similarity.html
 * 
 * @author vijay
 * @author painter
 * 
 */
public class WuPalmerMetric extends BaseSimilarityMetric {
	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {
		initLCSes(concept1, concept2, simInfo);
		
		if (simInfo.getLcses().size() > 0) {
			int lcsDepth = 0;
			
			// Test for the LCS with the greatest depth
			// to find the lowest common synonym
			for (String lcs : simInfo.getLcses()) {
				
				//
				// Note:
				//   The depth is inflated for all concepts by +1 
				//   due to creating a fake node to build the concept
				//   graph at C000000. But Wu-Palmer adds one to the
				//   distance, so it is OK to leave here
				int d = simSvc.getDepth(lcs);
				
				// Find the max depth of the LCS
				if (d > lcsDepth)
					lcsDepth = d;
			}
			
			//
			// Compute Wu-Palmer Similarity:
			//
			// https://wn.readthedocs.io/en/latest/api/wn.similarity.html
			//
			// Why does this work? 2*k -> 2 times the LCS depth
			// i or j will be zero as it is the same concept
			//
			// The other will be the shortest distance between the LCS minus 1 hop
			//
			double lcsDist = simInfo.getLcsDist().doubleValue();
			double k = (double) (lcsDepth);
			double sim = (2.0*k) / (2.0*k + lcsDist - 1.0);
			return sim;
		}
		return 0d;
	}

	public WuPalmerMetric(ConceptSimilarityService simSvc) {
		super(simSvc);
	}

}
