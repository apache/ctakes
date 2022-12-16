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
package org.apache.ctakes.ytex.uima;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.ytex.uima.types.DocKey;
import org.apache.ctakes.ytex.uima.types.KeyValuePair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Read documents from db. Config parameters:
 * <ul>
 * <li>queryGetDocumentKeys the query to get the document keys</li>
 * <li>queryGetDocument the query to get a document given a key. should have
 * named parameters that match the columns of the result set returned by
 * queryGetDocumentKeys</li>
 * <li>keyTypeName the uima type of the document key to be added to the cas.
 * defaults to org.apache.ctakes.ytex.uima.types.DocKey.
 * <li>keyNameToLowerCase convert the column names returned by
 * queryGetDocumentKeys to lower case, default true</li>
 * </ul>
 * 
 * @TODO more doc
 * @author vijay
 * 
 */
@PipeBitInfo(
		name = "Database Reader",
		description = "Read documents from a database.",
		role = PipeBitInfo.Role.READER )
public class DBCollectionReader extends CollectionReader_ImplBase {
	private static final Log log = LogFactory.getLog(DBCollectionReader.class);

	/**
	 * the query to get the document keys set in config file
	 */
	protected String queryGetDocumentKeys;
	/**
	 * the queyr to get a document given a key. set in config file
	 */
	protected String queryGetDocument;
	/**
	 * the key type. if not set, will default to
	 * org.apache.ctakes.ytex.uima.types.DocKey
	 */
	protected String keyTypeName = "org.apache.ctakes.ytex.uima.types.DocKey";

	protected DataSource dataSource;
	protected JdbcTemplate jdbcTemplate;	
	protected NamedParameterJdbcTemplate namedJdbcTemplate;
	protected TransactionTemplate txTemplate;
	protected boolean keyNameToLowerCase = true;

	public boolean isKeyNameToLowerCase() {
		return keyNameToLowerCase;
	}

	public void setKeyNameToLowerCase(boolean keyNameToLowerCase) {
		this.keyNameToLowerCase = keyNameToLowerCase;
	}

	List<Map<String, Object>> listDocumentIds;
	int i = 0;

	@Override
	public void initialize() throws ResourceInitializationException {
		initializePreLoad();
		loadDocumentIds();
	}

	protected void initializePreLoad() throws ResourceInitializationException {
		super.initialize();
		ProcessingResourceMetaData metaData = getProcessingResourceMetaData();
		ConfigurationParameterSettings paramSettings = metaData
				.getConfigurationParameterSettings();
		this.queryGetDocumentKeys = (String) paramSettings
				.getParameterValue("queryGetDocumentKeys");
		this.queryGetDocument = (String) paramSettings
				.getParameterValue("queryGetDocument");
		this.keyTypeName = (String) paramSettings
				.getParameterValue("keyTypeName");
		Boolean keyNameToLowerCase = (Boolean) paramSettings
				.getParameterValue("keyNameToLowerCase");
		if (keyNameToLowerCase != null)
			this.keyNameToLowerCase = keyNameToLowerCase.booleanValue();
		String dbURL = (String) paramSettings.getParameterValue("dbURL");
		String dbDriver = (String) paramSettings.getParameterValue("dbDriver");
		initDB(dbDriver, dbURL);
	}

	protected void initDB(String dbDriver, String dbURL)
			throws ResourceInitializationException {
		if (dbURL != null && dbURL.length() > 0) {
			try {

				if (dbDriver == null || dbDriver.length() == 0) {
					dbDriver = ApplicationContextHolder.getYtexProperties()
							.getProperty("db.driver");
				}
				dataSource = new SimpleDriverDataSource((Driver) Class.forName(
						dbDriver).newInstance(), dbURL);
				txTemplate = new TransactionTemplate(
						new DataSourceTransactionManager(dataSource));
			} catch (InstantiationException e) {
				throw new ResourceInitializationException(e);
			} catch (IllegalAccessException e) {
				throw new ResourceInitializationException(e);
			} catch (ClassNotFoundException e) {
				throw new ResourceInitializationException(e);
			}
		} else {
			txTemplate = (TransactionTemplate) ApplicationContextHolder
					.getApplicationContext().getBean("txTemplate");
			dataSource = (DataSource) ApplicationContextHolder
					.getApplicationContext().getBean(
							"collectionReaderDataSource");
		}		
		jdbcTemplate = new JdbcTemplate(dataSource);
		namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
	}

