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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.ctakes.ytex.kernel.BaseSparseDataFormatter;
import org.apache.ctakes.ytex.kernel.FileUtil;
import org.apache.ctakes.ytex.kernel.InstanceData;
import org.apache.ctakes.ytex.kernel.KernelUtil;
import org.apache.ctakes.ytex.kernel.SparseData;
import org.apache.ctakes.ytex.kernel.SparseDataFormatter;
import org.apache.ctakes.ytex.kernel.SparseDataFormatterFactory;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;

public class WekaFormatterFactory implements SparseDataFormatterFactory {

	public static class WekaFormatter extends BaseSparseDataFormatter {
		public static final String CLASS = "ytex_class";

		public static final String INSTANCE_ID = "instance_id";
		InstanceData instanceLabel = null;
		FastVector wekaAttributes = null;
		public WekaFormatter(KernelUtil kernelUtil) {
			super(kernelUtil);
		}

		/**
		 * Add a nominal attribute to the list of attributes
		 * 
		 * @param attributeName
		 * @param attributeValues
		 * @param addDummy0
		 *            if true, add a dummy attribute as the first attribute
		 *            value in the list of potential attribute values.
		 */
		private void addNominalAttribute(String attributeName,
				SortedSet<String> attributeValues, boolean addDummy0) {
			FastVector wordValues = new FastVector(attributeValues.size()
					+ (addDummy0 ? 1 : 0));
			if (addDummy0) {
				String dummyName;
				if (!attributeValues.contains("null"))
					dummyName = "null";
				else if (!attributeValues.contains("0"))
					dummyName = "0";
				else
					dummyName = Long.toString(System.currentTimeMillis());
				wordValues.addElement(dummyName);
			}
			for (String wordValue : attributeValues) {
				wordValues.addElement(wordValue);
			}
			Attribute attribute = new Attribute(attributeName, wordValues);
			wekaAttributes.addElement(attribute);
		}

		/**
		 * add sparse data to instances
		 * 
		 * @param instances
		 * @param bagOfWordsData
		 * @param instanceClasses
		 * @throws IOException
		 */
		private void addWordsToInstances(Instances instances,
				SparseData bagOfWordsData,
				SortedMap<Long, String> instanceClasses) throws IOException {
			for (Map.Entry<Long, String> entry : instanceClasses.entrySet()) {
				double[] zeroValues = new double[instances.numAttributes()];
				Arrays.fill(zeroValues, 0.0d);
				SparseInstance wekaInstance = new SparseInstance(1.0d,
						zeroValues);
				wekaInstance.setDataset(instances);
				// set instance id
				Attribute instanceId = instances.attribute(INSTANCE_ID);
				wekaInstance.setValue(instanceId.index(), entry.getKey()
						.doubleValue());
				// set document class
				Attribute classAttr = instances.attribute(CLASS);
				wekaInstance.setValue(classAttr.index(),
						classAttr.indexOfValue(entry.getValue()));
				// set numeric words
				if (bagOfWordsData.getInstanceNumericWords()
						.get(entry.getKey()) != null) {
					for (Map.Entry<String, Double> word : bagOfWordsData
							.getInstanceNumericWords().get(entry.getKey())
							.entrySet()) {
						Attribute wordAttr = instances.attribute(word.getKey());
						wekaInstance.setValue(wordAttr.index(), word.getValue()
								.doubleValue());
					}
				}
				// set nominal words
				if (bagOfWordsData.getInstanceNominalWords()
						.get(entry.getKey()) != null) {
					for (Map.Entry<String, String> word : bagOfWordsData
							.getInstanceNominalWords().get(entry.getKey())
							.entrySet()) {
						Attribute wordAttr = instances.attribute(word.getKey());
						int valueIndex = wordAttr.indexOfValue(word.getValue());
						if (valueIndex == -1) {
							throw new IOException("oops! " + word);
						}
						wekaInstance.setValue(wordAttr.index(), valueIndex);
					}
				}
				instances.add(wekaInstance);
			}
		}

		@Override
		public void clearFold() {
		}

