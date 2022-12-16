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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;


/**
 * Runs org.apache.ctakes.ytex.weka.WekaResultsImporter for the specified task and result file.
 * 
 * @see WekaResultsImporter
 * @author vijay
 * 
 */
public class ImportWekaResults  {

	/**
	 * @param args
	 */
	public static void docPredImport(String predictions, String task)
			throws Exception {
		WekaResultsImporter importer = (WekaResultsImporter) KernelContextHolder
				.getApplicationContext().getBean("wekaResultsImporter");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(predictions));
			importer.importDocumentResults(task, reader);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	private static Options initOptions() {
		Options options = new Options();
		OptionGroup og = new OptionGroup();
		og.addOption(OptionBuilder.withArgName("cvDir").hasArg()
				.withDescription("fold cross-validation results directory")
				.create("cvDir"));
		og.addOption(OptionBuilder.withArgName("document").hasArg()
				.withDescription("document prediction output")
				.create("docPred"));
		og.addOption(OptionBuilder.withArgName("document").hasArg()
				.withDescription("prediction").create("pred"));
		options.addOptionGroup(og);
		options.addOption(OptionBuilder.withArgName("algo").hasArg()
				.withDescription("algorithm").create("algo"));
		options.addOption(OptionBuilder.withArgName("name").hasArg()
				.withDescription("name").isRequired().create("name"));
		options.addOption(OptionBuilder.withArgName("experiment").hasArg()
				.withDescription("experiment").create("experiment"));
		options.addOption(OptionBuilder.withArgName("label").hasArg()
				.withDescription("label / task").create("label"));
		options.addOption(OptionBuilder.withArgName("yes/no").hasArg()
				.withDescription("store probabilities, default yes").create(
						"storeProb"));
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
			try {
				CommandLine line = oparser.parse(options, args);
				if (line.hasOption("cvDir")) {
					importDirectory(line);
				} else if (line.hasOption("docPred")) {
					docPredImport(options.getOption("docPred").getValue(),
							options.getOption("label").getValue());
				} else if (line.hasOption("pred")) {
					importPred(line);
				}
			} catch (ParseException e) {
				printHelp(options);
				throw e;
			}
		}
	}

	private static void importPred(CommandLine line) {
		// TODO Auto-generated method stub

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
	private static void importDirectory(CommandLine line) throws Exception {
		WekaResultsImporter importer = (WekaResultsImporter) KernelContextHolder
				.getApplicationContext().getBean("wekaResultsImporter");
		File cvDir = new File(line.getOptionValue("cvDir"));
		if (cvDir.listFiles() == null || cvDir.listFiles().length == 0) {
			System.err.println("directory is empty: " + cvDir);
		} else {
			for (File resultDir : cvDir.listFiles()) {
				String model = resultDir + File.separator + "model.obj";
				String output = resultDir + File.separator + "predict.txt";
				String optionsFile = resultDir + File.separator
						+ "options.properties";
				if (checkFileRead(output) && checkFileRead(optionsFile)) {
					String options = null;
					Integer fold = null;
					InputStream isOptions = null;
					try {
						isOptions = new FileInputStream(optionsFile);
						Properties props = new Properties();
						props.load(isOptions);
						options = props.getProperty("options");
						String strFold = props.getProperty("fold");
						if(strFold != null) {
							try {
								fold = Integer.parseInt(strFold);
							} catch(NumberFormatException nfe) {
								
							}
						}
					} finally {
						isOptions.close();
					}
					if (options != null) {
						BufferedReader r = null;
						try {
							r = new BufferedReader(new FileReader(output));
							importer.importClassifierEvaluation(line
									.getOptionValue("name"), fold, line
									.getOptionValue("algo", "weka"), line
									.getOptionValue("label"), options, line
									.getOptionValue("experiment"), r);
						} finally {
							try {
								r.close();
							} catch (Exception e) {
							}
						}
					}
				}
			}
		}
	}

	private static boolean checkFileRead(String file) {
		return (new File(file)).canRead();
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java org.apache.ctakes.ytex.weka.ImportWekaResults\n", options);
	}

}
