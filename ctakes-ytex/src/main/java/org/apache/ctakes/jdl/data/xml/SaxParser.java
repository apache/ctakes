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
import java.net.MalformedURLException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * To parse SAX.
 * 
 * @author mas
 */
public class SaxParser extends DefaultHandler {
	private String srcXml;

	/**
	 * @param srcXml
	 *            the srcXml
	 */
	public SaxParser(final String srcXml) {
		setSrcXml(srcXml);
	}

	/**
	 * SAX Parser.
	 */
	public final void parse() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);
		SAXParser parser;
		try {
			parser = factory.newSAXParser();
			parser.parse(srcXml, this);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @return the srcXml
	 */
	public final String getSrcXml() {
		return srcXml;
	}

	/**
	 * @param srcXml
	 *            the srcXml to set
	 */
	public final void setSrcXml(final String srcXml) {
		this.srcXml = srcXml;
		parse();
	}

	/**
	 * @throws ParserConfigurationException
	 *             exception
	 * @throws SAXException
	 *             exception
	 * @throws MalformedURLException
	 *             exception
	 */
	public final void parse(String srcXsd) throws ParserConfigurationException, SAXException, MalformedURLException {
		SAXParserFactory saxfactory = SAXParserFactory.newInstance();
		saxfactory.setNamespaceAware(true);
		// saxfactory.setValidating(true);

		SAXParser parser = saxfactory.newSAXParser();
		XMLReader reader = parser.getXMLReader();

		reader.setFeature(NAMESPACES_FEATURE_ID, false);
		reader.setFeature(NAMESPACE_PREFIXES_FEATURE_ID, false);
		reader.setFeature(VALIDATION_FEATURE_ID, true);
		reader.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
		reader.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, false);
		reader.setFeature(DYNAMIC_VALIDATION_FEATURE_ID, false);
		// reader.setContentHandler(handler);
		// reader.setErrorHandler(handler);
		reader.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", new File(srcXsd).toURI().toURL().toString());
		// reader.parse(inp);
	}

	/** Namespaces feature id (http://xml.org/sax/features/namespaces). */
	protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";

	/**
	 * Namespace prefixes feature id
	 * (http://xml.org/sax/features/namespace-prefixes).
	 */
	protected static final String NAMESPACE_PREFIXES_FEATURE_ID = "http://xml.org/sax/features/namespace-prefixes";

	/** Validation feature id (http://xml.org/sax/features/validation). */
	protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

	/**
	 * Schema validation feature id
	 * (http://apache.org/xml/features/validation/schema).
	 */
	protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

	/**
	 * Schema full checking feature id
	 * (http://apache.org/xml/features/valid...full-checking).
	 */
	protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";

	/**
	 * Dynamic validation feature id
	 * (http://apache.org/xml/features/validation/dynamic).
	 */
	protected static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";

	/**
	 * Schema nons location id.
	 */
	protected static final String SCHEMA_NONS_LOCATION_ID = "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
}
