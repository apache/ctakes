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
 * 
 * This metric is an implementation of the semantic relatedness measure described
 * by Leacock and Chodorow (1998).
 * 
 * See reference paper: https://aclanthology.org/J06-1003.pdf
 * Page 19, Sec 2.5.3 (7)
 * 
 * sim(c1,c2) = -log ( len(c1,c2) / 2 * max_depth )
 * 
 */
public class LCHMetric extends BaseSimilarityMetric {
	/**
	 * natural log(max depth * 2)
	 */
	double maxDepth = 0d;

	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {
		if (maxDepth != 0d) {
			initLCSes(concept1, concept2, simInfo);
			if (simInfo.getLcsDist() > 0) {

				// Compute the length between the concepts
				double lch = Math.log((double) simInfo.getLcsDist() / (double)(2 * maxDepth)) * -1.0d;
				return lch;
				
			}
		}
		return 0d;
	}

	public LCHMetric(ConceptSimilarityService simSvc, Integer maxDepth) {
		super(simSvc);
		if (maxDepth != null) {
			// Math.log is the natural log by default
			// The cTakes concept graphs are adding an extra node which 
			// represents C0000000 as the root node rather than the source
			// specific root and inflating the depth, we need to reduce
			// maxDepth by 2 to correct for this
			if ( maxDepth > 3 ) {
				this.maxDepth = maxDepth - 2;
			} else {
				this.maxDepth = maxDepth;
			}
		}
	}

}
