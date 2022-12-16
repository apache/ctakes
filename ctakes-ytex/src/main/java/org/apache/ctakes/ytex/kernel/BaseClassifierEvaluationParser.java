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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.dao.ClassifierEvaluationDao;
import org.apache.ctakes.ytex.kernel.model.ClassifierEvaluation;
import org.apache.ctakes.ytex.kernel.model.ClassifierInstanceEvaluation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


/**
 * miscellaneous methods used for parsing various output types
 * 
 * @author vhacongarlav
 * 
 */
public abstract class BaseClassifierEvaluationParser implements
		ClassifierEvaluationParser {
	private static final Log log = LogFactory
			.getLog(BaseClassifierEvaluationParser.class);

	public static Pattern wsPattern = Pattern.compile("\\s|\\z");
	public static Pattern wsDotPattern = Pattern.compile("\\s|\\.|\\z");

	private ClassifierEvaluationDao classifierEvaluationDao;

	public static class InstanceClassInfo {
		long instanceId;
		boolean train;
		String targetClassName;

		public InstanceClassInfo() {
			super();
		}

		public InstanceClassInfo(long instanceId, boolean train,
				String targetClassName) {
			super();
			this.instanceId = instanceId;
			this.train = train;
			this.targetClassName = targetClassName;
		}

		public long getInstanceId() {
			return instanceId;
		}

		public void setInstanceId(long instanceId) {
			this.instanceId = instanceId;
		}

		public boolean isTrain() {
			return train;
		}

		public void setTrain(boolean train) {
			this.train = train;
		}

		public String getTargetClassName() {
			return targetClassName;
		}

		public void setTargetClassName(String targetClassName) {
			this.targetClassName = targetClassName;
		}
	}

	public ClassifierEvaluationDao getClassifierEvaluationDao() {
		return classifierEvaluationDao;
	}

	public void setClassifierEvaluationDao(
			ClassifierEvaluationDao classifierEvaluationDao) {
		this.classifierEvaluationDao = classifierEvaluationDao;
	}

	public static String extractFirstToken(String line, Pattern tokDelimPattern) {
		Matcher wsMatcher = tokDelimPattern.matcher(line);
		String token = null;
		if (wsMatcher.find() && wsMatcher.start() > 0) {
			token = line.substring(0, wsMatcher.start());
		}
		return token;
	}

	public List<Long> parseInstanceIds(String instanceIdFile)
			throws IOException {
		BufferedReader instanceIdReader = null;
		List<Long> instanceIds = new ArrayList<Long>();
		try {
			instanceIdReader = new BufferedReader(
					new FileReader(instanceIdFile));
			String instanceId = null;
			while ((instanceId = instanceIdReader.readLine()) != null)
				instanceIds.add(Long.parseLong(instanceId));
			return instanceIds;
		} catch (FileNotFoundException e) {
			log.warn(instanceIdFile
					+ " not available, instance_ids will not be stored");
			return null;
		} finally {
			if (instanceIdReader != null)
				instanceIdReader.close();
		}
	}

	/**
	 * parse a number out of the libsvm command line that matches the specified
	 * pattern.
	 * 
	 * @param pCost
	 * @param options
	 * @return null if option not present
	 */
	protected Double parseDoubleOption(Pattern pCost, String options) {
		Matcher m = pCost.matcher(options);
		if (m.find()) {
			String toParse = m.group(1);
			try {
				return Double.parseDouble(toParse);
			} catch (NumberFormatException nfe) {
				log.warn("could not parse: " + toParse, nfe);
			}
		}
		return null;
	}

	/**
	 * 
	 * parse a number out of the libsvm command line that matches the specified
	 * pattern.
	 * 
	 * @param pKernel
	 * @param options
	 * @return null if option not present
	 */
	protected Integer parseIntOption(Pattern pKernel, String options) {
		Matcher m = pKernel.matcher(options);
		if (m.find())
			return Integer.parseInt(m.group(1));
		else
			return null;
	}

	protected void initClassifierEvaluation(String instanceIdFile,
			ClassifierEvaluation eval) {
		eval.setFold(FileUtil.parseFoldFromFileName(instanceIdFile));
		eval.setRun(FileUtil.parseRunFromFileName(instanceIdFile));
		eval.setLabel(FileUtil.parseLabelFromFileName(instanceIdFile));
	}

	protected void initClassifierEvaluationFromProperties(Properties props,
			ClassifierEvaluation eval) {
		eval.setName(props.getProperty("kernel.name"));
		eval.setExperiment(props.getProperty("kernel.experiment"));
		String strParam1 = props.getProperty("kernel.param1");
		if (strParam1 != null && strParam1.length() > 0)
			eval.setParam1(Double.parseDouble(strParam1));
		eval.setParam2(props.getProperty("kernel.param2"));
		eval.setOptions(props.getProperty(ParseOption.EVAL_LINE.getOptionKey()));
	}

	/**
	 * load properties from <tt>outputDir/options.properties</tt>. returns empty
	 * properties if the file does not exist
	 * 
	 * @param outputDir
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Properties loadProps(File outputDir) throws FileNotFoundException,
			IOException {
		return FileUtil.loadProperties(outputDir.getPath() + File.separator
				+ "options.properties", true);
	}

	protected boolean checkFileRead(String file) {
		return (new File(file)).canRead();
	}

	protected String getFileBaseName(Properties kernelProps) {
		return kernelProps.getProperty(
				ParseOption.DATA_BASENAME.getOptionKey(),
				ParseOption.DATA_BASENAME.getDefaultValue());
	}

	protected void storeSemiSupervised(Properties kernelProps,
			ClassifierEvaluation ce, BiMap<Integer, String> classIdToNameMap) {
		boolean storeInstanceEval = YES.equalsIgnoreCase(kernelProps
				.getProperty(ParseOption.STORE_INSTANCE_EVAL.getOptionKey(),
						ParseOption.STORE_INSTANCE_EVAL.getDefaultValue()));
		boolean storeUnlabeled = YES.equalsIgnoreCase(kernelProps.getProperty(
				ParseOption.STORE_UNLABELED.getOptionKey(),
				ParseOption.STORE_UNLABELED.getDefaultValue()));
		boolean storeIR = YES.equalsIgnoreCase(kernelProps.getProperty(
				ParseOption.STORE_IRSTATS.getOptionKey(),
				ParseOption.STORE_IRSTATS.getDefaultValue()));
		// save the classifier evaluation
		this.getClassifierEvaluationDao().saveClassifierEvaluation(ce, classIdToNameMap,
				storeInstanceEval || storeUnlabeled, storeIR, 0);
	}

	/**
	 * used by semil & svmlin to store semisupervised predictions. these train
	 * ml and make test predictions in a single step.
	 * 
	 * @param ce
	 *            updated
	 * @param listClassInfo
	 *            the class info 0 - instance id, 1 - train/test, 2 - target
	 *            class id
	 * @param storeUnlabeled
	 *            should the unlabeled predictions be stored?
	 * @param classIds
	 *            predicted class ids
	 */
	protected void updateSemiSupervisedPredictions(ClassifierEvaluation ce,
			List<List<Long>> listClassInfo, boolean storeUnlabeled,
			int[] classIds) {
		for (int i = 0; i < classIds.length; i++) {
			List<Long> classInfo = listClassInfo.get(i);
			long instanceId = classInfo.get(0);
			boolean train = classInfo.get(1) == 1;
			int targetClassId = classInfo.get(2).intValue();
			// if we are storing unlabeled instance ids, save this instance
			// evaluation
			// else only store it if this is a test instance id - save it
			if (storeUnlabeled || !train) {
				ClassifierInstanceEvaluation cie = new ClassifierInstanceEvaluation();
				cie.setClassifierEvaluation(ce);
				cie.setInstanceId(instanceId);
				cie.setPredictedClassId(classIds[i]);
				if (targetClassId != 0)
					cie.setTargetClassId(targetClassId);
				// add the instance eval to the parent
				ce.getClassifierInstanceEvaluations().put(instanceId, cie);
			}
		}
	}

	protected void updateSemiSupervisedPredictions(ClassifierEvaluation ce,
			List<InstanceClassInfo> listClassInfo, boolean storeUnlabeled,
			String[] predictedClassNames, Map<String, Integer> classNameToIdMap) {
		for (int i = 0; i < predictedClassNames.length; i++) {
			InstanceClassInfo classInfo = listClassInfo.get(i);
			boolean train = classInfo.isTrain();
			// if we are storing unlabeled instance ids, save this instance
			// evaluation
			// else only store it if this is a test instance id - save it
			if (storeUnlabeled || !train) {
				ClassifierInstanceEvaluation cie = new ClassifierInstanceEvaluation();
				cie.setClassifierEvaluation(ce);
				cie.setInstanceId(classInfo.getInstanceId());
				cie.setPredictedClassId(classNameToIdMap.get(predictedClassNames[i]));
				int targetClassId = classNameToIdMap.get(classInfo.getTargetClassName());
				if (targetClassId != 0)
					cie.setTargetClassId(targetClassId);
				// add the instance eval to the parent
				ce.getClassifierInstanceEvaluations().put(cie.getInstanceId(), cie);
			}
		}
	}
	
	protected BiMap<Integer, String> loadClassIdMap(File dataDir, String label)
			throws IOException {
		BiMap<Integer, String> classIndexMap = HashBiMap.create();
		String filename = FileUtil.getScopedFileName(dataDir.getPath(), label,
				null, null, "class.properties");
		File f = new File(filename);
		if (f.exists()) {
			BufferedReader r = null;
			try {
				r = new BufferedReader(new FileReader(f));
				Properties props = new Properties();
				props.load(r);
				for (String key : props.stringPropertyNames()) {
					classIndexMap.put(Integer.parseInt(key),
							props.getProperty(key));
				}
			} finally {
				try {
					r.close();
				} catch (IOException e) {
				}
			}
		}
		return classIndexMap;
	}

	protected List<InstanceClassInfo> loadInstanceClassInfo(File dataDir,
			String classFileName) throws IOException {
		List<InstanceClassInfo> listClassInfo = null;
		// load instance ids and their class ids
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(classFileName));
			listClassInfo = new ArrayList<InstanceClassInfo>();
			String line = null;
			while ((line = r.readLine()) != null) {
				if (line.trim().length() > 0) {
					String classInfoToks[] = line.split("\\s");
					if (classInfoToks.length != 3) {
						log.error("error parsing line: " + line);
						return null;
					}
					listClassInfo
							.add(new InstanceClassInfo(Long
									.parseLong(classInfoToks[0]), Integer
									.parseInt(classInfoToks[1]) != 0,
									classInfoToks[2]));
				}
			}
		} catch (FileNotFoundException fe) {
			log.warn("class.txt file not available: " + classFileName, fe);
			listClassInfo = null;
		} finally {
			if (r != null) {
				r.close();
			}
		}
		return listClassInfo;
	}

	protected List<List<Long>> loadClassInfo(File dataDir, String classFileName)
			throws IOException {
		List<List<Long>> listClassInfo = null;
		// load instance ids and their class ids
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(classFileName));
			listClassInfo = new ArrayList<List<Long>>();
			String line = null;
			while ((line = r.readLine()) != null) {
				if (line.trim().length() > 0) {
					String classInfoToks[] = line.split("\\s");
					List<Long> classInfo = new ArrayList<Long>(3);
					for (String tok : classInfoToks) {
						classInfo.add(Long.parseLong(tok));
					}
					if (classInfo.size() != 3) {
						log.error("error parsing line: " + line);
						return null;
					}
					listClassInfo.add(classInfo);
				}
			}
		} catch (FileNotFoundException fe) {
			log.warn("class.txt file not available: " + classFileName, fe);
			listClassInfo = null;
		} finally {
			if (r != null) {
				r.close();
			}
		}
		return listClassInfo;
	}
}