		@Override
		public void clearLabel() {
		}

		@Override
		public void exportFold(SparseData sparseData,
				SortedMap<Long, String> sortedMap, boolean train, String label,
				Integer run, Integer fold) throws IOException {
			Instances inst = initializeInstances(sparseData, sortedMap, train,
					label, run, fold);
			String filename = FileUtil.getDataFilePrefix(outdir, label, run,
					fold, train) + ".arff";
			ArffSaver saver = new ArffSaver();
			saver.setDestination(new File(filename));
			saver.setFile(new File(filename));
			saver.setInstances(inst);
			saver.writeBatch();
		}

		/**
		 * initialize attributes
		 * 
		 * @param bagOfWordsData
		 * @param classNames
		 */
		protected void initializeAttributes(SparseData bagOfWordsData,
				SortedSet<String> classNames) {
			wekaAttributes = new FastVector(bagOfWordsData.getNumericWords()
					.size()
					+ bagOfWordsData.getNominalWordValueMap().size()
					+ 2);
			// add instance id attribute
			wekaAttributes.addElement(new Attribute(INSTANCE_ID));
			// add numeric word attributes
			for (String word : bagOfWordsData.getNumericWords()) {
				Attribute attribute = new Attribute(word);
				wekaAttributes.addElement(attribute);
			}
			// add nominal word attributes
			for (Map.Entry<String, SortedSet<String>> nominalWordEntry : bagOfWordsData
					.getNominalWordValueMap().entrySet()) {
				addNominalAttribute(nominalWordEntry.getKey(),
						nominalWordEntry.getValue(), true);
			}
			// add class attribute
			addNominalAttribute(CLASS, classNames, false);
		}

		@Override
		public void initializeExport(InstanceData instanceLabel,
				Properties properties, SparseData sparseData)
				throws IOException {
			super.initializeExport(instanceLabel, properties, sparseData);
			this.instanceLabel = instanceLabel;
		}

		@Override
		public void initializeFold(SparseData sparseData, String label,
				Integer run, Integer fold,
				SortedMap<Boolean, SortedMap<Long, String>> foldInstanceLabelMap)
				throws IOException {
			if (SCOPE_FOLD.equals(exportProperties.getProperty(SCOPE))) {
				this.initializeAttributes(sparseData, instanceLabel
						.getLabelToClassMap().get(label));
			}
		}

		/**
		 * initialize the weka Instances
		 * 
		 * @param arffRelation
		 * @param sql
		 * @param classLabels
		 * @param idfMap
		 * @param docLengthMap
		 * @return
		 * @throws IOException
		 */
		public Instances initializeInstances(SparseData sparseData,
				SortedMap<Long, String> instanceClasses, boolean train,
				String label, Integer run, Integer fold) throws IOException {
			// add label, run, fold, train/test to relation
			String arffRelation = this.exportProperties.getProperty(
					"arffRelation", "org.apache.ctakes.ytex");
			String relation = arffRelation + "_"
					+ FileUtil.getDataFilePrefix(null, label, run, fold, train);
			Instances instances = new Instances(relation, wekaAttributes, 0);
			instances.setClassIndex(instances.numAttributes() - 1);
			// add instances
			addWordsToInstances(instances, sparseData, instanceClasses);
			return instances;
		}

		/**
		 * initialize attributes on a per-label basis even if the data is the
		 * same across all labels. The class ids might be different for
		 * different labels.
		 */
		@Override
		public void initializeLabel(
				String label,
				SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>> labelInstances,
				Properties properties, SparseData sparseData)
				throws IOException {
			if (SCOPE_LABEL.equals(properties.getProperty(SCOPE))
					|| properties.getProperty(SCOPE) == null
					|| properties.getProperty(SCOPE).length() == 0) {
				this.initializeAttributes(sparseData, instanceLabel
						.getLabelToClassMap().get(label));
			}
		}

	}

	private KernelUtil kernelUtil;

	public SparseDataFormatter getFormatter() {
		return new WekaFormatter(this.getKernelUtil());
	}

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}
}