	protected void loadDocumentIds() {
		if (listDocumentIds == null) {
			listDocumentIds = txTemplate
					.execute(new TransactionCallback<List<Map<String, Object>>>() {

						@Override
						public List<Map<String, Object>> doInTransaction(
								TransactionStatus arg0) {
							return jdbcTemplate
									.queryForList(queryGetDocumentKeys);
						}
					});
			i = 0;
		}
	}

	@Override
	public void getNext(final CAS aCAS) throws IOException, CollectionException {
		try {
			getNext(aCAS.getJCas());
		} catch (CASException e) {
			throw new CollectionException(e);
		}
	}

	public void getNext(final JCas aCAS) throws IOException,
			CollectionException {
		if (i < listDocumentIds.size()) {
			final Map<String, Object> id = listDocumentIds.get(i++);
			if (log.isInfoEnabled()) {
				log.info("loading document with id = " + id);
			}
			getDocumentById(aCAS, id);
			addDocKey(aCAS, id);
		} else {
			// shouldn't get here?
			throw new CollectionException("no documents to process",
					new Object[] {});
		}
	}

	private void addDocKey(JCas aCAS, Map<String, Object> id)
			throws CollectionException {
		DocKey docKey = new DocKey(aCAS);
		FSArray keyValuePairs = new FSArray(aCAS, id.size());
		int i = 0;
		for (Map.Entry<String, Object> idVal : id.entrySet()) {
			String key = idVal.getKey();
			Object val = idVal.getValue();
			KeyValuePair p = new KeyValuePair(aCAS);
			p.setKey(key);
			if (val instanceof Number) {
				p.setValueLong(((Number) val).longValue());
			} else if (val instanceof String) {
				p.setValueString((String) val);
			} else {
				log.warn("Don't know how to handle key attribute, converting to string, key="
						+ key + ", value=" + val);
				p.setValueString(val.toString());
			}
			keyValuePairs.set(i, p);
			i++;
		}
		docKey.setKeyValuePairs(keyValuePairs);
		docKey.addToIndexes();

	}

	protected void getDocumentById(final JCas aCAS, final Map<String, Object> id) {
		Map<String, Object> idMapTmp = id;
		if (this.isKeyNameToLowerCase()) {
			idMapTmp = new HashMap<String, Object>();
			for (Map.Entry<String, Object> e : id.entrySet()) {
				idMapTmp.put(e.getKey().toLowerCase(), e.getValue());
			}
		}
		final Map<String, Object> idQuery = idMapTmp;
		this.txTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus arg0) {
				namedJdbcTemplate.query(queryGetDocument, idQuery,
						new RowCallbackHandler() {
							boolean bFirstRowRead = false;

							@Override
							public void processRow(ResultSet rs)
									throws SQLException {
								if (!bFirstRowRead) {
									LobHandler lobHandler = new DefaultLobHandler();
									String clobText = lobHandler
											.getClobAsString(rs, 1);
									aCAS.setDocumentText(clobText);
									bFirstRowRead = true;
								} else {
									log.error("Multiple documents for document key: "
											+ idQuery);
								}
							}
						});
				return null;
			}
		});
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(i, listDocumentIds.size(),
				Progress.ENTITIES) };
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return i < listDocumentIds.size();
	}

	@Override
	public void close() throws IOException {
		this.listDocumentIds = null;
		this.i = 0;
	}

}
