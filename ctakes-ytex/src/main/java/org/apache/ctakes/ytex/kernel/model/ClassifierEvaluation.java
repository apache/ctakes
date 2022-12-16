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
import java.util.HashMap;
import java.util.Map;

public class ClassifierEvaluation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int classifierEvaluationId;
	String name;
	String experiment;
	int fold;
	int run;
	String algorithm;
	String label;
	String options;
	byte[] model;
	Double param1;
	String param2;
	
	public Double getParam1() {
		return param1;
	}
	public void setParam1(Double param1) {
		this.param1 = param1;
	}
	public String getParam2() {
		return param2;
	}
	public void setParam2(String param2) {
		this.param2 = param2;
	}
	Map<Long, ClassifierInstanceEvaluation> classifierInstanceEvaluations = new HashMap<Long, ClassifierInstanceEvaluation>();

	public String getExperiment() {
		return experiment;
	}
	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}	
	public byte[] getModel() {
		return model;
	}
	public void setModel(byte[] model) {
		this.model = model;
	}
	public int getClassifierEvaluationId() {
		return classifierEvaluationId;
	}
	public void setClassifierEvaluationId(int classifierEvaluationId) {
		this.classifierEvaluationId = classifierEvaluationId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getFold() {
		return fold;
	}
	public void setFold(int fold) {
		this.fold = fold;
	}
	public int getRun() {
		return run;
	}
	public void setRun(int run) {
		this.run = run;
	}
	public String getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getOptions() {
		return options;
	}
	public void setOptions(String options) {
		this.options = options;
	}
	public Map<Long, ClassifierInstanceEvaluation> getClassifierInstanceEvaluations() {
		return classifierInstanceEvaluations;
	}
	public void setClassifierInstanceEvaluations(
			Map<Long, ClassifierInstanceEvaluation> classifierInstanceEvaluations) {
		this.classifierInstanceEvaluations = classifierInstanceEvaluations;
	}
	
}
