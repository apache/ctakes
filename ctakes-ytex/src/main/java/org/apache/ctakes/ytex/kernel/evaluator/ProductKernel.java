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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * compute the product of delegate kernels
 * 
 * @author vijay
 * 
 */
public class ProductKernel extends CacheKernel {
	private static final Logger LOGGER = LoggerFactory.getLogger("ProductKernel");
	/**
	 * use array instead of list. when running thread dumps, see a lot of action
	 * in list.size(). may be a fluke, but can't hurt
	 */
	Kernel[] delegateKernels;

	public List<Kernel> getDelegateKernels() {
		return Arrays.asList(delegateKernels);
	}

	public void setDelegateKernels(List<Kernel> delegateKernels) {
		this.delegateKernels = new Kernel[delegateKernels.size()];
		for (int i = 0; i < this.delegateKernels.length; i++) {
            this.delegateKernels[i] = delegateKernels.get(i);
        }
	}

	@Override
	public double innerEvaluate(Object o1, Object o2) {
		double d = 1;
		for (Kernel k : delegateKernels) {
			d *= k.evaluate(o1, o2);
			if (d == 0) {
                break;
            }
		}
		if ( LOGGER.isTraceEnabled()) {
			LOGGER.trace("K<{},{}> = {}", o1, o2, d);
		}
		return d;
	}
}
