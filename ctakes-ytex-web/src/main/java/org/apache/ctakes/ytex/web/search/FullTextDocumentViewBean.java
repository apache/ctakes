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

import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringEscapeUtils;


/**
 * Jsf bean to view results of fullTextSearch.jspx.
 * Relies on documentID parameter.
 * @author vijay
 *
 */
public class FullTextDocumentViewBean {
	private String docText;
	private DocumentSearchService documentSearchService;

	private int documentID;

	public int getDocumentID() {
		this.loadDocument();
		return documentID;
	}

	public String getDocText() {
		this.loadDocument();
		return docText;
	}
	public DocumentSearchService getDocumentSearchService() {
		return documentSearchService;
	}

	public void setDocumentSearchService(DocumentSearchService documentSearchService) {
		this.documentSearchService = documentSearchService;
	}

	public void loadDocument() {
		if (this.docText == null) {
			String strDocumentID = (String) FacesContext.getCurrentInstance()
					.getExternalContext().getRequestParameterMap().get(
							"documentID");
			documentID = Integer.parseInt(strDocumentID); 
			String docTextUnformatted = this.documentSearchService
					.getFullTextSearchDocument(documentID);
			this.docText = StringEscapeUtils.escapeXml(docTextUnformatted).replaceAll("\n", "<br>");
		}
	}
}
