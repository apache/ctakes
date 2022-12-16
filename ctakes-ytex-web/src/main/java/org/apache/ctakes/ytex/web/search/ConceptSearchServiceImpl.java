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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

public class ConceptSearchServiceImpl implements ConceptSearchService,
		InitializingBean {
	public static class ConceptFirstWordRowMapper implements
			RowMapper<ConceptFirstWord> {

		public ConceptFirstWord mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			ConceptFirstWord fword = new ConceptFirstWord();
			fword.setConceptId(rs.getString("conceptId"));
			fword.setFword(rs.getString("fword"));
			fword.setText(rs.getString("text"));
			return fword;
		}

	}

	private String conceptIdToTermQuery;
	private Pattern conceptPattern;
	private DataSource dataSource;
	private String fwordToConceptIdQuery;

	private JdbcTemplate jdbcTemplate;

	private Properties searchProperties;
	private Properties ytexProperties;

	public void afterPropertiesSet() throws Exception {
		this.fwordToConceptIdQuery = prepareQuery(this.getSearchProperties()
				.getProperty("retrieveConceptByFword"));
		this.conceptIdToTermQuery = prepareQuery(this.getSearchProperties()
				.getProperty("retrieveTermByConceptId"));
		this.conceptPattern = Pattern.compile(searchProperties.getProperty(
				"conceptIdPattern", "\\AC\\d{7}\\Z"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.va.vacs.esld.dao.UMLSFirstWordDao#getUMLSbyFirstWord(java.lang.String
	 * )
	 */
	public List<ConceptFirstWord> getConceptByFirstWord(String textStart) {
		String words[] = textStart.toLowerCase().split("\\s+");
		String fword = textStart.toLowerCase();
		// int nFWordLength = fword.length();
		String text = textStart.toLowerCase();
		int nTextLength = textStart.length();
		if (words.length > 1) {
			fword = words[0];
			// nFWordLength = fword.length();
		}
		// return this.jdbcTemplate.query(query, new
		// UMLSFirstWordRowMapper(),
		// new Object[] { fword.length(), fword, nTextLength, text });
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("fword", fword);
		args.put("fwordlen", fword.length());
		args.put("term", text);
		args.put("termlen", nTextLength);
		return this.jdbcTemplate.query(fwordToConceptIdQuery,
				new ConceptFirstWordRowMapper(), args);
	}

	private List<ConceptFirstWord> getConceptById(String conceptId) {
		String term = getTermByConceptId(conceptId);
		if (term != null) {
			List<ConceptFirstWord> terms = new ArrayList<ConceptFirstWord>(1);
			ConceptFirstWord cft = new ConceptFirstWord();
			cft.setConceptId(conceptId);
			cft.setFword(term);
			cft.setText(term);
			terms.add(cft);
			return terms;
		} else {
			return new ArrayList<ConceptFirstWord>(0);
		}
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

	public Properties getSearchProperties() {
		return searchProperties;
	}

	public String checkTermByConceptId(String conceptId) {
		Matcher m = conceptPattern.matcher(conceptId);
		if (m.find()) {
			return getTermByConceptId(conceptId);
		} else {
			return null;
		}
	}

	public String getTermByConceptId(String conceptId) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("conceptId", conceptId);
		return this.jdbcTemplate.queryForObject(this.conceptIdToTermQuery,
				String.class, args);
	}

	public Properties getYtexProperties() {
		return ytexProperties;
	}

	private String prepareQuery(String queryTemplate) {
		String dbName = this.getYtexProperties().getProperty("db.name");
		String dbSchema = this.getYtexProperties().getProperty("db.schema");
		String umlsSchema = this.getYtexProperties().getProperty("umls.schema",
				dbSchema);
		String umlsCatalog = this.getYtexProperties().getProperty(
				"umls.catalog", dbName);
		String query = queryTemplate.replaceAll("@db\\.schema@", this
				.getYtexProperties().getProperty("db.schema"));
		query = query.replaceAll("@umls\\.schema@", umlsSchema);
		query = query.replaceAll("@umls\\.catalog@", umlsCatalog);
		return query;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;		
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setSearchProperties(Properties searchProperties) {
		this.searchProperties = searchProperties;
	}

	public void setYtexProperties(Properties ytexProperties) {
		this.ytexProperties = ytexProperties;
	}
}
