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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.BaseClassifierEvaluationParser;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;
import org.apache.ctakes.ytex.kernel.model.ClassifierEvaluation;
import org.apache.ctakes.ytex.kernel.model.SemiLClassifierEvaluation;

import com.google.common.collect.BiMap;


/**
 * Parse semiL output, store in DB. With semiL there is no test data set - just
 * training data & unlabelled data. Need the following files:
 * <ul>
 * <li>*.output - semil prediction output
 * <li>options.properties - options passed to semil (semil.distance), other
 * options (kernel.name, kernel.experiment)
 * <li>_test_data.txt - test class ids
 * <li>_train_id.txt - training label ids
 * <li>_test_id.txt - test label ids
 * </ul>
 * 
 * The semil output may contain the output of multiple runs. Will create a
 * classifier_eval record for each run. SemiL may change the label of labeled
 * data (soft label); currently the relabled instances will not be stored.
 * 
 * @author vhacongarlav
 * 
 */
public class SemiLEvaluationParser extends BaseClassifierEvaluationParser {
	public final static Log log = LogFactory
			.getLog(SemiLEvaluationParser.class);
	/**
	 * parse options
	 * 
	 * <pre>
	 *  gamma=10.000000 mu=0.500000 lambda=0.010100 hard_label=1 Laplacian=1 percentage of labeled points =0.000000
	 * </pre>
	 */
	public static Pattern pGamma = Pattern.compile("gamma=([\\d\\.\\-\\+e]+)");
	public static Pattern pMu = Pattern.compile("mu=([\\d\\.\\-\\+e]+)");
	public static Pattern pLambda = Pattern
			.compile("lambda=([\\d\\.\\-\\+e]+)");
	public static Pattern pLabel = Pattern.compile("hard_label=([01])");
	public static Pattern pLaplacian = Pattern.compile("Laplacian=([01])");
	public static Pattern pPercent = Pattern
			.compile("labeled points =([\\d\\.\\-\\+e]+)");
	/**
	 * distance files of the form <tt>label1_dist_pearson_5.txt</tt> parse out
	 * the metric and degree from the file name.
	 */
	public static Pattern pOutput = Pattern.compile("dist_(\\w+)_(\\d+)");

