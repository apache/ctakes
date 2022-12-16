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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.ytex.kernel.BaseClassifierEvaluationParser;
import org.apache.ctakes.ytex.kernel.model.ClassifierEvaluation;
import org.apache.ctakes.ytex.kernel.model.ClassifierInstanceEvaluation;
import org.apache.ctakes.ytex.kernel.model.SVMClassifierEvaluation;


public class LibSVMParser extends BaseClassifierEvaluationParser {
	public static Pattern labelsPattern = Pattern.compile("labels\\s+(.*)");
	public static Pattern totalSVPattern = Pattern.compile("total_sv (\\d+)");
	public static Pattern pKernel = Pattern.compile("-t\\s+(\\d)");
	public static Pattern pGamma = Pattern.compile("-g\\s+([\\d\\.eE-]+)");
	public static Pattern pCost = Pattern.compile("-c\\s+([\\d\\.eE-]+)");
	public static Pattern pWeight = Pattern
			.compile("-w-{0,1}\\d\\s+[\\d\\.]+\\b");
	public static Pattern pDegree = Pattern.compile("-d\\s+(\\d+)");

	/**
	 * parse svm-train model file to get the number of support vectors. Needed
	 * for model selection
	 * 
	 * @param modelFile
	 * @return
	 * @throws IOException
	 */
	public Integer parseModel(String modelFile) throws IOException {
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(modelFile));
			String line = null;
			while ((line = r.readLine()) != null) {
				Matcher m = totalSVPattern.matcher(line);
				if (m.find()) {
					return new Integer(m.group(1));
				}
			}
		} finally {
			try {
				if (r != null)
					r.close();
			} catch (Exception e) {
				System.err.println("reading model file");
				e.printStackTrace(System.err);
			}
		}
		return null;
	}

	// /**
	// * Parse svm-predict input (instance file) and predictions (prediction
	// file)
	// *
	// * @param predictionFile
	// * @param instanceFile
	// * @return
	// * @throws Exception
	// * @throws IOException
	// */
	// public ClassifierEvaluationResults parse(String predictionFile,
	// String instanceFile, Properties props) throws IOException {
	// ClassifierEvaluationResults results = new ClassifierEvaluationResults();
	// List<ClassifierEvaluationResult> listResults = new
	// ArrayList<ClassifierEvaluationResult>();
	// results.setResults(listResults);
	// BufferedReader instanceReader = null;
	// BufferedReader predictionReader = null;
	// try {
	// instanceReader = new BufferedReader(new FileReader(instanceFile));
	// predictionReader = new BufferedReader(
	// new FileReader(predictionFile));
	// String instanceLine = null;
	// String predictionLine = null;
	// int nLine = 0;
	// // 1st line in libSVMOutputReader lists labels
	//
	// results.setClassIds(parseClassIds(predictionReader));
	// // when working with high cutoffs resulting in mainly zero vectors
	// // we sometimes have a trivial classification problem (1 class)
	// // if (results.getClassIds().size() < 2)
	// // throw new Exception("error parsing class ids");
	// while (((instanceLine = instanceReader.readLine()) != null)
	// && ((predictionLine = predictionReader.readLine()) != null)) {
	// nLine++;
	// ClassifierEvaluationResult result = new ClassifierEvaluationResult();
	// listResults.add(result);
	// String predictTokens[] = wsPattern.split(predictionLine);
	// String classIdPredicted = predictTokens[0];
	// String classIdTarget = extractFirstToken(instanceLine,
	// wsPattern);
	// result.setTargetClassId(Integer.parseInt(classIdTarget));
	// result.setPredictedClassId(Integer.parseInt(classIdPredicted));
	// if (predictTokens.length > 1) {
	// double probabilities[] = new double[results.getClassIds()
	// .size()];
	// for (int i = 1; i < predictTokens.length; i++) {
	// probabilities[i - 1] = Double
	// .parseDouble(predictTokens[i]);
	// }
	// result.setProbabilities(probabilities);
	// }
	// }
	// } finally {
	// if (instanceReader != null) {
	// try {
	// instanceReader.close();
	// } catch (Exception e) {
	// System.err.println("testGramReader");
	// e.printStackTrace(System.err);
	// }
	// }
	// if (predictionReader != null) {
	// try {
	// predictionReader.close();
	// } catch (Exception e) {
	// e.printStackTrace(System.err);
	// }
	// }
	// }
	// return results;
	// }

	/**
	 * parse class ids from first line in prediction file. this correspond to
	 * probabilities
	 * 
	 * @param predictionReader
	 * @return
	 * @throws IOException
	 */
	protected List<Integer> parseClassIds(BufferedReader predictionReader)
			throws IOException {
		List<Integer> labels = null;
		String labelLine = predictionReader.readLine();
		Matcher labelMatcher = labelsPattern.matcher(labelLine);
		if (labelMatcher.find()) {
			String labelsA[] = wsPattern.split(labelMatcher.group(1));
			if (labelsA != null && labelsA.length > 0) {
				labels = new ArrayList<Integer>(labelsA.length);
				for (String label : labelsA)
					labels.add(Integer.parseInt(label));
			}
		}
		return labels;
	}

	protected SVMClassifierEvaluation initClassifierEval(String name,
			String experiment, String label, String options,
			String instanceIdFile) {
		SVMClassifierEvaluation eval = new SVMClassifierEvaluation();
		initClassifierEval(name, experiment, label, options, instanceIdFile,
				eval);
		return eval;
	}

	private void initClassifierEval(String name, String experiment,
			String label, String options, String instanceIdFile,
			ClassifierEvaluation eval) {
		initClassifierEvaluation(instanceIdFile, eval);
		eval.setName(name);
		eval.setExperiment(experiment);
		eval.setOptions(options);
	}

	/**
	 * parse predicted class ids, probabilities; correlate to target class ids
	 * and instance ids.
	 * 
	 * @param predictionFile
	 *            prediction (output)
	 * @param instanceFile
	 *            input data file; contains target class ids
	 * @param props
	 * @param instanceIdFile
	 *            instance ids corresponding to lines in input data file
	 * @param eval
	 * @throws IOException
	 */
	protected void parsePredictions(String predictionFile, String instanceFile,
			Properties props, String instanceIdFile,
			SVMClassifierEvaluation eval) throws IOException {
		boolean storeProbabilities = YES.equalsIgnoreCase(props.getProperty(
				ParseOption.STORE_PROBABILITIES.getOptionKey(),
				ParseOption.STORE_PROBABILITIES.getDefaultValue()));
		List<Long> instanceIds = null;
		if (instanceIdFile != null)
			instanceIds = parseInstanceIds(instanceIdFile);
		BufferedReader instanceReader = null;
		BufferedReader predictionReader = null;
		try {
			instanceReader = new BufferedReader(new FileReader(instanceFile));
			predictionReader = new BufferedReader(
					new FileReader(predictionFile));
			String instanceLine = null;
			String predictionLine = null;
			int nLine = 0;
			// 1st line in libSVMOutputReader lists class ids - parse them out
			List<Integer> classIds = parseClassIds(predictionReader);
			// iterate through input data file and output predictions
			// simultaneously
			while (((instanceLine = instanceReader.readLine()) != null)
					&& ((predictionLine = predictionReader.readLine()) != null)) {
				// get instance id corresponding to this line
				long instanceId = instanceIds.size() > nLine ? instanceIds
						.get(nLine) : nLine;
				nLine++;
				// allocate instanceEval
				ClassifierInstanceEvaluation instanceEval = new ClassifierInstanceEvaluation();
				// parse out predicted class from output predictions
				String predictTokens[] = wsPattern.split(predictionLine);
				String classIdPredicted = predictTokens[0];
				String classIdTarget = extractFirstToken(instanceLine,
						wsPattern);
				// parse out target class from input data file
				instanceEval.setTargetClassId(Integer.parseInt(classIdTarget));
				instanceEval.setPredictedClassId(Integer
						.parseInt(classIdPredicted));
				instanceEval.setInstanceId(instanceId);
				instanceEval.setClassifierEvaluation(eval);
				// add the instance to the map
				eval.getClassifierInstanceEvaluations().put(instanceId,
						instanceEval);
				// parse class id probabilities
				if (storeProbabilities && predictTokens.length > 1) {
					for (int i = 1; i < predictTokens.length; i++) {
						instanceEval.getClassifierInstanceProbabilities().put(
								classIds.get(i - 1),
								Double.parseDouble(predictTokens[i]));
					}
				}
			}
		} finally {
			if (instanceReader != null) {
				try {
					instanceReader.close();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
			if (predictionReader != null) {
				try {
					predictionReader.close();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}

	protected void parseOptions(SVMClassifierEvaluation eval, String options) {
		// -q -b 1 -t 2 -w1 41 -g 1000 -c 1000 training_data_11_fold9_train.txt
		// training_data_11_fold9_model.txt
		if (options != null) {
			eval.setKernel(parseIntOption(pKernel, options));
			if (eval.getKernel() == null)
				eval.setKernel(0);
			eval.setDegree(parseIntOption(pDegree, options));
			eval.setWeight(parseWeight(options));
			eval.setCost(parseDoubleOption(pCost, options));
			eval.setGamma(parseDoubleOption(pGamma, options));
		}
	}

	/**
	 * parse the weight options out of the libsvm command line. they are of the
	 * form -w0 1 -w2 1.5 ...
	 * 
	 * @param options
	 * @return null if no weight options, else weight options
	 */
	private String parseWeight(String options) {
		StringBuilder bWeight = new StringBuilder();
		Matcher m = pWeight.matcher(options);
		boolean bWeightParam = false;
		while (m.find()) {
			bWeightParam = true;
			bWeight.append(m.group()).append(" ");
		}
		if (bWeightParam)
			return bWeight.toString();
		else
			return null;
	}

	/**
	 * parse directory. Expect following files:
	 * <ul>
	 * <li>model.txt - libsvm model file
	 * <li>options.properties - properties file with needed parameter settings
	 * (see ParseOption)
	 * <li>predict.txt - predictions on test set
	 * </ul>
	 */
	@Override
	public void parseDirectory(File dataDir, File outputDir) throws IOException {
		String model = outputDir.getPath() + File.separator + "model.txt";
		String predict = outputDir.getPath() + File.separator + "predict.txt";
		String optionsFile = outputDir.getPath() + File.separator
				+ "options.properties";
		if (checkFileRead(model) && checkFileRead(predict)
				&& checkFileRead(optionsFile)) {
			// read options.properties
			Properties props = this.loadProps(outputDir);
			SVMClassifierEvaluation eval = new SVMClassifierEvaluation();
			// set algorithm
			eval.setAlgorithm("libsvm");
			// parse results
			parseResults(dataDir, outputDir, model, predict, eval, props);
			// store results
			storeResults(dataDir, props, eval);
		}
	}

	/**
	 * store the parsed classifier evaluation
	 * 
	 * @param props
	 * @param eval
	 * @throws IOException
	 */
	protected void storeResults(File dataDir, Properties props,
			SVMClassifierEvaluation eval) throws IOException {
		// store the classifier evaluation
		getClassifierEvaluationDao().saveClassifierEvaluation(
				eval,
				this.loadClassIdMap(dataDir, eval.getLabel()),
				YES.equalsIgnoreCase(props.getProperty(
						ParseOption.STORE_INSTANCE_EVAL.getOptionKey(),
						ParseOption.STORE_INSTANCE_EVAL.getDefaultValue())));
	}

	/**
	 * parse the results in the specified output dir. use reference data from
	 * dataDir.
	 * 
	 * @param dataDir
	 * @param outputDir
	 * @param model
	 * @param predict
	 * @param eval
	 * @param props
	 * @throws IOException
	 */
	protected void parseResults(File dataDir, File outputDir, String model,
			String predict, SVMClassifierEvaluation eval, Properties props)
			throws IOException {
		// initialize common properties
		initClassifierEvaluationFromProperties(props, eval);
		// parse number of support vectors from model
		eval.setSupportVectors(this.parseModel(model));
		// parse options from command line
		parseOptions(eval,
				props.getProperty(ParseOption.EVAL_LINE.getOptionKey()));
		// parse fold, run, label from file base name
		String fileBaseName = this.getFileBaseName(props);
		initClassifierEvaluation(fileBaseName, eval);
		// parse predictions
		String instanceIdFile = dataDir + File.separator + fileBaseName
				+ "test_id.txt";
		String instanceFile = dataDir + File.separator + fileBaseName
				+ "test_data.txt";
		this.parsePredictions(predict, instanceFile, props, instanceIdFile,
				eval);
	}

}
