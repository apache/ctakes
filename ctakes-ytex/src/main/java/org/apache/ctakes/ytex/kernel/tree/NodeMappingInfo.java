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
package org.apache.ctakes.ytex.kernel.tree;

import java.util.Set;

public class NodeMappingInfo {
	private String nodeType;

	private Set<String> values;

	public NodeMappingInfo() {
		super();
	}

	public NodeMappingInfo(String nodeType, Set<String> values) {
		super();
		this.nodeType = nodeType;
		this.values = values;
	}

	public String getNodeType() {
		return nodeType;
	}

	public Set<String> getValues() {
		return values;
	}
	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public void setValues(Set<String> values) {
		this.values = values;
	}
}
