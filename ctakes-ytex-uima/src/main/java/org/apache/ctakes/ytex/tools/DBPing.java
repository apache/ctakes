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
package org.apache.ctakes.ytex.tools;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/*
 * verify db parameters.  takes as input property file
 */
public class DBPing {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		InputStream is = null;
		try {
			is = new FileInputStream(args[0]);
			Properties props = new Properties();
			props.load(is);
			System.exit(ping(props));
		} catch (Exception e) {
			e.printStackTrace();
			System.out
					.println("DBPing: Connection to db failed - please check your settings and try again");
			System.exit(1);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public static int ping(Properties props) throws Exception {
		if (props.getProperty("db.driver") == null) {
			System.out.println("DBPing: db.driver not defined");
			return 1;
		}
		if (props.getProperty("db.url") == null) {
			System.out.println("DBPing: db.url not defined");
			return 1;
		}
		Class.forName(props.getProperty("db.driver"));
		Connection c = null;
		try {
			c = DriverManager.getConnection(props.getProperty("db.url"),
					props.getProperty("db.username"),
					props.getProperty("db.password"));
			System.out.println("DBPing: connection succeeded");
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (Exception e) {
				}
			}
		}
		return 0;
	}

}
