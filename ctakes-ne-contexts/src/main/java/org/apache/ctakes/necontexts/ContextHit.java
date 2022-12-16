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
package org.apache.ctakes.necontexts;

import java.util.HashMap;
import java.util.Map;

import org.apache.ctakes.core.fsm.output.BaseTokenImpl;


/**
 * Object represents a hit relating to a focus annotation.
 * 
 * @author Mayo Clinic
 */
public class ContextHit extends BaseTokenImpl {
	private Map<Object, Object> iv_metaMap = new HashMap<Object, Object>();

	public ContextHit(int startOffset, int endOffset) {
		super(startOffset, endOffset);
	}

	public void addMetaData(Object key, Object metaData) {
		iv_metaMap.put(key, metaData);
	}

	public Object getMetaData(Object key) {
		return iv_metaMap.get(key);
	}
}