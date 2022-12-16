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

import org.apache.ctakes.ytex.kernel.tree.Node;

/**
 * Extract a node attribute and run the delegate kernel on the attribute.
 * 
 * @author vijay
 * 
 */
public class NodeAttributeKernel implements Kernel {

	private Kernel delegateKernel;
	private String attributeName;

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public Kernel getDelegateKernel() {
		return delegateKernel;
	}

	public void setDelegateKernel(Kernel delegateKernel) {
		this.delegateKernel = delegateKernel;
	}

	@Override
	public double evaluate(Object o1, Object o2) {
		Node n1 = (Node) o1;
		Node n2 = (Node) o2;
		if (n1 != null && n2 != null && n1.getType().equals(n2.getType())) {
			Object attr1 = n1.getValue().get(attributeName);
			Object attr2 = n2.getValue().get(attributeName);
			if (attr1 != null && attr1 != null) {
				return delegateKernel.evaluate(attr1, attr2);
			}
		}
		return 0;
	}
}
