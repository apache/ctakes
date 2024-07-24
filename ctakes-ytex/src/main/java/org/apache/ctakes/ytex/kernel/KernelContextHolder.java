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
package org.apache.ctakes.ytex.kernel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class KernelContextHolder {
	static ApplicationContext kernelApplicationContext = null;
	static {
		String beanRefContext = "classpath*:org/apache/ctakes/ytex/kernelBeanRefContext.xml";
//		kernelApplicationContext = (ApplicationContext) ContextSingletonBeanFactoryLocator
//				.getInstance(beanRefContext).useBeanFactory(
//						"kernelApplicationContext").getFactory();

		kernelApplicationContext
				= (ApplicationContext)SpringContextUtil.INSTANCE
				.getApplicationContext( beanRefContext )
				.getBean( "kernelApplicationContext" );

	}

	public static ApplicationContext getApplicationContext() {
		return kernelApplicationContext;
	}


	public enum SpringContextUtil {
		INSTANCE;
		private ApplicationContext _context;
		public ApplicationContext getApplicationContext( final String contextPath ) {
			if ( _context == null ) {
				_context = new ClassPathXmlApplicationContext( contextPath );
			}
			return _context;
		}
	}

}
