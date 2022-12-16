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
package org.apache.ctakes.jdl;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.sql.SQLException;

import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.jdl.data.base.JdlConnection;
import org.apache.ctakes.jdl.data.loader.CsvLoader;
import org.apache.ctakes.jdl.data.loader.XmlLoader;
import org.apache.ctakes.jdl.data.xml.DomUtil;
import org.apache.ctakes.jdl.data.xml.SchemaUtil;
import org.apache.ctakes.jdl.data.xml.Validation;
import org.apache.ctakes.jdl.data.xml.jaxb.ObjectFactoryUtil;
import org.apache.ctakes.jdl.schema.xdl.CsvLoadType;
import org.apache.ctakes.jdl.schema.xdl.JdbcType;
import org.apache.ctakes.jdl.schema.xdl.LoadType;
import org.apache.ctakes.jdl.schema.xdl.XmlLoadType;

/**
 * Java data loader Application.
 * 
 * @author mas
 */
public class AppJdl {
	private static final Log log = LogFactory.getLog(AppJdl.class);
	private String srcConn;
	private String srcData;
	private String srcLoad;
	public static final URL XSD = AppJdl.class
			.getResource("/org/apache/ctakes/jdl/xdl.xsd");

	/**
	 * @param srcConn
	 *            the conn file
	 * @param srcData
	 *            the data file
	 * @param srcLoad
	 *            the load file
	 */
	public AppJdl(String srcConn, String srcData, String srcLoad) {
		this.srcConn = srcConn;
		this.srcData = srcData;
		this.srcLoad = srcLoad;
	}

	/**
	 * Execute the loader of the data into the database.
	 */
	public void execute() {
		Validation validation = new Validation(SchemaUtil.urlToSchema(XSD),
				srcConn);
		if (validation.succeed()) {
			validation.setDocument(srcLoad);
			if (validation.succeed()) {
				JdlConnection jdlConnection = null;
				try {
					JdbcType jdbc = ObjectFactoryUtil
							.getJdbcTypeBySrcXml(srcConn);
					LoadType load = ObjectFactoryUtil
							.getLoadTypeBySrcXml(srcLoad);
					jdlConnection = new JdlConnection(jdbc);
					CsvLoadType csv = load.getCsv();
					if (csv != null) {
						try {
							CsvLoader csvLoader = new CsvLoader(csv, new File(
									srcData));
							csvLoader.dataInsert(jdlConnection);
						} catch (FileNotFoundException e) {
							throw new RuntimeException(e);
						}
					}
					XmlLoadType xml = load.getXml();
					if (xml != null) {
						XmlLoader xPathParsing = new XmlLoader(xml,
								DomUtil.srcToDocument(srcData));
						xPathParsing.dataInsert(jdlConnection);
					}
				} catch (JAXBException e) {
					e.printStackTrace();
				} finally {
					try {
						if (jdlConnection != null)
							jdlConnection.closeConnection();
					} catch (SQLException e) {
						log.error("closing connection", e);
					}
				}
			} else {
				System.err.println(validation.getError());
			}
		} else {
			System.err.println(validation.getError());
		}
	}
}
