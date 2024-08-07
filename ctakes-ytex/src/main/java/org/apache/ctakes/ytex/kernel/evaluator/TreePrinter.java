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
package org.apache.ctakes.ytex.kernel.evaluator;

import org.apache.ctakes.ytex.kernel.tree.InstanceTreeBuilder;
import org.apache.ctakes.ytex.kernel.tree.Node;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.Map;


public class TreePrinter {

	public static void main(String args[]) throws IOException, ClassNotFoundException {
		String beanRefContext = "classpath*:org/apache/ctakes/ytex/kernelBeanRefContext.xml";
		String contextName = "kernelApplicationContext";
//		ApplicationContext appCtx = (ApplicationContext) ContextSingletonBeanFactoryLocator
//				.getInstance(beanRefContext)
//				.useBeanFactory(contextName).getFactory();

		ApplicationContext appCtx
				= (ApplicationContext)SpringContextUtil.INSTANCE
				.getApplicationContext( beanRefContext )
				.getBean( contextName );

		ApplicationContext appCtxSource = appCtx;
		InstanceTreeBuilder builder = appCtxSource.getBean(
				"instanceTreeBuilder", InstanceTreeBuilder.class);
		Map<Long, Node> instanceMap = builder.loadInstanceTrees(args[0]);
		for(Node node : instanceMap.values())
			printTree(node, 0);
	}

	private static void printTree(Node node, int depth) {
		for(int i = 0; i<= depth; i++) {
			System.out.print("  ");
		}
		System.out.println(node);
		for(Node child : node.getChildren()) {
			printTree(child, depth+1);
		}
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
