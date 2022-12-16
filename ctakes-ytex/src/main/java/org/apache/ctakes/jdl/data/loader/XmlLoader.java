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
package org.apache.ctakes.jdl.data.loader;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;


import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.apache.commons.lang.StringUtils;
import org.apache.ctakes.jdl.data.base.JdlConnection;
import org.apache.ctakes.jdl.data.xml.DomUtil;
import org.apache.ctakes.jdl.schema.xdl.XmlLoadType;
import org.apache.ctakes.jdl.schema.xdl.XmlLoadType.Column;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Loader of XML file.
 * 
 * @author mas
 */
public class XmlLoader extends Loader {
	private JXPathContext context;
	private XmlLoadType loader;

	/**
	 * @param loader
	 *            the loader
	 * @param document
	 *            the document
	 */
	public XmlLoader(final XmlLoadType loader, final Document document) {
		context = JXPathContext.newContext(document);
		this.loader = loader;
	}

	/**
	 * @param loader
	 *            the xml to manage
	 * @return the sql string
	 */
	public final String getSqlInsert(final XmlLoadType loader) {
		String query = "insert into " + loader.getTable() + " (";
		String values = ") values (";
		for (Column column : loader.getColumn()) {
			query += column.getName() + ",";
			values += "?,";
		}
		return StringUtils.removeEnd(query, ",") + StringUtils.removeEnd(values, ",") + ")";
	}

	/**
	 * @param jdlConnection
	 *            the jdlConnection to manage
	 */
	@Override
	public final void dataInsert(final JdlConnection jdlConnection) {
		String sql = getSqlInsert(loader);
		Number ncommit = loader.getCommit();
		int r = 0;
		try {
			Iterator<?> iterator = context.iteratePointers(loader.getXroot());
			while (iterator.hasNext()) {
				r++;
				NodePointer pointer = (NodePointer) iterator.next();
				Node node = (Node) pointer.getNode();
				JXPathContext context = JXPathContext.newContext(DomUtil.nodeToDocument(node));
				try {
					int c = 0;
					PreparedStatement preparedStatement = jdlConnection.getOpenConnection().prepareStatement(sql);
					if (ncommit == null) {
						jdlConnection.setAutoCommit(true);
					} else {
						jdlConnection.setAutoCommit(false);
					}
					for (Column column : loader.getColumn()) {
						c++;
						Object value = column.getConstant();
						if (value == null) {
							if (column.getSeq() != null) {
								value = r + column.getSeq().intValue();
							} else if (column.getXpath() != null) {
								value = this.context.getValue(column.getXpath());
							} else {
								value = context.getPointer(column.getXleaf()).getValue();
							}
						}
						preparedStatement.setObject(c, value);
					}
					executeBatch(preparedStatement);
					if (!jdlConnection.isAutoCommit() && (r % ncommit.intValue() == 0)) {
						jdlConnection.commitConnection();
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			if (!jdlConnection.isAutoCommit()) {
				jdlConnection.commitConnection();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
