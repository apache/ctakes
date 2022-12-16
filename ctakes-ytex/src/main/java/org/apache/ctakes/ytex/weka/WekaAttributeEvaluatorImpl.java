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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;

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
import org.apache.ctakes.ytex.kernel.KernelContextHolder;
import org.apache.ctakes.ytex.kernel.KernelUtil;
import org.apache.ctakes.ytex.kernel.SparseData;
import org.apache.ctakes.ytex.kernel.SparseDataExporter;
import org.apache.ctakes.ytex.kernel.SparseDataFormatter;
import org.apache.ctakes.ytex.kernel.dao.ClassifierEvaluationDao;
import org.apache.ctakes.ytex.kernel.model.ClassifierEvaluation;
import org.apache.ctakes.ytex.kernel.model.CrossValidationFold;
import org.apache.ctakes.ytex.kernel.model.FeatureEvaluation;
import org.apache.ctakes.ytex.kernel.model.FeatureRank;
import org.apache.ctakes.ytex.weka.WekaFormatterFactory.WekaFormatter;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeSelection;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaAttributeEvaluatorImpl implements WekaAttributeEvaluator {
	public class WekaAttributeEvaluatorFormatter extends WekaFormatter {
		String corpusName;

		String featureSetName;

		String splitName;

		public WekaAttributeEvaluatorFormatter(String corpusName,
				String featureSetName, String splitName) {
			super(getKernelUtil());
			this.featureSetName = featureSetName;
			this.corpusName = corpusName;
			this.splitName = splitName;
		}

		@Override
		public void exportFold(SparseData sparseData,
				SortedMap<Long, String> instanceClasses, boolean train,
				String label, Integer run, Integer fold) throws IOException {
			if (train) {
				Instances inst = this.initializeInstances(sparseData,
						instanceClasses, train, label, run, fold);
				try {
					evaluateAttributes(corpusName, featureSetName, splitName,
							inst, label, run, fold);
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		}

	}

	private static final Log log = LogFactory
			.getLog(WekaAttributeEvaluatorImpl.class);

	/**
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("property file")
				.hasArg()
				.withDescription(
						"load parameters from property file.  If queries defiend as for SparseDataExporter, use them to load the instances")
				.create("prop"));
		options.addOption(OptionBuilder
				.withArgName("train_data.arff")
				.hasArg()
				.withDescription(
						"use specified weka arff file to load instances for evaluation.")
				.create("arff"));
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			Properties props = FileUtil.loadProperties(
					line.getOptionValue("prop"), true);
			String corpusName = props.getProperty("org.apache.ctakes.ytex.corpusName");
			String splitName = props.getProperty("org.apache.ctakes.ytex.splitName");
			String featureSetName = props.getProperty("org.apache.ctakes.ytex.featureSetName");
			WekaAttributeEvaluator wekaEval = KernelContextHolder
					.getApplicationContext().getBean(
							WekaAttributeEvaluator.class);
			if (line.hasOption("arff")) {
				wekaEval.evaluateAttributesFromFile(corpusName, splitName,
						featureSetName, line.getOptionValue("arff"));
			} else {
				wekaEval.evaluateAttributesFromProps(corpusName, splitName,
						featureSetName, props);
			}
		} catch (ParseException pe) {
			printHelp(options);
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"java "
								+ WekaAttributeEvaluatorImpl.class.getName()
								+ " evaluate attributes using a weka AttributeEvaluator",
						options);
	}

	private ASEvaluation asEvaluation;
	private AttributeSelection attributeSelection;

	private ClassifierEvaluationDao classifierEvaluationDao;

	private KernelUtil kernelUtil;

	private SparseDataExporter sparseDataExporter;

	/**
	 * evaluate attributes, store in db
	 * 
	 * @param inst
	 *            instances
	 * @param label
	 *            {@link FeatureEvaluation#getLabel()}
	 * @param run
	 *            {@link ClassifierEvaluation#getRun()} to map to fold
	 * @param fold
	 *            {@link ClassifierEvaluation#getFold()} to map to fold
	 * @throws Exception
	 */
	public void evaluateAttributes(String corpusName, String featureSetName,
			String splitName, Instances inst, String label, Integer run,
			Integer fold) throws Exception {
		AttributeSelection ae = this.getAttributeSelection();
		ae.SelectAttributes(inst);
		double rankedAttributes[][] = ae.rankedAttributes();
		FeatureEvaluation fe = initializeFeatureEvaluation(corpusName,
				featureSetName, splitName, label, run, fold);
		List<FeatureRank> featureRanks = new ArrayList<FeatureRank>(
				rankedAttributes.length);
		for (int i = 0; i < rankedAttributes.length; i++) {
			int index = (int) rankedAttributes[i][0];
			double eval = rankedAttributes[i][1];
			FeatureRank r = new FeatureRank();
			r.setFeatureEval(fe);
			r.setFeatureName(inst.attribute(index).name());
			r.setRank(i + 1);
			r.setEvaluation(eval);
			featureRanks.add(r);
		}
		// delete this feature evaluation if it exists
		classifierEvaluationDao.deleteFeatureEvaluation(corpusName,
				featureSetName, label, fe.getEvaluationType(),
				fe.getCrossValidationFoldId(), fe.getParam1(), fe.getParam2());
		classifierEvaluationDao.saveFeatureEvaluation(fe, featureRanks);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.weka.WekaAttributeEvaluator#evaluateAttributesFromFile(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public void evaluateAttributesFromFile(String corpusName,
			String featureSetName, String splitName, String file)
			throws Exception {
		DataSource ds = new DataSource(file);
		Instances inst = ds.getDataSet();
		String label = FileUtil.parseLabelFromFileName(inst.relationName());
		Integer run = FileUtil.parseRunFromFileName(inst.relationName());
		Integer fold = FileUtil.parseFoldFromFileName(inst.relationName());
		evaluateAttributes(corpusName, featureSetName, splitName, inst, label,
				run, fold);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.weka.WekaAttributeEvaluator#evaluateAttributesFromProps(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public void evaluateAttributesFromProps(String corpusName,
			String splitName, String featureSetName, Properties props)
			throws Exception {
		sparseDataExporter.exportData(props,
				new WekaAttributeEvaluatorFormatter(corpusName, featureSetName,
						splitName), null);
	}

	public ASEvaluation getAsEvaluation() {
		return asEvaluation;
	}

	public AttributeSelection getAttributeSelection() {
		return attributeSelection;
	}

	public ClassifierEvaluationDao getClassifierEvaluationDao() {
		return classifierEvaluationDao;
	}

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public SparseDataExporter getSparseDataExporter() {
		return sparseDataExporter;
	}

	public FeatureEvaluation initializeFeatureEvaluation(String corpusName,
			String featureSetName, String splitName, String label, Integer run,
			Integer fold) {
		FeatureEvaluation fe = new FeatureEvaluation();
		fe.setCorpusName(corpusName);
		fe.setFeatureSetName(featureSetName);
		fe.setEvaluationType(this.getAsEvaluation().getClass().getSimpleName());
		fe.setLabel(label);
		if (run != null && fold != null) {
			CrossValidationFold cvFold = this.classifierEvaluationDao
					.getCrossValidationFold(corpusName, splitName, label, run,
							fold);
			if (cvFold != null)
				fe.setCrossValidationFoldId(cvFold.getCrossValidationFoldId());
			else {
				log.warn("could not obtain cv_fold_id. label=" + label
						+ ", run=" + run + ", fold=" + fold);
			}
		}
		return fe;
	}

	public void setAsEvaluation(ASEvaluation asEvaluation) {
		this.asEvaluation = asEvaluation;
	}

	public void setAttributeSelection(AttributeSelection attributeSelection) {
		this.attributeSelection = attributeSelection;
	}

	public void setClassifierEvaluationDao(
			ClassifierEvaluationDao classifierEvaluationDao) {
		this.classifierEvaluationDao = classifierEvaluationDao;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	public void setSparseDataExporter(SparseDataExporter sparseDataExporter) {
		this.sparseDataExporter = sparseDataExporter;
	}

}
