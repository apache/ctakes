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

import java.io.File;
import java.io.IOException;

public interface ClassifierEvaluationParser {
	public static final String YES = "yes";
	public static final String NO = "no";

	/**
	 * Property keys for various parse options
	 * 
	 * @author vhacongarlav
	 */
	public enum ParseOption {
		/**
		 * key <tt>kernel.StoreInstanceEval</tt>.
		 * 
		 */
		STORE_INSTANCE_EVAL("kernel.StoreInstanceEval", NO),
		/**
		 * key <tt>kernel.StoreProbabilities</tt>.
		 */
		STORE_PROBABILITIES("kernel.StoreProbabilities", NO),
		/**
		 * key <tt>kernel.StoreUnlabeled</tt>
		 */
		STORE_UNLABELED("kernel.StoreUnlabeled", NO),
		/**
		 * key <tt>kernel.StoreIRStats</tt>
		 */
		STORE_IRSTATS("kernel.StoreIRStats", YES),
		/**
		 * key <tt>kernel.data.basename</tt>
		 * base name of file; other file names constructed relative to this.
		 * label/fold/run taken from this file name. 
		 */
		DATA_BASENAME("kernel.data.basename", ""),
//		/**
//		 * key <tt>kernel.distance</tt>
//		 * distance measure for semiL. default is euclidean.
//		 */
//		DISTANCE("kernel.distance", "euclidean"),
//		/**
//		 * key <tt>kernel.degree</tt>
//		 * degree for knn graph for semiL. default is 10
//		 */
//		DEGREE("kernel.degree", "10"),
		/**
		 * key <tt>kernel.train.line</tt>
		 * options used to train model.
		 */
		EVAL_LINE("kernel.train.line", null);
		String optionKey;
		public String getOptionKey() {
			return optionKey;
		}

		String defaultValue;

		public String getDefaultValue() {
			return defaultValue;
		}

		ParseOption(String optionKey, String defaultValue) {
			this.optionKey = optionKey;
			this.defaultValue = defaultValue;
		}
	}

	public void parseDirectory(File dataDir, File outputDir) throws IOException;

}