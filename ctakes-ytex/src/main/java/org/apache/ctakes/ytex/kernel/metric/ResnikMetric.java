package org.apache.ctakes.ytex.kernel.metric;

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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * compute Resnik score to provide functionality as found in UMLS::Similarity
 * 
 * UMLS::Similarity::res.pm
 * Module implementing the semantic relatedness measure described 
 * by Resnik (1995)
 * 
 * @author painter
 * 
 */
public class ResnikMetric extends BaseSimilarityMetric {
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
		// don't bother if the concept graph is null
		if (!validCG)
			return 0d;
		
		// get the minimum lcs of the two concepts
		double lcsIC = initLcsIC(concept1, concept2, conceptFilter, simInfo,
				this.intrinsicIC);
		
		// Test for zero
		if (lcsIC == 0d) {
			return 0d;
		}
		
		//
		// Resnik simply returns the minimum IC score of the LCSes
		//
		// Note: When comparing results to the Perl UMLS::Similarity metric from CPAN
		//       you would need to specify the "--intrinsic sanchez" method
		//       to find comparable IC as cTakes is only computing IC in this way
		//
		return lcsIC;
	}

	public ResnikMetric(ConceptSimilarityService simSvc, boolean intrinsicIC) {
		super(simSvc);
		this.intrinsicIC = intrinsicIC;
		this.validCG = simSvc.getConceptGraph() != null;
		if (!this.intrinsicIC && validCG) {
			rootConcept = simSvc.getConceptGraph().getRoot();
		}
	}

}
