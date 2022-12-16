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
package org.apache.ctakes.jdl.data.xml.jaxb;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;


import org.apache.ctakes.jdl.common.FileUtil;
import org.apache.ctakes.jdl.data.xml.jaxb.ObjectFactoryUtil;
import org.apache.ctakes.jdl.schema.xdl.ConnType;
import org.apache.ctakes.jdl.schema.xdl.JdbcType;
import org.apache.ctakes.jdl.schema.xdl.LoadType;
import org.apache.ctakes.jdl.test.Resources;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public final class ObjectFactoryUtilTest {
	private static final String CX = FileUtil.getFile(Resources.CONN_X).toString();
	@DataPoint
	public static String L1C = Resources.LOAD1C;
	@DataPoint
	public static String L1X = Resources.LOAD1X;
	@DataPoint
	public static String L2C = Resources.LOAD2C;
	@DataPoint
	public static String L2X = Resources.LOAD1X;

	public void getJdbcTypeBySrcXml() throws JAXBException {
		Object obj = ObjectFactoryUtil.getJdbcTypeBySrcXml(CX);
		assertThat(obj, instanceOf(JdbcType.class));
	}

	@Test(expected = UnmarshalException.class)
	public void getJdbcTypeByStrXml() throws JAXBException {
		ObjectFactoryUtil.getJdbcTypeByStrXml("<root />");
	}

	public void getConnTypeBySrcXml() throws JAXBException {
		Object obj = ObjectFactoryUtil.getJdbcTypeBySrcXml(CX);
		assertThat(obj, instanceOf(ConnType.class));
	}

	@Test(expected = UnmarshalException.class)
	public void getConnTypeByStrXml() throws JAXBException {
		ObjectFactoryUtil.getConnTypeByStrXml("<root />");
	}

	@Theory
	public void getBindTypeBySrcXml(String xml) throws JAXBException {
		xml = FileUtil.getFile(xml).toString();
		Object obj = ObjectFactoryUtil.getLoadTypeBySrcXml(xml);
		assertThat(obj, instanceOf(LoadType.class));
	}

	@Test(expected = UnmarshalException.class)
	public void getBindTypeByStrXml() throws JAXBException {
		ObjectFactoryUtil.getJdbcTypeByStrXml("<root />");
	}
}
