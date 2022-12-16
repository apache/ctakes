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
package org.apache.ctakes.ytex.kernel.metric;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "LCSPath")
public class LCSPath implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private List<String> concept1Path;

	private List<String> concept2Path;
	
	private String lcs;

	public LCSPath() {
		super();
	}

	@XmlAttribute 
	public List<String> getConcept1Path() {
		return concept1Path;
	}

	@XmlAttribute 
	public List<String> getConcept2Path() {
		return concept2Path;
	}

	@XmlAttribute
	public String getLcs() {
		return lcs;
	}

	public void setConcept1Path(List<String> concept1Path) {
		this.concept1Path = concept1Path;
	}

	public void setConcept2Path(List<String> concept2Path) {
		this.concept2Path = concept2Path;
	}

	public void setLcs(String lcs) {
		this.lcs = lcs;
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		if (getConcept1Path() != null && this.getConcept1Path().size() > 0) {
			formatPath(b, "->", getConcept1Path().iterator());
			b.append("->*");
		}
		b.append(this.getLcs());
		if (getConcept2Path() != null && this.getConcept2Path().size() > 0) {
			b.append("*<-");
			formatPath(b, "<-", getConcept2Path().iterator());
		}
		return b.toString();

	}

	private void formatPath(StringBuilder b, String link,
			Iterator<String> pathIter) {
		while (pathIter.hasNext()) {
			b.append(pathIter.next());
			if (pathIter.hasNext()) {
				b.append(link);
			}
		}
	}
}
