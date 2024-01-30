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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * compute corpus-ic or intrinsic-ic based lin measure.
 * 
 * @author vijay
 * 
 */
public class LinMetric extends BaseSimilarityMetric {
	private static final Log log = LogFactory.getLog(LinMetric.class);
	private boolean intrinsicIC = true;
	private boolean validCG = false;
	private String rootConcept = simSvc.getConceptGraph().getRoot();

	public boolean isIntrinsicIC() {
		return intrinsicIC;
	}

	public void setIntrinsicIC(boolean intrinsicIC) {
		this.intrinsicIC = intrinsicIC;
	}

	@Override
	public double similarity(String concept1, String concept2,
			Map<String, Double> conceptFilter, SimilarityInfo simInfo) {

		// Test that there is a valid concept graph
		if (!validCG)
			return 0d;
		
		// Compute the IC values for each concept
		double ic1 = simSvc.getIC(concept1, this.intrinsicIC);
		double ic2 = simSvc.getIC(concept2, this.intrinsicIC);
		
		// Get the LCS with the lowest IC score
		double lcsIC = initLcsIC(concept1, concept2, conceptFilter, simInfo,
				this.intrinsicIC);
		
		// if the corpus IC is 0 and the concept is not the root, then we don't
		// have any IC on the concept and can't measure similarity - return 0
		if (!intrinsicIC && ic1 == 0 && !rootConcept.equals(concept1))
			return 0d;

		if (!intrinsicIC && ic2 == 0 && !rootConcept.equals(concept2))
			return 0d;
		
		// Compute the Lin score
		double sim = (2d * lcsIC) / ( ic1 + ic2 );
		return sim;	
		
	}

	/**
	 * This constructor allows us to specify if we want the standard Lin
	 * metric or the Intrinsic Lin by passing a boolean flag
	 * @param simSvc
	 * @param intrinsicIC if true, then compute the intrinsic Lin metric
	 */
	public LinMetric(ConceptSimilarityService simSvc, boolean intrinsicIC) {
		super(simSvc);
		this.intrinsicIC = intrinsicIC;
		this.validCG = simSvc.getConceptGraph() != null;
		if (!this.intrinsicIC && validCG) {
			rootConcept = simSvc.getConceptGraph().getRoot();
		}
	}

}
