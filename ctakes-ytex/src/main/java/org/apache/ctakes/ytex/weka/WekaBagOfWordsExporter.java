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

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.ctakes.ytex.kernel.BagOfWordsDecorator;
import org.apache.ctakes.ytex.kernel.BagOfWordsExporter;


/**
 * @author vhacongarlav
 * 
 */
public interface WekaBagOfWordsExporter extends BagOfWordsExporter {

	/**
	 * @param arffRelation
	 *            relation of arff file to generate
	 * @param instanceClassQuery
	 *            query with result columns: column 1 - integer instance id,
	 *            column 2 - string class label
	 * @param numericWordQuery
	 *            query with result colums: column 1 - integer instance id,
	 *            column 2 - word, column 3 - numeric word value
	 * @param nominalWordQuery
	 *            query with result colums: column 1 - integer instance id,
	 *            column 2 - word, column 3 - string word value
	 * @param writer
	 *            where arff file will be written
	 * @throws IOException
	 */
	public abstract void exportBagOfWords(String arffRelation,
			String instanceClassQuery, String numericWordQuery,
			String nominalWordQuery, BufferedWriter writer) throws IOException;

	public abstract void exportBagOfWords(String propertyFile,
			BagOfWordsDecorator bDecorator) throws IOException;

}