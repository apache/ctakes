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
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;


import org.apache.commons.lang.SystemUtils;
import org.apache.ctakes.jdl.common.FileUtil;
import org.apache.ctakes.jdl.test.Resources;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class FileUtilTest {
	@DataPoint
	public static String CX = Resources.CONN_X;
	@DataPoint
	public static String L1C = Resources.LOAD1C;
	@DataPoint
	public static String L2C = Resources.LOAD2C;
	@DataPoint
	public static String L1X = Resources.LOAD1X;
	@DataPoint
	public static String L2X = Resources.LOAD2X;

	@Test
	public void getJavaClassPaths() {
		assertThat(FileUtil.getJavaClassPaths(), notNullValue());
	}

	@Theory
	public void getFile(String fileName) {
		assertThat(FileUtil.getFile(fileName), notNullValue());
	}

	@Theory
	public void getCanonical(String fileName) throws IOException {
		assertThat(FileUtil.getCanonical(null, SystemUtils.USER_HOME), is(SystemUtils.USER_HOME));
		assertThat(FileUtil.getCanonical(null), is(SystemUtils.USER_DIR));
		assertThat(FileUtil.getCanonical(new File(fileName)), is(new File(fileName).getCanonicalPath()));
	}

	@Theory
	public void fullPath(String fileName) throws IOException {
		for (String token : FileUtil.getJavaClassPaths()) {
			assertThat(FileUtil.fullPath(new File(token), fileName), is(new File(token + SystemUtils.FILE_SEPARATOR + fileName).getCanonicalPath()));
		}
	}
}
