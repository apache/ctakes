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
package org.apache.ctakes.ytex.libsvm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import org.apache.ctakes.ytex.kernel.BaseSparseDataFormatter;
import org.apache.ctakes.ytex.kernel.FileUtil;
import org.apache.ctakes.ytex.kernel.InstanceData;
import org.apache.ctakes.ytex.kernel.KernelUtil;
import org.apache.ctakes.ytex.kernel.SparseData;
import org.apache.ctakes.ytex.kernel.SparseDataFormatter;
import org.apache.ctakes.ytex.kernel.SparseDataFormatterFactory;

import com.google.common.collect.BiMap;


public class LibSVMFormatterFactory implements SparseDataFormatterFactory {
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
		return new LibSVMFormatter(getKernelUtil());
	}

	public static class LibSVMFormatter extends BaseSparseDataFormatter {
		@Override
		public void initializeExport(InstanceData instanceLabel,
				Properties properties, SparseData sparseData)
				throws IOException {
			super.initializeExport(instanceLabel, properties, sparseData);
		}

		public LibSVMFormatter(KernelUtil kernelUtil) {
			super(kernelUtil);
		}

		@Override
		public void initializeLabel(
				String label,
				SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>> labelInstances,
				Properties properties, SparseData sparseData)
				throws IOException {
			kernelUtil.exportClassIds(this.outdir,
					this.labelToClassIndexMap.get(label), label);
		}

		/**
		 * write a file with the attribute names corresponding to the indices in
		 * the libsvm data file
		 */
		@Override
		public void initializeFold(SparseData sparseData, String label,
				Integer run, Integer fold,
				SortedMap<Boolean, SortedMap<Long, String>> foldInstanceLabelMap)
				throws IOException {
			exportAttributeNames(sparseData, label, run, fold);
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
			exportDataForLabel(filename, idFilename, sparseData,
					instanceClassMap, this.labelToClassIndexMap.get(label));
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
		 * @throws IOException
		 */
		protected void exportDataForLabel(String filename, String idFilename,
				SparseData bagOfWordsData,
				SortedMap<Long, String> instanceClassMap,
				BiMap<String, Integer> classToIndexMap) throws IOException {
			BufferedWriter wData = null;
			BufferedWriter wId = null;
			try {
				wData = new BufferedWriter(new FileWriter(filename));
				wId = new BufferedWriter(new FileWriter(idFilename));
				exportDataForInstances(bagOfWordsData, instanceClassMap,
						classToIndexMap, wData, wId);
			} finally {
				if (wData != null)
					wData.close();
				if (wId != null)
					wId.close();
			}
		}

		/**
		 * 
		 * @param bagOfWordsData
		 *            data to be exported
		 * @param instanceClassMap
		 *            instance ids - class name map
		 * @param classToIndexMap
		 *            class name - class id map
		 * @param wData
		 *            file to write data to
		 * @param wId
		 *            file to write ids to
		 * @return list of instance ids corresponding to order with which they
		 *         were exported
		 * @throws IOException
		 */
		protected List<Long> exportDataForInstances(SparseData bagOfWordsData,
				SortedMap<Long, String> instanceClassMap,
				BiMap<String, Integer> classToIndexMap, BufferedWriter wData,
				BufferedWriter wId) throws IOException {
			List<Long> instanceIds = new ArrayList<Long>();
			for (Map.Entry<Long, String> instanceClass : instanceClassMap
					.entrySet()) {
				long instanceId = instanceClass.getKey();
				instanceIds.add(instanceId);
				// allocate line with sparse attribute indices and values
				SortedMap<Integer, Double> instanceValues = getSparseLineValues(
						bagOfWordsData, numericAttributeMap,
						nominalAttributeMap, instanceId);
				// data file
				// write class id
				int classId = classToIndexMap.get(instanceClass.getValue());
				// write id to id file
				wId.write(Long.toString(instanceId));
				wId.newLine();
				wData.write(Integer.toString(classId));
				// write attributes
				// add the attributes
				writeLibsvmLine(wData, instanceValues);
			}
			return instanceIds;
		}

		protected void writeLibsvmLine(BufferedWriter wData,
				SortedMap<Integer, Double> instanceValues) throws IOException {
			for (SortedMap.Entry<Integer, Double> instanceValue : instanceValues
					.entrySet()) {
				wData.write("\t");
				wData.write(Integer.toString(instanceValue.getKey()));
				wData.write(":");
				wData.write(Double.toString(instanceValue.getValue()));
			}
			wData.newLine();
		}

		/**
		 * clean up fold specific state
		 */
		@Override
		public void clearFold() {
			this.numericAttributeMap.clear();
			this.nominalAttributeMap.clear();
		}

		@Override
		public void clearLabel() {
		}
	}

}
