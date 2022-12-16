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
 * represent a containment relationship between annotations, e.g. sentences
 * contain words.
 * 
 * @author vijay
 * 
 */
public class AnnotationContainmentLink implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
//	int annotationContainmentLinkId;
	int parentAnnotationId;
	int parentUimaTypeId;
	int childAnnotationId;
	int childUimaTypeId;
	
	
//	public int getAnnotationContainmentLinkId() {
//		return annotationContainmentLinkId;
//	}
//	public void setAnnotationContainmentLinkId(int annotationContainmentLinkId) {
//		this.annotationContainmentLinkId = annotationContainmentLinkId;
//	}
	public int getParentAnnotationId() {
		return parentAnnotationId;
	}
	public void setParentAnnotationId(int parentAnnotationId) {
		this.parentAnnotationId = parentAnnotationId;
	}
	public int getParentUimaTypeId() {
		return parentUimaTypeId;
	}
	public void setParentUimaTypeId(int parentUimaTypeId) {
		this.parentUimaTypeId = parentUimaTypeId;
	}
	public int getChildAnnotationId() {
		return childAnnotationId;
	}
	public void setChildAnnotationId(int childAnnotationId) {
		this.childAnnotationId = childAnnotationId;
	}
	public int getChildUimaTypeId() {
		return childUimaTypeId;
	}
	public void setChildUimaTypeId(int childUimaTypeId) {
		this.childUimaTypeId = childUimaTypeId;
	}
	
}
