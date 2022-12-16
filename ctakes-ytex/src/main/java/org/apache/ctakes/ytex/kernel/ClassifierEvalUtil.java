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
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClassifierEvalUtil {
	private static final Log log = LogFactory.getLog(ClassifierEvalUtil.class);
	Properties props;

	public ClassifierEvalUtil(String propFile) throws IOException {
		if (propFile != null)
			props = FileUtil.loadProperties(propFile, true);
		else
			props = System.getProperties();
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String propFile = null;
		if (args.length > 0)
			propFile = args[0];
		ClassifierEvalUtil ceUtil = new ClassifierEvalUtil(propFile);
		ceUtil.generateEvalFiles();
	}

	private void generateEvalFiles() throws IOException {
		String algo = props.getProperty("kernel.algo");
		if ("semil".equalsIgnoreCase(algo)) {
			generateSemilEvalParams();
		} else if ("svmlight".equalsIgnoreCase(algo)
				|| "libsvm".equalsIgnoreCase(algo)) {
			generateSvmEvalParams(algo.toLowerCase());
		} else if ("svmlin".equalsIgnoreCase(algo)) {
			generateSvmLinParams(algo.toLowerCase());
		}
	}

	private void generateSvmLinParams(String lowerCase) throws IOException {
		File kernelDataDir = new File(props.getProperty("kernel.data", "."));
		String weightPropsFile = props.getProperty(
				"kernel.svmlin.classweights", kernelDataDir
						+ "/classWeights.properties");
		if (log.isDebugEnabled()) {
			log.debug("loading weights from " + weightPropsFile);
		}
		Properties weightProps = FileUtil
				.loadProperties(weightPropsFile, false);
		if (weightProps == null) {
			log.warn("could not load weights from file: " + weightPropsFile);
		}
		Properties props = new Properties();
		File[] labelFiles = kernelDataDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("code.properties");
			}
		});
		if (labelFiles != null && labelFiles.length > 0) {
			// iterate over label files
			for (File labelFile : labelFiles) {
				populateSvmlinParameters(labelFile, kernelDataDir, weightProps,
						props);
				// writeSvmlinEvalFile(labelFile, kernelDataDir);
			}
		}
		writeProps(kernelDataDir + "/parameters.properties", props);
	}

	private String getSvmlinDataFileForLabel(File labelFile, File kernelDataDir) {
		String labelFileName = labelFile.getName();
		String label = FileUtil.parseLabelFromFileName(labelFileName);
		Integer run = FileUtil.parseRunFromFileName(labelFileName);
		Integer fold = FileUtil.parseFoldFromFileName(labelFileName);
		File dataFile = null;
		// check fold scope
		if (fold != null && fold.intValue() != 0) {
			dataFile = new File(FileUtil.getScopedFileName(
					kernelDataDir.getPath(), label, run, fold, "data.txt"));
		}
		// no matches, check label scope
		if ((dataFile == null || !dataFile.exists()) && label != null
				&& label.length() > 0) {
			dataFile = new File(FileUtil.getScopedFileName(
					kernelDataDir.getPath(), label, null, null, "data.txt"));
		}
		// no matches, check unscoped
		if (dataFile == null || !dataFile.exists()) {
			dataFile = new File(FileUtil.getScopedFileName(
					kernelDataDir.getPath(), null, null, null, "data.txt"));
		}
		if (dataFile != null && dataFile.exists()) {
			return dataFile.getName();
		} else {
			log.warn("no data files match label file: " + labelFile);
			return null;
		}
	}

	/**
	 * set following properties in props
	 * <ul>
	 * <li>[codeFile basename].dataFile
	 * <li>[codeFile basename].kernel.evalLines
	 * <li>[labelFile basename].param.R
	 * </ul>
	 * 
	 * @param codeFile
	 * @param kernelDataDir
	 * @param weightProps
	 * @param props
	 * @throws IOException
	 */
	private void populateSvmlinParameters(File codeFile, File kernelDataDir,
			Properties weightProps, Properties paramProps) throws IOException {
		// cut off the .properties from the file name - this is the prefix for
		// the properties
		String codeFileBasename = codeFile.getName();
		codeFileBasename = codeFileBasename.substring(0,
				codeFileBasename.length() - ".properties".length());
		// determine the scoped data file name
		String dataFile = getSvmlinDataFileForLabel(codeFile, kernelDataDir);
		if (dataFile != null) {
			// if the dataFile could be found, set the property
			paramProps.setProperty(codeFileBasename + ".dataFile", dataFile);
			// generate the parameter grid
			List<String> algos = Arrays.asList(addOptionPrefix(props
					.getProperty("cv.svmlin.algo").split(","), "-A "));
			List<String> lambdaU = Arrays.asList(addOptionPrefix(props
					.getProperty("cv.svmlin.lambdaW").split(","), "-W "));
			List<String> lambdaW = Arrays.asList(addOptionPrefix(props
					.getProperty("cv.svmlin.lambdaU").split(","), "-U "));
			List<String> evalLines = parameterGrid(algos, lambdaU, lambdaW);
			// set the parameter grid property
			paramProps.setProperty(codeFileBasename + ".kernel.evalLines",
					listToString(evalLines));
			if (weightProps != null) {
				// determine the positive class fraction for each label file
				Properties codeProps = FileUtil.loadProperties(
						codeFile.getAbsolutePath(), false);
				// iterate through the code files
				for (String labelfile : codeProps.getProperty("codes", "")
						.split(",")) {
					// get the class id for the given label file
					String className = codeProps
							.getProperty(labelfile + ".className");
					// figure out the key to look up the positive class fraction
					// in the classWeights.properties file
					// if a label is specified then the key is label.n.class.m,
					// else just class.m
					String label = FileUtil.parseLabelFromFileName(labelfile);
					String key = label != null && label.length() > 0 ? "label"
							+ label + "_" : "";
					key = key + "class" + className;
					String posClassFrac = weightProps.getProperty(key);
					if (posClassFrac != null) {
						// set the class fraction property
						// use basename of label file as key prefix
						paramProps.put(labelfile + ".param.R", posClassFrac);
					}
				}
			}
		}
	}
	

	// private void writeSvmlinEvalFile(File labelFile, File kernelDataDir)
	// throws IOException {
	// String dataFile = getSvmlinDataFileForLabel(labelFile, kernelDataDir);
	// if (dataFile != null) {
	// List<String> classFracs = new ArrayList<String>(1);
	// String posClassFrac = getClassFrac(labelFile);
	// if (posClassFrac != null) {
	// classFracs.add("-R " + posClassFrac);
	// } else {
	// classFracs.add("");
	// }
	// List<String> algos = Arrays.asList(addOptionPrefix(props
	// .getProperty("cv.svmlin.algo").split(","), "-A "));
	// List<String> lambdaU = Arrays.asList(addOptionPrefix(props
	// .getProperty("cv.svmlin.lambdaW").split(","), "-W "));
	// List<String> lambdaW = Arrays.asList(addOptionPrefix(props
	// .getProperty("cv.svmlin.lambdaU").split(","), "-U "));
	// List<String> evalLines = parameterGrid(classFracs, algos, lambdaU,
	// lambdaW);
	// Properties props = new Properties();
	// props.setProperty("kernel.dataFile", dataFile);
	// props.setProperty("kernel.evalLines", listToString(evalLines));
	// String evalFile = labelFile.getPath().substring(0,
	// labelFile.getPath().length() - 3)
	// + "properties";
	// writeProps(evalFile, props);
	// }
	// }

	// /**
	// * get the positive class fraction. get this from the
	// * kernel.classrel.[label] or kernel.classrel property
	// *
	// * @param labelFile
	// * @return class fraction if specified
	// */
	// private String getClassFrac(File labelFile) {
	// String classFrac = null;
	// String label = FileUtil.parseLabelFromFileName(labelFile.getName());
	// if (label != null) {
	// classFrac = props.getProperty("kernel.classrel." + label);
	// } else {
	// classFrac = props.getProperty("kernel.classrel");
	// }
	// return classFrac;
	// }

	private void generateSvmEvalParams(String svmType) throws IOException {
		File kernelDataDir = new File(props.getProperty("kernel.data", "."));
		File[] trainFiles = kernelDataDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("train_data.txt");
			}
		});
		Properties params = new Properties();
		if (trainFiles != null && trainFiles.length > 0) {
			// iterate over label files
			for (File trainFile : trainFiles) {
				writeSvmEvalFile(params, trainFile, kernelDataDir, svmType);
			}
		}
		writeProps(kernelDataDir + "/parameters.properties", params);
	}

	/**
	 * generate parameter grid for each training file. add a property [file base
	 * name].kernel.evalLines=xxx to props.
	 * 
	 * @param props
	 *            properties to populate
	 * @param trainFile
	 * @param kernelDataDir
	 * @param svmType
	 * @throws IOException
	 */
	private void writeSvmEvalFile(Properties params, File trainFile,
			File kernelDataDir, String svmType) throws IOException {
		// list to hold the svm command lines
		List<String> evalLines = new ArrayList<String>();
		// label-specific weight parameters from a property file
		List<String> weightParams = getWeightParams(trainFile, svmType);
		// kernels to test
		List<String> kernels = Arrays.asList(props.getProperty("kernel.types")
				.split(","));
		// cost params
		List<String> costs = Arrays.asList(addOptionPrefix(
				props.getProperty("cv.costs").split(","), "-c "));
		// other general params
		List<String> libsvmEval = Arrays.asList(props.getProperty(
				"cv." + svmType + ".train.line", "").split(","));
		// iterate through kernel types, generate parameter grids
		for (String kernel : kernels) {
			List<String> kernelOpts = Arrays.asList(new String[] { "-t "
					+ kernel });
			if ("0".equals(kernel) || "4".equals(kernel)) {
				// linear/custom kernel - just cost & weight param
				evalLines.addAll(parameterGrid(libsvmEval, kernelOpts, costs,
						weightParams));
			} else if ("1".equals(kernel)) {
				// polynomial kernel - cost & weight & degree param
				evalLines.addAll(parameterGrid(libsvmEval, kernelOpts, costs,
						weightParams, Arrays.asList(addOptionPrefix(props
								.getProperty("cv.poly.degrees").split(","),
								"-d "))));
			} else if ("2".equals(kernel) || "3".equals(kernel)) {
				// polynomial kernel - cost & weight & gamma param
				evalLines.addAll(parameterGrid(libsvmEval, kernelOpts, costs,
						weightParams, Arrays
								.asList(addOptionPrefix(
										props.getProperty("cv.rbf.gammas")
												.split(","), "-g "))));
			}
		}
		if (evalLines.size() > 0) {
			String basename = trainFile.getName().substring(0,
					trainFile.getName().length() - 4);
			params.put(basename + ".kernel.evalLines", listToString(evalLines));
			// String evalFile = trainFile.getPath().substring(0,
			// trainFile.getPath().length() - 3)
			// + "properties";
			// Properties evalProps = new Properties();
			// evalProps.put("kernel.evalLines", listToString(evalLines));
			// writeProps(evalFile, evalProps);
		}
	}

	private List<String> getWeightParams(File trainFile, String svmType)
			throws IOException {
		if ("libsvm".equals(svmType)) {
			String label = FileUtil.parseLabelFromFileName(trainFile.getName());
			// default label to 0
			label = label != null && label.length() > 0 ? label : "0";
			Properties weightProps = new Properties();
			weightProps.putAll(props);
			if (props.getProperty("kernel.classweights") != null) {
				Properties tmp = FileUtil.loadProperties(
						props.getProperty("kernel.classweights"), false);
				if(tmp != null)
					weightProps.putAll(tmp);
			}
			String weights = weightProps.getProperty("kernel.weight."
					+ label);
			if (weights != null && weights.length() > 0) {
				return Arrays.asList(weights.split(","));
			}
		}
		return new ArrayList<String>(0);
	}

	private void generateSemilEvalParams() throws IOException {
		File kernelDataDir = new File(props.getProperty("kernel.data", "."));
		List<String> evalLines = generateSemilEvalLines();
		File[] labelFiles = kernelDataDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("label.txt");
			}
		});
		if (labelFiles != null && labelFiles.length > 0) {
			// iterate over label files
			for (File labelFile : labelFiles) {
				List<String> distFiles = getSemilDistFilesForLabel(labelFile,
						kernelDataDir);
				if (distFiles != null)
					writeSemilEvalFile(distFiles, evalLines, labelFile);
			}
		}
	}

	/**
	 * convert list of strings to comma-delimited string;
	 * 
	 * @param listStr
	 * @return
	 */
	private String listToString(List<String> listStr) {
		StringBuilder b = new StringBuilder();
		boolean bfirst = true;
		for (String str : listStr) {
			if (!bfirst)
				b.append(",");
			b.append(str);
			bfirst = false;
		}
		return b.toString();
	}

	/**
	 * write file for label
	 * 
	 * @param distFiles
	 * @param evalLines
	 * @param labelFile
	 * @throws IOException
	 */
	private void writeSemilEvalFile(List<String> distFiles,
			List<String> evalLines, File labelFile) throws IOException {
		String labelFileName = labelFile.getPath();
		String evalFileName = labelFileName.substring(0,
				labelFileName.length() - 3) + "properties";
		Properties props = new Properties();
		props.setProperty("kernel.distFiles", listToString(distFiles));
		props.setProperty("kernel.evalLines", listToString(evalLines));
		writeProps(evalFileName, props);
	}

	private void writeProps(String evalFileName, Properties evalProps)
			throws IOException {
		if ("no".equalsIgnoreCase(props.getProperty("kernel.overwriteEvalFile",
				"yes"))) {
			File evalFile = new File(evalFileName);
			if (evalFile.exists()) {
				log.warn("skipping because eval file exists: " + evalFileName);
				return;
			}
		}
		BufferedWriter w = null;
		try {

			w = new BufferedWriter(new FileWriter(evalFileName));
			evalProps.store(w, null);
		} finally {
			if (w != null)
				w.close();
		}
	}

	/**
	 * generate command lines for semil
	 * 
	 * @return
	 */
	private List<String> generateSemilEvalLines() {
		// cv.rbf.gammas
		String gammas = props.getProperty("cv.rbf.gammas");
		List<String> gammaOpts = null;
		if (gammas != null && gammas.length() > 0) {
			gammaOpts = Arrays
					.asList(addOptionPrefix(gammas.split(","), "-g "));
		}
		// cv.semil.methods
		List<String> methods = Arrays.asList(props.getProperty(
				"cv.semil.methods", "").split(","));
		// semil.line
		List<String> semil = Arrays.asList(props.getProperty("cv.semil.line",
				"").split(","));
		return parameterGrid(semil, gammaOpts, methods);
	}

	private String[] addOptionPrefix(String[] args, String prefix) {
		String[] options = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			options[i] = prefix + args[i];
		}
		return options;
	}

	/**
	 * recursively generate parameter grid
	 * 
	 * @param lines
	 *            current lines
	 * @param params
	 *            variable number of List<String> arguments
	 * @return
	 */
	private List<String> parameterGrid(List<String> lines, Object... params) {
		List<String> newLines = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		List<String> paramList = (List<String>) params[0];
		if (paramList != null && paramList.size() > 0) {
			// only iterate over the list if it is non-empty
			for (String line : lines) {
				for (String param : paramList) {
					newLines.add(line + " " + param);
				}
			}
		} else {
			// else newLines = lines
			newLines.addAll(lines);
		}
		if (params.length > 1) {
			return parameterGrid(newLines,
					Arrays.copyOfRange(params, 1, params.length));
		} else {
			return newLines;
		}
	}

	private List<String> getSemilDistFilesForLabel(File labelFile,
			File kernelDataDir) {
		String labelFileName = labelFile.getName();
		String label = FileUtil.parseLabelFromFileName(labelFileName);
		Integer run = FileUtil.parseRunFromFileName(labelFileName);
		Integer fold = FileUtil.parseFoldFromFileName(labelFileName);
		File[] distFiles = null;
		// check fold scope
		if (fold != null) {
			String filePrefix = FileUtil.getFoldFilePrefix(null, label, run,
					fold) + "_dist_";
			distFiles = kernelDataDir.listFiles(new FileUtil.PrefixFileFilter(
					filePrefix));
		}
		// no matches, check label scope
		if ((distFiles == null || distFiles.length == 0) && label != null) {
			String filePrefix = FileUtil.getFoldFilePrefix(null, label, null,
					null) + "_dist_";
			distFiles = kernelDataDir.listFiles(new FileUtil.PrefixFileFilter(
					filePrefix));
		}
		// no matches, check unscoped
		if (distFiles == null || distFiles.length == 0) {
			distFiles = kernelDataDir.listFiles(new FileUtil.PrefixFileFilter(
					"dist_"));
		}
		if (distFiles != null && distFiles.length > 0) {
			List<String> listDistFiles = new ArrayList<String>(distFiles.length);
			for (File distFile : distFiles) {
				listDistFiles.add(distFile.getName());
			}
			return listDistFiles;
		} else {
			log.warn("no dist files match label file: " + labelFile);
			return null;
		}
	}

}
