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
package org.apache.ctakes.ytex.sparsematrix;

import java.io.IOException;

import org.apache.ctakes.ytex.kernel.InstanceData;


public interface InstanceDataExporter {

	public static final String FIELD_DELIM = "\t";
	public static final String RECORD_DELIM = "\n";
	public static final String STRING_ESCAPE = "";
	public static final boolean INCLUDE_HEADER = false; 

	public abstract void outputInstanceData(InstanceData instanceData,
			String filename) throws IOException;

	public abstract void outputInstanceData(InstanceData instanceData,
			String filename, String fieldDelim, String recordDelim,
			String stringEscape, boolean includeHeader) throws IOException;

}