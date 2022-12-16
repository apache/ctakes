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

/**
 * Mapped to ref_segment_regex
 * @author vijay
 *
 */
public class SegmentRegex implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	int segmentRegexID;
	String regex;
	String segmentID;
	boolean limitToRegex;
	public boolean isLimitToRegex() {
		return limitToRegex;
	}
	public void setLimitToRegex(boolean limitToRegex) {
		this.limitToRegex = limitToRegex;
	}
	public int getSegmentRegexID() {
		return segmentRegexID;
	}
	public void setSegmentRegexID(int segmentRegexID) {
		this.segmentRegexID = segmentRegexID;
	}
	public String getRegex() {
		return regex;
	}
	public void setRegex(String regex) {
		this.regex = regex;
	}
	public String getSegmentID() {
		return segmentID;
	}
	public void setSegmentID(String segmentID) {
		this.segmentID = segmentID;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + segmentRegexID;
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
		SegmentRegex other = (SegmentRegex) obj;
		if (segmentRegexID != other.segmentRegexID)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SegmentRegex [regex=" + regex + ", segmentID=" + segmentID
				+ ", segmentRegexID=" + segmentRegexID + "]";
	}
	public SegmentRegex() {
		super();
	}

}
