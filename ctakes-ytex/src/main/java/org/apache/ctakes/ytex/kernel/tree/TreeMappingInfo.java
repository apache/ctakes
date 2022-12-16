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

import java.util.ArrayList;
import java.util.List;

public class TreeMappingInfo {
	String instanceIDField;
	QueryMappingInfo instanceQueryMappingInfo;
	List<QueryMappingInfo> nodeQueryMappingInfos = new ArrayList<QueryMappingInfo>();
	String prepareScript;
	String prepareScriptStatementDelimiter = ";";


	public String getPrepareScript() {
		return prepareScript;
	}

	public void setPrepareScript(String prepareScript) {
		this.prepareScript = prepareScript;
	}

	public String getPrepareScriptStatementDelimiter() {
		return prepareScriptStatementDelimiter;
	}

	public void setPrepareScriptStatementDelimiter(
			String prepareScriptStatementDelimiter) {
		this.prepareScriptStatementDelimiter = prepareScriptStatementDelimiter;
	}

	public String getInstanceIDField() {
		return instanceIDField;
	}

	public void setInstanceIDField(String instanceIDField) {
		this.instanceIDField = instanceIDField;
	}

	public QueryMappingInfo getInstanceQueryMappingInfo() {
		return instanceQueryMappingInfo;
	}

	public void setInstanceQueryMappingInfo(
			QueryMappingInfo instanceQueryMappingInfo) {
		this.instanceQueryMappingInfo = instanceQueryMappingInfo;
	}

	public List<QueryMappingInfo> getNodeQueryMappingInfos() {
		return nodeQueryMappingInfos;
	}

	public void setNodeQueryMappingInfos(
			List<QueryMappingInfo> nodeQueryMappingInfos) {
		this.nodeQueryMappingInfos = nodeQueryMappingInfos;
	}
}
