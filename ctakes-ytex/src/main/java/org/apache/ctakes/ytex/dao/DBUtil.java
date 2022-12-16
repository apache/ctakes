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
package org.apache.ctakes.ytex.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Oracle differs from sql server & mysql in handling of empty string. In
 * oracle, empty string is null, in other platforms, empty string is non-null.
 * We need a stand-in for the empty string in oracle; we use the string with one
 * blank in its place.
 * 
 * @author vijay
 * 
 */
public class DBUtil {

	private static final Log log = LogFactory.getLog(DBUtil.class);
	private static Properties ytexProperties;
	private static boolean oracle;
	private static boolean mysql;
	private static boolean mssql;
	private static boolean hsql;
	private static String escapeBegin;
	private static String escapeEnd;

	static {
		InputStream ytexPropsIn = null;
		try {
			ytexPropsIn = DBUtil.class.getResourceAsStream("/org/apache/ctakes/ytex/ytex.properties");
			ytexProperties = new Properties();
			ytexProperties.load(ytexPropsIn);
			oracle = "orcl".equals(ytexProperties.getProperty("db.type"));
			mysql = "mysql".equals(ytexProperties.getProperty("db.type"));
			hsql = "hsql".equals(ytexProperties.getProperty("db.type"));
			
			if (mssql) {
				escapeBegin = "[";
				escapeEnd = "]";
			} else if (mysql) {
				escapeBegin = "`";
				escapeEnd = "`";
			} else  {
				escapeBegin = "";
				escapeEnd = "";
			} 
		} catch (Exception e) {
			log.error("initalizer", e);
		} finally {
			if (ytexPropsIn != null) {
				try {
					ytexPropsIn.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static String getYTEXTablePrefix() {
		if (mssql)
			return ytexProperties.getProperty("db.schema", "dbo") + ".";
		else
			return "";
	}

	public static String getEmptyString() {
		if (oracle)
			return " ";
		else
			return "";
	}

	public static String getEscapeBegin() {
		return escapeBegin;
	}

	public static String getEscapeEnd() {
		return escapeEnd;
	}

	public static String nullToEmptyString(String param) {
		if (param == null)
			return getEmptyString();
		else
			return param;
	}

	public static String formatFieldName(String fieldName) {
		return getEscapeBegin() + fieldName + getEscapeEnd();
	}

	public static String formatTableName(String tableName) {
		return getEscapeBegin()
				+ tableName.replaceAll("\\.", getEscapeEnd() + '.'
						+ getEscapeBegin()) + getEscapeEnd();
	}
}
