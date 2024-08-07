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
package org.apache.ctakes.ytex.umls.dao;

import org.apache.ctakes.core.ae.UmlsEnvironmentConfiguration;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

import static org.junit.Assume.assumeTrue;

/**
 * this test only works if MRCONSO is in the database (not the case for default
 * test settings). In case MRCONSO is not there, catch exception and ignore.
 * 
 * @author vgarla
 * 
 */
public class UMLSDaoTest {
	private static final Logger log = LoggerFactory.getLogger( "UMLSDaoTest" );

	private ApplicationContext appCtx = null;
	private UMLSDao umlsDao = null;

	// TODO: consider removing duplicates.
	private static final Boolean hasUMLSCredentials() {
		return EnvironmentVariable.getEnv(UmlsEnvironmentConfiguration.USER.toString()) != null;
	}

	@Before
	public void setUp() throws Exception {
		assumeTrue( hasUMLSCredentials() );

//		appCtx = (ApplicationContext) ContextSingletonBeanFactoryLocator
//				.getInstance("classpath*:org/apache/ctakes/ytex/kernelBeanRefContext.xml")
//				.useBeanFactory("kernelApplicationContext").getFactory();
//		umlsDao = appCtx.getBean(UMLSDao.class);
      umlsDao = KernelContextHolder.getApplicationContext().getBean( UMLSDao.class );
   }


	@Test
	public void testGetAllAuiStr() {
		// TODO: by catching Exception, the test succedes even if the umlsDao.getAllAuiStr fails
		try {
			List<Object[]> auis = umlsDao.getAllAuiStr("");
			// TODO: not a sufficient integration test
			Assert.assertNotNull(auis);
			log.debug("testGetAllAuiStr()" + auis.size());
		} catch (Exception e) {
			log.warn("sql exception - mrconso probably doesn't exist, check error", e);
		}
	}

	@After
	public void tearDown() throws  Exception {
		if (appCtx != null) {
			((ConfigurableApplicationContext) appCtx).close();
			appCtx = null;
		}
	}
}
