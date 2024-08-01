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

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;


public class XmlLoaderTest {
	private static final String CX = Resources.CONN_X;
	private static final String D1X = Resources.DATA1X;
	private static final String D2X = Resources.DATA2X;
	private static final String L1X = Resources.LOAD1X;
	private static final String L2X = Resources.LOAD2X;
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
//		XmlLoadType loader;
//		XmlLoader xmlLoader;
////		loader = ObjectFactoryUtil.getLoadTypeBySrcXml(L1X).getXml();
//		loader = ObjectFactoryUtil.getXmlLoadTypeBySrcXml(L1X);
//		xmlLoader = new XmlLoader(loader, DomUtil.srcToDocument(D1X));
//		assertThat(xmlLoader.getSqlInsert(loader), is(SQL));
////		loader = ObjectFactoryUtil.getLoadTypeBySrcXml(L2X).getXml();
//		loader = ObjectFactoryUtil.getXmlLoadTypeBySrcXml(L2X);
//		xmlLoader = new XmlLoader(loader, DomUtil.srcToDocument(D2X));
//		assertThat(xmlLoader.getSqlInsert(loader), is(SQL));
//	}
}
