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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import org.apache.ctakes.ytex.kernel.model.KernelEvaluation;

import com.google.common.collect.BiMap;


public interface KernelUtil {

	public abstract void loadProperties(String propertyFile, Properties props)
			throws FileNotFoundException, IOException,
			InvalidPropertiesFormatException;

	/**
	 * read query
	 * 
	 * <pre>
	 * [instance id] [class name] [train/test boolean optioanl] [label optional] [fold optional] [run optional]
	 * </pre>
	 * 
	 * return map of
	 * 
	 * <pre>
	 * [label, [run, [fold, [train/test , [instance id, class]]]]]
	 * </pre>
	 * 
	 * <ul>
	 * <li>if label not defined, will be ""
	 * <li>if run not defined, will be 0
	 * <li>if fold not defined, will be 0
	 * <li>if train not defined, will be 1
	 * </ul>
	 * 
	 */
	public abstract InstanceData loadInstances(String strQuery);

	public abstract void fillGramMatrix(
			final KernelEvaluation kernelEvaluation,
			final SortedSet<Long> trainInstanceLabelMap,
			final double[][] trainGramMatrix);

	public abstract double[][] loadGramMatrix(SortedSet<Long> instanceIds,
			String name, String splitName, String experiment, String label,
			int run, int fold, double param1, String param2);

	/**
	 * generate folds from the label to instance map. use properties specified
	 * in props to generate folds.
	 * 
	 * @param instanceLabel
	 * @param props
	 */
	public abstract void generateFolds(InstanceData instanceLabel,
			Properties props);

	public abstract void fillLabelToClassToIndexMap(
			Map<String, SortedSet<String>> labelToClasMap,
			Map<String, BiMap<String, Integer>> labelToClassIndexMap);

	/**
	 * export the class id to class name map.
	 * 
	 * @param classIdMap
	 * @param label
	 * @param run
	 * @param fold
	 * @throws IOException
	 */
	public void exportClassIds(String outdir, Map<String, Integer> classIdMap,
			String label) throws IOException;
}