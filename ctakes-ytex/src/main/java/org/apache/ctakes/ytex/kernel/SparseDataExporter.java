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
package org.apache.ctakes.ytex.kernel;

import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public interface SparseDataExporter {
	
	public enum ScopeEnum {
		LABEL("label"),
		FOLD("fold");
		private String scope;
		ScopeEnum(String scope) {
			this.scope = scope;
		}
		public String getScope() {
			return scope;
		}
	}

	public abstract void exportData(String propertiesFile, String format)
			throws IOException, InvalidPropertiesFormatException;

	public abstract void exportData(Properties props,
			SparseDataFormatter formatter, BagOfWordsDecorator bDecorator)
			throws IOException;

}