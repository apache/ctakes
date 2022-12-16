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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 * Utility to manage Schema.
 * 
 * @author mas
 */
public final class SchemaUtil {
	private SchemaUtil() {
	}

	/**
	 * @param srcXsd
	 *            the srcXsd to convert
	 * @return the schema
	 */
	public static Schema srcToSchema(final String srcXsd) {
		SchemaFactory factory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		try {
			File f = new File(srcXsd);
			if (f.exists())
				return factory.newSchema(new File(srcXsd));
			else {
				InputStream is = null;
				try {
					is = SchemaUtil.class.getClassLoader().getResourceAsStream(
							srcXsd);
					return factory.newSchema(new StreamSource(is));
				} finally {
					if (is != null)
						is.close();
				}
			}

		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param url
	 *            the url to convert
	 * @return the schema
	 */
	public static Schema urlToSchema(final URL url) {
		SchemaFactory factory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			return factory.newSchema(url);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param strXsd
	 *            the strXsd to convert
	 * @return the schema
	 */
	public static Schema strToSchema(final String strXsd) {
		SchemaFactory factory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			return factory
					.newSchema(new StreamSource(new StringReader(strXsd)));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
