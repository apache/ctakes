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


/**
 * Expects numeric values as input. Returns the product of the specified values,
 * 
 * @author vijay
 * 
 */
public class AttributeProductKernel implements Kernel {

	@Override
	public double evaluate(Object o1, Object o2) {
		double d = 0;
		Number num1 = (Number) o1;
		Number num2 = (Number) o2;
		if (num1 != null && num2 != null) {
			d = num1.doubleValue() * num2.doubleValue();
		}
		return d;
	}

}
