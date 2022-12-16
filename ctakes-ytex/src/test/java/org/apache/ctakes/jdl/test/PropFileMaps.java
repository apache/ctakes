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
package org.apache.ctakes.jdl.test;

import java.util.Properties;

import org.apache.ctakes.jdl.common.PropFile;


/**
 * Access to maps.properties file.
 * 
 * @author mas
 */
public class PropFileMaps {
	private static final boolean INTERNAL = true;
	private static final String SRC_FILE = "/maps.properties";
	private static Properties properties = PropFile.getProperties(SRC_FILE, INTERNAL);

	/**
	 * @param property
	 *            the property to get
	 * @return
	 */
	public static final String getProperty(final String property) {
		return PropFile.getProperty(properties, property);
	}

	/**
	 * Reset the properties.
	 */
	public static final void reset() {
		properties = PropFile.getProperties(SRC_FILE, INTERNAL);
	}

	public static final String ID = getProperty("id");
	public static final String KEY = getProperty("key");

	// If true you can use: mvn clean test -P jdbc,h2
	public static final boolean DEMO = Boolean.parseBoolean(getProperty("demo"));
}
