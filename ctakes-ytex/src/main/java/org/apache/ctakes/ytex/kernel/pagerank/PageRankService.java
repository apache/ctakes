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
package org.apache.ctakes.ytex.kernel.pagerank;

import java.util.Map;

import org.apache.ctakes.ytex.kernel.model.ConceptGraph;


public interface PageRankService {

	/**
	 * PageRank for conceptGraph. Page = concept. in-links = parents. out-links
	 * = children.
	 * 
	 * @param dampingVector
	 *            topic vector/personalized pagerank vector. If null will use
	 *            normal pagerank with a damping vector where every value is 1/N
	 * @param cg
	 *            concept graph
	 * @param iter
	 *            max number of iterations
	 * @param threshold
	 *            convergence threshold
	 * @param dampingFactor
	 * @return pageRank 'vector'. key = concept (page), value = rank
	 */
	public abstract double[] rank(Map<String, Double> dampingVector,
			ConceptGraph cg, int iter, double threshold, double dampingFactor);

	/**
	 * call rank() with default values for iter (30), threshold(1e-4),
	 * dampingFactor(0.85)
	 * 
	 * @param dampingVector
	 * @param cg
	 * @return
	 */
	public abstract double[] rank(Map<String, Double> dampingVector,
			ConceptGraph cg);

	public abstract double sim(String concept1, String concept2, ConceptGraph cg,
			int iter, double threshold, double dampingFactor);

	public abstract double[] rank2(Map<Integer, Double> dampingVector, ConceptGraph cg, int iter,
			double threshold, double dampingFactor);

}