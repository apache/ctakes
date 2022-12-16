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
package org.apache.ctakes.ytex.semil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.ctakes.ytex.kernel.FileUtil;
import org.apache.ctakes.ytex.kernel.InstanceData;
import org.apache.ctakes.ytex.kernel.KernelUtil;
import org.apache.ctakes.ytex.kernel.SparseData;
import org.apache.ctakes.ytex.kernel.SparseDataFormatter;
import org.apache.ctakes.ytex.kernel.SparseDataFormatterFactory;
import org.apache.ctakes.ytex.svmlight.SVMLightFormatterFactory.SVMLightFormatter;
import org.apache.ctakes.ytex.weka.WekaFormatterFactory.WekaFormatter;


/**
 * Export data for use with SemiL. I would have liked to have computed the
 * distance using the COLT library; however this was far too slow.
 * 
 * Produce following files:
 * <ul>
 * <li>[scope]_data.txt - sparse data file. Can be converted into distance
 * matrix for SemiL using R or Matlab. R script provided. If you want to use
 * semiL to generate a distance matrix, use the libsvm formatter.
 * <li>[fold]_label.txt - semiL label file, one for each fold. class labels
 * corresponding to rows. test data automatically unlabeled. The same
 * data/distance matrix can be used with different label files - the rows across
 * folds refer to the same instance ids. What differs is the labels for the test
 * instances (0 for test instance's fold).
 * <li>[fold]_class.txt - contains instance ids and target class ids for each
 * fold. matrix with 3 columns: instance id, train/test, target class id. Used
 * by SemiLEvaluationParser to evaluate SemiL predictions.
 * </ul>
 * 
 * @author vhacongarlav
 * 
 */
public class SemiLFormatterFactory implements SparseDataFormatterFactory {
	
	public static class SemiLDataFormatter extends SVMLightFormatter {
		protected InstanceData instanceLabel = null;

		NumberFormat semilNumberFormat = new DecimalFormat("#.######");
		public SemiLDataFormatter(KernelUtil kernelUtil) {
			super(kernelUtil);
		}

		// /**
		// * cosine distance: <tt>1-aa'/sqrt(aa' * bb')</tt>
		// */
		// public static Statistic.VectorVectorFunction COSINE = new
		// VectorVectorFunction() {
		// DoubleDoubleFunction fun = new DoubleDoubleFunction() {
		// public final double apply(double a, double b) {
		// return Math.abs(a - b) / Math.abs(a + b);
		// }
		// };
		//
		// public final double apply(DoubleMatrix1D a, DoubleMatrix1D b) {
		// double ab = a.zDotProduct(b);
		// double sqrt_ab = Math.sqrt(a.zDotProduct(a) * b.zDotProduct(b));
		// return 1 - ab / sqrt_ab;
		// }
		// };

		/**
		 * 
		 * @param foldInstanceLabelMap
		 * @param sparseData
		 * @param label
		 * @param run
		 * @param fold
		 * @throws IOException
		 */
		protected void exportData(SparseData sparseData, String label,
				Integer run, Integer fold) throws IOException {
			exportAttributeNames(sparseData, label, run, fold);
			String filename = FileUtil.getScopedFileName(outdir, label, run,
					fold, "data.txt");
			exportSparseMatrix(filename, sparseData);

		}

		@Override
		public void exportFold(SparseData sparseData,
				SortedMap<Long, String> instanceClassMap, boolean train,
				String label, Integer run, Integer fold) throws IOException {
			// do nothing
		}

		/**
		 * Write the 'label' file. This contains the training labels (label !=
		 * 0) and test labels (labeled as 0) and unlabeled data (labeled as 0).
		 * The order of the label file corresponds to the order in the data
		 * file.
		 * 
		 * @param lblFilename
		 *            filename to write to
		 * @param mapInstanceIdToClass
		 *            sorted map of instance id to class. this must correspond
		 *            to the order in the data file
		 * @throws IOException
		 */
		protected void exportLabel(String lblFilename,
				SortedMap<Long, Integer> mapInstanceIdToClass)
				throws IOException {
			BufferedWriter wLabel = null;
			try {
				wLabel = new BufferedWriter(new FileWriter(lblFilename));
				for (Map.Entry<Long, Integer> entryInstanceIdToClass : mapInstanceIdToClass
						.entrySet()) {
					wLabel.write(Integer.toString(entryInstanceIdToClass
							.getValue()));
					wLabel.write("\n");
				}
			} finally {
				if (wLabel != null)
					try {
						wLabel.close();
					} catch (Exception ignore) {
					}
			}
		}

