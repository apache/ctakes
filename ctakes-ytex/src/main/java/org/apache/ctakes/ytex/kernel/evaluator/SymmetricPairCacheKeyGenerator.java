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

import java.lang.reflect.Method;

import org.apache.ctakes.ytex.kernel.OrderedPair;


/**
 * cache key for a method that takes 2 arguments, and is symmetric - the order
 * of the arguments doesn't matter.
 * 
 * @author vijay
 * 
 */
public class SymmetricPairCacheKeyGenerator implements CacheKeyGenerator {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getCacheKey(Method method, Object[] args) {
		return new OrderedPair((Comparable) args[0], (Comparable) args[1]);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getCacheKey(Object o1, Object o2) {
		return new OrderedPair((Comparable) o1, (Comparable) o2);
	}

}
