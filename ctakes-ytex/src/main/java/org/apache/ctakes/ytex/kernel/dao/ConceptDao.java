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
package org.apache.ctakes.ytex.kernel.dao;

import java.io.IOException;
import java.util.Set;

import org.apache.ctakes.ytex.kernel.model.ConceptGraph;

/**
 * create/retrieve concept graphs. store concept graph on file system as they
 * can get big (>10MB). This is not a problem for sql server/oracle, but may
 * require increasing the max_packet_size on mysql.
 * 
 * @author vijay
 * 
 */
public interface ConceptDao {

	/**
	 * retrieve an existing concept graph. This attempts to find the concept
	 * graph as follows:
	 * <ul>
	 * <li>classpath: attempt to load
	 * org/apache/ctakes/ytex/conceptGraph/[name].gz</li>
	 * <li>relative to org.apache.ctakes.ytex.conceptGraphDir: if that fails
	 * then relative to the directory defined by the system property/ytex
	 * property org.apache.ctakes.ytex.conceptGraphDir</li>
	 * <li>relative to ytex.properties: if the org...conceptGraphDir property is
	 * not defined, then look for [directory of
	 * ytex.properties]/conceptGraph/[name].gz</li>
	 * </ul>
	 * 
	 * @param name
	 *            name of concept graph. Will retrieve from classpath/file
	 *            system. @see #createConceptGraph
	 * @return
	 */
	public abstract ConceptGraph getConceptGraph(String name);

	/**
	 * create the concept graph with specified name using specified query. Will
	 * create a file [name].gz under dir.
	 * 
	 * @param dir
	 *            directory of concept graph (optional). If null will use
	 *            directory specified by the system property/ytex property
	 *            org.apache.ctakes.ytex.conceptGraphDir. If the property is not
	 *            defined, use [directory of ytex.properties]/conceptGraph
	 * @param name
	 *            name of concept graph
	 * @param query
	 *            returns 2 string columns, 1st column is the child concept, 2nd
	 *            column is the parent concept.
	 * @param checkCycle
	 *            if true will check for cycles and remove them (default true).
	 *            If this is set to false, then only pagerank can be used on
	 *            this graph.
	 * @param forbiddenConcepts
	 *            set of concepts whose edges will not be added to the concept
	 *            graph (optional). By default, this includes umls concepts like
	 *            C1274012 (Ambiguous concept).
	 * @return ConceptGraph the generated concept graph
	 */
	public abstract void createConceptGraph(String dir, String name,
			String query, final boolean checkCycle,
			final Set<String> forbiddenConcepts) throws IOException;

}