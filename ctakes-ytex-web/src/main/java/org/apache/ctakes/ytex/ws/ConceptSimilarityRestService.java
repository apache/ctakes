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
package org.apache.ctakes.ytex.ws;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.ctakes.ytex.kernel.metric.ConceptPairSimilarity;

@Path("/rest/")
@Produces("application/xml")
public interface ConceptSimilarityRestService {
	
	@GET
	@Path("/getDefaultConceptGraph")
	public SimServiceInfo getDefaultConceptGraph();
	
	@GET
	@Path("/getConceptGraphs")
	public List<SimServiceInfo> getConceptGraphs();


	@GET
	@Path("/similarity")
	public ConceptPairSimilarity similarity(
			@QueryParam("conceptGraph") String conceptGraph,
			@QueryParam("concept1") String concept1,
			@QueryParam("concept2") String concept2,
			@QueryParam("metrics") String metrics,
			@QueryParam("lcs") String lcs);
}
