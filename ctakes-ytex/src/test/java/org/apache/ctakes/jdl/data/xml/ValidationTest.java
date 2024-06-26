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
package org.apache.ctakes.jdl.data.xml;

import org.apache.ctakes.jdl.AppJdl;
import org.apache.ctakes.jdl.common.FileUtil;
import org.apache.ctakes.jdl.test.Resources;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import javax.xml.validation.Schema;
import java.net.URL;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class ValidationTest {
	private static final URL XSD = AppJdl.XSD;
	private static Validation validation;
	@DataPoint
	public static String CX = Resources.CONN_X;
	@DataPoint
	public static String L1C = Resources.LOAD1C;
	@DataPoint
	public static String L1X = Resources.LOAD1X;
	@DataPoint
	public static String L2C = Resources.LOAD2C;
	@DataPoint
	public static String L2X = Resources.LOAD2X;

	@BeforeClass
	public static void initClass() {
		Schema schema = SchemaUtil.urlToSchema(XSD);
		validation = new Validation(schema);
	}

	@Test
	public void setSchema() {
		validation.setSchema( Objects.requireNonNull( SchemaUtil.urlToSchema( XSD ) ) );
		assertThat(validation.getError(), nullValue());
	}

	@Theory
	public void setDocument(String xml) {
		xml = Objects.requireNonNull( FileUtil.getFile( xml ) ).getPath();
		validation.setDocument(DomUtil.srcToDocument(xml));
		assertThat(validation.succeed(), is(true));
		validation.setDocument(xml);
		assertThat(validation.succeed(), is(true));
	}

	@After
	public void getError() {
		assertThat(validation.getError(), nullValue());
	}

	@Theory
	public void succeed(String xml) {
		xml = Objects.requireNonNull( FileUtil.getFile( xml ) ).getPath();
		validation.setDocument(DomUtil.srcToDocument(xml));
		assertThat(validation.succeed(), is(true));
		validation.setDocument(xml);
		assertThat(validation.succeed(), is(true));
	}
}
