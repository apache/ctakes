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
package org.apache.ctakes.assertion.train;

import java.io.File;
import java.util.ArrayList;

import org.apache.ctakes.assertion.eval.AssertionEvaluation;
import org.apache.ctakes.assertion.util.AssertionConst;

public class ReadAndPreprocessForAttributeModels {

	public static void main(String[] args) throws Exception {
		
		for (String source : AssertionConst.preprocessRootDirectory.keySet()) {
			
			ArrayList<String> params = new ArrayList<String>();

			// Always preprocess something to a main directory, usually for training
			String froot = AssertionConst.preprocessRootDirectory.get(source);
			if (!(new File(froot).exists())) {
				File f = new File(froot);
				if (!f.getParentFile().exists()) {
					throw new RuntimeException("Can't find parent " + f.getParentFile().getPath());
				}
				if (!f.getParentFile().isDirectory()) {
					throw new RuntimeException("What should be the parent is not a directory " + f.getParentFile().getPath());
				}
				(new File(froot)).mkdir();
			}
			params.add("--train-dir"); 		params.add(froot);

			// Some corpora (SHARP) may have predetermined dev/test splits. Check {link: AssertionConst}.
			if (AssertionConst.preprocessForDev.containsKey(source) ) {
				String fdev = AssertionConst.preprocessForDev.get(source);
				if (!(new File(fdev).exists())) {
					(new File(fdev)).mkdir();
				}
				params.add("--dev-dir"); 	params.add(fdev);
			}
			if (AssertionConst.preprocessForTest.containsKey(source) ) {
				String ftest = AssertionConst.preprocessForTest.get(source);
				if (!(new File(ftest).exists())) {
					(new File(ftest)).mkdir();
				}
				params.add("--test-dir"); 	params.add(ftest);
			}
			
			// Specify preprocessing directory (See AssertionConst)
			params.add("--preprocess-only"); 	params.add(source);
			
			String[] paramList = params.toArray(new String[]{});
			
//			System.out.println(Arrays.asList(paramList).toString());
			
			// Run the actual assertion preprocessing on just one data source
			AssertionEvaluation.main( paramList );
		}
		
		
		
	}
}
