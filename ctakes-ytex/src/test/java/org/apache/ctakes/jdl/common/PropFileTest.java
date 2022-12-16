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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Properties;


import org.apache.ctakes.jdl.common.FileUtil;
import org.apache.ctakes.jdl.common.PropFile;
import org.apache.ctakes.jdl.test.PropFileMaps;
import org.apache.ctakes.jdl.test.Resources;
import org.junit.Test;

public class PropFileTest {
	@Test
	public void getProperties() {
		Properties properties = null;
		assertThat(properties, nullValue());
		properties = PropFile.getProperties("/" + Resources.MAPS_P, true);
		assertThat(properties, notNullValue());
		assertThat(properties.getProperty(Resources.MAP_ID), is(Resources.MAP_NAME));
		assertThat(properties.getProperty(Resources.MAP_KEY), is(Resources.MAP_VALUE));
		properties = PropFile.getProperties(FileUtil.getFile(Resources.MAPS_P).toString(), false);
		assertThat(properties, notNullValue());
		assertThat(properties.getProperty(Resources.MAP_ID), is(Resources.MAP_NAME));
		assertThat(properties.getProperty(Resources.MAP_KEY), is(Resources.MAP_VALUE));
	}

	@Test
	public void getProperty() {
		assertThat(PropFileMaps.getProperty(Resources.MAP_ID), is(Resources.MAP_NAME));
		assertThat(PropFileMaps.getProperty(Resources.MAP_KEY), is(Resources.MAP_VALUE));
		assertThat(PropFileMaps.getProperty("code"), nullValue());
		assertThat(PropFileMaps.getProperty("descr"), nullValue());
	}
}
