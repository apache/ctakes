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
import java.util.SortedMap;

import org.apache.ctakes.ytex.kernel.FileUtil;
import org.apache.ctakes.ytex.kernel.KernelUtil;
import org.apache.ctakes.ytex.kernel.SparseData;
import org.apache.ctakes.ytex.kernel.SparseDataFormatter;
import org.apache.ctakes.ytex.kernel.SparseDataFormatterFactory;
import org.apache.ctakes.ytex.semil.SemiLFormatterFactory.SemiLDataFormatter;


/**
 * For each scope, create a data.txt file. The SGT tools will be used to convert
 * this into an adjacency graph. For each train/test fold, create a label and
 * class file. The label file will be used by sgt for prediction, the class file
 * will be used to parse the results.
 * 
 * @author vijay
 * 
 */
public class SGTFormatterFactory implements SparseDataFormatterFactory {
	private KernelUtil kernelUtil;

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	@Override
	public SparseDataFormatter getFormatter() {
		return new SGTFormatter(kernelUtil);
	}

	public static class SGTFormatter extends SemiLDataFormatter {
		public SGTFormatter(KernelUtil kernelUtil) {
			super(kernelUtil);
		}

		@Override
		protected void exportData(SparseData sparseData, String label,
				Integer run, Integer fold) throws IOException {
			exportAttributeNames(sparseData, label, run, fold);
			String filename = FileUtil.getScopedFileName(outdir, label, run,
					fold, "data.txt");
			BufferedWriter wData = null;
			try {
				wData = new BufferedWriter(new FileWriter(filename));
				for (long instanceId : sparseData.getInstanceIds()) {
					// get line with sparse attribute indices and values
					SortedMap<Integer, Double> instanceValues = getSparseLineValues(
							sparseData, numericAttributeMap,
							nominalAttributeMap, instanceId);
					// the class is irrelevant - we create label files used by
					// sgt
					wData.write(Integer.toString(0));
					// write the line
					writeLibsvmLine(wData, instanceValues);
				}
			} finally {
				if (wData != null) {
					wData.close();
				}
			}
		}
	}

}
