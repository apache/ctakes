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
package org.apache.ctakes.ytex.uima.resource;

/*
 * 
 * @author vijay
 * Copyright: (c) 2009   Mayo Foundation for Medical Education and 
 * Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
 * triple-shield Mayo logo are trademarks and service marks of MFMER.
 *
 * Except as contained in the copyright notice above, or as used to identify 
 * MFMER as the author of this software, the trade names, trademarks, service
 * marks, or product names of the copyright holder shall not be used in
 * advertising, promotion or otherwise in connection with this software without
 * prior written authorization of the copyright holder.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.apache.ctakes.core.resource.JdbcConnectionResource;
import org.apache.ctakes.ytex.uima.ApplicationContextHolder;
import org.apache.log4j.Logger;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;

/**
 * copied from mayo JdbcConnectionResourceImpl.
 * extended to set transaction isolation level.
 * <p/>
 * modified to default to settings in ytex.properties in case
 * config parameters not specified in descriptor
 * <p/>
 * remove refs to wrapped sql connection
 * 
 * @author Mayo Clinic
 */
public class InitableJdbcConnectionResourceImpl implements JdbcConnectionResource,
		SharedResourceObject
{
    private Logger iv_logger = Logger.getLogger(getClass().getName());

    /**
	 * JDBC driver ClassName.
	 */
	public static final String PARAM_DRIVER_CLASS = "DriverClassName";

	/**
	 * JDBC URL that specifies db network location and db name.
	 */
	public static final String PARAM_URL = "URL";

	/**
	 * Username for db authentication.
	 */
	public static final String PARAM_USERNAME = "Username";

	/**
	 * Password for db authentication.
	 */
	public static final String PARAM_PASSWORD = "Password";

	/**
	 * Flag that determines whether to keep JDBC connection open no matter what.
	 */
	public static final String PARAM_KEEP_ALIVE = "KeepConnectionAlive";

    /**
     * Transaction isolation level.  Value should be a static fieldname from
     * java.sql.Connection such as TRANSACTION_READ_UNCOMMITTED.  This parameter
     * is optional. 
     */
    public static final String PARAM_ISOLATION = "TransactionIsolation";    
    
	private Connection iv_conn;

	public void load(DataResource dr) throws ResourceInitializationException
	{
		ConfigurationParameterSettings cps = dr.getMetaData()
				.getConfigurationParameterSettings();

		Properties ytexProperties = ApplicationContextHolder
				.getYtexProperties();

		String driverClassName = (String) cps
				.getParameterValue(PARAM_DRIVER_CLASS);
		if (driverClassName == null)
			driverClassName = ytexProperties.getProperty("db.driver");

		String urlStr = (String) cps.getParameterValue(PARAM_URL);
		if (urlStr == null)
			urlStr = ytexProperties.getProperty("db.url");

		String username = (String) cps.getParameterValue(PARAM_USERNAME);
		if (username == null)
			username = ytexProperties.getProperty("db.username");

		String password = (String) cps.getParameterValue(PARAM_PASSWORD);
		if (password == null)
			password = ytexProperties.getProperty("db.password");

//		Boolean keepAlive = new Boolean((String) cps.getParameterValue(PARAM_KEEP_ALIVE));
        
        String isolationStr = (String) cps.getParameterValue(PARAM_ISOLATION);
        
		try
		{            
//			if (keepAlive.booleanValue())
//			{
//                iv_logger.info("Instantiating wrapped connection.");
//				iv_conn = new WrappedConnection(username,
//						password,
//						driverClassName,
//						urlStr);
//			}
//			else
//			{
				Class.forName(driverClassName);
				iv_conn = DriverManager.getConnection(
						urlStr,
						username,
						password);
//			}
//
			iv_logger.info("Connection established to: " + urlStr);
            
            if (isolationStr != null)
            {
                // use java reflection to obtain the corresponding level integer
                Class<?> connClass = Class.forName("java.sql.Connection");
                Field f = connClass.getField(isolationStr);
                int level = f.getInt(null);
                iv_logger.info("Connection transaction isolation level set: " +
                        isolationStr + "(" + level +")");
                iv_conn.setTransactionIsolation(level);
            }            
		}
		catch (Exception e)
		{
			throw new ResourceInitializationException(e);
		}
	}

	public Connection getConnection()
	{
		return iv_conn;
	}
}
