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
import java.util.Map;

import javax.faces.event.ActionEvent;


/**
 * Jsf bean for full text search.
 * 
 * 
 * @author vijay
 *
 */
public class FullTextSearchBean {
	private String searchTerm;
	private List<Map<String,Object>> searchResultList;
	private DocumentSearchService documentSearchService;
	
	public String getSearchTerm() {
		return searchTerm;
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}

	public List<Map<String, Object>> getSearchResultList() {
		return searchResultList;
	}

	public void setSearchResultList(List<Map<String, Object>> searchResultList) {
		this.searchResultList = searchResultList;
	}

	public DocumentSearchService getDocumentSearchService() {
		return documentSearchService;
	}

	public void setDocumentSearchService(DocumentSearchService documentSearchService) {
		this.documentSearchService = documentSearchService;
	}

	public void searchListen(ActionEvent event) {
		if(searchTerm != null && searchTerm.trim().length() > 0)
			this.searchResultList = documentSearchService.fullTextSearch(searchTerm);
	}
}
