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
package org.apache.ctakes.jdl.data.loader;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;


import org.apache.ctakes.jdl.common.FileUtil;
import org.apache.ctakes.jdl.data.base.JdlConnection;
import org.apache.ctakes.jdl.data.loader.CsvLoader;
import org.apache.ctakes.jdl.data.xml.jaxb.ObjectFactoryUtil;
import org.apache.ctakes.jdl.schema.xdl.CsvLoadType;
import org.apache.ctakes.jdl.test.Resources;
import org.junit.BeforeClass;
import org.junit.Test;

public class CsvLoaderTest {
	private static final String CX = FileUtil.getFile(Resources.CONN_X).toString();
	private static final String D1C = FileUtil.getFile(Resources.DATA1C).toString();
	private static final String D2C = FileUtil.getFile(Resources.DATA2C).toString();
	private static final String L1C = FileUtil.getFile(Resources.LOAD1C).toString();
	private static final String L2C = FileUtil.getFile(Resources.LOAD2C).toString();
	private static final String SQL = "insert into tab_test (id,name,thekey,thevalue,code,descr) values (?,?,?,?,?,?)";

	@BeforeClass
	public static void initClass() throws JAXBException, FileNotFoundException {
		JdlConnection jdlConnection = new JdlConnection(ObjectFactoryUtil.getJdbcTypeBySrcXml(CX));
		jdlConnection.getClass();
	}

	@Test
	public void getSqlInsert() throws JAXBException, FileNotFoundException {
		CsvLoadType loader;
		CsvLoader csvLoader;
		loader = ObjectFactoryUtil.getLoadTypeBySrcXml(L1C).getCsv();
		csvLoader = new CsvLoader(loader, new File(D1C));
		assertThat(csvLoader.getSqlInsert(loader), is(SQL));
		loader = ObjectFactoryUtil.getLoadTypeBySrcXml(L2C).getCsv();
		csvLoader = new CsvLoader(loader, new File(D2C));
		assertThat(csvLoader.getSqlInsert(loader), is(SQL));
	}
}
