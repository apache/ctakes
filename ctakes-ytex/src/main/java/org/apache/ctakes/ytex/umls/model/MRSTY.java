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

public class MRSTY implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String cui;
	String tui;
	String stn;
	String sty;
	String atui;
	int cvf;
	public String getCui() {
		return cui;
	}
	public void setCui(String cui) {
		this.cui = cui;
	}
	public String getTui() {
		return tui;
	}
	public void setTui(String tui) {
		this.tui = tui;
	}
	public String getStn() {
		return stn;
	}
	public void setStn(String stn) {
		this.stn = stn;
	}
	public String getSty() {
		return sty;
	}
	public void setSty(String sty) {
		this.sty = sty;
	}
	public String getAtui() {
		return atui;
	}
	public void setAtui(String atui) {
		this.atui = atui;
	}
	public int getCvf() {
		return cvf;
	}
	public void setCvf(int cvf) {
		this.cvf = cvf;
	}
	@Override
	public String toString() {
		return "MRSTY [cui=" + cui + ", tui=" + tui + "]";
	}
	public MRSTY() {
		super();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cui == null) ? 0 : cui.hashCode());
		result = prime * result + ((tui == null) ? 0 : tui.hashCode());
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
		MRSTY other = (MRSTY) obj;
		if (cui == null) {
			if (other.cui != null)
				return false;
		} else if (!cui.equals(other.cui))
			return false;
		if (tui == null) {
			if (other.tui != null)
				return false;
		} else if (!tui.equals(other.tui))
			return false;
		return true;
	}
	
	
}
