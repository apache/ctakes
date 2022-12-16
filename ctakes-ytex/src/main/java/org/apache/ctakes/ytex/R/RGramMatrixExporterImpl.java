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
package org.apache.ctakes.ytex.R;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.SortedSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.FileUtil;
import org.apache.ctakes.ytex.kernel.InstanceData;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;
import org.apache.ctakes.ytex.kernel.KernelUtil;
import org.apache.ctakes.ytex.kernel.dao.KernelEvaluationDao;
import org.apache.ctakes.ytex.sparsematrix.InstanceDataExporter;


public class RGramMatrixExporterImpl implements RGramMatrixExporter {
	private static final Log log = LogFactory.getLog(RGramMatrixExporter.class);

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
			RGramMatrixExporter exporter = (RGramMatrixExporter) KernelContextHolder
					.getApplicationContext().getBean(RGramMatrixExporter.class);
			exporter.exportGramMatrix(FileUtil.loadProperties(
					line.getOptionValue("prop"), true));
		} catch (ParseException pe) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java " + RGramMatrixExporterImpl.class.getName()
					+ " export gram matrix for use in R/Matlab", options);
		}
	}

	private InstanceDataExporter instanceDataExporter;
	private KernelEvaluationDao kernelEvaluationDao;

	private KernelUtil kernelUtil;

	private void exportGramMatrices(String name, String experiment,
			double param1, String param2, String splitName, String scope,
			String outdir, InstanceData instanceData) throws IOException {
		if (scope == null || scope.length() == 0) {
			exportGramMatrix(name, experiment, param1, param2, splitName,
					outdir, instanceData, null, 0, 0);
		} else {
			for (String label : instanceData.getLabelToInstanceMap().keySet()) {
				if ("label".equals(scope)) {
					exportGramMatrix(name, experiment, param1, param2,
							splitName, outdir, instanceData, label, 0, 0);
				} else if ("fold".equals(scope)) {
					for (int run : instanceData.getLabelToInstanceMap()
							.get(label).keySet()) {
						for (int fold : instanceData.getLabelToInstanceMap()
								.get(label).get(run).keySet()) {
							exportGramMatrix(name, experiment, param1, param2,
									splitName, outdir, instanceData, label,
									run, fold);
						}
					}
				}
			}
		}
	}

	private void exportGramMatrix(String name, String experiment,
			double param1, String param2, String splitName, String outdir,
			InstanceData instanceData, String label, int run, int fold)
			throws IOException {
		SortedSet<Long> instanceIds = instanceData.getAllInstanceIds(label,
				run, fold);
		String filePrefix = FileUtil.getDataFilePrefix(outdir, label, run,
				fold, null);
		double[][] gramMatrix = kernelUtil.loadGramMatrix(instanceIds, name,
				splitName, experiment, label, run, fold, param1, param2);
		if (gramMatrix != null)
			outputGramMatrix(gramMatrix, instanceIds, filePrefix);
	}

	// private KernelEvaluation getKernelEval(String name, String splitName,
	// String experiment, String label, int run, int fold, double param1,
	// String param2) {
	// int foldId = 0;
	// if (run != 0 && fold != 0) {
	// CrossValidationFold f = this.classifierEvaluationDao
	// .getCrossValidationFold(name, splitName, label, run, fold);
	// if (f != null)
	// foldId = f.getCrossValidationFoldId();
	// }
	// KernelEvaluation kEval = this.kernelEvaluationDao.getKernelEval(name,
	// experiment, label, foldId, param1, param2);
	// if (kEval == null) {
	// log.warn("could not find kernelEvaluation.  name=" + name
	// + ", experiment=" + experiment + ", label=" + label
	// + ", fold=" + fold + ", run=" + run);
	// }
	// return kEval;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.R.RGramMatrixExporter#exportGramMatrix(java.util.Properties)
	 */
	@Override
	public void exportGramMatrix(Properties props) throws IOException {
		String name = props.getProperty("org.apache.ctakes.ytex.corpusName");
		String splitName = props.getProperty("org.apache.ctakes.ytex.splitName");
		String experiment = props.getProperty("org.apache.ctakes.ytex.experiment");
		String param2 = props.getProperty("org.apache.ctakes.ytex.param2");
		double param1 = Double.parseDouble(props
				.getProperty("org.apache.ctakes.ytex.param1", "0"));
		String scope = props.getProperty("scope");
		String outdir = props.getProperty("outdir");
		InstanceData instanceData = this.getKernelUtil().loadInstances(
				props.getProperty("instanceClassQuery"));
		exportGramMatrices(name, experiment, param1, param2, splitName, scope,
				outdir, instanceData);
		instanceDataExporter.outputInstanceData(instanceData,
				FileUtil.addFilenameToDir(outdir, "instance.txt"));
	}

	//
	// private void exportGramMatrix(String name, String experiment,
	// double param1, String param2, String outdir,
	// InstanceData instanceData, String label, int foldId)
	// throws IOException {
	// SortedSet<Integer> instanceIds = getAllInstanceIdsForLabel(
	// instanceData, label);
	// double[][] gramMatrix = new double[instanceIds.size()][instanceIds
	// .size()];
	// KernelEvaluation kernelEval = this.kernelEvaluationDao.getKernelEval(
	// name, experiment, label, 0, param1, param2);
	// if (kernelEval != null) {
	// kernelUtil.fillGramMatrix(kernelEval, instanceIds, gramMatrix,
	// null, null);
	// outputInstanceData(instanceData, label, outdir);
	// outputGramMatrix(kernelEval, gramMatrix, instanceIds,
	// FileUtil.getDataFilePrefix(outdir, label, 0, 0, null));
	// } else {
	// log.info("no kernel eval for label=" + label);
	// }
	//
	// }

	// /**
	// * get all instance ids for the specified label
	// *
	// * @param instanceData
	// * @param label
	// * @return
	// */
	// private SortedSet<Integer> getAllInstanceIdsForLabel(
	// InstanceData instanceData, String label) {
	// SortedSet<Integer> instanceIds = new TreeSet<Integer>();
	// for (int run : instanceData.getLabelToInstanceMap().get(label).keySet())
	// {
	// for (int fold : instanceData.getLabelToInstanceMap().get(label)
	// .get(run).keySet()) {
	// for (SortedMap<Integer, String> instanceLabelMap : instanceData
	// .getLabelToInstanceMap().get(label).get(run).get(fold)
	// .values()) {
	// instanceIds.addAll(instanceLabelMap.keySet());
	// }
	// }
	// }
	// return instanceIds;
	// }

	public InstanceDataExporter getInstanceDataExporter() {
		return instanceDataExporter;
	}

	public KernelEvaluationDao getKernelEvaluationDao() {
		return kernelEvaluationDao;
	}

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	private void outputGramMatrix(double[][] gramMatrix,
			SortedSet<Long> instanceIds, String dataFilePrefix)
			throws IOException {
		BufferedWriter w = null;
		BufferedWriter wId = null;
		try {
			w = new BufferedWriter(new FileWriter(dataFilePrefix + "data.txt"));
			wId = new BufferedWriter(new FileWriter(dataFilePrefix
					+ "instance_id.txt"));
			Long instanceIdArray[] = instanceIds.toArray(new Long[] {});
			// write instance id corresponding to row
			for (int h = 0; h < instanceIdArray.length; h++) {
				wId.write(Long.toString(instanceIdArray[h]));
				wId.write("\n");
			}
			for (int i = 0; i < instanceIdArray.length; i++) {
				// write line from gram matrix
				for (int j = 0; j < instanceIdArray.length; j++) {
					w.write(Double.toString(gramMatrix[i][j]));
					if (j < instanceIdArray.length - 1)
						w.write(" ");
				}
				w.write("\n");
			}
		} finally {
			if (w != null) {
				w.close();
			}
			if (wId != null) {
				wId.close();
			}
		}
	}

	public void setInstanceDataExporter(
			InstanceDataExporter instanceDataExporter) {
		this.instanceDataExporter = instanceDataExporter;
	}

	public void setKernelEvaluationDao(KernelEvaluationDao kernelEvaluationDao) {
		this.kernelEvaluationDao = kernelEvaluationDao;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	// private void exportGramMatrices(String name, String experiment,
	// String outdir, String instanceQuery) throws IOException {
	// SortedMap<Integer, SortedMap<Boolean, SortedMap<Integer, Integer>>>
	// instanceFolds = new TreeMap<Integer, SortedMap<Boolean,
	// SortedMap<Integer, Integer>>>();
	// SortedMap<String, SortedMap<Integer, String>> instanceLabels = new
	// TreeMap<String, SortedMap<Integer, String>>();
	//
	// }
	//
	// private void exportLabel(String name, String experiment, String outdir) {
	// }
	//
	// private static class InstanceFoldData {
	// SortedMap<Boolean, SortedMap<Integer, Integer>> folds;
	//
	// public void addEntry(boolean train, int fold, int run) {
	// SortedMap<Integer, Integer> foldToRun = folds.get(train);
	// if (foldToRun == null) {
	// if (fold != 0) {
	// foldToRun = new TreeMap<Integer, Integer>();
	// foldToRun.put(fold, run);
	// }
	// }
	// folds.put(train, foldToRun);
	// }
	// }
	//
	// private void loadInstanceData(
	// String strQuery,
	// final SortedMap<Integer, SortedMap<Boolean, SortedMap<Integer, Integer>>>
	// instanceFolds,
	// final SortedMap<String, SortedMap<Integer, String>> instanceLabels) {
	// jdbcTemplate.query(strQuery, new RowCallbackHandler() {
	//
	// @Override
	// public void processRow(ResultSet rs) throws SQLException {
	// String label = "";
	// int run = 0;
	// int fold = 0;
	// Boolean train = null;
	// int instanceId = rs.getInt(1);
	// String className = rs.getString(2);
	// if (rs.getMetaData().getColumnCount() >= 3)
	// train = rs.getBoolean(3);
	// if (rs.getMetaData().getColumnCount() >= 4)
	// label = rs.getString(4);
	// if (rs.getMetaData().getColumnCount() >= 5)
	// fold = rs.getInt(5);
	// if (rs.getMetaData().getColumnCount() >= 6)
	// run = rs.getInt(6);
	// // set instance className for label
	// SortedMap<Integer, String> instClassName = instanceLabels
	// .get(label);
	// if (instClassName == null) {
	// instClassName = new TreeMap<Integer, String>();
	// instClassName.put(instanceId, labels);
	// }
	// labels.put(label, className);
	// // set fold data
	// if (train != null) {
	// // we split into train/test - save this in the instanceFolds
	// SortedMap<Boolean, SortedMap<Integer, Integer>> folds = instanceFolds
	// .get(instanceId);
	// if (folds == null) {
	// folds = new TreeMap<Boolean, SortedMap<Integer, Integer>>();
	// instanceFolds.put(instanceId, folds);
	// }
	// // we split into folds / runs
	// SortedMap<Integer, Integer> foldToRun = folds.get(train);
	// if (foldToRun == null) {
	// if (fold != 0) {
	// foldToRun = new TreeMap<Integer, Integer>();
	// foldToRun.put(fold, run);
	// }
	// }
	// // add train/test flag
	// // foldToRun is null if we don't have any folds
	// folds.put(train, foldToRun);
	// }
	// }
	// });
	// }
}
