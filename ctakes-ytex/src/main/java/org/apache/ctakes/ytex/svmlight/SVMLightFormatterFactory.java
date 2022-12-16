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
package org.apache.ctakes.ytex.svmlight;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.ctakes.ytex.kernel.FileUtil;
import org.apache.ctakes.ytex.kernel.InstanceData;
import org.apache.ctakes.ytex.kernel.KernelUtil;
import org.apache.ctakes.ytex.kernel.SparseData;
import org.apache.ctakes.ytex.kernel.SparseDataFormatter;
import org.apache.ctakes.ytex.kernel.SparseDataFormatterFactory;
import org.apache.ctakes.ytex.libsvm.LibSVMFormatterFactory.LibSVMFormatter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


/**
 * export for svmlight. Same format as for libsvm with following changes:
 * <ul>
 * <li>
 * Only binary classification - classes must be -1 or 1
 * <li>Transductive classification: test instances 'unlabelled' in training set
 * (class 0)
 * <ul/>
 * If a test set is available, will use transductive svm format.
 */
public class SVMLightFormatterFactory implements SparseDataFormatterFactory {
	KernelUtil kernelUtil;

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.libsvm.SparseDataFormatterFactory#getFormatter()
	 */
	@Override
	public SparseDataFormatter getFormatter() {
		return new SVMLightFormatter(this.getKernelUtil());
	}

	public static class SVMLightFormatter extends LibSVMFormatter {
		protected SortedMap<Boolean, SortedMap<Long, String>> foldInstanceLabelMap;

		public SVMLightFormatter(KernelUtil kernelUtil) {
			super(kernelUtil);
		}

		/**
		 * export the given train/test set
		 */
		@Override
		public void exportFold(SparseData sparseData,
				SortedMap<Long, String> instanceClassMap, boolean train,
				String label, Integer run, Integer fold) throws IOException {
			String filename = FileUtil.getDataFilePrefix(outdir, label, run,
					fold, train) + "_data.txt";
			String idFilename = FileUtil.getDataFilePrefix(outdir, label, run,
					fold, train) + "_id.txt";
			if (train && this.foldInstanceLabelMap.size() == 2) {
				// train and test set available - export transductive data
				exportTransductiveData(filename, idFilename, sparseData,
						instanceClassMap,
						this.foldInstanceLabelMap.get(Boolean.FALSE).keySet(),
						this.labelToClassIndexMap.get(label));
			} else {
				// test set, or training set only
				// 'normal' export
				super.exportDataForLabel(filename, idFilename, sparseData,
						instanceClassMap, labelToClassIndexMap.get(label));
			}
		}

		/**
		 * Export data file and id file
		 * 
		 * @param filename
		 * @param idFilename
		 * @param bagOfWordsData
		 * @param instanceClassMap
		 * @param numericAttributeMap
		 * @param nominalAttributeMap
		 * @param label
		 * @return instance ids in order they are in the output file
		 * @throws IOException
		 */
		protected List<Long> exportTransductiveData(String filename,
				String idFilename, SparseData bagOfWordsData,
				SortedMap<Long, String> trainClassMap, Set<Long> testInstances,
				BiMap<String, Integer> classToIndexMap) throws IOException {
			List<Long> instanceIds = new ArrayList<Long>();
			BufferedWriter wData = null;
			BufferedWriter wId = null;
			try {
				wData = new BufferedWriter(new FileWriter(filename));
				wId = new BufferedWriter(new FileWriter(idFilename));
				instanceIds.addAll(exportDataForInstances(bagOfWordsData,
						trainClassMap, classToIndexMap, wData, wId));
				SortedMap<Long, String> testClassMap = new TreeMap<Long, String>();
				for (Long instanceId : testInstances) {
					// for sparse datasets may duplicate instances in train/test
					// set. Don't do that for transductive learning
					if (!trainClassMap.containsKey(instanceId))
						testClassMap.put(instanceId, "0");
				}
				instanceIds.addAll(exportDataForInstances(bagOfWordsData,
						testClassMap, classToIndexMap, wData, wId));
				return instanceIds;
			} finally {
				if (wData != null)
					wData.close();
				if (wId != null)
					wId.close();
			}
		}

