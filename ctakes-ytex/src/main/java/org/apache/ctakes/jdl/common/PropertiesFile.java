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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Access to a properties file.
 * 
 * @author mas
 */
public class PropertiesFile {
	private Properties properties;

	/**
	 * External.
	 * 
	 * @param srcFile
	 *            the srcFile to load
	 * @throws IOException
	 *             exception
	 */
	public final void loadInputStream(final String srcFile) throws IOException {
		properties = new Properties();
		properties.load(new FileInputStream(srcFile));
	}

	/**
	 * Internal.
	 * 
	 * @param srcFile
	 *            the srcFile to load
	 * @throws IOException
	 *             exception
	 */
	public final void loadResourceAsStream(final String srcFile) throws IOException {
		properties = new Properties();
		properties.load(PropertiesFile.class.getResourceAsStream(srcFile));
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public final void setProperties(final Properties properties) {
		this.properties = properties;
	}

	/**
	 * @param property
	 *            the property to get
	 * @return the property
	 */
	public final String getProperty(final String property) {
		if (properties != null) {
			return properties.getProperty(property);
		}
		return null;
	}

	/**
	 * Clears this hashtable.
	 */
	public final void clear() {
		this.properties.clear();
	}
}
