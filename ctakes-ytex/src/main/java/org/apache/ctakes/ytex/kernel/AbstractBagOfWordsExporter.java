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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class AbstractBagOfWordsExporter {

	protected JdbcTemplate jdbcTemplate;
	protected PlatformTransactionManager transactionManager;
	protected TransactionTemplate txNew;

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(
			PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		txNew = new TransactionTemplate(transactionManager);
		txNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
	}

	public AbstractBagOfWordsExporter() {
		super();
	}

	public void setDataSource(DataSource ds) {
		this.jdbcTemplate = new JdbcTemplate(ds);		
	}

	public DataSource getDataSource(DataSource ds) {
		return this.jdbcTemplate.getDataSource();
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
			final BagOfWordsData bagOfWordsData) {
		txNew.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus txStatus) {
				jdbcTemplate.query(new PreparedStatementCreator() {

					@Override
					public PreparedStatement createPreparedStatement(
							Connection conn) throws SQLException {
						return conn.prepareStatement(sql,
								ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY);
					}

				}, new RowCallbackHandler() {

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						int instanceId = rs.getInt(1);
						String word = rs.getString(2);
						double wordValue = rs.getDouble(3);
						addNumericWordToInstance(bagOfWordsData, instanceId,
								word, wordValue);
					}
				});
				return null;
			}

		});
	}

	protected void addNumericWordToInstance(BagOfWordsData bagOfWordsData,
			int instanceId, String word, double wordValue) {
		// add the numeric word to the map of words for this document
		SortedMap<String, Double> words = bagOfWordsData
				.getInstanceNumericWords().get(instanceId);
		if (words == null) {
			words = new TreeMap<String, Double>();
			bagOfWordsData.getInstanceNumericWords().put(instanceId, words);
		}
		words.put(word, wordValue);
		bagOfWordsData.getNumericWords().add(word);
		// increment the length of the document by the wordValue
		Integer docLength = bagOfWordsData.getDocLengthMap().get(instanceId);
		if (docLength == null) {
			docLength = 0;
		}
		bagOfWordsData.getDocLengthMap().put(instanceId,
				(docLength + (int) wordValue));
		// add to the number of docs that have the word
		Integer docsWithWord = bagOfWordsData.getIdfMap().get(word);
		if (docsWithWord == null) {
			docsWithWord = 0;
		}
		bagOfWordsData.getIdfMap().put(word, docsWithWord + 1);
	}

	protected void addNominalWordToInstance(BagOfWordsData bagOfWordsData,
			int instanceId, String word, String wordValue) {
		SortedMap<String, String> instanceWords = bagOfWordsData
				.getInstanceNominalWords().get(instanceId);
		SortedSet<String> wordValueSet = bagOfWordsData
				.getNominalWordValueMap().get(word);
		if (instanceWords == null) {
			instanceWords = new TreeMap<String, String>();
			bagOfWordsData.getInstanceNominalWords().put(instanceId,
					instanceWords);
		}
		if (wordValueSet == null) {
			wordValueSet = new TreeSet<String>();
			bagOfWordsData.getNominalWordValueMap().put(word, wordValueSet);
		}
		// add the word-value for the instance
		instanceWords.put(word, wordValue);
		// add the value to the set of valid values
		wordValueSet.add(wordValue);
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
			final BagOfWordsData bagOfWordsData) {
		txNew.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus txStatus) {
				jdbcTemplate.query(new PreparedStatementCreator() {

					@Override
					public PreparedStatement createPreparedStatement(
							Connection conn) throws SQLException {
						return conn.prepareStatement(sql,
								ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY);
					}

				}, new RowCallbackHandler() {

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						int instanceId = rs.getInt(1);
						String word = rs.getString(2);
						String wordValue = rs.getString(3);
						addNominalWordToInstance(bagOfWordsData, instanceId,
								word, wordValue);
					}
				});
				return null;
			}
		});
	}

	protected void loadProperties(String propertyFile, Properties props)
			throws FileNotFoundException, IOException,
			InvalidPropertiesFormatException {
		InputStream in = null;
		try {
			in = new FileInputStream(propertyFile);
			if (propertyFile.endsWith(".xml"))
				props.loadFromXML(in);
			else
				props.load(in);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	protected void loadData(BagOfWordsData bagOfWordsData,
			String instanceNumericWordQuery, String instanceNominalWordQuery,
			BagOfWordsDecorator bDecorator) {
		if (instanceNumericWordQuery.trim().length() > 0)
			this.getNumericInstanceWords(instanceNumericWordQuery,
					bagOfWordsData);
		// added to support adding gram matrix index in GramMatrixExporter
		// TODO fix this
		// currently not using weka gram matrix
//		if (bDecorator != null)
//			bDecorator.decorateNumericInstanceWords(
//					bagOfWordsData.getInstanceNumericWords(),
//					bagOfWordsData.getNumericWords());
//		if (instanceNominalWordQuery.trim().length() > 0)
//			this.getNominalInstanceWords(instanceNominalWordQuery,
//					bagOfWordsData);
//		if (bDecorator != null)
//			bDecorator.decorateNominalInstanceWords(
//					bagOfWordsData.getInstanceNominalWords(),
//					bagOfWordsData.getNominalWordValueMap());
	}
}