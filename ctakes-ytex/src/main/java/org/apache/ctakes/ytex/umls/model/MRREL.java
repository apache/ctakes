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

/**
 * mapped to umls MRREL table
 */
public class MRREL implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String cui1;
	private String cui2;

	private String rel;

	private String rela;
	private String rui;
	private String sab;

	@Override
	public String toString() {
		return "MRREL [cui1=" + cui1 + ", cui2=" + cui2 + ", rui=" + rui + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rui == null) ? 0 : rui.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MRREL other = (MRREL) obj;
		if (rui == null) {
			if (other.rui != null)
				return false;
		} else if (!rui.equals(other.rui))
			return false;
		return true;
	}

	public String getCui1() {
		return cui1;
	}

	public String getCui2() {
		return cui2;
	}

	public String getRel() {
		return rel;
	}

	public String getRela() {
		return rela;
	}

	public String getRui() {
		return rui;
	}

	public String getSab() {
		return sab;
	}

	public void setCui1(String cui1) {
		this.cui1 = cui1;
	}

	public void setCui2(String cui2) {
		this.cui2 = cui2;
	}

	public void setRel(String rel) {
		this.rel = rel;
	}

	public void setRela(String rela) {
		this.rela = rela;
	}

	public void setRui(String rui) {
		this.rui = rui;
	}

	public void setSab(String sab) {
		this.sab = sab;
	}
}
