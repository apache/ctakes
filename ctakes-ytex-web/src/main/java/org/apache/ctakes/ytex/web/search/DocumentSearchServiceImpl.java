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
package org.apache.ctakes.ytex.web.search;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;


public class DocumentSearchServiceImpl implements DocumentSearchService,
		InitializingBean {
	public static class DocumentSearchResultMapper implements
			RowMapper<DocumentSearchResult> {

		public DocumentSearchResult mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DocumentSearchResult result = new DocumentSearchResult();
			result.setCuiText(rs.getString("cui_text"));
			result.setDocumentDate(rs.getDate("doc_date"));
			result.setDocumentID(rs.getInt("document_id"));
			result.setDocumentTitle(rs.getString("doc_title"));
			result.setDocumentTypeName(rs.getString("document_type_name"));
			result.setSentenceText(rs.getString("sentence_text"));
			return result;
		}
	}
	private static final Log log = LogFactory
			.getLog(DocumentSearchServiceImpl.class);
	private DataSource dataSource;	
	private JdbcTemplate jdbcTemplate;
	private String query;

	private Properties searchProperties;

	private SessionFactory sessionFactory;

	private Properties ytexProperties;
	public void afterPropertiesSet() throws Exception {
		this.query = searchProperties.getProperty("retrieveDocumentByCUI")
				.replaceAll("@db\\.schema@",
						this.getYtexProperties().getProperty("db.schema"));
	}

	/**
	 * Extended search
	 * 
	 * @param code
	 *            concept CUI or code. this is the only required argument
	 * @param documentTypeName
	 *            document type name. (in VACS @see DocumentType)
	 * @param dateFrom
	 *            document date greater than or equal to this
	 * @param dateTo
	 *            document date less than or equal to this
	 * @param patientId
	 *            patient id (study id in VACS)
	 * @param negationStatus
	 *            true - only affirmed terms. false - only negated terms.
	 * @return list of results matching query
	 */
	public List<DocumentSearchResult> extendedSearch(String code,
			String documentTypeName, Date dateFrom, Date dateTo,
			Integer patientId, Boolean negationStatus) {
		Map<String, Object> mapArgs = this.initMapArgs(code);
		if (documentTypeName != null) {
			mapArgs.put("document_type_name", documentTypeName);
		}
		if (dateFrom != null) {
			mapArgs.put("from_doc_date", dateFrom);
		}
		if (dateTo != null) {
			mapArgs.put("to_doc_date", dateTo);
		}
		if (patientId != null) {
			mapArgs.put("patient_id", patientId);
		}
		if (negationStatus != null) {
			mapArgs.put("certainty", negationStatus ? 0 : -1);
		}
		if (log.isDebugEnabled()) {
			log.debug("executing query, query=" + query
					+ ", args=" + mapArgs);
		}
		return this.jdbcTemplate.query(query,
				new DocumentSearchResultMapper(), mapArgs);
	}

	/**
	 * perform full text search
	 * 
	 * @param searchTerm
	 * @return list of maps for each record. map keys correspond to search query
	 *         headings. map (i.e. query) must contain DOCUMENT_ID (integer) and
	 *         NOTE (string) fields.
	 */
	public List<Map<String, Object>> fullTextSearch(String searchTerm) {
		return this.jdbcTemplate.queryForList(
				this.searchProperties.getProperty("retrieveDocumentFullText"),
				searchTerm);
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * retrieve note for specified document id, retrieved via full text search
	 * 
	 * @param documentId
	 * @return note text.
	 */
	public String getFullTextSearchDocument(int documentId) {
		return this.jdbcTemplate.queryForObject(this.searchProperties
				.getProperty("retrieveFullTextSearchDocument"), String.class,
				documentId);
	}

	public Properties getSearchProperties() {
		return searchProperties;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public Properties getYtexProperties() {
		return ytexProperties;
	}

	private Map<String, Object> initMapArgs(String code) {
		Map<String, Object> mapArgs = new HashMap<String, Object>(1);
		mapArgs.put("code", code);
		mapArgs.put("document_type_name", null);
		mapArgs.put("from_doc_date", null);
		mapArgs.put("to_doc_date", null);
		mapArgs.put("patient_id", null);
		mapArgs.put("certainty", null);
		return mapArgs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.va.vacs.esld.dao.DocumentSearchDao#searchByCui(java.lang.String)
	 */
	public List<DocumentSearchResult> searchByCui(String code) {
		Map<String, Object> mapArgs = this.initMapArgs(code);
		return this.jdbcTemplate.query(query, new DocumentSearchResultMapper(),
				mapArgs);
		// String query =
		// "select new ytex.web.search.DocumentSearchResult(d.documentID, substring(d.docText, 1,10), current_timestamp(), substring(d.docText, 1,10), substring(d.docText, 1,10), substring(d.docText, ne.begin+1,ne.end-ne.begin)) from OntologyConceptAnnotation o inner join o.namedEntityAnnotation ne inner join o.namedEntityAnnotation.document d";
		// Query q =
		// this.getSessionFactory().getCurrentSession().createQuery(searchProperties.getProperty("retrieveDocumentByCUIHql"));
		// q.setParameter("code", code);
		// q.setMaxResults(100);
		// return q.list();
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;		
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setSearchProperties(Properties searchProperties) {
		this.searchProperties = searchProperties;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setYtexProperties(Properties ytexProperties) {
		this.ytexProperties = ytexProperties;
	}
}