	/**
	 * 
	 * @param fileBaseName
	 *            e.g. label1_run1_fold1
	 * @param dataDir
	 *            where train, test, id files are located
	 * @param outputDir
	 *            where classifier output is stored
	 */
	public void parseDirectory(File dataDir, File outputDir) throws IOException {
		Properties kernelProps = this.loadProps(outputDir);
		// get the name of the label file
		String labelBase = kernelProps.getProperty("kernel.label.basename");
		if (labelBase != null && labelBase.length() > 0) {
			// load instance ids and their class ids
			// construct the name of the class.txt file
			String classFileName = dataDir + File.separator
					+ labelBase.substring(0, labelBase.length() - "label".length())
					+ "class.txt";			
			List<InstanceClassInfo> listClassInfo = super.loadInstanceClassInfo(dataDir,
					classFileName);
			// process .output files
			if (listClassInfo != null) {
				for (File output : outputDir.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".output");
					}
				})) {
					parseSemiLOutput(dataDir, labelBase, kernelProps, output,
							listClassInfo);
				}
			}
		} else {
			log.warn("couldn't parse directory; kernel.label.base not defined. Dir: "
					+ outputDir);
		}
	}

	/**
	 * parse semil output file
	 * 
	 * @param fileBaseName
	 *            parse label, run and fold out of this, e.g.
	 *            label1_run1_fold1_xxx
	 * @param kernelProps
	 *            from options.properties
	 * @param output
	 *            semil output file with predictions
	 * @param listClassInfo
	 *            instance and class ids
	 * @param saveInstanceEval
	 *            should the instance-level evaluations be saved?
	 * @throws IOException
	 */
	private void parseSemiLOutput(File dataDir, String fileBaseName, Properties kernelProps,
			File output, List<InstanceClassInfo> listClassInfo) throws IOException {
		BufferedReader outputReader = null;
		try {
			outputReader = new BufferedReader(new FileReader(output));
			String optionsLine = null;
			String predictLine = null;
			while ((optionsLine = outputReader.readLine()) != null
					&& (predictLine = outputReader.readLine()) != null) {
				SemiLClassifierEvaluation ce = new SemiLClassifierEvaluation();
				// set label, fold, etc
				this.initClassifierEvaluation(fileBaseName, ce);
				// set name, experiment
				this.initClassifierEvaluationFromProperties(kernelProps, ce);
				BiMap<Integer, String> classIdToNameMap = loadClassIdMap(
						dataDir, ce.getLabel());
				// parse options
				parseOptions(ce, optionsLine, kernelProps, output.getName());
				boolean storeUnlabeled = YES.equalsIgnoreCase(kernelProps
						.getProperty(
								ParseOption.STORE_UNLABELED.getOptionKey(),
								ParseOption.STORE_UNLABELED.getDefaultValue()));
				parsePredictedClasses(ce, predictLine, listClassInfo,
						storeUnlabeled, classIdToNameMap);
				// save the classifier evaluation
				this.storeSemiSupervised(kernelProps, ce, classIdToNameMap);
			}
		} finally {
			if (outputReader != null) {
				try {
					outputReader.close();
				} catch (Exception ignore) {
				}
			}
		}
	}

	/**
	 * parse class predictions for test instances out of semil output.
	 * 
	 * @param ce
	 *            evaluation to update
	 * @param predictLine
	 *            line with predictions
	 * @param listClassInfo
	 * @param storeUnlabeled
	 *            should all predictions - not only for test instances be
	 *            stored?
	 */
	private void parsePredictedClasses(ClassifierEvaluation ce,
			String predictLine, List<InstanceClassInfo> listClassInfo,
			boolean storeUnlabeled, BiMap<Integer, String> classIdToNameMap) {
		String strClassIds[] = predictLine.split("\\s");
		String classNames[] = new String[strClassIds.length];
		for (int i = 0; i < strClassIds.length; i++) {
			classNames[i] = classIdToNameMap.get(Integer.parseInt(strClassIds[i]));
		}
		updateSemiSupervisedPredictions(ce, listClassInfo, storeUnlabeled,
				classNames, classIdToNameMap.inverse());
	}

	/**
	 * parse options out of file, into object. get the distance type from
	 * options.properties
	 * 
	 * <pre>
	 * gamma=10.000000  mu=0.500000  lambda=0.010100 hard_label=1 Laplacian=1 percentage of labeled points =0.000000 data_size=242
	 * </pre>
	 * 
	 * @param ce
	 * @param optionsLine
	 */
	private void parseOptions(SemiLClassifierEvaluation ce, String optionsLine,
			Properties kernelProps, String outputName) {
		ce.setOptions(optionsLine);
		ce.setGamma(this.parseDoubleOption(pGamma, optionsLine));
		ce.setLambda(this.parseDoubleOption(pLambda, optionsLine));
		ce.setMu(this.parseDoubleOption(pMu, optionsLine));
		ce.setPercentLabeled(this.parseDoubleOption(pPercent, optionsLine));
		ce.setNormalizedLaplacian(this.parseIntOption(pLaplacian, optionsLine) == 1);
		ce.setSoftLabel(this.parseIntOption(pLabel, optionsLine) == 1);
		Matcher mOutput = pOutput.matcher(outputName);
		if (mOutput.find()) {
			ce.setDistance(mOutput.group(1));
			ce.setDegree(Integer.parseInt(mOutput.group(2)));
		}
		// ce.setDistance(kernelProps.getProperty(
		// ParseOption.DISTANCE.getOptionKey(),
		// ParseOption.DISTANCE.getDefaultValue()));
		// ce.setDegree(Integer.parseInt(kernelProps.getProperty(
		// ParseOption.DEGREE.getOptionKey(),
		// ParseOption.DEGREE.getDefaultValue())));
		ce.setAlgorithm("semiL");
	}

	/**
	 * 
	 * @param labelFile
	 *            contains class ids for each instance. first token of each line
	 *            is the class id.
	 * @param instanceIds
	 *            instance ids corresponding to lines
	 * @return
	 * @throws IOException
	 */
	Map<Integer, Integer> getInstanceIdClass(String labelFile,
			List<Integer> instanceIds) throws IOException {
		Map<Integer, Integer> mapInstanceIdClass = new HashMap<Integer, Integer>(
				instanceIds.size());
		BufferedReader instanceReader = null;
		try {
			instanceReader = new BufferedReader(new FileReader(labelFile));
			int nLine = 0;
			String instanceLine = null;
			while ((instanceLine = instanceReader.readLine()) != null) {
				mapInstanceIdClass.put(instanceIds.get(nLine), Integer
						.parseInt(extractFirstToken(instanceLine, wsPattern)));
				nLine++;
			}
		} finally {
			if (instanceReader != null) {
				try {
					instanceReader.close();
				} catch (Exception e) {
					log.error(labelFile, e);
				}
			}
		}
		return mapInstanceIdClass;
	}

	public static void main(String args[]) throws IOException {
		if (args.length < 2) {
			System.out.println("Usage: java "
					+ SemiLEvaluationParser.class.getName()
					+ "dataDir outputDir");
		} else {
			BaseClassifierEvaluationParser parser = KernelContextHolder
					.getApplicationContext().getBean(
							SemiLEvaluationParser.class);
			parser.parseDirectory(new File(args[0]), new File(args[1]));
		}
	}
}
