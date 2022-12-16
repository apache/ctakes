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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.semil.SemiLFormatterFactory.SemiLDataFormatter;


import com.google.common.base.Strings;

/**
 * for each train/test pair create the following files:
 * <ul>
 * <li>[prefix]class.txt - as in semil: instance id \t train/test flag \t target
 * class id</li
 * <li>[prefix]code.properties - map of codes to classes. currently only do a
 * one-against-all coding
 * <li>[prefix]code[n]_label.txt - for each class
 * </ul>
 * 
 * @author vijay
 * 
 */
public class SVMLinFormatterFactory implements SparseDataFormatterFactory {
	public static class SVMLinDataFormatter extends SemiLDataFormatter {
		private static final Log log = LogFactory
				.getLog(SVMLinDataFormatter.class);

		public SVMLinDataFormatter(KernelUtil kernelUtil) {
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
					// write the line
					writeLibsvmLine(wData, instanceValues);
				}
			} finally {
				if (wData != null) {
					wData.close();
				}
			}
		}

		/**
		 * recode the classes. the codes are bits in some sort of class coding
		 * scheme. this creates one-against-all codes.
		 * <p>
		 * creates [scope]code.properties. file to write the codes to. When
		 * parsing results, we will read this properties file.
		 * <p>
		 * creates [scope]code[n]_label.txt. Class label files for
		 * one-against-all classification.
		 * 
		 * @param trainInstanceIdToClass
		 *            map of training instance id to class id
		 * @return map of code to map of instance id - recoded class id
		 * @throws IOException
		 */
		private void exportOneAgainstAllCodes(String label, Integer run,
				Integer fold, SortedMap<Long, Integer> trainInstanceIdToClass,
				Map<Integer, String> codeToClassNameMap) throws IOException {
			// file to write the map between codes and classes
			String classFileName = FileUtil.getScopedFileName(outdir, label,
					run, fold, "code.properties");
			SortedSet<Integer> classIds = new TreeSet<Integer>();
			classIds.addAll(trainInstanceIdToClass.values());
			classIds.remove(0);
			// if there is only 1 class, abort
			if (classIds.size() < 2) {
				log.warn("<2 classes, skipping export for label " + label
						+ " run " + run + " fold " + fold);
				return;
			}
			Properties props = new Properties();
			StringBuilder bCodeList = new StringBuilder();
			int code = 1;
			Integer[] classIdArray = classIds.toArray(new Integer[0]);
			for (int i = 0; i < classIdArray.length; i++) {
				int classId = classIdArray[i];
				String className = codeToClassNameMap.get(classId);
				// recode the instances
				SortedMap<Long, Integer> mapRecodedInstanceIdToClass = new TreeMap<Long, Integer>();
				for (Map.Entry<Long, Integer> instanceIdToClassEntry : trainInstanceIdToClass
						.entrySet()) {
					int trainClassId = instanceIdToClassEntry.getValue();
					int codedClassId = 0; // default to unlabeled
					if (trainClassId == classId) {
						codedClassId = 1;
					} else if (trainClassId != 0) {
						codedClassId = -1;
					}
					mapRecodedInstanceIdToClass.put(
							instanceIdToClassEntry.getKey(), codedClassId);
				}
				String labelFileBaseName = FileUtil.getScopedFileName(outdir,
						label, run, fold,
						"class" + codeToClassNameMap.get(classId) + ".txt");
				exportLabel(labelFileBaseName, mapRecodedInstanceIdToClass);
				// add the map from code to class
				props.setProperty(labelFileBaseName + ".class",
						Integer.toString(classId));
				props.setProperty(labelFileBaseName + ".className", className);
				// add the key to the classWeights.properties file that will
				// have the positive class fraction.  the key is of the form 
				// label<label>_class<class>
				props.setProperty("classrel",
						formatWeightKey(label, className));
				// add the code to the list of codes
				bCodeList.append(labelFileBaseName).append(",");
				// if there are just 2 classes, stop here
				if (classIdArray.length == 2) {
					props.setProperty("classOther",
							Integer.toString(classIdArray[1]));
					props.setProperty("classOtherName",
							codeToClassNameMap.get(classIdArray[1]));
					break;
				}
				// increment the code
				code++;
			}
			props.setProperty("codes", bCodeList.toString());
			Writer w = null;
			try {
				w = new BufferedWriter(new FileWriter(classFileName));
				props.store(w, "oneAgainstAll");
			} finally {
				if (w != null) {
					try {
						w.close();
					} catch (Exception e) {
					}
				}
			}
			// return mapCodeToInstanceClass;
		}

		@Override
		public void initializeFold(SparseData sparseData, String label,
				Integer run, Integer fold,
				SortedMap<Boolean, SortedMap<Long, String>> foldInstanceLabelMap)
				throws IOException {
			if (SCOPE_FOLD.equals(this.exportProperties.getProperty(SCOPE))) {
				exportData(sparseData, label, run, fold);
			}
			String idFileName = FileUtil.getScopedFileName(outdir, label, run,
					fold, "id.txt");
			SortedMap<Long, Integer> trainInstanceIdToClass = super
					.getTrainingClassMap(idFileName,
							foldInstanceLabelMap.get(true),
							foldInstanceLabelMap.get(false),
							this.labelToClassIndexMap.get(label),
							sparseData.getInstanceIds());
			exportOneAgainstAllCodes(label, run, fold, trainInstanceIdToClass,
					this.labelToClassIndexMap.get(label).inverse());
		}
	}

	private KernelUtil kernelUtil;

	@Override
	public SparseDataFormatter getFormatter() {
		return new SVMLinDataFormatter(kernelUtil);
	}

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	public static String formatWeightKey(String label, String className) {
		return (Strings.isNullOrEmpty(label) ? "" : "label" + label
				+ "_")
				+ "class" + className;
	}

}
