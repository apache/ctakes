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
package org.apache.ctakes.jdl.data.base;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.sql.SQLException;

import javax.xml.bind.JAXBException;


import org.apache.ctakes.jdl.common.FileUtil;
import org.apache.ctakes.jdl.data.base.JdlConnection;
import org.apache.ctakes.jdl.data.xml.jaxb.ObjectFactoryUtil;
import org.apache.ctakes.jdl.schema.xdl.JdbcType;
import org.apache.ctakes.jdl.test.Resources;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdlConnectionTest {
	private static JdbcType jdbc;
	private static JdlConnection jdlConnection;
	private static final String CX = FileUtil.getFile(Resources.CONN_X).toString();

	@BeforeClass
	public static void initClass() throws JAXBException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		jdbc = ObjectFactoryUtil.getJdbcTypeBySrcXml(CX);
		jdlConnection = new JdlConnection(jdbc);
	}

	@Test
	public void isConnected() throws SQLException {
		assertThat(jdlConnection.isConnected(), is(true));
	}

	@Before
	public void getOpenConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		assumeThat(jdbc.getDriver(), not(Resources.ENV_DRIVER));
		assumeThat(jdbc.getUrl(), not(Resources.ENV_URL));
		assertThat(jdlConnection.getOpenConnection().isClosed(), is(false));
	}

	@AfterClass
	public static void closeConnection() throws SQLException {
		jdlConnection.closeConnection();
		assertThat(jdlConnection.isConnected(), is(false));
	}

	@Test
	public void autoCommit() throws SQLException {
		jdlConnection.setAutoCommit(true);
		assertThat(jdlConnection.isAutoCommit(), is(true));
		jdlConnection.setAutoCommit(false);
		assertThat(jdlConnection.isAutoCommit(), is(false));
		jdlConnection.setAutoCommit(true);
		assertThat(jdlConnection.isAutoCommit(), is(true));
	}
}
