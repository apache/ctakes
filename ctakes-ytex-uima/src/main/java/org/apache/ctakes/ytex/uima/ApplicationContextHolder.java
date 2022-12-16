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
package org.apache.ctakes.ytex.uima;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * get the spring bean application context. default to the context defined in
 * org/apache/ctkaes/ytex/uima/beanRefContext.xml. this can be overriden in
 * ytex.properties, key ytex.beanRefContext
 * 
 * @author vijay
 */
public class ApplicationContextHolder {
	private static final Log log = LogFactory
			.getLog(ApplicationContextHolder.class);
	private static Properties ytexProperties;
	private static BeanFactoryLocator beanFactory;
	private static ApplicationContext ytexApplicationContext;

	static {
		InputStream ytexPropsIn = null;
		String beanRefContext = "classpath*:org/apache/ctakes/ytex/uima/beanRefContext.xml";
		try {
			log.info("loading ytex.properties from: "
					+ ApplicationContextHolder.class
							.getResource("/org/apache/ctakes/ytex/ytex.properties"));
			ytexPropsIn = ApplicationContextHolder.class
					.getResourceAsStream("/org/apache/ctakes/ytex/ytex.properties");

			ytexProperties = new Properties();
			ytexProperties.load(ytexPropsIn);
			ytexProperties.putAll(System.getProperties());
			beanRefContext = ytexProperties.getProperty("ytex.beanRefContext",
					beanRefContext);
			if (log.isInfoEnabled())
				log.info("beanRefContext=" + beanRefContext);
			beanFactory = ContextSingletonBeanFactoryLocator
					.getInstance(beanRefContext);
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

	public static ApplicationContext getApplicationContext() {
		return (ApplicationContext)beanFactory.useBeanFactory(
				"ytexApplicationContext").getFactory();
	}

	public static Properties getYtexProperties() {
		return ytexProperties;
	}

}
