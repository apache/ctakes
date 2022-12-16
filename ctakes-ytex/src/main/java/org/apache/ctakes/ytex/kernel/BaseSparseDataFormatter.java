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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import com.google.common.collect.BiMap;

public abstract class BaseSparseDataFormatter implements SparseDataFormatter {
	protected KernelUtil kernelUtil;

	/**
	 * directory to export files to, with trailing separator added on if
	 * necessary
	 */
	protected String outdir = null;
	/**
	 * map of numeric attribute - attribute index.
	 */
	protected Map<String, Integer> numericAttributeMap = new HashMap<String, Integer>();
	/**
	 * map of nominal attribute - [nominal attribute value - attribute index].
	 */
	protected Map<String, Map<String, Integer>> nominalAttributeMap = new HashMap<String, Map<String, Integer>>();
	/**
	 * map of label - [class name - class index]
	 */
	protected Map<String, BiMap<String, Integer>> labelToClassIndexMap = new HashMap<String, BiMap<String, Integer>>();
	/**
	 * 1-based attribute index
	 */
	protected int maxAttributeIndex = 0;
	/**
	 * export properties - properties file that controls what to do for this
	 * export
	 */
	protected Properties exportProperties;

	public BaseSparseDataFormatter(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	protected void exportAttributeNames(SparseData sparseData, String label,
			Integer run, Integer fold) throws IOException {
		// reset attribute name/index state
		this.nominalAttributeMap.clear();
		this.numericAttributeMap.clear();
		this.maxAttributeIndex = 0;
		// construct file name
		String filename = FileUtil.getScopedFileName(outdir, label, run, fold,
				"attributes.txt");
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(filename));
			// write attributes
			exportAttributeNames(w, sparseData);
		} finally {
			if (w != null)
				w.close();
		}
	}

	/**
	 * assign indices to each attribute.
	 * 
	 * @param outdir
	 *            directory to write file to
	 * @param sparseData
	 * @param numericAttributeMap
	 * @param nominalAttributeMap
	 *            for nominal indices, create an index for each value.
	 * @throws IOException
	 */
	protected int exportAttributeNames(BufferedWriter w, SparseData sparseData)
			throws IOException {
		// add numeric indices
		for (String attributeName : sparseData.getNumericWords()) {
			addNumericAttribute(w, attributeName);
		}
		// add nominal indices
		for (SortedMap.Entry<String, SortedSet<String>> nominalAttribute : sparseData
				.getNominalWordValueMap().entrySet()) {
			Map<String, Integer> attrValueIndexMap = new HashMap<String, Integer>(
					nominalAttribute.getValue().size());
			for (String attrValue : nominalAttribute.getValue()) {
				w.write(nominalAttribute.getKey());
				if (nominalAttribute.getValue().size() > 1) {
					w.write("\t");
					w.write(attrValue);
				}
				w.write("\n");
				attrValueIndexMap.put(attrValue, ++maxAttributeIndex);
			}
			nominalAttributeMap.put(nominalAttribute.getKey(),
					attrValueIndexMap);
		}
		return maxAttributeIndex;
	}

	protected void addNumericAttribute(BufferedWriter w, String attributeName)
			throws IOException {
		w.write(attributeName);
		w.write("\n");
		numericAttributeMap.put(attributeName, ++maxAttributeIndex);
	}

	/**
	 * create a map of attribute index - attribute value for the given instance.
	 * 
	 * @param bagOfWordsData
	 * @param numericAttributeMap
	 * @param nominalAttributeMap
	 * @param instanceId
	 * @return
	 */
	protected SortedMap<Integer, Double> getSparseLineValues(
			SparseData bagOfWordsData,
			Map<String, Integer> numericAttributeMap,
			Map<String, Map<String, Integer>> nominalAttributeMap,
			long instanceId) {
		SortedMap<Integer, Double> instanceValues = new TreeMap<Integer, Double>();
		// get numeric values for instance
		if (bagOfWordsData.getInstanceNumericWords().containsKey(instanceId)) {
			for (Map.Entry<String, Double> numericValue : bagOfWordsData
					.getInstanceNumericWords().get(instanceId).entrySet()) {
				// look up index for attribute and put in map
				instanceValues.put(
						numericAttributeMap.get(numericValue.getKey()),
						numericValue.getValue());
			}
		}
		if (bagOfWordsData.getInstanceNominalWords().containsKey(instanceId)) {
			for (Map.Entry<String, String> nominalValue : bagOfWordsData
					.getInstanceNominalWords().get(instanceId).entrySet()) {
				// look up index for attribute and value and put in map
				instanceValues.put(
						nominalAttributeMap.get(nominalValue.getKey()).get(
								nominalValue.getValue()), 1d);
			}
		}
		return instanceValues;
	}

	protected void exportSparseRow(SparseData bagOfWordsData, long instanceId,
			BufferedWriter wData, int row) throws IOException {
		SortedMap<Integer, Double> instanceValues = getSparseLineValues(
				bagOfWordsData, numericAttributeMap, nominalAttributeMap,
				instanceId);
		// write attributes
		// add the attributes
		for (SortedMap.Entry<Integer, Double> instanceValue : instanceValues
				.entrySet()) {
			// row = instance number
			wData.write(Integer.toString(row));
			wData.write("\t");
			// column = attribute index
			wData.write(Integer.toString(instanceValue.getKey()));
			wData.write("\t");
			// value = value
			// TODO fix me!
			// instance id formatted as double
			if (instanceValue.getKey() == 1) {
				wData.write(Long.toString(instanceValue.getValue().longValue()));
			} else {
				wData.write(Double.toString(instanceValue.getValue()));
			}
			wData.write("\n");
		}
	}

	/**
	 * export sparse matrix data for use in matlab/R. creates _data.txt with
	 * following columns:
	 * <ul>
	 * <li>row (int)
	 * <li>column (int)
	 * <li>cell value (double)
	 * </ul>
	 * also exports instance data (instance.txt). By default tab delimited
	 * without header. This can be read as a normal 3-column matrix into
	 * matlab/R, and then converted into a sparse matrix using
	 * Matrix::sparseMatrix (R) or sparse (matlab).
	 */
	protected void exportSparseMatrix(String filename, SparseData sparseData)
			throws IOException {
		BufferedWriter wData = null;
		try {
			wData = new BufferedWriter(new FileWriter(filename));
			int row = 1;
			for (long instanceId : sparseData.getInstanceIds()) {
				exportSparseRow(sparseData, instanceId, wData, row);
				row++;
			}
		} finally {
			if (wData != null)
				wData.close();
		}
	}

	// protected List<Integer> getInstanceIdsForScope(InstanceData
	// instanceLabel,
	// String label, Integer run, Integer fold) {
	// List<Integer> instanceIds = new ArrayList<Integer>();
	// SortedSet<Long> sortedInstanceIds = new TreeSet<Long>();
	// if (label == null || label.length() == 0) {
	// // add all instance ids
	// for (SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean,
	// SortedMap<Long, String>>>> runMap : instanceLabel.labelToInstanceMap
	// .values()) {
	// for (SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>
	// foldMap : runMap
	// .values()) {
	// for (SortedMap<Boolean, SortedMap<Long, String>> trainTestFold : foldMap
	// .values()) {
	// for (SortedMap<Long, String> trainMap : trainTestFold
	// .values())
	// sortedInstanceIds.addAll(trainMap.keySet());
	// }
	// }
	// }
	// } else if (label != null && label.length() > 0 && run == null) {
	// // label scope
	// }
	// return instanceIds;
	// }

	/**
	 * get needed properties out of outdir. convert class names into integers
	 * for libsvm. attempt to parse the class name into an integer. if this
	 * fails, use an index that we increment. index corresponds to class name's
	 * alphabetical order.
	 */
	@Override
	public void initializeExport(InstanceData instanceLabel,
			Properties properties, SparseData sparseData) throws IOException {
		this.exportProperties = properties;
		this.outdir = properties.getProperty("outdir");
		FileUtil.createOutdir(outdir);
		kernelUtil.fillLabelToClassToIndexMap(
				instanceLabel.getLabelToClassMap(), this.labelToClassIndexMap);
	}

	/**
	 * add the 'unlabeled' class id to the classIndexMap if it isn't there
	 * already
	 */
	protected void updateLabelClassMapTransductive() {
		for (Map<String, Integer> classIndexMap : labelToClassIndexMap.values()) {
			classIndexMap.put("0", 0);
		}
	}

}
