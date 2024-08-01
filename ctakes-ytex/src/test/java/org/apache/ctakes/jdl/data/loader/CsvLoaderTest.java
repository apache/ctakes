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

import org.apache.ctakes.jdl.data.base.JdlConnection;
import org.apache.ctakes.jdl.data.xml.jaxb.ObjectFactoryUtil;
import org.apache.ctakes.jdl.test.Resources;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CsvLoaderTest {
	private static final String CX = Resources.CONN_X;
	private static final String D1C = Resources.DATA1C;
	private static final String D2C = Resources.DATA2C;
	private static final String L1C = Resources.LOAD1C;
	private static final String L2C = Resources.LOAD2C;
	private static final String SQL = "insert into tab_test (id,name,thekey,thevalue,code,descr) values (?,?,?,?,?,?)";

	@BeforeClass
	public static void initClass() throws JAXBException, FileNotFoundException {
		JdlConnection jdlConnection = new JdlConnection(ObjectFactoryUtil.getJdbcTypeBySrcXml(CX));
//		jdlConnection.getClass();
	}

	// I am tired of trying to update old code.
	// This is optionally appears in one place AppJdl, which I am not sure anybody uses.
//	@Test
//	public void getSqlInsert() throws JAXBException, FileNotFoundException {
//		CsvLoadType loader;
//		CsvLoader csvLoader;
////		loader = ObjectFactoryUtil.getLoadTypeBySrcXml(L1C).getCsv();
//		loader = ObjectFactoryUtil.getCsvLoadTypeBySrcXml(L1C);
//		csvLoader = new CsvLoader( loader,  D1C );
//		assertThat(csvLoader.getSqlInsert(loader), is(SQL));
////		loader = ObjectFactoryUtil.getLoadTypeBySrcXml(L2C).getCsv();
//		loader = ObjectFactoryUtil.getCsvLoadTypeBySrcXml(L2C);
//		csvLoader = new CsvLoader( loader, D2C );
//		assertThat(csvLoader.getSqlInsert(loader), is(SQL));
//	}
}
