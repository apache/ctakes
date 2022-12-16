/*
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
package org.apache.ctakes.coreference.util;

import libsvm.svm_node;

public class SvmUtils {
	/* Convert a 'vector' in string format into the libsvm format */
	public static svm_node[] getNodes(String nodeStr) {
		String[] vals = nodeStr.substring(2).split(" ");
		svm_node[] nodes = new svm_node[vals.length];
		for(int i = 0; i < vals.length; i++){
			String[] map = vals[i].split(":");
			nodes[i] = new svm_node();
			nodes[i].index = Integer.parseInt(map[0]);
			nodes[i].value = Double.parseDouble(map[1]);
		}
		return nodes;
	}

}
