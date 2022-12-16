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
package org.apache.ctakes.ytex.weka;

import java.io.IOException;

import org.apache.ctakes.ytex.kernel.KernelContextHolder;


public class ExportGramMatrix {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out
					.println("usage: java org.apache.ctakes.ytex.kernel.ExportGramMatrix <property file>");
		} else {
			GramMatrixExporter g = (GramMatrixExporter) KernelContextHolder
					.getApplicationContext().getBean("gramMatrixExporter");
			g.exportGramMatrix(args[0]);
		}
	}

}
