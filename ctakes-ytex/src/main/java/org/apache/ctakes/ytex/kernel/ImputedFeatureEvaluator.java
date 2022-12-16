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

import org.apache.ctakes.ytex.kernel.ImputedFeatureEvaluatorImpl.Parameters;


public interface ImputedFeatureEvaluator {
	public enum MeasureType {
		MUTUALINFO("mutualinfo"), INFOGAIN("infogain");
		String name;

		public String getName() {
			return name;
		}

		MeasureType(String name) {
			this.name = name;
		}
	};

	public static final String SUFFIX_PROP = "-propagated";
	public static final String SUFFIX_IMPUTED = "-imputed";
	public static final String SUFFIX_IMPUTED_FILTERED = "-imputed-filt";

	public abstract boolean evaluateCorpus(String propFile) throws IOException;

	boolean evaluateCorpus(Parameters params);

}