		/**
		 * create a map of attribute index - attribute value for the given
		 * instance.
		 * 
		 * @param bagOfWordsData
		 * @param numericAttributeMap
		 * @param nominalAttributeMap
		 * @param instanceId
		 * @return
		 */
		// protected SortedMap<Integer, Double> getSparseLineValues(
		// SparseData bagOfWordsData,
		// Map<String, Integer> numericAttributeMap,
		// Map<String, Map<String, Integer>> nominalAttributeMap,
		// int instanceId) {
		// SortedMap<Integer, Double> instanceValues = new TreeMap<Integer,
		// Double>();
		// // get numeric values for instance
		// if (bagOfWordsData.getInstanceNumericWords()
		// .containsKey(instanceId)) {
		// for (Map.Entry<String, Double> numericValue : bagOfWordsData
		// .getInstanceNumericWords().get(instanceId).entrySet()) {
		// // look up index for attribute and put in map
		// instanceValues.put(numericAttributeMap.get(numericValue
		// .getKey()), numericValue.getValue());
		// }
		// }
		// if (bagOfWordsData.getInstanceNominalWords()
		// .containsKey(instanceId)) {
		// for (Map.Entry<String, String> nominalValue : bagOfWordsData
		// .getInstanceNominalWords().get(instanceId).entrySet()) {
		// // look up index for attribute and value and put in map
		// instanceValues.put(
		// nominalAttributeMap.get(nominalValue.getKey()).get(
		// nominalValue.getValue()), 1d);
		// }
		// }
		// return instanceValues;
		// }

		/**
		 * add the "0" class for transductive learning
		 */
		@Override
		public void initializeExport(InstanceData instanceLabel,
				Properties properties, SparseData sparseData)
				throws IOException {
			this.exportProperties = properties;
			this.outdir = properties.getProperty("outdir");
			FileUtil.createOutdir(outdir);
			fillLabelToClassIndexMap(instanceLabel.getLabelToClassMap());
		}

		protected void fillLabelToClassIndexMap(Map<String, SortedSet<String>> labelToClassMap) {
			for (Map.Entry<String, SortedSet<String>> labelToClass : labelToClassMap.entrySet()) {
				BiMap<String, Integer> classToIndexMap = HashBiMap.create();
				labelToClassIndexMap
						.put(labelToClass.getKey(), classToIndexMap);
				if (labelToClass.getValue().size() == 2) {
					// use +1 and -1 for binary classification
					classToIndexMap.put(labelToClass.getValue().first(), -1);
					classToIndexMap.put(labelToClass.getValue().last(), 1);
				} else {
					int nIndex = 1;
					for (String className : labelToClass.getValue()) {
						Integer classNumber = null;
						try {
							classNumber = Integer.parseInt(className);
						} catch (NumberFormatException fe) {
						}
						if (classNumber == null) {
							classToIndexMap.put(className, nIndex++);
						} else {
							classToIndexMap.put(className, classNumber);
						}
					}
				}
			}
			updateLabelClassMapTransductive();
		}

		/**
		 * clean up fold specific state
		 */
		@Override
		public void clearFold() {
			this.numericAttributeMap.clear();
			this.nominalAttributeMap.clear();
			this.foldInstanceLabelMap = null;
		}

		@Override
		public void initializeFold(SparseData sparseData, String label,
				Integer run, Integer fold,
				SortedMap<Boolean, SortedMap<Long, String>> foldInstanceLabelMap)
				throws IOException {
			super.initializeFold(sparseData, label, run, fold,
					foldInstanceLabelMap);
			this.foldInstanceLabelMap = foldInstanceLabelMap;
		}

	}

}
