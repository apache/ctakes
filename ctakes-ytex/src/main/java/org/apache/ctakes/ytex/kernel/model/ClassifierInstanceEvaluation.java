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

public class ClassifierInstanceEvaluation implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int predictedClassId;
	Integer targetClassId;
	ClassifierEvaluation classifierEvaluation;
	int classifierInstanceEvaluationId;
	Map<Integer, Double> classifierInstanceProbabilities = new HashMap<Integer, Double>();
	long instanceId;
	public int getPredictedClassId() {
		return predictedClassId;
	}
	public ClassifierEvaluation getClassifierEvaluation() {
		return classifierEvaluation;
	}
	public int getClassifierInstanceEvaluationId() {
		return classifierInstanceEvaluationId;
	}
	public Map<Integer, Double> getClassifierInstanceProbabilities() {
		return classifierInstanceProbabilities;
	}
	public long getInstanceId() {
		return instanceId;
	}
	public void setPredictedClassId(int classId) {
		this.predictedClassId = classId;
	}
	public void setClassifierEvaluation(ClassifierEvaluation classifierEvaluation) {
		this.classifierEvaluation = classifierEvaluation;
	}
	public void setClassifierInstanceEvaluationId(int classifierInstanceEvaluationId) {
		this.classifierInstanceEvaluationId = classifierInstanceEvaluationId;
	}
	public void setClassifierInstanceProbabilities(
			Map<Integer, Double> classifierInstanceProbabilities) {
		this.classifierInstanceProbabilities = classifierInstanceProbabilities;
	}
	public void setInstanceId(long instanceId) {
		this.instanceId = instanceId;
	}
	public Integer getTargetClassId() {
		return targetClassId;
	}
	public void setTargetClassId(Integer targetClassId) {
		this.targetClassId = targetClassId;
	}
}
