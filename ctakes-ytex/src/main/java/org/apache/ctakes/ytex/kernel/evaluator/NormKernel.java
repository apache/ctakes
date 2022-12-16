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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.tree.Node;


/**
 * Return norm of delegate kernel: <code>k(x,y)/sqrt(k(x,x)*k(y,y)</code>. If
 * the object is a org.apache.ctakes.ytex.kernel.Node, then save the norm in the node for future
 * reference. else if cacheNorm = true, save the norm in the cache for future
 * reference. If the delegate kernel is fast (e.g. it's using caching itself /
 * trivial operation) caching the norm will slow things down.
 * 
 * @author vijay
 * 
 */
public class NormKernel implements Kernel {
	private static final Log log = LogFactory.getLog(NormKernel.class);

	private Cache normCache;
	private CacheManager cacheManager;
	private Kernel delegateKernel;
	private boolean cacheNorm = true;

	public boolean isCacheNorm() {
		return cacheNorm;
	}

	public void setCacheNorm(boolean cacheNorm) {
		this.cacheNorm = cacheNorm;
	}

	public NormKernel(Kernel delegateKernel) {
		this.delegateKernel = delegateKernel;
	}

	public NormKernel() {
		super();
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public Kernel getDelegateKernel() {
		return delegateKernel;
	}

	public void setDelegateKernel(Kernel delegateKernel) {
		this.delegateKernel = delegateKernel;
	}

	/**
	 * compute the norm.
	 * 
	 * @param o1
	 * @return
	 */
	public double getNorm(Object o1) {
		Double norm = null;
		if (o1 != null) {
			if (o1 instanceof Node) {
				// look in node if this is a node
				norm = ((Node) o1).getNorm();
			} else if (this.isCacheNorm()) {
				// look in cache otherwise
				Element cachedNorm = null;
				cachedNorm = normCache.get(o1);
				if (cachedNorm != null) {
					norm = (Double) cachedNorm.getValue();
				}
			}
			if (norm == null) {
				// couldn't get cached norm - compute it
				norm = Math.sqrt(delegateKernel.evaluate(o1, o1));
			}
			if (o1 instanceof Node) {
				((Node) o1).setNorm(norm);
			} else if (this.isCacheNorm()) {
				normCache.put(new Element(o1, norm));
			}
		}
		return norm;
	}

	public double evaluate(Object o1, Object o2) {
		double d = 0;
		if (o1 == null || o2 == null) {
			d = 0;
		} else {
			double norm1 = getNorm(o1);
			double norm2 = getNorm(o2);
			if (norm1 != 0 && norm2 != 0)
				d = delegateKernel.evaluate(o1, o2) / (norm1 * norm2);
		}
		if (log.isTraceEnabled()) {
			log.trace("K<" + o1 + "," + o2 + "> = " + d);
		}
		return d;
	}

	public void init() {
		normCache = cacheManager.getCache("normCache");
	}
}