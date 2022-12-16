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

import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;

/**
 * simple caching interceptor. we require a cacheName and cacheKeyGenerator. we
 * don't use AOP style configuration because we reuse the same classes (kernels)
 * in very different contexts. sometimes we want to cache, sometimes we don't.
 * therefore, use old-school ProxyFactoryBean with this interceptor.
 * 
 * This turns out to be very slow - a lot of time is spent in AOP-type stuff.
 * This is due to the very high throughput when evaluating kernels.
 * 
 * @author vijay
 * 
 */
public class MethodCachingInterceptor implements MethodInterceptor,
		InitializingBean {

	private CacheManager cacheManager;
	private String cacheName;
	private Cache cache;
	private CacheKeyGenerator cacheKeyGenerator;
	private String methodName;

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public CacheKeyGenerator getCacheKeyGenerator() {
		return cacheKeyGenerator;
	}

	public void setCacheKeyGenerator(CacheKeyGenerator cacheKeyGenerator) {
		this.cacheKeyGenerator = cacheKeyGenerator;
	}

	/**
	 * @return the cacheManager
	 */
	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public String getCacheName() {
		return cacheName;
	}

	public Object invoke(final MethodInvocation methodInvocation)
			throws Throwable {
		Object methodReturn = null;
		if (methodName == null
				|| methodName.equals(methodInvocation.getMethod().getName())) {
			final Object cacheKey = this.cacheKeyGenerator.getCacheKey(
					methodInvocation.getMethod(),
					methodInvocation.getArguments());
			final Element cacheElement = cache.get(cacheKey);
			if (cacheElement == null) {
				methodReturn = methodInvocation.proceed();
				cache.put(new Element(cacheKey, (Serializable) methodReturn));
			} else {
				methodReturn = cacheElement.getValue();
			}
		} else {
			methodReturn = methodInvocation.proceed();
		}

		return methodReturn;
	}

	/**
	 * @param cacheManager
	 *            the cacheManager to set
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cache = cacheManager.getCache(cacheName);
	}
}
