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
package org.apache.ctakes.ytex.uima.mapper;

import java.util.HashSet;
import java.util.Set;

/**
 * semi-ugly hack to communicate options from the MapperService to the
 * individual Mappers. The individual mappers are non-spring beans and should
 * stay that way.
 * 
 * @author vijay
 * 
 */
public class MapperConfig {
	private static MapperConfig mcDefault = new MapperConfig(
			new HashSet<String>(0), 0);

	static ThreadLocal<MapperConfig> tlConfig = new ThreadLocal<MapperConfig>() {

		@Override
		protected MapperConfig initialValue() {
			return mcDefault;
		}

	};

	protected static MapperConfig getConfig() {
		return tlConfig.get();
	}

	protected static void setConfig(Set<String> typesStoreCoveredText,
			int coveredTextMaxLen) {
		tlConfig.set(new MapperConfig(typesStoreCoveredText, coveredTextMaxLen));
	}

	protected static void unsetConfig() {
		tlConfig.set(null);
	}
	private int coveredTextMaxLen;

	private Set<String> typesStoreCoveredText;

	public MapperConfig(Set<String> typesStoreCoveredText, int coveredTextMaxLen) {
		super();
		this.typesStoreCoveredText = typesStoreCoveredText;
		this.coveredTextMaxLen = coveredTextMaxLen;
	}

	public int getCoveredTextMaxLen() {
		return coveredTextMaxLen;
	}

	public Set<String> getTypesStoreCoveredText() {
		return typesStoreCoveredText;
	}
}
