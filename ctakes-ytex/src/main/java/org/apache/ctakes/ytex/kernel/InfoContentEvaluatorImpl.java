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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.ctakes.ytex.kernel.dao.ClassifierEvaluationDao;
import org.apache.ctakes.ytex.kernel.dao.ConceptDao;
import org.apache.ctakes.ytex.kernel.model.ConcRel;
import org.apache.ctakes.ytex.kernel.model.ConceptGraph;
import org.apache.ctakes.ytex.kernel.model.FeatureEvaluation;
import org.apache.ctakes.ytex.kernel.model.FeatureRank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;


/**
 * Calculate the information content of each concept in a corpus wrt the
 * specified concept graph. Required properties:
 * <ul>
 * <li>org.apache.ctakes.ytex.conceptGraphName - required - name of conceptGraph. @see ConceptDao
 * <li>org.apache.ctakes.ytex.corpusName - required - name of corpus
 * <li>org.apache.ctakes.ytex.conceptSetName - optional - you may want to experiment with
 * different sets of concepts from a corpus, e.g. concepts from certain
 * sections, or different ways of counting concepts.
 * <li>org.apache.ctakes.ytex.freqQuery - query to obtain raw concept frequencies for the corpus
 * </ul>
 * to execute, either specify these options via system properties (-D options)
 * on the command line, or supply this class with the path to a properties file
 * used for evaluation, or both (-D overrides properties file).
 * <p>
 * The information content of each concept is stored in the feature_rank table.
 * The related record in the feature_eval table has
 * <ul>
 * <li>type = infocontent
 * <li>feature_set_name = conceptSetName
 * <li>param1 = conceptGraphName
 * </ul>
 * 
 * @author vijay
 * 
 */
