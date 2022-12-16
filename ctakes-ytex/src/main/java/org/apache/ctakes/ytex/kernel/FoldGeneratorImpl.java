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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.dao.ClassifierEvaluationDao;
import org.apache.ctakes.ytex.kernel.model.CrossValidationFold;
import org.apache.ctakes.ytex.kernel.model.CrossValidationFoldInstance;


/**
 * utility generates cv fold splits, stores in db. Takes as a command line
 * parameter -prop [property file]. Also reads properties from org.apache.ctakes.ytex.properties.
 * Required properties:
 * <ul>
 * <li>org.apache.ctakes.ytex.corpusName
 * <li>instanceClassQuery
 * </ul>
 * 
 * Optional properties:
 * <ul>
 * <li>minPerClass default 1 minimum number of instances per class/fold. if not
 * enough instances of a specific class, the instance will be repeated across
 * folds. E.g. if you have only one example of one class, and 2 folds, that one
 * example will be duplicated in both folds.
 * <li>rand random number seed, defaults to current time millis
 * <li>org.apache.ctakes.ytex.splitName default null cv_fold.split_name
 * <li>folds default 2
 * <li>runs default 5
 * </ul>
 * 
 * @author vijay
 */
public class FoldGeneratorImpl implements FoldGenerator {
	private static final Log log = LogFactory.getLog(FoldGeneratorImpl.class);

	/**
	 * iterate through the labels, split instances into folds
	 * 
	 * @param mapClassToInstanceId
	 * @param nFolds
	 * @param nMinPerClass
	 * @param nSeed
	 * @return list with nFolds sets of instance ids corresponding to the folds
	 */
	private static List<Set<Long>> createFolds(
			Map<String, List<Long>> mapClassToInstanceId, int nFolds,
			int nMinPerClass, Random r) {
		List<Set<Long>> folds = new ArrayList<Set<Long>>(nFolds);
		Map<String, List<Set<Long>>> mapLabelFolds = new HashMap<String, List<Set<Long>>>();
		for (Map.Entry<String, List<Long>> classToInstanceId : mapClassToInstanceId
				.entrySet()) {
			List<Long> instanceIds = classToInstanceId.getValue();
			Collections.shuffle(instanceIds, r);
			List<Set<Long>> classFolds = new ArrayList<Set<Long>>(nFolds);
			int blockSize = instanceIds.size() / nFolds;
			for (int i = 0; i < nFolds; i++) {
				Set<Long> foldInstanceIds = new HashSet<Long>(blockSize);
				if (instanceIds.size() <= nMinPerClass) {
					// we don't have minPerClass for the given class
					// just add all of them to each fold
					foldInstanceIds.addAll(instanceIds);
				} else if (blockSize < nMinPerClass) {
					// too few of the given class - just randomly select
					// nMinPerClass
					double fraction = (double) nMinPerClass
							/ (double) instanceIds.size();
					// iterate through the list, start somewhere in the middle
					int instanceIdIndex = (int) (r.nextDouble() * instanceIds
							.size());
					while (foldInstanceIds.size() < nMinPerClass) {
						// go back to beginning of list if we hit the end
						if (instanceIdIndex >= instanceIds.size()) {
							instanceIdIndex = 0;
						}
						// randomly select this line
						if (r.nextDouble() <= fraction) {
							long instanceId = instanceIds.get(instanceIdIndex);
							foldInstanceIds.add(instanceId);
						}
						// go to next line
						instanceIdIndex++;
					}
				} else {
					int nStart = i * blockSize;
					int nEnd = (i == nFolds - 1) ? instanceIds.size() : nStart
							+ blockSize;
					for (int instanceIdIndex = nStart; instanceIdIndex < nEnd; instanceIdIndex++) {
						foldInstanceIds.add(instanceIds.get(instanceIdIndex));
					}
				}
				classFolds.add(foldInstanceIds);
			}
			mapLabelFolds.put(classToInstanceId.getKey(), classFolds);
		}
		for (int i = 0; i < nFolds; i++) {
			Set<Long> foldInstanceIds = new HashSet<Long>();
			for (List<Set<Long>> labelFold : mapLabelFolds.values()) {
				foldInstanceIds.addAll(labelFold.get(i));
			}
			folds.add(foldInstanceIds);
		}
		return folds;
	}

