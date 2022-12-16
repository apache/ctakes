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
import java.util.Properties;
import java.util.SortedMap;

/**
 * stateful class called by the sparseDataExporter to export sparse data in a
 * specific format. This is created, called for each fold, training, and test
 * set, then thrown away.
 * 
 * @author vijay
 * 
 */
public interface SparseDataFormatter {

	/**
	 * scope property key
	 */
	public static final String SCOPE = "scope";
	/**
	 * fold value for scope
	 */
	public static final String SCOPE_FOLD = "fold";
	/**
	 * label value for scope
	 */
	public static final String SCOPE_LABEL = "label";
	/**
	 * value <tt>instance_id</tt>. SparseMatrix adds the instance_id attribute
	 * to the matrix. This is a reserved attribute name.
	 */
	public static final String ATTR_INSTANCE_ID = "instance_id";

	/**
	 * initialize data structures for the fold that will be exported. called
	 * before export.
	 * 
	 * @param sparseData
	 * @param label
	 * @param run
	 * @param fold
	 * @param foldInstanceLabelMap
	 * @throws IOException
	 */
	void initializeFold(SparseData sparseData, String label, Integer run,
			Integer fold,
			SortedMap<Boolean, SortedMap<Long, String>> foldInstanceLabelMap)
			throws IOException;

	/**
	 * export the fold train/test set. called once per train/test set, 2x per
	 * fold.
	 * 
	 * @param sparseData
	 * @param sortedMap
	 * @param train
	 * @param label
	 * @param run
	 * @param fold
	 * @throws IOException
	 */
	void exportFold(SparseData sparseData,
			SortedMap<Long, String> sortedMap, boolean train, String label,
			Integer run, Integer fold) throws IOException;

	/**
	 * initialize export - called once
	 * 
	 * @param instanceLabel
	 * @param properties
	 * @throws IOException
	 */
	void initializeExport(InstanceData instanceLabel, Properties properties,
			SparseData sparseData) throws IOException;

	/**
	 * clear all data structures set up during initializeFold
	 */
	void clearFold();

	void initializeLabel(
			String label,
			SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>> labelInstances,
			Properties properties, SparseData sparseData) throws IOException;

	void clearLabel();

}
