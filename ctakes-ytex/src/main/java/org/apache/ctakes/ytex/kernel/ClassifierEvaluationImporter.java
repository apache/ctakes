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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * parse classifier evaluation results. expect input data files to classifier in
 * working directory. expect output in dir option or subdirectories thereof.
 * expect an options.properties in each directory that contains classifier
 * output. See {@link #ClassifierEvaluationImporter()} for a list of options in
 * options.properties. You can override options via system properties (java -D
 * options).
 * 
 * @author vijay
 */
public class ClassifierEvaluationImporter {
	private static final Log log = LogFactory
			.getLog(ClassifierEvaluationImporter.class);

	private Map<String, ClassifierEvaluationParser> nameToParserMap;

	public Map<String, ClassifierEvaluationParser> getNameToParserMap() {
		return nameToParserMap;
	}

	public void setNameToParserMap(
			Map<String, ClassifierEvaluationParser> nameToParserMap) {
		this.nameToParserMap = nameToParserMap;
	}

	@SuppressWarnings("static-access")
	private static Options initOptions() {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("cvDir")
				.hasArg()
				.withDescription(
						"results directory, defaults to working directory")
				.isRequired(false).create("dir"));
		options.addOption(OptionBuilder.withArgName("type").hasArg()
				.withDescription("libsvm (default) or svmlight or semil")
				.isRequired(true).create("type"));
		return options;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Options options = initOptions();
		if (args.length == 0) {
			printHelp(options);
		} else {
			CommandLineParser oparser = new GnuParser();
			ClassifierEvaluationImporter importer = KernelContextHolder
					.getApplicationContext().getBean(
							ClassifierEvaluationImporter.class);
			try {
				CommandLine line = oparser.parse(options, args);
				importer.importDirectory(line);
			} catch (ParseException e) {
				printHelp(options);
				throw e;
			}
		}
	}

	private ClassifierEvaluationParser getParser(CommandLine line) {
		String type = line.getOptionValue("type", "libsvm");
		return this.nameToParserMap.get(type);
	}

	/**
	 * Expect directory with subdirectories for each evaluation. Subdirectories
	 * must contain following in order for results to be processed:
	 * <ul>
	 * <li>
	 * model.txt: libsvm model trained on training set
	 * <li>predict.txt: libsvm predictions on test set
	 * <li>options.properties: libsvm command line options
	 * </ul>
	 * 
	 * @param line
	 * @throws Exception
	 */
	public void importDirectory(CommandLine line) throws IOException {
		ClassifierEvaluationParser lparser = getParser(line);
		File directory = new File(line.getOptionValue("dir", "."));
		importDirectory(directory, lparser);
	}

	/**
	 * recursively import directory. We assume this directory contains
	 * evaluation results if it has no subdirectories. Else we
	 * look in subdirectories.
	 * 
	 * @param directory
	 * @param lparser
	 * @throws IOException
	 */
	public void importDirectory(File directory,
			ClassifierEvaluationParser lparser) throws IOException {
		File subdirs[] = directory.listFiles(new FileUtil.DirectoryFileFilter());
		if(subdirs == null || subdirs.length == 0) {
			// no subdirectories - assume this is a 'results' directory
			try {
				lparser.parseDirectory(new File("."), directory);
			} catch (IOException ioe) {
				log.error("error parsing directory: " + directory, ioe);
			}
		} else {
			// look in subdirectories
			for (File subdir : subdirs) {
				importDirectory(subdir, lparser);
			}
		}
		//
		// if ("semil".equals(type) && checkFileRead(optionsFile)) {
		// lparser.parseDirectory(new File("."), resultDir);
		// } else if (("libsvm".equals(type) || "svmlight".equals(type))
		// && checkFileRead(model) && checkFileRead(predict)
		// && checkFileRead(optionsFile)) {
		// String options = null;
		// Double param1 = null;
		// String param2 = null;
		// InputStream isOptions = null;
		// try {
		// isOptions = new FileInputStream(optionsFile);
		// Properties props = new Properties();
		// props.load(isOptions);
		// options = props.getProperty("kernel.eval.line");
		// String strParam1 = props.getProperty("kernel.param1", null);
		// if (strParam1 != null) {
		// try {
		// param1 = Double.parseDouble(strParam1);
		// } catch (Exception e) {
		// log.warn("error parasing param1: " + strParam1, e);
		// }
		// }
		// param2 = props.getProperty("kernel.param2");
		// } finally {
		// isOptions.close();
		// }
		// if (options != null) {
		// try {
		// ClassifierEvaluation eval = lparser
		// .parseClassifierEvaluation(line
		// .getOptionValue("name"), line
		// .getOptionValue("experiment"), line
		// .getOptionValue("label"), options,
		// predict, line.getOptionValue("test"),
		// model, line
		// .getOptionValue("instanceId"),
		// output, "yes".equals(line
		// .getOptionValue("storeProb",
		// "no")));
		// eval.setParam1(param1);
		// eval.setParam2(param2);
		// KernelContextHolder.getApplicationContext().getBean(
		// ClassifierEvaluationDao.class)
		// .saveClassifierEvaluation(eval,
		// storeInstanceEval);
		// } catch (Exception e) {
		// // continue processing - don't give up because of one
		// // bad file
		// log.warn("error importing results, resultDir="
		// + resultDir.getAbsolutePath(), e);
		// }
		// }
		// }
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java org.apache.ctakes.ytex.libsvm.ClassifierEvaluationImporter\n",
				options);
	}

}
