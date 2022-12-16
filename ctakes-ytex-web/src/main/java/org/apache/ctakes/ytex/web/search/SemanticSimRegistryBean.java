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
package org.apache.ctakes.ytex.web.search;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class SemanticSimRegistryBean {
	String defaultConceptGraphName;
	SortedMap<String, String> semanticSimDescriptionMap;
	List<SemanticSimServiceBean> semanticSimServiceList;

	SortedMap<String, SemanticSimServiceBean> semanticSimServiceMap;

	public String getDefaultConceptGraphName() {
		return defaultConceptGraphName;
	}

	public SortedMap<String, String> getSemanticSimDescriptionMap() {
		return semanticSimDescriptionMap;
	}

	public List<SemanticSimServiceBean> getSemanticSimServiceList() {
		return semanticSimServiceList;
	}

	public SortedMap<String, SemanticSimServiceBean> getSemanticSimServiceMap() {
		return semanticSimServiceMap;
	}

	public void setSemanticSimServiceList(
			List<SemanticSimServiceBean> semanticSimServiceList) {
		this.semanticSimServiceList = semanticSimServiceList;
		if (semanticSimServiceList != null && semanticSimServiceList.size() > 0) {
			semanticSimServiceMap = new TreeMap<String, SemanticSimServiceBean>();
			semanticSimDescriptionMap = new TreeMap<String, String>();
			defaultConceptGraphName = semanticSimServiceList.get(0)
					.getConceptSimilarityService().getConceptGraphName();
			for (SemanticSimServiceBean s : semanticSimServiceList) {
				semanticSimServiceMap.put(s.getConceptSimilarityService()
						.getConceptGraphName(), s);
				semanticSimDescriptionMap.put(s.getDescription(), s
						.getConceptSimilarityService().getConceptGraphName());
			}
		}
	}

}
