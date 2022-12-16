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
package org.apache.ctakes.ytex.sparsematrix;

import java.io.BufferedWriter;
import java.io.IOException;
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
import org.apache.ctakes.ytex.libsvm.LibSVMFormatterFactory.LibSVMFormatter;


/**
 * export sparse data matrix to data.txt. Data can be scoped to label/fold.
 * instance_id added as first column in data matrix. instance data (class,
 * label, run, fold) exported to instance.txt.
 * 
 * @todo add options to control instance.txt output format.
 * @see BaseSparseDataFormatter#exportSparseMatrix
 * @author vijay
 */
public class SparseMatrixFormatterFactory implements SparseDataFormatterFactory {

	InstanceDataExporter instanceDataExporter;
	KernelUtil kernelUtil;

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	public InstanceDataExporter getInstanceDataExporter() {
		return instanceDataExporter;
	}

	public void setInstanceDataExporter(
			InstanceDataExporter instanceDataExporter) {
		this.instanceDataExporter = instanceDataExporter;
	}

	@Override
	public SparseDataFormatter getFormatter() {
		return new SparseMatrixDataFormatter(getInstanceDataExporter(),
				getKernelUtil());
	}

	public static class SparseMatrixDataFormatter extends LibSVMFormatter {
		InstanceData instanceLabel;
		InstanceDataExporter instanceDataExporter;

		public SparseMatrixDataFormatter(
				InstanceDataExporter instanceDataExporter, KernelUtil kernelUtil) {
			super(kernelUtil);
			this.instanceDataExporter = instanceDataExporter;
		}

		@Override
		public void initializeExport(InstanceData instanceLabel,
				Properties properties, SparseData sparseData)
				throws IOException {
			super.initializeExport(instanceLabel, properties, sparseData);
			this.instanceLabel = instanceLabel;
			instanceDataExporter.outputInstanceData(instanceLabel,
					FileUtil.addFilenameToDir(outdir, "instance.txt"));
			if (properties.getProperty(SCOPE) == null
					|| properties.getProperty(SCOPE).length() == 0) {
				exportSparseMatrix(sparseData, null, null, null);
			}
		}

		@Override
		public void initializeLabel(
				String label,
				SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>> labelInstances,
				Properties properties, SparseData sparseData)
				throws IOException {
			// super.initializeLabel(label, labelInstances, properties,
			// sparseData);
			if (SCOPE_LABEL.equals(this.exportProperties.getProperty(SCOPE))) {
				exportSparseMatrix(sparseData, label, null, null);
			}
		}

		@Override
		public void initializeFold(SparseData sparseData, String label,
				Integer run, Integer fold,
				SortedMap<Boolean, SortedMap<Long, String>> foldInstanceLabelMap)
				throws IOException {
			if (SCOPE_FOLD.equals(this.exportProperties.getProperty(SCOPE))) {
				exportSparseMatrix(sparseData, label, run, fold);
			}
		}

		@Override
		public void exportFold(SparseData sparseData,
				SortedMap<Long, String> instanceClassMap, boolean train,
				String label, Integer run, Integer fold) throws IOException {
			// do nothing
		}

		private void exportSparseMatrix(SparseData sparseData, String label,
				Integer run, Integer fold) throws IOException {
			exportAttributeNames(sparseData, label, run, fold);
			String filename = FileUtil.getScopedFileName(outdir, label, run,
					fold, "data.txt");
			this.exportSparseMatrix(filename, sparseData);
		}

		/**
		 * add instance_id as first in the map of numeric attribute
		 */
		protected int exportAttributeNames(BufferedWriter w,
				SparseData sparseData) throws IOException {
			super.addNumericAttribute(w, ATTR_INSTANCE_ID);
			return super.exportAttributeNames(w, sparseData);
		}

		/**
		 * add instance_id to sparse line values
		 */
		@Override
		protected SortedMap<Integer, Double> getSparseLineValues(
				SparseData bagOfWordsData,
				Map<String, Integer> numericAttributeMap,
				Map<String, Map<String, Integer>> nominalAttributeMap,
				long instanceId) {
			SortedMap<Integer, Double> instanceLine = super
					.getSparseLineValues(bagOfWordsData, numericAttributeMap,
							nominalAttributeMap, instanceId);
			instanceLine.put(this.numericAttributeMap.get(ATTR_INSTANCE_ID),
					(double) instanceId);
			return instanceLine;
		}

		// /**
		// * id - export 2-column matrix column 1 - instance id column 2 - class
		// * id
		// *
		// * data - export 3-column matrix column 1 - row index column 2 -
		// column
		// * index column 3 - value
		// *
		// * @param filename
		// * @param idFilename
		// * @param bagOfWordsData
		// * @param instanceClassMap
		// * @param numericAttributeMap
		// * @param nominalAttributeMap
		// * @param label
		// * @throws IOException
		// */
		// protected void exportDataForLabel(String filename, String idFilename,
		// SparseData bagOfWordsData,
		// SortedMap<Integer, String> instanceClassMap,
		// Map<String, Integer> classToIndexMap) throws IOException {
		// BufferedWriter wData = null;
		// BufferedWriter wId = null;
		// try {
		// wData = new BufferedWriter(new FileWriter(filename));
		// wId = new BufferedWriter(new FileWriter(idFilename));
		// // iterate over rows
		// int row = 1;
		// for (Map.Entry<Integer, String> instanceClass : instanceClassMap
		// .entrySet()) {
		// int instanceId = instanceClass.getKey();
		// // allocate line with sparse attribute indices and values
		// SortedMap<Integer, Double> instanceValues = getSparseLineValues(
		// bagOfWordsData, numericAttributeMap,
		// nominalAttributeMap, instanceId);
		// // data file
		// // write class id
		// int classId = classToIndexMap.get(instanceClass.getValue());
		// wId.write(Integer.toString(instanceId));
		// wId.write("\t");
		// wId.write(Integer.toString(classId));
		// wId.write("\n");
		// // write attributes
		// exportSparseRow(bagOfWordsData, instanceId, wData, row);
		// // increment row index
		// row++;
		// }
		// } finally {
		// if (wData != null)
		// wData.close();
		// if (wId != null)
		// wId.close();
		// }
		// }
	}

}
