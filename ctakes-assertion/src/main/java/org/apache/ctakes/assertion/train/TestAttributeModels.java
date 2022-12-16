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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.apache.ctakes.assertion.eval.AssertionEvaluation;
import org.apache.ctakes.assertion.util.AssertionConst;



/**
 * For each assertion attribute (polarity, conditional, etc), run against the test directories
 * for that attribute, using models that are under the models-dir.
 * Note that this uses constants within {@link AssertionConst} for the directory names.
 */
public class TestAttributeModels {
	final static String RUN_ID = "ytex_";

	public static void main(String[] args) throws Exception {
		
		for (String attribute : AssertionConst.annotationTypes) {
			
			ArrayList<String> params = new ArrayList<String>();
			AssertionEvaluation.useEvaluationLogFile = true;
			AssertionEvaluation.evaluationLogFilePath = "eval/"+RUN_ID+new Date().toString().replaceAll(" ","_") + ".txt";
			
			params.add("--test-dir"); 	params.add(AssertionConst.testDirectories.get(attribute));
			params.add("--models-dir"); params.add(AssertionConst.modelDirectory);
			params.add("--ytex-negation");
			params.add("--evaluation-output-dir");	params.add(AssertionConst.evalOutputDir);
			params.add("--test-only");	
			
			// Build up an "ignore" string
			for (String ignoreAttribute : AssertionConst.allAnnotationTypes) {
				if (!ignoreAttribute.equals(attribute)) { 

					if (ignoreAttribute.equals("historyOf")) {
						ignoreAttribute = ignoreAttribute.substring(0, ignoreAttribute.length()-2);
					}

					params.add("--ignore-" + ignoreAttribute);
				}
			}
			String[] paramList = params.toArray(new String[]{});
			
			System.out.println(Arrays.asList(paramList).toString());
			
			// Run the actual assertion test on just one attribute
			AssertionEvaluation.main( paramList );
		}
		
		
		
	}
}
