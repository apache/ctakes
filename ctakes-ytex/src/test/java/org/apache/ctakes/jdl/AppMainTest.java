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
package org.apache.ctakes.jdl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.xml.bind.JAXBException;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.ctakes.jdl.AppMain;
import org.apache.ctakes.jdl.common.FileUtil;
import org.apache.ctakes.jdl.data.base.JdlConnection;
import org.apache.ctakes.jdl.data.xml.jaxb.ObjectFactoryUtil;
import org.apache.ctakes.jdl.schema.xdl.JdbcType;
import org.apache.ctakes.jdl.test.PropFileMaps;
import org.apache.ctakes.jdl.test.Resources;
import org.apache.ctakes.jdl.test.SqlJdl;
import org.junit.BeforeClass;
import org.junit.Test;

public class AppMainTest {
	private static JdbcType jdbc;
	private static JdlConnection jdlConnection;
	private static String CX = FileUtil.getFile(Resources.CONN_X).toString();
	private static String D1C = FileUtil.getFile(Resources.DATA1C).toString();
	private static String D1X = FileUtil.getFile(Resources.DATA1X).toString();
	private static String L1C = FileUtil.getFile(Resources.LOAD1C).toString();
	private static String L1X = FileUtil.getFile(Resources.LOAD1X).toString();
	private static String C = "-" + AppMain.OPT_XDL_CONN;
	private static String D = "-" + AppMain.OPT_XDL_DATA;
	private static String L = "-" + AppMain.OPT_XDL_LOAD;

	@BeforeClass
	public static void initClass() throws JAXBException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
		jdbc = ObjectFactoryUtil.getJdbcTypeBySrcXml(CX);
		jdlConnection = new JdlConnection(jdbc);
	}

	@Test
	public void parsingCLI() throws ParseException {
		String[] args;
		CommandLine cl;
		args = new String[] { C, "conn.xml", D, "data.xml", L, "load.csv" };
		cl = AppMain.parsingCLI(args);
		assertThat(cl.getOptionValue(AppMain.OPT_XDL_CONN), is("conn.xml"));
		assertThat(cl.getOptionValue(AppMain.OPT_XDL_DATA), is("data.xml"));
		assertThat(cl.getOptionValue(AppMain.OPT_XDL_LOAD), is("load.csv"));
		args = new String[] { C, "conn.xml", D, "data.xml", L, "load.xml" };
		cl = AppMain.parsingCLI(args);
		assertThat(cl.getOptionValue(AppMain.OPT_XDL_CONN), is("conn.xml"));
		assertThat(cl.getOptionValue(AppMain.OPT_XDL_DATA), is("data.xml"));
		assertThat(cl.getOptionValue(AppMain.OPT_XDL_LOAD), is("load.xml"));
	}

	@Test
	public void main() throws JAXBException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		assumeThat(jdbc.getDriver(), not(Resources.ENV_DRIVER));
		assumeThat(jdbc.getUrl(), not(Resources.ENV_URL));
		assumeThat(PropFileMaps.DEMO, is(true));
		demo();
	}

	public static void demo() throws JAXBException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String[] args;
		Connection connection = jdlConnection.getOpenConnection();
		SqlJdl.create(connection);
		// csv
		args = new String[] { C, CX, D, D1C, L, L1C };
		AppMain.main(args);
		SqlJdl.select(connection, true);
		SqlJdl.delete(connection);
		// xml
		args = new String[] { C, CX, D, D1X, L, L1X };
		AppMain.main(args);
		SqlJdl.select(connection, true);
		SqlJdl.delete(connection);
		// clear
		SqlJdl.drop(connection);
		jdlConnection.closeConnection();
	}
}
