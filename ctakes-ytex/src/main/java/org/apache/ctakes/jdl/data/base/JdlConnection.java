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
package org.apache.ctakes.jdl.data.base;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;


import org.apache.commons.lang.StringUtils;
import org.apache.ctakes.jdl.schema.xdl.JdbcType;

/**
 * Connection with db.
 * 
 * @author mas
 */
public class JdlConnection {
	private String driver;
	private String url;
	private String user;
	private String password;
	private Connection connection;

	/**
	 * @param jdbc
	 *            the jdbc
	 */
	public JdlConnection(final JdbcType jdbc) {
		driver = StringUtils.defaultIfEmpty(jdbc.getDriver(), driver);
		url = StringUtils.defaultIfEmpty(jdbc.getUrl(), url);
		user = StringUtils.defaultIfEmpty(jdbc.getUsername(), user);
		password = StringUtils.defaultIfEmpty(jdbc.getPassword(), password);
	}

	/**
	 * @return the connected
	 * @throws SQLException
	 *             exception
	 */
	public final boolean isConnected() throws SQLException {
		return (connection != null && !connection.isClosed());
	}

	/**
	 * Attempts to establish a connection.
	 * 
	 * @throws InstantiationException
	 *             exception
	 * @throws IllegalAccessException
	 *             exception
	 * @throws ClassNotFoundException
	 *             exception
	 * @throws SQLException
	 *             exception
	 */
	private void openConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		// Class.forName(driver);
		DriverManager.registerDriver((Driver) Class.forName(driver).newInstance());
		connection = DriverManager.getConnection(url, user, password);
	}

	/**
	 * @return the openConnection
	 * @throws InstantiationException
	 *             exception
	 * @throws IllegalAccessException
	 *             exception
	 * @throws ClassNotFoundException
	 *             exception
	 * @throws SQLException
	 *             exception
	 */
	public final Connection getOpenConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		if (!isConnected()) {
			openConnection();
		}
		return connection;
	}

	/**
	 * Makes all changes made since the previous commit/rollback permanent and
	 * releases any database locks currently held by this Connection object.
	 * 
	 * @throws SQLException
	 *             exception
	 */
	public final void commitConnection() throws SQLException {
		if (isConnected() && !connection.getAutoCommit()) {
			connection.commit();
		}
	}

	/**
	 * Undoes all changes made in the current transaction and releases any
	 * database locks currently held by this Connection object.
	 * 
	 * @throws SQLException
	 *             exception
	 */
	public final void rollbackConnection() throws SQLException {
		if (isConnected() && !connection.getAutoCommit()) {
			connection.rollback();
		}
	}

	/**
	 * Releases this Connection object's database.
	 * 
	 * @throws SQLException
	 *             exception
	 */
	public final void closeConnection() throws SQLException {
		if (isConnected()) {
			connection.close();
		}
	}

	/**
	 * @return the autoCommit
	 * @throws SQLException
	 *             exception
	 */
	public final boolean isAutoCommit() throws SQLException {
		return (isConnected()) ? connection.getAutoCommit() : false;
	}

	/**
	 * @param autoCommit
	 *            the autoCommit to set
	 * @throws SQLException
	 *             exception
	 */
	public final void setAutoCommit(final boolean autoCommit) throws SQLException {
		if (isConnected()) {
			connection.setAutoCommit(autoCommit);
		}
	}
}
