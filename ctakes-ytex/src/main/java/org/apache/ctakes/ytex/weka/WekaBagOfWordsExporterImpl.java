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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

import org.apache.ctakes.ytex.kernel.AbstractBagOfWordsExporter;
import org.apache.ctakes.ytex.kernel.BagOfWordsData;
import org.apache.ctakes.ytex.kernel.BagOfWordsDecorator;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;

public class WekaBagOfWordsExporterImpl extends AbstractBagOfWordsExporter
		implements WekaBagOfWordsExporter {
	private static final String INSTANCE_ID = "instance_id";
	private static final String CLASS = "ytex_class";
	
	private void addWordsToInstances(Instances instances,
			BagOfWordsData bagOfWordsData) throws IOException {
		for (Map.Entry<Integer, String> entry : bagOfWordsData
				.getDocumentClasses().entrySet()) {
			double[] zeroValues = new double[instances.numAttributes()];
			Arrays.fill(zeroValues, 0.0d);
			SparseInstance wekaInstance = new SparseInstance(1.0d, zeroValues);
			wekaInstance.setDataset(instances);
			// set instance id
			Attribute instanceId = instances.attribute(INSTANCE_ID);
			wekaInstance.setValue(instanceId.index(), entry.getKey()
					.doubleValue());
			// set document class
			Attribute classAttr = instances.attribute(CLASS);
			wekaInstance.setValue(classAttr.index(),
					classAttr.indexOfValue(entry.getValue()));
			// set numeric words
			if (bagOfWordsData.getInstanceNumericWords().get(entry.getKey()) != null) {
				for (Map.Entry<String, Double> word : bagOfWordsData
						.getInstanceNumericWords().get(entry.getKey())
						.entrySet()) {
					Attribute wordAttr = instances.attribute(word.getKey());
					wekaInstance.setValue(wordAttr.index(), word.getValue()
							.doubleValue());
				}
			}
			// set nominal words
			if (bagOfWordsData.getInstanceNominalWords().get(entry.getKey()) != null) {
				for (Map.Entry<String, String> word : bagOfWordsData
						.getInstanceNominalWords().get(entry.getKey())
						.entrySet()) {
					Attribute wordAttr = instances.attribute(word.getKey());
					int valueIndex = wordAttr.indexOfValue(word.getValue());
					if (valueIndex == -1) {
						throw new IOException("oops! " + word);
					}
					wekaInstance.setValue(wordAttr.index(), valueIndex);
				}
			}
			instances.add(wekaInstance);
		}
	}

	public void exportBagOfWords(String arffRelation,
			String instanceClassQuery, String instanceNumericWordQuery,
			String instanceNominalWordQuery, BufferedWriter writer)
			throws IOException {
		exportBagOfWords(arffRelation, instanceClassQuery,
				instanceNumericWordQuery, instanceNominalWordQuery, writer,
				null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.yale.cbb.uima.weka.BagOfWordsExporter#exportBagOfWords(java.lang.
	 * String, java.lang.String, java.lang.String, java.lang.String,
	 * java.io.BufferedWriter)
	 */
	public void exportBagOfWords(String arffRelation,
			String instanceClassQuery, String instanceNumericWordQuery,
			String instanceNominalWordQuery, BufferedWriter writer,
			BagOfWordsDecorator bDecorator) throws IOException {
		BagOfWordsData bagOfWordsData = new BagOfWordsData();
		// load instance classes
		getInstances(instanceClassQuery, bagOfWordsData);
		loadData(bagOfWordsData, instanceNumericWordQuery,
				instanceNominalWordQuery, bDecorator);
		// add instance for each document
		// initialize the instances
		Instances instances = initializeInstances(arffRelation, bagOfWordsData,
				bDecorator);
		this.addWordsToInstances(instances, bagOfWordsData);
		writer.write(instances.toString());
	}

	public void exportBagOfWords(String propertyFile) throws IOException {
		exportBagOfWords(propertyFile, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.yale.cbb.uima.weka.BagOfWordsExporter#exportBagOfWords(java.lang.
	 * String)
	 */
	public void exportBagOfWords(String propertyFile,
			BagOfWordsDecorator bDecorator) throws IOException {
		Properties props = new Properties();
		loadProperties(propertyFile, props);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(
					props.getProperty("arffFile")));
			exportBagOfWords(props.getProperty("arffRelation"),
					props.getProperty("instanceClassQuery"),
					props.getProperty("numericWordQuery", ""),
					props.getProperty("nominalWordQuery", ""), writer,
					bDecorator);
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	/**
	 * initialize the weka Instances
	 * 
	 * @param arffRelation
	 * @param sql
	 * @param classLabels
	 * @param idfMap
	 * @param docLengthMap
	 * @return
	 */
	protected Instances initializeInstances(String arffRelation,
			BagOfWordsData bagOfWordsData, BagOfWordsDecorator bDecorator) {
		FastVector wekaAttributes = new FastVector(bagOfWordsData
				.getNumericWords().size()
				+ bagOfWordsData.getNominalWordValueMap().size() + 2);
		// add instance id attribute
		wekaAttributes.addElement(new Attribute(INSTANCE_ID));
		// add numeric word attributes
		for (String word : bagOfWordsData.getNumericWords()) {
			Attribute attribute = new Attribute(word);
			wekaAttributes.addElement(attribute);
		}
		// add nominal word attributes
		for (Map.Entry<String, SortedSet<String>> nominalWordEntry : bagOfWordsData
				.getNominalWordValueMap().entrySet()) {
			FastVector wordValues = new FastVector(nominalWordEntry.getValue()
					.size());
			for (String wordValue : nominalWordEntry.getValue()) {
				wordValues.addElement(wordValue);
			}
			Attribute attribute = new Attribute(nominalWordEntry.getKey(),
					wordValues);
			wekaAttributes.addElement(attribute);
		}
		// add class attribute
		FastVector wekaClassLabels = new FastVector(bagOfWordsData.getClasses()
				.size());
		for (String classLabel : bagOfWordsData.getClasses()) {
			wekaClassLabels.addElement(classLabel);
		}
		wekaAttributes.addElement(new Attribute(CLASS, wekaClassLabels));
		Instances instances = new Instances(arffRelation, wekaAttributes, 0);
		instances.setClassIndex(instances.numAttributes() - 1);
		return instances;
	}

	protected void getInstances(final String sql,
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
					private Set<String> numericColumnHeaders;
					private Set<String> nominalColumnHeaders;

					private void initMetaData(ResultSet rs) throws SQLException {
						if (numericColumnHeaders == null) {
							numericColumnHeaders = new HashSet<String>();
							nominalColumnHeaders = new HashSet<String>();

							ResultSetMetaData rsmd = rs.getMetaData();
							for (int i = 3; i <= rsmd.getColumnCount(); i++) {
								int colType = rsmd.getColumnType(i);
								if (colType == Types.CHAR
										|| colType == Types.BOOLEAN
										|| colType == Types.VARCHAR) {
									nominalColumnHeaders.add(rsmd
											.getColumnLabel(i));
								} else if (colType == Types.DECIMAL
										|| colType == Types.BIGINT
										|| colType == Types.DOUBLE
										|| colType == Types.FLOAT
										|| colType == Types.DECIMAL
										|| colType == Types.INTEGER
										|| colType == Types.NUMERIC
										|| colType == Types.REAL) {
									numericColumnHeaders.add(rsmd
											.getColumnLabel(i));
								}
							}
						}

					}

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						this.initMetaData(rs);
						int instanceId = rs.getInt(1);
						String classLabel = rs.getString(2);
						bagOfWordsData.getDocumentClasses().put(instanceId,
								classLabel);
						bagOfWordsData.getClasses().add(classLabel);
						// add other attributes
						for (String columnHeader : this.numericColumnHeaders) {
							double wordValue = rs.getDouble(columnHeader);
							if (!rs.wasNull()) {
								addNumericWordToInstance(bagOfWordsData,
										instanceId, columnHeader, wordValue);
							}
						}
						for (String columnHeader : this.nominalColumnHeaders) {
							String wordValue = rs.getString(columnHeader);
							if (!rs.wasNull()) {
								addNominalWordToInstance(bagOfWordsData,
										instanceId, columnHeader, wordValue);
							}
						}

					}
				});
				return null;
			}
		});
	}
}
