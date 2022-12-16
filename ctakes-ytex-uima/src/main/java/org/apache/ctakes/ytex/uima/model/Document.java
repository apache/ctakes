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
package org.apache.ctakes.ytex.uima.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapped to document table.
 * Contains document text (from JCas.getDocumentText()).
 * Contains gzipped xmi CAS.
 * @author vijay
 *
 */
public class Document implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String analysisBatch;

	/**
	 * the document CAS - serialised in XMI format and compressed.
	 */
	byte[] cas;

	/**
	 * document plain text
	 */
	String docText;

	List<DocumentAnnotation> documentAnnotations = new ArrayList<DocumentAnnotation>();

	List<DocumentClass> documentClasses = new ArrayList<DocumentClass>();

	/**
	 * the document id
	 */
	Integer documentID;
	
	/**
	 * external document id
	 */
	long instanceID;
	
	String instanceKey;

	public Document() {
		super();
	}

	public String getAnalysisBatch() {
		return analysisBatch;
	}

	public byte[] getCas() {
		return cas;
	}

	public String getDocText() {
		return docText;
	}

	public List<DocumentAnnotation> getDocumentAnnotations() {
		return documentAnnotations;
	}

	public List<DocumentClass> getDocumentClasses() {
		return documentClasses;
	}

	public Integer getDocumentID() {
		return documentID;
	}

	public long getInstanceID() {
		return instanceID;
	}

	public String getInstanceKey() {
		return instanceKey;
	}

	public void setAnalysisBatch(String analysisBatch) {
		this.analysisBatch = analysisBatch;
	}

	public void setCas(byte[] cas) {
		this.cas = cas;
	}

	public void setDocText(String docText) {
		this.docText = docText;
	}

	public void setDocumentAnnotations(
			List<DocumentAnnotation> documentAnnotations) {
		this.documentAnnotations = documentAnnotations;
	}

	public void setDocumentClasses(List<DocumentClass> documentClasses) {
		this.documentClasses = documentClasses;
	}

	public void setDocumentID(Integer documentID) {
		this.documentID = documentID;
	}

	public void setInstanceID(long instanceID) {
		this.instanceID = instanceID;
	}

	public void setInstanceKey(String uimaDocumentID) {
		this.instanceKey = uimaDocumentID;
	}

	@Override
	public String toString() {
		return this.getClass().getCanonicalName() + " [documentID="
				+ documentID + ", documentAnnotations=" + documentAnnotations
				+ "]";
	}
}