	@SuppressWarnings("static-access")
	public static void main(String args[]) throws ParseException, IOException {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("prop")
				.hasArg()
				.withDescription(
						"property file with query to retrieve instance id - label - class triples")
				.create("prop"));
		// OptionGroup group = new OptionGroup();
		// group
		// .addOption(OptionBuilder
		// .withArgName("query")
		// .hasArg()
		// .withDescription(
		// "query to retrieve instance id - label - class triples")
		// .create("query"));
		// group
		// .addOption(OptionBuilder
		// .withArgName("prop")
		// .hasArg()
		// .withDescription(
		// "property file with query to retrieve instance id - label - class triples")
		// .create("prop"));
		// group.isRequired();
		// options.addOptionGroup(group);
		// options.addOption(OptionBuilder.withArgName("name").hasArg()
		// .isRequired().withDescription("name. required").create("name"));
		// options.addOption(OptionBuilder.withArgName("runs").hasArg()
		// .withDescription("number of runs, default 1").create("runs"));
		// options.addOption(OptionBuilder.withArgName("folds").hasArg()
		// .withDescription("number of folds, default 4").create("folds"));
		// options.addOption(OptionBuilder.withArgName("minPerClass").hasArg()
		// .withDescription("minimum instances per class, default 1")
		// .create("minPerClass"));
		// options.addOption(OptionBuilder.withArgName("rand").hasArg()
		// .withDescription(
		// "random number seed; default current time in millis")
		// .create("rand"));
		try {
			if (args.length == 0)
				printHelp(options);
			else {
				CommandLineParser parser = new GnuParser();
				CommandLine line = parser.parse(options, args);
				String propFile = line.getOptionValue("prop");
				Properties props = FileUtil.loadProperties(propFile, true);
				// Integer rand = line.hasOption("rand") ? Integer.parseInt(line
				// .getOptionValue("rand")) : null;
				// int runs = Integer.parseInt(line.getOptionValue("runs",
				// "1"));
				// int minPerClass = Integer.parseInt(line.getOptionValue(
				// "minPerClass", "1"));
				// int folds = Integer.parseInt(line.getOptionValue("folds",
				// "4"));
				String corpusName = props.getProperty("org.apache.ctakes.ytex.corpusName");
				String splitName = props.getProperty("org.apache.ctakes.ytex.splitName");
				String query = props.getProperty("instanceClassQuery");
				int folds = Integer.parseInt(props.getProperty("folds", "2"));
				int runs = Integer.parseInt(props.getProperty("runs", "5"));
				int minPerClass = Integer.parseInt(props.getProperty(
						"minPerClass", "1"));
				Integer rand = props.containsKey("rand") ? Integer
						.parseInt(props.getProperty("rand")) : null;
				boolean argsOk = true;
				if (corpusName == null) {
					log.error("missing parameter: org.apache.ctakes.ytex.corpusName");
					argsOk = false;
				}
				if (query == null) {
					log.error("missing parameter: instanceClassQuery");
					argsOk = false;
				}
				if (!argsOk) {
					printHelp(options);
					System.exit(1);
				} else {
					KernelContextHolder
							.getApplicationContext()
							.getBean(FoldGenerator.class)
							.generateRuns(corpusName, splitName, query, folds,
									minPerClass, rand, runs);
				}
			}
		} catch (ParseException pe) {
			printHelp(options);
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"java org.apache.ctakes.ytex.kernel.FoldGeneratorImpl splits training data into mxn training/test sets for mxn-fold cross validation",
						options);
	}

	ClassifierEvaluationDao classifierEvaluationDao;

	KernelUtil kernelUtil;

	/**
	 * generate folds for a run
	 * 
	 * @param labels
	 * @param mapInstanceToClassLabel
	 * @param name
	 * @param splitName
	 * @param run
	 * @param query
	 * @param nFolds
	 * @param nMinPerClass
	 * @param r
	 */
	public void generateFolds(Set<String> labels, InstanceData instances,
			String corpusName, String splitName, int run, String query,
			int nFolds, int nMinPerClass, Random r) {
		for (String label : instances.getLabelToInstanceMap().keySet()) {
			// there should not be any runs/folds/train test split - just unpeel
			// until we get to the instance - class map
			SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>> runMap = instances
					.getLabelToInstanceMap().get(label);
			SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>> foldMap = runMap
					.values().iterator().next();
			SortedMap<Boolean, SortedMap<Long, String>> trainMap = foldMap
					.values().iterator().next();
			SortedMap<Long, String> mapInstanceIdToClass = trainMap.values()
					.iterator().next();
			List<Set<Long>> folds = createFolds(nFolds, nMinPerClass, r,
					mapInstanceIdToClass);
			// insert the folds
			insertFolds(folds, corpusName, splitName, label, run);
		}
	}

	/**
	 * inver the map of instance id to class, call createFolds
	 * 
	 * @param nFolds
	 * @param nMinPerClass
	 * @param r
	 * @param mapInstanceIdToClass
	 * @return
	 */
	private List<Set<Long>> createFolds(int nFolds, int nMinPerClass, Random r,
			SortedMap<Long, String> mapInstanceIdToClass) {
		// invert the mapInstanceIdToClass
		Map<String, List<Long>> mapClassToInstanceId = new TreeMap<String, List<Long>>();
		for (Map.Entry<Long, String> instance : mapInstanceIdToClass.entrySet()) {
			String className = instance.getValue();
			long instanceId = instance.getKey();
			List<Long> classInstanceIds = mapClassToInstanceId.get(className);
			if (classInstanceIds == null) {
				classInstanceIds = new ArrayList<Long>();
				mapClassToInstanceId.put(className, classInstanceIds);
			}
			classInstanceIds.add(instanceId);
		}
		// stratified split into folds
		List<Set<Long>> folds = createFolds(mapClassToInstanceId, nFolds,
				nMinPerClass, r);
		return folds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.FoldGenerator#generateRuns(java.lang.String,
	 * java.lang.String, int, int, java.lang.Integer, int)
	 */
	@Override
	public void generateRuns(String corpusName, String splitName, String query,
			int nFolds, int nMinPerClass, Integer nSeed, int nRuns) {
		Random r = new Random(nSeed != null ? nSeed
				: System.currentTimeMillis());
		SortedSet<String> labels = new TreeSet<String>();
		InstanceData instances = kernelUtil.loadInstances(query);
		this.getClassifierEvaluationDao().deleteCrossValidationFoldByName(
				corpusName, splitName);
		for (int run = 1; run <= nRuns; run++) {
			generateFolds(labels, instances, corpusName, splitName, run, query,
					nFolds, nMinPerClass, r);
		}
	}

	public ClassifierEvaluationDao getClassifierEvaluationDao() {
		return classifierEvaluationDao;
	}

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	/**
	 * insert the folds into the database
	 * 
	 * @param folds
	 * @param corpusName
	 * @param run
	 */
	private void insertFolds(List<Set<Long>> folds, String corpusName,
			String splitName, String label, int run) {
		// iterate over fold numbers
		for (int foldNum = 1; foldNum <= folds.size(); foldNum++) {
			Set<CrossValidationFoldInstance> instanceIds = new HashSet<CrossValidationFoldInstance>();
			// iterate over instances in each fold
			for (int trainFoldNum = 1; trainFoldNum <= folds.size(); trainFoldNum++) {
				// add the instance, set the train flag
				for (long instanceId : folds.get(trainFoldNum - 1))
					instanceIds.add(new CrossValidationFoldInstance(instanceId,
							trainFoldNum != foldNum));
			}
			classifierEvaluationDao.saveFold(new CrossValidationFold(
					corpusName, splitName, label, run, foldNum, instanceIds));
			// insert test set
			// classifierEvaluationDao.saveFold(new CrossValidationFold(name,
			// label, run, foldNum, false, folds.get(foldNum - 1)));
			// insert training set
			// Set<Integer> trainInstances = new TreeSet<Integer>();
			// for (int trainFoldNum = 1; trainFoldNum <= folds.size();
			// trainFoldNum++) {
			// if (trainFoldNum != foldNum)
			// trainInstances.addAll(folds.get(trainFoldNum - 1));
			// }
			// classifierEvaluationDao.saveFold(new CrossValidationFold(name,
			// label, run, foldNum, true, trainInstances));
		}
	}

	public void setClassifierEvaluationDao(
			ClassifierEvaluationDao classifierEvaluationDao) {
		this.classifierEvaluationDao = classifierEvaluationDao;
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	@Override
	public SortedMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> generateRuns(
			SortedMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> labelToInstanceMap,
			int nFolds, int nMinPerClass, Integer nSeed, int nRuns) {
		// allocate map to return
		SortedMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> labelToInstanceFoldMap = new TreeMap<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>>();
		// initialize random seed
		Random r = new Random(nSeed != null ? nSeed
				: System.currentTimeMillis());
		// iterate over labels
		for (Map.Entry<String, SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>> labelRun : labelToInstanceMap
				.entrySet()) {
			String label = labelRun.getKey();
			// extract the instance id - class map
			SortedMap<Long, String> instanceClassMap = labelRun.getValue()
					.get(0).get(0).get(true);
			// allocate the run to fold map
			SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>> runMap = new TreeMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>();
			labelToInstanceFoldMap.put(label, runMap);
			// iterate over runs
			for (int run = 1; run <= nRuns; run++) {
				// generate folds for run
				List<Set<Long>> folds = createFolds(nFolds, nMinPerClass, r,
						instanceClassMap);
				SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>> foldMap = new TreeMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>();
				// add the fold map to the run map
				runMap.put(run, foldMap);
				// iterate over folds
				for (int trainFoldNum = 1; trainFoldNum <= folds.size(); trainFoldNum++) {
					// add train/test sets for the fold
					SortedMap<Boolean, SortedMap<Long, String>> trainTestMap = new TreeMap<Boolean, SortedMap<Long, String>>();
					foldMap.put(trainFoldNum, trainTestMap);
					trainTestMap.put(true, new TreeMap<Long, String>());
					trainTestMap.put(false, new TreeMap<Long, String>());
					// populate the train/test sets
					Set<Long> testIds = folds.get(trainFoldNum - 1);
					// iterate over all instances
					for (Map.Entry<Long, String> instanceClass : instanceClassMap
							.entrySet()) {
						long instanceId = instanceClass.getKey();
						String clazz = instanceClass.getValue();
						// add the instance to the test set if it is in testIds,
						// else to the train set
						trainTestMap.get(!testIds.contains(instanceId)).put(
								instanceId, clazz);
					}
				}
			}
		}
		return labelToInstanceFoldMap;
	}
}
