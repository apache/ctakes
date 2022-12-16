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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.model.ClassifierEvaluation;
import org.apache.ctakes.ytex.kernel.model.ClassifierInstanceEvaluation;
import org.hibernate.SessionFactory;


/**
 * parse weka instance output when classifier run with -p option. load results
 * into db.
 */
public class WekaResultsImporterImpl implements WekaResultsImporter {
	// inst# actual predicted error prediction (instance_id,C0000726,C0000731)
	// inst#     actual  predicted error prediction (instance_id)
	// private static final Pattern patHeader = Pattern
	// .compile("\\sinst#\\s+actual\\s+predicted\\s+error\\s+");
	private static final Pattern patHeader = Pattern
			.compile("\\s*inst#.*actual.*predicted.*error");
	// 1 1:0 1:0 0.988 (330478,101,0)
	// private static final Pattern patResult = Pattern
	// .compile("\\s+(\\d+)\\s+(\\d+)\\:\\d+\\s+(\\d+)\\:\\d+\\s+\\+{0,1}\\s+(\\d\\.\\d+)\\s+\\((.*)\\)");
	private static final Pattern patResult = Pattern
			.compile("\\s+(\\d+)\\s+(\\d+)\\:.*\\s+(\\d+)\\:.*\\s+\\+{0,1}\\s+(.*)\\s+\\((.*)\\)");
	private SessionFactory sessionFactory;
	private static final Log log = LogFactory
			.getLog(DocumentResultInstanceImporter.class);

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * this imports the classification results for a document
	 */
	private DocumentResultInstanceImporter docResultInstanceImporter;

	public DocumentResultInstanceImporter getDocResultInstanceImporter() {
		return docResultInstanceImporter;
	}

	public void setDocResultInstanceImporter(
			DocumentResultInstanceImporter docResultInstanceImporter) {
		this.docResultInstanceImporter = docResultInstanceImporter;
	}

	/**
	 * Delegate to importResults
	 * 
	 * @see org.apache.ctakes.ytex.weka.WekaResultsImporter#importDocumentResults(java.lang.String,
	 *      java.io.BufferedReader)
	 */
	public void importDocumentResults(String task, BufferedReader reader)
			throws IOException {
		this.importResults(docResultInstanceImporter, task, reader);
	}

	/**
	 * Parse results, pass them off to WekaResultInstanceImporter
	 * 
	 * @see org.apache.ctakes.ytex.weka.WekaResultsImporter#importResults(org.apache.ctakes.ytex.weka.
	 *      WekaResultInstanceImporter, java.lang.String,
	 *      java.io.BufferedReader)
	 */
	public void importResults(
			WekaResultInstanceImporter resultInstanceImporter, String task,
			BufferedReader reader) throws IOException {
		String line = null;
		boolean keepGoing = true;
		boolean inResults = false;
		while ((line = reader.readLine()) != null && keepGoing) {
			if (!inResults) {
				// not yet in the results section - see if we found the header
				inResults = patHeader.matcher(line).find();
			} else {
				// in results section - see if this line contains some results
				Matcher matcher = patResult.matcher(line);
				if (matcher.find()) {
					// matches - parse it, pass it to the instance importer
					int instanceNum = Integer.parseInt(matcher.group(1));
					int classGold = Integer.parseInt(matcher.group(2));
					int classAuto = Integer.parseInt(matcher.group(3));
					// split something like
					// *0.988 0.012
					// or
					// *0.988,0.012
					// or just
					// 0.988
					String[] arrPredictionStr = matcher.group(4).split(
							"\\*|,|\\s");
					List<Double> listPredictions = new ArrayList<Double>(
							arrPredictionStr.length);
					for (String predStr : arrPredictionStr) {
						if (predStr.length() > 0)
							listPredictions.add(new Double(predStr));
					}
					List<String> instanceKey = Arrays.asList(matcher.group(5)
							.split(","));
					resultInstanceImporter.importInstanceResult(instanceNum,
							instanceKey, task, classAuto, classGold,
							listPredictions);
				} else {
					// hit end of results - stop
					keepGoing = false;
				}
			}
		}
	}

	public void importClassifierEvaluation(String name, Integer fold,
			String algorithm, String label, String options, String experiment,
			BufferedReader reader) throws IOException {
		ClassifierEvaluation ce = new ClassifierEvaluation();
		ce.setName(name);
		ce.setFold(fold);
		ce.setAlgorithm(algorithm);
		ce.setLabel(label);
		ce.setOptions(options);
		ce.setExperiment(experiment);
		this.getSessionFactory().getCurrentSession().save(ce);
		ClassifierEvaluationInstanceImporter instanceImporter = new ClassifierEvaluationInstanceImporter(
				ce, false);
		this.importResults(instanceImporter, label, reader);
	}

	public class ClassifierEvaluationInstanceImporter implements
			WekaResultInstanceImporter {
		private ClassifierEvaluation classifierEvaluation;
		private boolean storeProbabilities;

		public ClassifierEvaluationInstanceImporter(
				ClassifierEvaluation classifierEvaluation,
				boolean storeProbabilities) {
			super();
			this.classifierEvaluation = classifierEvaluation;
			this.storeProbabilities = storeProbabilities;
		}

		@Override
		public void importInstanceResult(Integer instanceNumber,
				List<String> instanceKey, String task, int classAuto,
				int classGold, List<Double> predictions) {
			ClassifierInstanceEvaluation ci = new ClassifierInstanceEvaluation();
			if (instanceKey.size() > 0) {
				try {
					ci.setInstanceId(Integer.parseInt(instanceKey.get(0)));
				} catch (NumberFormatException nfe) {
				}
			} else {
				ci.setInstanceId(instanceNumber);
			}
			ci.setPredictedClassId(classAuto);
			ci.setTargetClassId(classGold);
			if (storeProbabilities && predictions != null
					&& predictions.size() > 0) {
				for (int i = 0; i < predictions.size(); i++) {
					ci.getClassifierInstanceProbabilities().put(i,
							predictions.get(i));
				}
			}
			ci.setClassifierEvaluation(classifierEvaluation);
			sessionFactory.getCurrentSession().save(ci);
		}

	}

}
