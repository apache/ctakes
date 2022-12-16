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

import java.util.List;

/**
 * Import the weka results for the specified instance.
 */
public interface WekaResultInstanceImporter {
	/**
	 * 
	 * @param instanceNumber
	 *            optional instance number when multiple instance classified,
	 *            used for outputting errors/warnings
	 * @param instanceKey
	 *            list of attributes to resolve instance - foreign key to
	 *            document/sentence/whatever
	 * @param task
	 *            classification task
	 * @param classAuto
	 *            classifer's predicted class
	 * @param classGold
	 *            gold standard class index
	 * @param prediction
	 *            probabilities of belonging to specified classes, if run with
	 *            -distribution option. else just probability of belonging to
	 *            predicted class.
	 */
	public void importInstanceResult(Integer instanceNumber,
			List<String> instanceKey, String task, int classAuto,
			int classGold, List<Double> predictions);
}
