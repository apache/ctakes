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

/**
 * we run into out of memory errors when preloading the intrinsic ic for large
 * concept graphs. 'compress' the depth a tiny bit by using short instead of
 * int.
 * <p>
 * Tried using float instead of double, but didn't get into the under 1gb range
 * for very large concept graphs, so just use double to avoid precision errors.
 * 
 * @author vijay
 * 
 */
public class ConceptInfo {
	private String conceptId;
	private short depth;
	// private float corpusIC;
	// private float intrinsicIC;
	private double corpusIC;
	private double intrinsicIC;

	public ConceptInfo() {
		super();
	}

	public ConceptInfo(String conceptId, int depth, double corpusIC,
			double intrinsicIC) {
		super();
		this.conceptId = conceptId;
		this.depth = (short) depth;
		// this.corpusIC = (float) corpusIC;
		// this.intrinsicIC = (float) intrinsicIC;
		this.corpusIC = corpusIC;
		this.intrinsicIC = intrinsicIC;
	}

	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public int getDepth() {
		return (int) depth;
	}

	public void setDepth(int depth) {
		this.depth = (short) depth;
	}

	public double getCorpusIC() {
		return (double) corpusIC;
	}

	public void setCorpusIC(double corpusIC) {
		// this.corpusIC = (float) corpusIC;
		this.corpusIC = (double) corpusIC;
	}

	public double getIntrinsicIC() {
		return (double) intrinsicIC;
	}

	public void setIntrinsicIC(double intrinsicIC) {
		// this.intrinsicIC = (float) intrinsicIC;
		this.intrinsicIC = (double) intrinsicIC;
	}

}
