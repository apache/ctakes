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
package org.apache.ctakes.ytex.umls.model;

import java.io.Serializable;

public class UmlsAuiFirstWord implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String aui;

	String fstem;

	String fword;

	String stemmedStr;

	String tokenizedStr;
	
	public UmlsAuiFirstWord() {
		super();
	}
	public UmlsAuiFirstWord(String aui, String fword) {
		super();
		this.aui = aui;
		this.fword = fword;
	}
	public String getAui() {
		return aui;
	}
	public String getFstem() {
		return fstem;
	}
	public String getFword() {
		return fword;
	}
	public String getStemmedStr() {
		return stemmedStr;
	}

	public String getTokenizedStr() {
		return tokenizedStr;
	}

	public void setAui(String aui) {
		this.aui = aui;
	}

	public void setFstem(String fstem) {
		this.fstem = fstem;
	}

	public void setFword(String fword) {
		this.fword = fword;
	}

	public void setStemmedStr(String stemmedStr) {
		this.stemmedStr = stemmedStr;
	}

	public void setTokenizedStr(String tokenizedStr) {
		this.tokenizedStr = tokenizedStr;
	}

	@Override
	public String toString() {
		return "UmlsAuiFirstWord [aui=" + aui + ", fword=" + fword + "]";
	}

}