public class InfoContentEvaluatorImpl implements InfoContentEvaluator {
	/**
	 * @param args
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("property file")
				.hasArg()
				.isRequired()
				.withDescription(
						"property file with queries and other parameters. todo desc")
				.create("prop"));
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			Properties props = (Properties) KernelContextHolder
					.getApplicationContext().getBean("ytexProperties");
			Properties propsArgs = FileUtil.loadProperties(
					line.getOptionValue("prop"), true);
			props.putAll(propsArgs);
			if (!props.containsKey("org.apache.ctakes.ytex.conceptGraphName")
					|| !props.containsKey("org.apache.ctakes.ytex.corpusName")
					|| !props.containsKey("org.apache.ctakes.ytex.freqQuery")) {
				System.err.println("error: required parameter not specified");
				System.exit(1);
			} else {
				InfoContentEvaluator corpusEvaluator = KernelContextHolder
						.getApplicationContext().getBean(
								InfoContentEvaluator.class);
				corpusEvaluator.evaluateCorpusInfoContent(
						props.getProperty("org.apache.ctakes.ytex.freqQuery"),
						props.getProperty("org.apache.ctakes.ytex.corpusName"),
						props.getProperty("org.apache.ctakes.ytex.conceptGraphName"),
						props.getProperty("org.apache.ctakes.ytex.conceptSetName"));
				System.exit(0);
			}
		} catch (ParseException pe) {
			printHelp(options);
			System.exit(1);
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + InfoContentEvaluatorImpl.class.getName()
				+ " calculate information content of corpus wrt concept graph",
				options);
	}

	private ClassifierEvaluationDao classifierEvaluationDao;
	private ConceptDao conceptDao;

	// private CorpusDao corpusDao;
	private JdbcTemplate jdbcTemplate;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.kernel.CorpusEvaluator#evaluateCorpusInfoContent(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void evaluateCorpusInfoContent(final String freqQuery,
			final String corpusName, final String conceptGraphName,
			final String conceptSetName) {
		ConceptGraph cg = conceptDao.getConceptGraph(conceptGraphName);
		classifierEvaluationDao.deleteFeatureEvaluation(corpusName,
				conceptSetName, null, INFOCONTENT, 0, 0d, conceptGraphName);
		FeatureEvaluation eval = new FeatureEvaluation();
		eval.setCorpusName(corpusName);
		if (conceptSetName != null)
			eval.setFeatureSetName(conceptSetName);
		eval.setEvaluationType(INFOCONTENT);
		eval.setParam2(conceptGraphName);
		// CorpusEvaluation eval = corpusDao.getCorpus(corpusName,
		// conceptGraphName, conceptSetName);
		// if (eval == null) {
		// eval = new CorpusEvaluation();
		// eval.setConceptGraphName(conceptGraphName);
		// eval.setConceptSetName(conceptSetName);
		// eval.setCorpusName(corpusName);
		// this.corpusDao.addCorpus(eval);
		// }
		Map<String, Double> rawFreq = getFrequencies(freqQuery);
		double totalFreq = 0d;
		// map of cui to cumulative frequency
		Map<String, Double> conceptFreq = new HashMap<String, Double>(cg
				.getConceptMap().size());
		// recurse through the tree
		totalFreq = getFrequency(cg.getConceptMap().get(cg.getRoot()),
				conceptFreq, rawFreq);
		List<FeatureRank> featureRankList = new ArrayList<FeatureRank>(
				conceptFreq.size());
		// update information content
		double log2inv = -1d / Math.log(2);
		for (Map.Entry<String, Double> cfreq : conceptFreq.entrySet()) {
			if (cfreq.getValue() > 0) {
				FeatureRank featureRank = new FeatureRank(eval, cfreq.getKey(),
						log2inv * Math.log(cfreq.getValue() / totalFreq));
				featureRankList.add(featureRank);
			}
		}
		// the rank is irrelevant, but rank the features anyways
		featureRankList = FeatureRank.sortFeatureRankList(featureRankList,
				new FeatureRank.FeatureRankDesc());
		classifierEvaluationDao.saveFeatureEvaluation(eval, featureRankList);
	}

	public ClassifierEvaluationDao getClassifierEvaluationDao() {
		return classifierEvaluationDao;
	}

	public ConceptDao getConceptDao() {
		return conceptDao;
	}

	public DataSource getDataSource(DataSource ds) {
		return this.jdbcTemplate.getDataSource();
	}

	// public CorpusDao getCorpusDao() {
	// return corpusDao;
	// }
	//
	// public void setCorpusDao(CorpusDao corpusDao) {
	// this.corpusDao = corpusDao;
	// }

	/**
	 * get the frequency of each term in the corpus.
	 * 
	 * @param freqQuery
	 *            query returns 2 columns. 1st column - concept id (string), 2nd
	 *            column - frequency (double)
	 * @return
	 */
	@Override
	public Map<String, Double> getFrequencies(String freqQuery) {
		// get the raw frequency
		final Map<String, Double> rawFreq = new HashMap<String, Double>();
		jdbcTemplate.query(freqQuery, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				rawFreq.put(rs.getString(1), rs.getDouble(2));
			}
		});
		return rawFreq;
	}

	/**
	 * recursively sum frequency of parent and all its childrens' frequencies
	 * 
	 * @param parent
	 *            parent node
	 * @param conceptFreq
	 *            results stored here
	 * @param conceptIdToTermMap
	 *            raw frequencies here
	 * @return double sum of concept frequency in the subtree with parent as
	 *         root
	 */
	double getFrequency(ConcRel parent, Map<String, Double> conceptFreq,
			Map<String, Double> rawFreq) {
		double dFreq = 0d;
		if (conceptFreq.containsKey(parent.getConceptID())) {
			dFreq = conceptFreq.get(parent.getConceptID());
		} else {
			// get raw freq
			dFreq = rawFreq.containsKey(parent.getConceptID()) ? rawFreq
					.get(parent.getConceptID()) : 0d;
			// recurse
			for (ConcRel child : parent.getChildren()) {
				dFreq += getFrequency(child, conceptFreq, rawFreq);
			}
			conceptFreq.put(parent.getConceptID(), dFreq);
		}
		return dFreq;
	}

	public void setClassifierEvaluationDao(
			ClassifierEvaluationDao classifierEvaluationDao) {
		this.classifierEvaluationDao = classifierEvaluationDao;
	}

	public void setConceptDao(ConceptDao conceptDao) {
		this.conceptDao = conceptDao;
	}

	public void setDataSource(DataSource ds) {
		this.jdbcTemplate = new JdbcTemplate(ds);
	}
}
