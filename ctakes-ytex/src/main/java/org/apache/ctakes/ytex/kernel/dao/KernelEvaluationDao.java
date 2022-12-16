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
package org.apache.ctakes.ytex.kernel.dao;

import java.util.List;

import org.apache.ctakes.ytex.kernel.model.KernelEvaluation;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluationInstance;


public interface KernelEvaluationDao {

	public abstract void storeNorm(KernelEvaluation kernelEvaluation,
			long instanceId, double norm);

	public abstract Double getNorm(KernelEvaluation kernelEvaluation,
			long instanceId);

	public abstract void storeKernel(KernelEvaluation kernelEvaluation,
			long instanceId1, long instanceId2, double kernel);

	public abstract Double getKernel(KernelEvaluation kernelEvaluation,
			long instanceId1, long instanceId2);

	public List<KernelEvaluationInstance> getAllKernelEvaluationsForInstance(
			KernelEvaluation kernelEvaluation, long instanceId);

	/**
	 * store the kernel evaluation if it doesn't exist, else return the existing
	 * one
	 * 
	 * @param kernelEvaluation
	 * @return
	 */
	public abstract KernelEvaluation storeKernelEval(
			KernelEvaluation kernelEvaluation);

	public abstract KernelEvaluation getKernelEval(String name, String experiment,
			String label, int foldId, double param1, String param2);

}