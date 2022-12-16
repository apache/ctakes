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
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class SparseDataExporterImpl implements SparseDataExporter {

	private static final Log log = LogFactory
			.getLog(SparseDataExporterImpl.class);

	@SuppressWarnings("static-access")
	public static void main(String args[]) throws IOException {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("prop")
				.hasArg()
				.isRequired()
				.withDescription(
						"property file with queries and other parameters.")
				.create("prop"));
		options.addOption(OptionBuilder.withArgName("type").hasArg()
				.isRequired()
				.withDescription("export format; valid values: weka, libsvm")
				.create("type"));
		if (args.length == 0)
			printHelp(options);
		else {
			try {
				CommandLineParser parser = new GnuParser();
				CommandLine line = parser.parse(options, args);
				String propFile = line.getOptionValue("prop");
				String format = line.getOptionValue("type");
				SparseDataExporter exporter = KernelContextHolder
						.getApplicationContext().getBean(
								SparseDataExporter.class);
				exporter.exportData(propFile, format);
			} catch (ParseException pe) {
				printHelp(options);
			}
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();

		formatter.printHelp("java " + SparseDataExporterImpl.class.getName()
				+ " export sparse data", options);
	}

	protected JdbcTemplate jdbcTemplate;
	protected KernelUtil kernelUtil;

	protected NamedParameterJdbcTemplate namedJdbcTemplate;

	protected Map<String, SparseDataFormatterFactory> nameToFormatterMap = new HashMap<String, SparseDataFormatterFactory>();	

	protected TransactionTemplate txTemplateNew;

	public SparseDataExporterImpl() {
		super();
	}

	protected void addNominalWordToInstance(SparseData sparseData,
			long instanceId, String word, String wordValue) {
		// add the instance id to the set of instance ids if necessary
		if (!sparseData.getInstanceIds().contains(instanceId))
			sparseData.getInstanceIds().add(instanceId);
		SortedMap<String, String> instanceWords = sparseData
				.getInstanceNominalWords().get(instanceId);
		SortedSet<String> wordValueSet = sparseData.getNominalWordValueMap()
				.get(word);
		if (instanceWords == null) {
			instanceWords = new TreeMap<String, String>();
			sparseData.getInstanceNominalWords().put(instanceId, instanceWords);
		}
		if (wordValueSet == null) {
			wordValueSet = new TreeSet<String>();
			sparseData.getNominalWordValueMap().put(word, wordValueSet);
		}
		// add the word-value for the instance
		instanceWords.put(word, wordValue);
		// add the value to the set of valid values
		wordValueSet.add(wordValue);
	}

	protected void addNumericWordToInstance(SparseData sparseData,
			long instanceId, String word, double wordValue) {
		// add the instance id to the set of instance ids if necessary
		if (!sparseData.getInstanceIds().contains(instanceId))
			sparseData.getInstanceIds().add(instanceId);
		// add the numeric word to the map of words for this document
		SortedMap<String, Double> words = sparseData.getInstanceNumericWords()
				.get(instanceId);
		if (words == null) {
			words = new TreeMap<String, Double>();
			sparseData.getInstanceNumericWords().put(instanceId, words);
		}
		words.put(word, wordValue);
		sparseData.getNumericWords().add(word);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.SparseDataExporter#exportData(org.apache.ctakes.ytex.kernel.SparseData,
	 * org.apache.ctakes.ytex.kernel.SparseDataFormatter, java.util.Properties)
	 */
	public void exportData(InstanceData instanceLabel,
			SparseDataFormatter formatter, Properties properties,
			BagOfWordsDecorator bDecorator) throws IOException {
		String scope = properties.getProperty("scope", null);
		SparseData sparseData = null;
		if (scope == null) {
			sparseData = this.loadData(instanceLabel,
					properties.getProperty("numericWordQuery"),
					properties.getProperty("nominalWordQuery"),
					properties.getProperty("prepareScript"),
					properties.getProperty("prepareScriptDelimiter", ";"),
					bDecorator, null, null, null);
		}
		formatter.initializeExport(instanceLabel, properties, sparseData);
		for (String label : instanceLabel.getLabelToInstanceMap().keySet()) {
			if ("label".equals(scope)) {
				sparseData = this.loadData(instanceLabel,
						properties.getProperty("numericWordQuery"),
						properties.getProperty("nominalWordQuery"),
						properties.getProperty("prepareScript"),
						properties.getProperty("prepareScriptDelimiter", ";"),
						bDecorator, label, null, null);
			}
			formatter
					.initializeLabel(label, instanceLabel
							.getLabelToInstanceMap().get(label), properties,
							sparseData);
			for (int run : instanceLabel.getLabelToInstanceMap().get(label)
					.keySet()) {
				for (int fold : instanceLabel.getLabelToInstanceMap()
						.get(label).get(run).keySet()) {
					if (log.isInfoEnabled()
							&& (label.length() > 0 || run > 0 || fold > 0))
						log.info("exporting, label " + label + " run " + run
								+ " fold " + fold);
					if ("fold".equals(scope)) {
						sparseData = this.loadData(instanceLabel, properties
								.getProperty("numericWordQuery"), properties
								.getProperty("nominalWordQuery"), properties
								.getProperty("prepareScript"), properties
								.getProperty("prepareScriptDelimiter", ";"),
								bDecorator, label, fold, run);
					}
					formatter.initializeFold(sparseData, label, run, fold,
							instanceLabel.getLabelToInstanceMap().get(label)
									.get(run).get(fold));
					for (boolean train : instanceLabel.getLabelToInstanceMap()
							.get(label).get(run).get(fold).keySet()) {
						formatter.exportFold(sparseData, instanceLabel
								.getLabelToInstanceMap().get(label).get(run)
								.get(fold).get(train), train, label,
								0 == run ? null : run, 0 == fold ? null : fold);
					}
					formatter.clearFold();
				}
			}
			formatter.clearLabel();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.SparseDataExporter#exportData(java.util.Properties,
	 * org.apache.ctakes.ytex.kernel.SparseDataFormatter, org.apache.ctakes.ytex.kernel.BagOfWordsDecorator)
	 */
	@Override
	public void exportData(Properties props, SparseDataFormatter formatter,
			BagOfWordsDecorator bDecorator) throws IOException {
		InstanceData instanceLabel = this.getKernelUtil().loadInstances(
				props.getProperty("instanceClassQuery"));
		if (props.containsKey("folds")) {
			this.getKernelUtil().generateFolds(instanceLabel, props);
		}
		// load label - instance id maps
		// sparseData.setLabelToInstanceMap(this.getKernelUtil().loadInstances(
		// props.getProperty("instanceClassQuery"),
		// sparseData.getLabelToClassMap()));
		this.exportData(instanceLabel, formatter, props, bDecorator);
		// this.loadData(sparseData,
		// props.getProperty("numericWordQuery"),
		// props.getProperty("nominalWordQuery"), bDecorator);
		// this.exportData(sparseData, formatter, props);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.SparseDataExporter#exportData(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void exportData(String propertiesFile, String format)
			throws IOException, InvalidPropertiesFormatException {
		Properties props = new Properties();
		this.getKernelUtil().loadProperties(propertiesFile, props);
		this.exportData(props, nameToFormatterMap.get(format.toLowerCase())
				.getFormatter(), null);
	}

	public DataSource getDataSource(DataSource ds) {
		return this.jdbcTemplate.getDataSource();
	}

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public Map<String, SparseDataFormatterFactory> getNameToFormatterMap() {
		return nameToFormatterMap;
	}

	/**
	 * run the prepare script if defined.
	 * 
	 * @param prepareScript
	 *            sequence of sql statements to be executed with named params.
	 * @param prepareScriptDelimiter
	 *            delimiter separating the sql statements.
	 * @param params
	 *            for named parameters in sql statements.
	 */
	protected void prepare(final String prepareScript,
			final String prepareScriptDelimiter,
			final Map<String, Object> params) {
		if (prepareScript != null && prepareScript.length() > 0) {
			String[] statements = prepareScript.split(prepareScriptDelimiter);
			// throw out empty lines
			for (String sql : statements) {
				if (sql != null && sql.trim().length() > 0) {
					this.namedJdbcTemplate.update(sql, params);
				}
			}
		}
	}

	/**
	 * 
	 * @param sql
	 *            result set has 3 columns. 1st column - integer - instance id.
	 *            2nd column - word. 3rd column - word value.
	 * @param instanceWordMap
	 *            map of instance id to word-word value.
	 * @param wordValueMap
	 *            map of word to valid values for the word.
	 * @return populate maps with results of query.
	 */
	protected void getNominalInstanceWords(final String sql,
			final String prepareScript, final String prepareScriptDelimiter,
			final SparseData sparseData, final Map<String, Object> params) {
		txTemplateNew.execute(new TransactionCallback<Object>() {

			// new PreparedStatementCreator() {
			// @Override

			// public PreparedStatement createPreparedStatement(
			// Connection conn) throws SQLException {
			// return conn.prepareStatement(sql,
			// ResultSet.TYPE_FORWARD_ONLY,
			// ResultSet.CONCUR_READ_ONLY);
			// }
			//
			// } @Override
			public Object doInTransaction(TransactionStatus txStatus) {
				prepare(prepareScript, prepareScriptDelimiter, params);
				namedJdbcTemplate.query(sql, params, new RowCallbackHandler() {

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						long instanceId = rs.getLong(1);
						String word = rs.getString(2);
						String wordValue = rs.getString(3);
						addNominalWordToInstance(sparseData, instanceId, word,
								wordValue);
					}
				});
				return null;
			}
		});
	}

	/**
	 * 
	 * @param sql
	 *            result 1st column: instance id, 2nd column: word, 3rd column:
	 *            numeric word value
	 * @param instanceNumericWords
	 *            map of instance id - [map word - word value] to be populated
	 */
	protected void getNumericInstanceWords(final String sql,
			final String prepareScript, final String prepareScriptDelimiter,
			final SparseData sparseData, final Map<String, Object> params) {
		txTemplateNew.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus txStatus) {
				prepare(prepareScript, prepareScriptDelimiter, params);
				namedJdbcTemplate.query(sql, params
				// new PreparedStatementCreator() {
				//
				// @Override
				// public PreparedStatement createPreparedStatement(
				// Connection conn) throws SQLException {
				// return conn.prepareStatement(sql,
				// ResultSet.TYPE_FORWARD_ONLY,
				// ResultSet.CONCUR_READ_ONLY);
				// }
				//
				// }
						, new RowCallbackHandler() {

							@Override
							public void processRow(ResultSet rs)
									throws SQLException {
								long instanceId = rs.getLong(1);
								String word = rs.getString(2);
								double wordValue = rs.getDouble(3);
								addNumericWordToInstance(sparseData,
										instanceId, word, wordValue);
							}
						});
				return null;
			}

		});
	}

	public TransactionTemplate getTxTemplateNew() {
		return txTemplateNew;
	}

	/**
	 * 
	 * @param instanceLabel
	 *            instance data: label - fold - instance id - class map
	 * @param instanceNumericWordQuery
	 *            query to get numeric attributes
	 * @param instanceNominalWordQuery
	 *            query to get nominal attributes
	 * @param prepareScript
	 *            prepare script to be executed in same tx as instance attribute
	 *            queries
	 * @param prepareScriptDelimiter
	 *            delimiter for statements in prepare script
	 * @param bDecorator
	 *            decorator to add attributes
	 * @param label
	 * @param fold
	 * @param run
	 * @return
	 */
	protected SparseData loadData(InstanceData instanceLabel,
			String instanceNumericWordQuery, String instanceNominalWordQuery,
			String prepareScript, String prepareScriptDelimiter,
			BagOfWordsDecorator bDecorator, String label, Integer fold,
			Integer run) {
		SparseData sparseData = new SparseData();
		Map<String, Object> params = new HashMap<String, Object>();
		if (label != null && label.length() > 0)
			params.put("label", label);
		if (fold != null && fold != 0)
			params.put("fold", fold);
		if (run != null && run != 0)
			params.put("run", run);
		// load numeric attributes
		if (instanceNumericWordQuery != null
				&& instanceNumericWordQuery.trim().length() > 0)
			this.getNumericInstanceWords(instanceNumericWordQuery,
					prepareScript, prepareScriptDelimiter, sparseData, params);
		// added to support adding gram matrix index in GramMatrixExporter
		if (bDecorator != null)
			bDecorator.decorateNumericInstanceWords(
					sparseData.getInstanceNumericWords(),
					sparseData.getNumericWords());
		// load nominal attributes
		if (instanceNominalWordQuery != null
				&& instanceNominalWordQuery.trim().length() > 0)
			this.getNominalInstanceWords(instanceNominalWordQuery,
					prepareScript, prepareScriptDelimiter, sparseData, params);
		if (bDecorator != null)
			bDecorator.decorateNominalInstanceWords(
					sparseData.getInstanceNominalWords(),
					sparseData.getNominalWordValueMap());
		return sparseData;
	}

	public void setDataSource(DataSource ds) {
		this.jdbcTemplate = new JdbcTemplate(ds);		
		this.namedJdbcTemplate = new NamedParameterJdbcTemplate(ds);
	}

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	public void setNameToFormatterMap(
			Map<String, SparseDataFormatterFactory> nameToFormatterMap) {
		this.nameToFormatterMap = nameToFormatterMap;
	}

	public void setTxTemplateNew(TransactionTemplate txTemplateNew) {
		this.txTemplateNew = txTemplateNew;
	}
}