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
package org.apache.ctakes.ytex.kernel.evaluator;

import java.util.List;

/**
 * apply all the delegate kernels to the objects, sum them up
 */
public class SumKernel extends CacheKernel {
	List<Kernel> delegateKernels;

	public List<Kernel> getDelegateKernels() {
		return delegateKernels;
	}

	public void setDelegateKernels(List<Kernel> delegateKernels) {
		this.delegateKernels = delegateKernels;
	}

	/**
	 * 
	 */
	@Override
	public double innerEvaluate(Object o1, Object o2) {
		double d = 0;
		for(Kernel k : delegateKernels) {
			d += k.evaluate(o1, o2);
		}
		return d;
	}
}
