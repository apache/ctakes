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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.ctakes.ytex.kernel.FileUtil;
import org.apache.ctakes.ytex.kernel.InstanceData;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;
import org.apache.ctakes.ytex.kernel.KernelUtil;
import org.apache.ctakes.ytex.kernel.dao.KernelEvaluationDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.google.common.collect.BiMap;


/**
 * export gram matrix for libsvm. input properties file with following keys:
 * <p/>
 * <li>kernel.name name of kernel evaluation (corresponds to name column in
 * kernel_eval table) - required
 * <li>outdir directory where files will be place - optional defaults to current
 * directory
 * <p/>
 * Output to outdir following files:
 * <li>train_data.txt - for each class label, a symmetric gram matrix for
 * training instances
 * <li>train_id.txt - instance ids corresponding to rows of training gram matrix
 * <li>test_data.txt - for each class label, a rectangular matrix of the test
 * instances kernel evaluations wrt training instances
 * <li>test_id.txt - instance ids corresponding to rows of test gram matrix
 * 
 * @author vijay
 */
public class LibSVMGramMatrixExporterImpl implements LibSVMGramMatrixExporter {
	@SuppressWarnings("static-access")
	public static void main(String args[]) throws IOException {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("prop")
				.hasArg()
				.isRequired()
				.withDescription(
						"property file with queries and other kernel parameters")
				.create("prop"));
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			LibSVMGramMatrixExporter exporter = (LibSVMGramMatrixExporter) KernelContextHolder
					.getApplicationContext()
					.getBean("libSVMGramMatrixExporter");
			exporter.exportGramMatrix(FileUtil.loadProperties(
					line.getOptionValue("prop"), true));
		} catch (ParseException pe) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"java " + LibSVMGramMatrixExporterImpl.class.getName()
							+ " export gram matrix in libsvm format", options);
		}
	}

	private JdbcTemplate jdbcTemplate = null;
	private KernelEvaluationDao kernelEvaluationDao = null;
	private KernelUtil kernelUtil;
	private LibSVMUtil libsvmUtil;

	private PlatformTransactionManager transactionManager;

	/**
	 * export the train or test gram matrix. the train gram matrix is square and
	 * symmetric. the test gram matrix is rectangular - each column corresponds
	 * to a training instance each row corresponds to a test instance.
	 * 
	 * @param gramMatrix
	 *            square symmetric matrix with all available instance data
	 * @param instanceIdToClassMap
	 *            folds
	 * @param train
	 *            true - export train set, false - export test set
	 * @param mapInstanceIdToIndex
	 *            map of instance id to index in gramMatrix
	 * @param filePrefix
	 *            - prefix to which we add train_data.txt
	 * @param mapClassToIndex
	 * @throws IOException
	 */
	private void exportFold(double[][] gramMatrix,
			Map<Boolean, SortedMap<Long, String>> instanceIdToClassMap,
			boolean train, Map<Long, Integer> mapInstanceIdToIndex,
			String filePrefix, Map<String, Integer> mapClassToIndex)
			throws IOException {
		String fileName = new StringBuilder(filePrefix).append("_data.txt")
				.toString();
		String idFileName = new StringBuilder(filePrefix).append("_id.txt")
				.toString();
		BufferedWriter w = null;
		BufferedWriter wId = null;
		// for both training and test sets, the column instance ids
		// are the training instance ids. This is already sorted,
		// but we stuff it in a list, so make sure it is sorted
		// the order has to be the same in both the train and test files
		List<Long> colInstanceIds = new ArrayList<Long>(instanceIdToClassMap
				.get(true).keySet());
		Collections.sort(colInstanceIds);
		// the rows - train or test instance ids and their class labels
		SortedMap<Long, String> rowInstanceToClassMap = instanceIdToClassMap
				.get(train);
		try {
			w = new BufferedWriter(new FileWriter(fileName));
			wId = new BufferedWriter(new FileWriter(idFileName));
			int rowIndex = 0;
			// the rows in the gramMatrix correspond to the entries in the
			// instanceLabelMap
			// both are in the same order
			for (Map.Entry<Long, String> instanceClass : rowInstanceToClassMap
					.entrySet()) {
				// classId - we assume that this is value is valid for libsvm
				// this can be a real number (for regression)
				String classId = instanceClass.getValue();
				// the instance id of this row
				long rowInstanceId = instanceClass.getKey();
				// the index to gramMatrix corresponding to this instance
				int rowInstanceIndex = mapInstanceIdToIndex.get(rowInstanceId);
				// write class Id
				w.write(mapClassToIndex.get(classId).toString());
				w.write("\t");
				// write row number - libsvm uses 1-based indexing
				w.write("0:");
				w.write(Integer.toString(rowIndex + 1));
				// write column entries
				for (int columnIndex = 0; columnIndex < colInstanceIds.size(); columnIndex++) {
					// column instance id
					long colInstanceId = colInstanceIds.get(columnIndex);
					// index into gram matrix for this instance id
					int colInstanceIndex = mapInstanceIdToIndex
							.get(colInstanceId);
					w.write("\t");
					// write column number
					w.write(Integer.toString(columnIndex + 1));
					w.write(":");
					// write value - gramMatrix is symmetric, so this will work
					// both ways
					w.write(Double
							.toString(gramMatrix[rowInstanceIndex][colInstanceIndex]));
				}
				// don't want carriage return, even on windows
				w.write("\n");
				// increment the row number
				rowIndex++;
				// write id to file
				wId.write(Long.toString(rowInstanceId));
				wId.write("\n");
			}
		} finally {
			if (w != null)
				w.close();
			if (wId != null)
				wId.close();
		}

	}

	/**
	 * Load the gram matrix based on scope. Write the gram matrix for each fold.
	 * Generate 4 files per fold: train_data.txt, train_id.txt, test_data.txt,
	 * test_id.txt.
	 * 
	 */
	private void exportGramMatrices(String name, String experiment,
			double param1, String param2, String scope, String splitName,
			String outdir, InstanceData instanceData,
			Map<String, BiMap<String, Integer>> labelToClassIndexMap)
			throws IOException {
		// the full, symmetric gram matrix
		double[][] gramMatrix = null;
		// the set of all instance ids
		SortedSet<Long> instanceIds = new TreeSet<Long>();
		// map of instance id to index in gramMatrix
		Map<Long, Integer> mapInstanceIdToIndex = new HashMap<Long, Integer>();
		if (scope == null || scope.length() == 0) {
			// empty scope - load gram matrix
			gramMatrix = loadGramMatrix(name, experiment, param1, param2,
					splitName, null, 0, 0, instanceData, instanceIds,
					mapInstanceIdToIndex);
			if (gramMatrix == null)
				return;
		}
		for (String label : instanceData.getLabelToInstanceMap().keySet()) {
			if ("label".equals(scope)) {
				// label scope - load gram matrix
				gramMatrix = loadGramMatrix(name, experiment, param1, param2,
						splitName, label, 0, 0, instanceData, instanceIds,
						mapInstanceIdToIndex);
				if (gramMatrix == null)
					return;
			}
			// write the properties file with the class id to class name map
			kernelUtil.exportClassIds(outdir, labelToClassIndexMap.get(label),
					label);
			for (int run : instanceData.getLabelToInstanceMap().get(label)
					.keySet()) {
				for (int fold : instanceData.getLabelToInstanceMap().get(label)
						.get(run).keySet()) {
					if ("fold".equals(scope)) {
						// fold scope - load gram matrix
						gramMatrix = loadGramMatrix(name, experiment, param1,
								param2, splitName, label, run, fold,
								instanceData, instanceIds, mapInstanceIdToIndex);
					}
					if (gramMatrix != null) {
						// get folds
						Map<Boolean, SortedMap<Long, String>> foldMap = instanceData
								.getLabelToInstanceMap().get(label).get(run)
								.get(fold);
						// export training fold
						exportFold(gramMatrix, foldMap, true,
								mapInstanceIdToIndex,
								FileUtil.getDataFilePrefix(outdir, label, run,
										fold, true),
								labelToClassIndexMap.get(label));
						// export test fold
						exportFold(gramMatrix, foldMap, false,
								mapInstanceIdToIndex,
								FileUtil.getDataFilePrefix(outdir, label, run,
										fold, false),
								labelToClassIndexMap.get(label));
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.libsvm.LibSVMGramMatrixExporter#exportGramMatrix(java.util.Properties
	 * )
	 */
	public void exportGramMatrix(Properties props) throws IOException {
		String name = props.getProperty("org.apache.ctakes.ytex.corpusName");
		String experiment = props.getProperty("org.apache.ctakes.ytex.experiment");
		String param2 = props.getProperty("org.apache.ctakes.ytex.param2");
		double param1 = Double.parseDouble(props
				.getProperty("org.apache.ctakes.ytex.param1", "0"));
		String scope = props.getProperty("scope");
		InstanceData instanceData = this.getKernelUtil().loadInstances(
				props.getProperty("instanceClassQuery"));
		String splitName = props.getProperty("org.apache.ctakes.ytex.splitName");
		String outdir = props.getProperty("outdir");
		Map<String, BiMap<String, Integer>> labelToClassIndexMap = new HashMap<String, BiMap<String, Integer>>();
		kernelUtil.fillLabelToClassToIndexMap(
				instanceData.getLabelToClassMap(), labelToClassIndexMap);
		exportGramMatrices(name, experiment, param1, param2, scope, splitName,
				outdir, instanceData, labelToClassIndexMap);
	}

	public DataSource getDataSource() {
		return jdbcTemplate.getDataSource();
	}

	public KernelEvaluationDao getKernelEvaluationDao() {
		return kernelEvaluationDao;
	}

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public LibSVMUtil getLibsvmUtil() {
		return libsvmUtil;
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	private double[][] loadGramMatrix(String name, String experiment,
			double param1, String param2, String splitName, String label,
			int run, int fold, InstanceData instanceData,
			SortedSet<Long> instanceIds, Map<Long, Integer> mapInstanceIdToIndex) {
		double[][] gramMatrix;
		instanceIds.clear();
		mapInstanceIdToIndex.clear();
		instanceIds.addAll(instanceData.getAllInstanceIds(label, run, fold));
		int index = 0;
		for (long instanceId : instanceIds) {
			mapInstanceIdToIndex.put(instanceId, index++);
		}
		gramMatrix = this.kernelUtil.loadGramMatrix(instanceIds, name,
				splitName, experiment, label, run, fold, param1, param2);
		return gramMatrix;
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setKernelEvaluationDao(KernelEvaluationDao kernelEvaluationDao) {
		this.kernelEvaluationDao = kernelEvaluationDao;
	}

	// private void exportFold(String name, String experiment, String outdir,
	// InstanceData instanceData, String label, int run, int fold,
	// double param1, String param2) throws IOException {
	// SortedMap<Integer, String> trainInstanceLabelMap = instanceData
	// .getLabelToInstanceMap().get(label).get(run).get(fold)
	// .get(true);
	// SortedMap<Integer, String> testInstanceLabelMap = instanceData
	// .getLabelToInstanceMap().get(label).get(run).get(fold)
	// .get(false);
	// double[][] trainGramMatrix = new
	// double[trainInstanceLabelMap.size()][trainInstanceLabelMap
	// .size()];
	// double[][] testGramMatrix = null;
	// if (testInstanceLabelMap != null) {
	// testGramMatrix = new
	// double[testInstanceLabelMap.size()][trainInstanceLabelMap
	// .size()];
	// }
	// KernelEvaluation kernelEval = this.kernelEvaluationDao.getKernelEval(
	// name, experiment, label, 0, param1, param2);
	// kernelUtil.fillGramMatrix(kernelEval, new TreeSet<Integer>(
	// trainInstanceLabelMap.keySet()), trainGramMatrix,
	// testInstanceLabelMap != null ? new TreeSet<Integer>(
	// testInstanceLabelMap.keySet()) : null, testGramMatrix);
	// outputGramMatrix(kernelEval, trainInstanceLabelMap, trainGramMatrix,
	// FileUtil.getDataFilePrefix(outdir, label, run, fold,
	// testInstanceLabelMap != null ? true : null));
	// if (testGramMatrix != null) {
	// outputGramMatrix(kernelEval, testInstanceLabelMap, testGramMatrix,
	// FileUtil.getDataFilePrefix(outdir, label, run, fold, false));
	// }
	// }
	//
	// private void outputGramMatrix(KernelEvaluation kernelEval,
	// SortedMap<Integer, String> instanceLabelMap, double[][] gramMatrix,
	// String dataFilePrefix) throws IOException {
	// StringBuilder bFileName = new StringBuilder(dataFilePrefix)
	// .append("_data.txt");
	// StringBuilder bIdFileName = new StringBuilder(dataFilePrefix)
	// .append("_id.txt");
	// BufferedWriter w = null;
	// BufferedWriter wId = null;
	// try {
	// w = new BufferedWriter(new FileWriter(bFileName.toString()));
	// wId = new BufferedWriter(new FileWriter(bIdFileName.toString()));
	// int rowIndex = 0;
	// // the rows in the gramMatrix correspond to the entries in the
	// // instanceLabelMap
	// // both are in the same order
	// for (Map.Entry<Integer, String> instanceClass : instanceLabelMap
	// .entrySet()) {
	// // default the class Id to 0
	// String classId = instanceClass.getValue();
	// int instanceId = instanceClass.getKey();
	// // write class Id
	// w.write(classId);
	// w.write("\t");
	// // write row number - libsvm uses 1-based indexing
	// w.write("0:");
	// w.write(Integer.toString(rowIndex + 1));
	// // write column entries
	// for (int columnIndex = 0; columnIndex < gramMatrix[rowIndex].length;
	// columnIndex++) {
	// w.write("\t");
	// // write column number
	// w.write(Integer.toString(columnIndex + 1));
	// w.write(":");
	// // write value
	// w.write(Double.toString(gramMatrix[rowIndex][columnIndex]));
	// }
	// w.newLine();
	// // increment the row number
	// rowIndex++;
	// // write id file
	// wId.write(Integer.toString(instanceId));
	// wId.newLine();
	// }
	// } finally {
	// if (w != null)
	// w.close();
	// if (wId != null)
	// wId.close();
	// }
	// }

	// /**
	// * instantiate gram matrices, generate output files
	// *
	// * @param name
	// * @param testInstanceQuery
	// * @param trainInstanceQuery
	// * @param outdir
	// * @throws IOException
	// */
	// private void exportGramMatrices(String name, String testInstanceQuery,
	// String trainInstanceQuery, String outdir) throws IOException {
	// Set<String> labels = new HashSet<String>();
	// SortedMap<Integer, Map<String, Integer>> trainInstanceLabelMap =
	// libsvmUtil
	// .loadClassLabels(trainInstanceQuery, labels);
	// double[][] trainGramMatrix = new
	// double[trainInstanceLabelMap.size()][trainInstanceLabelMap
	// .size()];
	// SortedMap<Integer, Map<String, Integer>> testInstanceLabelMap = null;
	// double[][] testGramMatrix = null;
	// if (testInstanceQuery != null) {
	// testInstanceLabelMap = libsvmUtil.loadClassLabels(
	// testInstanceQuery, labels);
	// testGramMatrix = new
	// double[testInstanceLabelMap.size()][trainInstanceLabelMap
	// .size()];
	// }
	// // fillGramMatrix(name, trainInstanceLabelMap, trainGramMatrix,
	// // testInstanceLabelMap, testGramMatrix);
	// for (String label : labels) {
	// outputGramMatrix(name, outdir, label, trainInstanceLabelMap,
	// trainGramMatrix, "training");
	// if (testGramMatrix != null) {
	// outputGramMatrix(name, outdir, label, testInstanceLabelMap,
	// testGramMatrix, "test");
	// }
	// }
	// libsvmUtil.outputInstanceIds(outdir, trainInstanceLabelMap, "training");
	// if (testInstanceLabelMap != null)
	// libsvmUtil.outputInstanceIds(outdir, testInstanceLabelMap, "test");
	// }

	// private void outputGramMatrix(String name, String outdir, String label,
	// SortedMap<Integer, Map<String, Integer>> instanceLabelMap,
	// double[][] gramMatrix, String type) throws IOException {
	// StringBuilder bFileName = new StringBuilder(outdir)
	// .append(File.separator).append(type).append("_data_")
	// .append(label).append(".txt");
	// BufferedWriter w = null;
	// try {
	// w = new BufferedWriter(new FileWriter(bFileName.toString()));
	// int rowIndex = 0;
	// // the rows in the gramMatrix correspond to the entries in the
	// // instanceLabelMap
	// // both are in the same order
	// for (Map.Entry<Integer, Map<String, Integer>> instanceLabels :
	// instanceLabelMap
	// .entrySet()) {
	// // default the class Id to 0
	// int classId = 0;
	// if (instanceLabels.getValue() != null
	// && instanceLabels.getValue().containsKey(label)) {
	// classId = instanceLabels.getValue().get(label);
	// }
	// // write class Id
	// w.write(Integer.toString(classId));
	// w.write("\t");
	// // write row number - libsvm uses 1-based indexing
	// w.write("0:");
	// w.write(Integer.toString(rowIndex + 1));
	// // write column entries
	// for (int columnIndex = 0; columnIndex < gramMatrix[rowIndex].length;
	// columnIndex++) {
	// w.write("\t");
	// // write column number
	// w.write(Integer.toString(columnIndex + 1));
	// w.write(":");
	// // write value
	// w.write(Double.toString(gramMatrix[rowIndex][columnIndex]));
	// }
	// w.newLine();
	// // increment the row number
	// rowIndex++;
	// }
	// } finally {
	// if (w != null)
	// w.close();
	// }
	// }

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	public void setLibsvmUtil(LibSVMUtil libsvmUtil) {
		this.libsvmUtil = libsvmUtil;
	}

	public void setTransactionManager(
			PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

}
