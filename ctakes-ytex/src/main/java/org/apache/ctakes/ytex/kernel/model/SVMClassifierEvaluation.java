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

public class SVMClassifierEvaluation extends ClassifierEvaluation {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Double cost;
	String weight;
	Integer degree;
	Double gamma;
	Integer kernel;
	Integer supportVectors;
	Double vcdim;
	
	public Double getVcdim() {
		return vcdim;
	}
	public void setVcdim(Double vcdim) {
		this.vcdim = vcdim;
	}
	public Double getCost() {
		return cost;
	}
	public void setCost(Double cost) {
		this.cost = cost;
	}
	public String getWeight() {
		return weight;
	}
	public void setWeight(String weight) {
		this.weight = weight;
	}
	public Integer getDegree() {
		return degree;
	}
	public void setDegree(Integer degree) {
		this.degree = degree;
	}
	public Double getGamma() {
		return gamma;
	}
	public void setGamma(Double gamma) {
		this.gamma = gamma;
	}
	public Integer getKernel() {
		return kernel;
	}
	public void setKernel(Integer kernel) {
		this.kernel = kernel;
	}
	public Integer getSupportVectors() {
		return supportVectors;
	}
	public void setSupportVectors(Integer supportVectors) {
		this.supportVectors = supportVectors;
	}

}
