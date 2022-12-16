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


public class FeatureEvaluation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String corpusName = DBUtil.getEmptyString();
	private int crossValidationFoldId = 0;
	private String evaluationType;
	private int featureEvaluationId;
	private String featureSetName = DBUtil.getEmptyString();
	private String label = DBUtil.getEmptyString();
	private double param1 = 0;
	private String param2 = DBUtil.getEmptyString();

	public FeatureEvaluation() {
		super();
	}

	public FeatureEvaluation(String name, String label,
			Integer crossValidationFoldId, String evaluationType) {
		super();
		this.corpusName = name;
		this.label = label;
		this.crossValidationFoldId = crossValidationFoldId;
		this.evaluationType = evaluationType;
	}

	public String getCorpusName() {
		return corpusName;
	}

	public int getCrossValidationFoldId() {
		return crossValidationFoldId;
	}

	public String getEvaluationType() {
		return evaluationType;
	}

	public int getFeatureEvaluationId() {
		return featureEvaluationId;
	}

	public String getFeatureSetName() {
		return featureSetName;
	}

	public String getLabel() {
		return label;
	}

	public String getParam2() {
		return param2;
	}

	public void setCorpusName(String name) {
		this.corpusName = name;
	}

	public void setCrossValidationFoldId(int crossValidationFoldId) {
		this.crossValidationFoldId = crossValidationFoldId;
	}

	public void setEvaluationType(String evaluationType) {
		this.evaluationType = evaluationType;
	}

	public void setFeatureEvaluationId(int featureEvaluationId) {
		this.featureEvaluationId = featureEvaluationId;
	}

	public void setFeatureSetName(String featureSetName) {
		this.featureSetName = DBUtil.nullToEmptyString(featureSetName);
	}

	public void setLabel(String label) {
		this.label = DBUtil.nullToEmptyString(label);
	}

	public void setParam2(String param2) {
		this.param2 = DBUtil.nullToEmptyString(param2);
	}

	public double getParam1() {
		return param1;
	}

	public void setParam1(double param1) {
		this.param1 = param1;
	}

}
