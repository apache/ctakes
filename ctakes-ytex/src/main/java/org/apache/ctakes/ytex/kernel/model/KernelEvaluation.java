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
package org.apache.ctakes.ytex.kernel.model;

import java.io.Serializable;

import org.apache.ctakes.ytex.dao.DBUtil;


public class KernelEvaluation implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int kernelEvaluationId;
	private String corpusName;
	private String label = DBUtil.getEmptyString();
	private String experiment = DBUtil.getEmptyString();
	private int foldId;
	private double param1 = 0;
	private String param2 = DBUtil.getEmptyString();

	public double getParam1() {
		return param1;
	}

	public void setParam1(double param1) {
		this.param1 = param1;
	}

	public String getParam2() {
		return param2;
	}

	public void setParam2(String param2) {
		this.param2 = param2;
	}

	public int getKernelEvaluationId() {
		return kernelEvaluationId;
	}

	public void setKernelEvaluationId(int kernelEvaluationId) {
		this.kernelEvaluationId = kernelEvaluationId;
	}

	public String getCorpusName() {
		return corpusName;
	}

	public void setCorpusName(String name) {
		this.corpusName = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getExperiment() {
		return experiment;
	}

	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}

	public int getFoldId() {
		return foldId;
	}

	public void setFoldId(int foldId) {
		this.foldId = foldId;
	}
}