		/**
		 * pick through the training and test sets, figure out the class id for
		 * all instance ids for training. If the instance is in the training set
		 * and labeled it will get the appropriate class label. If the instance
		 * is in the test set and labeled it will be unlabeled. If the instance
		 * is in the training and test sets, it will use whatever label was
		 * specified for the training set. Unlabeled instances are given the
		 * class id 0.
		 * <p/>
		 * Write the [prefix]class.txt file
		 * 
		 * @param idFilename
		 *            filename to write instance id\ttrain/test flag\ttarget
		 *            class to
		 * @param trainInstanceClassMap
		 *            instances for training
		 * @param testInstanceClassMap
		 *            instance for testing
		 * @param classToIndexMap
		 *            map of class to class ids
		 * @param instanceIds
		 *            sorted set of instance ids; the order with which class.txt
		 *            will be written, and the order with which instances will
		 *            appear in the training data file.
		 * @return map of instance id to class id for training
		 * @throws IOException
		 */
		protected SortedMap<Long, Integer> getTrainingClassMap(
				String idFilename,
				SortedMap<Long, String> trainInstanceClassMap,
				SortedMap<Long, String> testInstanceClassMap,
				Map<String, Integer> classToIndexMap,
				SortedSet<Long> instanceIds) throws IOException {
			SortedMap<Long, Integer> mapInstanceIdToClass = new TreeMap<Long, Integer>();
			BufferedWriter wId = null;
			try {
				wId = new BufferedWriter(new FileWriter(idFilename));
				for (Long instanceId : instanceIds) {
					// for training default to unlabeled
					int classIdTrain = 0;
					String classNameTrain = "0";
					if (trainInstanceClassMap.containsKey(instanceId)) {
						// if the instance is in the training set, then use that
						// label
						classNameTrain = trainInstanceClassMap.get(instanceId);
						classIdTrain = classToIndexMap
								.get(classNameTrain);
					}
					mapInstanceIdToClass.put(instanceId, classIdTrain);
					// check test set for gold class
					if (testInstanceClassMap != null
							&& testInstanceClassMap.containsKey(instanceId)) {
						classNameTrain = testInstanceClassMap.get(instanceId);
					} 
					// write instance id, if this is in the train set, and it's
					// class
					wId.write(Long.toString(instanceId));
					wId.write("\t");
					wId.write(trainInstanceClassMap.containsKey(instanceId) ? "1"
							: "0");
					wId.write("\t");
					wId.write(classNameTrain);
					wId.write("\n");
				}
			} finally {
				if (wId != null)
					try {
						wId.close();
					} catch (Exception ignore) {
					}
			}
			return mapInstanceIdToClass;
		}
		/**
		 * write distance up to 6 digit precision. only write distance if &lt;
		 * 0.999. format: <tt>
		 * row column dist
		 * </tt> 1-based indices.
		 * 
		 * @todo - 0.999 also for euclidean distance??
		 * 
		 * @param data
		 * @param wData
		 * @throws IOException
		 */
		// private void writeDistanceMatrix(SparseDoubleMatrix2D data,
		// String filename) throws IOException {
		// String distanceFuncName = this.exportProperties.getProperty(
		// "distance", "EUCLID");
		// Statistic.VectorVectorFunction func = Statistic.EUCLID;
		// if ("COSINE".equalsIgnoreCase(distanceFuncName)) {
		// func = COSINE;
		// }
		// DoubleMatrix2D dist = Statistic.distance(data, func);
		// BufferedWriter wData = null;
		// try {
		// wData = new BufferedWriter(new FileWriter(filename));
		// for (int row = 1; row < dist.rows(); row++) {
		// for (int col = row + 1; col < dist.columns(); col++) {
		// double d = dist.get(row, col);
		// if (d < 0.999) {
		// wData.write(Integer.toString(row + 1));
		// wData.write("    ");
		// wData.write(Integer.toString(col + 1));
		// wData.write("    ");
		// wData.write(semilNumberFormat.format(round(d, 6)));
		// wData.write("\n");
		// }
		// }
		// }
		// } finally {
		// if (wData != null)
		// try {
		// wData.close();
		// } catch (Exception ignore) {
		// }
		// }
		// }

		// private void exportDistance(SparseData sparseData, String label,
		// Integer run, Integer fold) throws IOException {
		// SparseDoubleMatrix2D data = new SparseDoubleMatrix2D(
		// this.instanceIds.size(), maxAttributeIndex);
		// int row = 0;
		// for (Integer instanceId : this.instanceIds) {
		// // write row to sparse data matrix
		// // get 'vector'
		// SortedMap<Integer, Double> instanceValues = getSparseLineValues(
		// sparseData, numericAttributeMap, nominalAttributeMap,
		// instanceId);
		// // write it to the matrix
		// for (SortedMap.Entry<Integer, Double> instanceValue : instanceValues
		// .entrySet()) {
		// // row = instance number
		// // column = attribute index
		// // value = value
		// data.set(row, instanceValue.getKey() - 1,
		// instanceValue.getValue());
		// }
		// // increment row index
		// row++;
		// }
		// String filename = FileUtil.getFoldFilePrefix(outdir, label, run,
		// fold) + "dist.txt";
		// this.writeDistanceMatrix(data, filename);
		// }

		@Override
		public void initializeExport(InstanceData instanceLabel,
				Properties properties, SparseData sparseData)
				throws IOException {
			super.initializeExport(instanceLabel, properties, sparseData);
			this.instanceLabel = instanceLabel;
			if (properties.getProperty(SCOPE) == null
					|| properties.getProperty(SCOPE).length() == 0) {
				exportData(sparseData, null, null, null);
			}
		}

