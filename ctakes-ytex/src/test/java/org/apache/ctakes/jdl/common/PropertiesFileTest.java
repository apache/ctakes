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
package org.apache.ctakes.jdl.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Properties;


import org.apache.ctakes.jdl.common.FileUtil;
import org.apache.ctakes.jdl.common.PropertiesFile;
import org.apache.ctakes.jdl.test.Resources;
import org.junit.After;
import org.junit.Test;

public class PropertiesFileTest {
	private static final PropertiesFile propertiesFile = new PropertiesFile();

	@Test
	public void loadInputStream() throws IOException {
		propertiesFile.loadInputStream(FileUtil.getFile(Resources.MAPS_P).toString());
		assertThat(propertiesFile.getProperty(Resources.MAP_ID), is(Resources.MAP_NAME));
		assertThat(propertiesFile.getProperty(Resources.MAP_KEY), is(Resources.MAP_VALUE));
	}

	@Test(expected = IOException.class)
	public void loadInputStreamExecption() throws IOException {
		propertiesFile.loadInputStream("/" + Resources.MAPS_P);
	}

	@Test
	public void loadResourceAsStream() throws IOException {
		propertiesFile.loadResourceAsStream("/" + Resources.MAPS_P);
		assertThat(propertiesFile.getProperty(Resources.MAP_ID), is(Resources.MAP_NAME));
		assertThat(propertiesFile.getProperty(Resources.MAP_KEY), is(Resources.MAP_VALUE));
	}

	@Test(expected = NullPointerException.class)
	public void loadResourceAsStreamExecption() throws IOException {
		propertiesFile.loadResourceAsStream(FileUtil.getFile(Resources.MAPS_P).toString());
	}

	@Test
	public void property() {
		Properties properties = new Properties();
		properties.setProperty("code", "descr");
		propertiesFile.setProperties(properties);
		assertThat(propertiesFile.getProperty("code"), is("descr"));
	}

	@After
	public void clear() {
		propertiesFile.clear();
		assertThat(propertiesFile.getProperty(Resources.MAP_ID), nullValue());
		assertThat(propertiesFile.getProperty(Resources.MAP_KEY), nullValue());
		assertThat(propertiesFile.getProperty("code"), nullValue());
	}
}
