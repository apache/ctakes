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

import java.io.Serializable;
import java.util.Date;

/**
 * Encapsulates document search results
 * @author vijay
 *
 */
public class DocumentSearchResult implements Serializable {
	@Override
	public String toString() {
		return "DocumentSearchResult [cuiText=" + cuiText + ", documentID="
				+ documentID + ", sentenceText=" + sentenceText + "]";
	}
	public int getDocumentID() {
		return documentID;
	}
	public void setDocumentID(int documentID) {
		this.documentID = documentID;
	}
	public String getSentenceText() {
		return sentenceText;
	}
	public void setSentenceText(String sentenceText) {
		this.sentenceText = sentenceText;
	}
	public Date getDocumentDate() {
		return documentDate;
	}
	public void setDocumentDate(Date documentDate) {
		this.documentDate = documentDate;
	}
	public String getDocumentTitle() {
		return documentTitle;
	}
	public void setDocumentTitle(String documentTitle) {
		this.documentTitle = documentTitle;
	}
	public String getDocumentTypeName() {
		return documentTypeName;
	}
	public void setDocumentTypeName(String documentTypeName) {
		this.documentTypeName = documentTypeName;
	}
	int documentID;
	String sentenceText;
	Date documentDate;
	String documentTitle;
	String documentTypeName;
	String cuiText;
	public String getCuiText() {
		return cuiText;
	}
	public void setCuiText(String cuiText) {
		this.cuiText = cuiText;
	}
	public DocumentSearchResult(int documentID, String sentenceText,
			Date documentDate, String documentTitle, String documentTypeName,
			String cuiText) {
		super();
		this.documentID = documentID;
		this.sentenceText = sentenceText;
		this.documentDate = documentDate;
		this.documentTitle = documentTitle;
		this.documentTypeName = documentTypeName;
		this.cuiText = cuiText;
	}
	public DocumentSearchResult() {
		super();
	}
	
	
}
