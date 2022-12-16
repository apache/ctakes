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

public class MRCONSO implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String aui;
	String code;
	String cui;
	int cvf;
	String ispref;
	String lat;
	String lui;
	String sab;
	String saui;
	String scui;
	String sdui;
	int srl;
	String str;
	String stt;
	String sui;
	String suppress;
	String ts;
	String tty;
	public MRCONSO() {
		super();
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MRCONSO other = (MRCONSO) obj;
		if (aui == null) {
			if (other.aui != null)
				return false;
		} else if (!aui.equals(other.aui))
			return false;
		return true;
	}
	public String getAui() {
		return aui;
	}
	public String getCode() {
		return code;
	}
	public String getCui() {
		return cui;
	}
	public int getCvf() {
		return cvf;
	}
	public String getIspref() {
		return ispref;
	}
	public String getLat() {
		return lat;
	}
	public String getLui() {
		return lui;
	}
	public String getSab() {
		return sab;
	}
	public String getSaui() {
		return saui;
	}
	public String getScui() {
		return scui;
	}
	public String getSdui() {
		return sdui;
	}
	public int getSrl() {
		return srl;
	}
	public String getStr() {
		return str;
	}
	public String getStt() {
		return stt;
	}
	public String getSui() {
		return sui;
	}
	public String getSuppress() {
		return suppress;
	}
	public String getTs() {
		return ts;
	}
	public String getTty() {
		return tty;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aui == null) ? 0 : aui.hashCode());
		return result;
	}
	public void setAui(String aui) {
		this.aui = aui;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public void setCui(String cui) {
		this.cui = cui;
	}
	public void setCvf(int cvf) {
		this.cvf = cvf;
	}
	public void setIspref(String ispref) {
		this.ispref = ispref;
	}
	public void setLat(String lat) {
		this.lat = lat;
	}
	public void setLui(String lui) {
		this.lui = lui;
	}
	public void setSab(String sab) {
		this.sab = sab;
	}
	public void setSaui(String saui) {
		this.saui = saui;
	}
	public void setScui(String scui) {
		this.scui = scui;
	}
	public void setSdui(String sdui) {
		this.sdui = sdui;
	}
	public void setSrl(int srl) {
		this.srl = srl;
	}
	public void setStr(String str) {
		this.str = str;
	}
	public void setStt(String stt) {
		this.stt = stt;
	}
	public void setSui(String sui) {
		this.sui = sui;
	}
	public void setSuppress(String suppress) {
		this.suppress = suppress;
	}
	public void setTs(String ts) {
		this.ts = ts;
	}
	public void setTty(String tty) {
		this.tty = tty;
	}
}
