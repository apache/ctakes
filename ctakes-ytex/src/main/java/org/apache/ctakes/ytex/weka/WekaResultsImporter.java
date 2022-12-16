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

import java.io.BufferedReader;
import java.io.IOException;

/**
 * parse weka instance output when classifier run with -p option. load results
 * into db.
 */
public interface WekaResultsImporter {

	/**
	 * load results into document class table. the document id must be the first
	 * attribute in the column of output values.
	 * 
	 * @return
	 */
	public abstract void importDocumentResults(String task,
			BufferedReader reader) throws IOException;

	/**
	 * Parse weka output file, pass results to the specified importer to save
	 * results
	 * 
	 * @param resultInstanceImporter
	 * @param task
	 * @param reader
	 * @throws IOException
	 */
	public abstract void importResults(
			WekaResultInstanceImporter resultInstanceImporter, String task,
			BufferedReader reader) throws IOException;

	public void importClassifierEvaluation(String name, Integer fold,
			String algorithm, String label, String options, String experiment,
			BufferedReader reader) throws IOException;

}