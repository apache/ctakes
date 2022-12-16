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
package org.apache.ctakes.ytex.uima.annotators;

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ctakes.ytex.uima.ApplicationContextHolder;
import org.apache.ctakes.ytex.uima.TestUtils;
import org.apache.ctakes.ytex.uima.types.DocKey;
import org.apache.ctakes.ytex.uima.types.KeyValuePair;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.base.Strings;

/**
 * TODO get rid of hard-coded path to Types.xml - load from classpath
 * 
 * @author vgarla
 * 
 */
public class DBConsumerTest {
	static ApplicationContext ctx = null;

	@BeforeClass
	public static void setup() {
		ctx = ApplicationContextHolder.getApplicationContext();
	}

	/**
	 * hsql will not write data unless the connection is explicitly closed. for
	 * debugging issues helpful to look at data; therefore uncomment this
	 * method. however, this will cause issues with other tests as they run in
	 * the same classloader, and the pool will be closed. most elegant solution
	 * would be for spring to close the connections on shutdown.
	 * 
	 * alternatively, fork tests.
	 */
	@AfterClass
	public static void cleanup() {
		((ConfigurableApplicationContext)ctx).close();
		// ApplicationContext ctx = ApplicationContextHolder
		// .getApplicationContext();
		// DataSource ds = ctx.getBean(DataSource.class);
		// try {
		// ((BasicDataSource)ds).close();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	// @Test
	// public void testGetCols() throws SQLException {
	// ApplicationContext ctx = ApplicationContextHolder
	// .getApplicationContext();
	// DataSource ds = ctx.getBean(DataSource.class);
	// Connection conn = ds.getConnection();
	// DatabaseMetaData dmd = conn.getMetaData();
	// // get columns for corresponding table
	// // mssql - add schema prefix
	// // oracle - convert table name to upper case
	// ResultSet rs = dmd.getColumns(
	// null,
	// "PUBLIC",
	// "ANNO_TOKEN",
	// null);
	// while(rs.next()) {
	// String colName = rs.getString("COLUMN_NAME");
	// System.out.println(colName);
	// }
	// rs.close();
	// conn.close();
	// }

	/**
	 * Verify that date parsing with a manually created date works
	 * 
	 * @throws UIMAException
	 * @throws Exception
	 */
	@Test
	public void testProcessAndSaveDoc() throws UIMAException, IOException {
		// JCas jCas =
		// JCasFactory.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
		String text = "Title: US Abdomen\n\nDr. Doolitle asked patient\nto take a deep breath\nand exhale slowly.  Patient coughed.  Prescribed acetominophen";
		AnalysisEngine engine = TestUtils.createTokenizerAE(null);
		JCas jCas = engine.newJCas();
		jCas.setDocumentText(text);
		// create a docKey so we can find the doc
		long key = System.currentTimeMillis();
		DocKey docKey = new DocKey(jCas);
		KeyValuePair kvp = new KeyValuePair(jCas);
		kvp.setKey("instance_id");
		kvp.setValueLong(key);
		FSArray fsa = new FSArray(jCas, 1);
		fsa.set(0, kvp);
		docKey.setKeyValuePairs(fsa);
		docKey.addToIndexes();
		// run the analysis engine
		engine.process(jCas);
		DataSource ds = ctx.getBean(DataSource.class);
		Properties ytexProperties = (Properties) ctx.getBean("ytexProperties");
		String schema = ytexProperties.getProperty("db.schema");
		String schemaPrefix = Strings.isNullOrEmpty(schema) ? "" : schema + ".";
		JdbcTemplate jt = new JdbcTemplate(ds);
		String query = String.format(
				"select count(*) from %sdocument where instance_id=%s",
				schemaPrefix, key);
		Assert.assertTrue(query, jt.queryForObject(query,Integer.class) == 1);
		query = String
				.format("select count(*) from %sdocument d inner join %sanno_base ab on ab.document_id = d.document_id inner join %sanno_segment s on s.anno_base_id = ab.anno_base_id where d.instance_id=%s",
						schemaPrefix, schemaPrefix, schemaPrefix, key);
		Assert.assertTrue(query, jt.queryForObject(query,Integer.class) == 1);
		query = String
				.format("select count(*) from %sdocument d inner join %sanno_base ab on ab.document_id = d.document_id inner join %sanno_sentence s on s.anno_base_id = ab.anno_base_id where d.instance_id=%s",
						schemaPrefix, schemaPrefix, schemaPrefix, key);
		Assert.assertTrue(query, jt.queryForObject(query,Integer.class) > 1);
		query = String
				.format("select count(*) from %sdocument d inner join %sanno_base ab on ab.document_id = d.document_id inner join %sanno_token s on s.anno_base_id = ab.anno_base_id where d.instance_id=%s",
						schemaPrefix, schemaPrefix, schemaPrefix, key);
		Assert.assertTrue(query, jt.queryForObject(query,Integer.class) > 1);
	}

}