		@Override
		public void initializeFold(SparseData sparseData, String label,
				Integer run, Integer fold,
				SortedMap<Boolean, SortedMap<Long, String>> foldInstanceLabelMap)
				throws IOException {
			if (SCOPE_FOLD.equals(this.exportProperties.getProperty(SCOPE))) {
				exportData(sparseData, label, run, fold);
			}
			String labelFileName = FileUtil.getScopedFileName(outdir, label,
					run, fold, "label.txt");
			String idFileName = FileUtil.getScopedFileName(outdir, label, run,
					fold, "class.txt");
			SortedMap<Long, Integer> trainInstanceIdToClass = getTrainingClassMap(
					idFileName, foldInstanceLabelMap.get(true),
					foldInstanceLabelMap.get(false),
					this.labelToClassIndexMap.get(label),
					sparseData.getInstanceIds());
			exportLabel(labelFileName, trainInstanceIdToClass);
			// exportLabel(idFileName, labelFileName,
			// foldInstanceLabelMap.get(true),
			// foldInstanceLabelMap.get(false),
			// this.labelToClassIndexMap.get(label),
			// sparseData.getInstanceIds());
		}

//		/**
//		 * export the data
//		 * 
//		 * @param filename
//		 * @param idFilename
//		 * @param lblFilename
//		 * @param bagOfWordsData
//		 * @param trainInstanceClassMap
//		 * @param testInstanceClassMap
//		 * @param classToIndexMap
//		 * @throws IOException
//		 */
//		private void exportLabel(String idFilename, String lblFilename,
//				SortedMap<Long, String> trainInstanceClassMap,
//				SortedMap<Long, String> testInstanceClassMap,
//				Map<String, Integer> classToIndexMap,
//				SortedSet<Long> instanceIds) throws IOException {
//			// BufferedWriter wId = null;
//			BufferedWriter wLabel = null;
//			SortedMap<Long, Integer> mapInstanceIdToClass = this
//					.getTrainingClassMap(idFilename, trainInstanceClassMap,
//							testInstanceClassMap, classToIndexMap, instanceIds);
//			try {
//				// wId = new BufferedWriter(new FileWriter(idFilename));
//				wLabel = new BufferedWriter(new FileWriter(lblFilename));
//				for (Long instanceId : instanceIds) {
//					// // for training default to unlabeled
//					// int classIdTrain = 0;
//					// if (trainInstanceClassMap.containsKey(instanceId)) {
//					// // if the instance is in the training set, then use that
//					// // label
//					// classIdTrain = classToIndexMap
//					// .get(trainInstanceClassMap.get(instanceId));
//					// }
//					// // check test set for gold class
//					// int classIdGold = 0;
//					// if (testInstanceClassMap != null
//					// && testInstanceClassMap.containsKey(instanceId))
//					// classIdGold = classToIndexMap.get(testInstanceClassMap
//					// .get(instanceId));
//					// else
//					// classIdGold = classIdTrain;
//					// // write instance id, if this is in the train set, and
//					// it's
//					// // class
//					// wId.write(Long.toString(instanceId));
//					// wId.write("\t");
//					// wId.write(trainInstanceClassMap.containsKey(instanceId) ?
//					// "1"
//					// : "0");
//					// wId.write("\t");
//					// wId.write(Integer.toString(classIdGold));
//					// wId.write("\n");
//					// write label file for semiL
//					int classIdTrain = mapInstanceIdToClass.get(instanceId);
//					wLabel.write(Integer.toString(classIdTrain));
//					wLabel.write("\n");
//				}
//			} finally {
//				// if (wId != null)
//				// try {
//				// wId.close();
//				// } catch (Exception ignore) {
//				// }
//				if (wLabel != null)
//					try {
//						wLabel.close();
//					} catch (Exception ignore) {
//					}
//			}
//		}

		@Override
		public void initializeLabel(
				String label,
				SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>> labelInstances,
				Properties properties, SparseData sparseData)
				throws IOException {
			super.initializeLabel(label, labelInstances, properties, sparseData);
			if (SCOPE_LABEL.equals(this.exportProperties.getProperty(SCOPE))) {
				exportData(sparseData, label, null, null);
			}
		}

		@Override
		protected void fillLabelToClassIndexMap(Map<String, SortedSet<String>> labelToClassMap) {
			kernelUtil.fillLabelToClassToIndexMap(labelToClassMap, this.labelToClassIndexMap);
			updateLabelClassMapTransductive();
		}		
		// /**
		// * round double to specified precision
		// *
		// * @param Rval
		// * @param Rpl
		// * @return
		// */
		// private double round(double Rval, int Rpl) {
		// double p = (double) Math.pow(10, Rpl);
		// Rval = Rval * p;
		// double tmp = Math.round(Rval);
		// return (double) tmp / p;
		// }

	}

	private KernelUtil kernelUtil;

	@Override
	public SparseDataFormatter getFormatter() {
		return new SemiLDataFormatter(this.getKernelUtil());
	}	

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

}
