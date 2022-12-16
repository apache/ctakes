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

import java.util.Properties;

public interface WekaAttributeEvaluator {

	/**
	 * evaluate attributes in an arff file, save rank in db
	 * 
	 * @param name
	 *            corresponds to feature_eval.name
	 * @param corpusName
	 *            cv_fold.name
	 * @param arffFile
	 * @throws Exception
	 */
	public abstract void evaluateAttributesFromFile(String corpusName,
			String featureSetName, String splitName, String arffFile)
			throws Exception;

	/**
	 * create instances from properties file, evaluate, save in db
	 * 
	 * @param name
	 *            feature set name. corresponds to feature_eval.name
	 * @param corpusName
	 *            cv_fold.name
	 * @param propFile
	 *            for SparseDataExporter
	 * @throws Exception
	 */
	public abstract void evaluateAttributesFromProps(String corpusName,
			String splitName, String featureSetName, Properties props)
			throws Exception;

}