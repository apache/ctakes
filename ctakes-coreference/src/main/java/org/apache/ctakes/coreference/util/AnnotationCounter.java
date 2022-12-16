/*
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
package org.apache.ctakes.coreference.util;

import java.util.Collection;

import org.apache.uima.jcas.tcas.Annotation;


public class AnnotationCounter {

	public static int countInterval (Collection<? extends Annotation> c, int b, int e) {
		//TODO: substitute hard-coded boundaries to flexible conditions
		int ret = 0;
		for (Annotation a : c)
			if (a.getBegin()>b && a.getEnd()<e) ++ret;
		return ret;
	}

	public static int countPoint (Collection<? extends Annotation> c, int b, int e) {
		//TODO: substitute hard-coded boundaries to flexible conditions
		int ret = 0;
		for (Annotation a : c) {
			int begin = a.getBegin();
			int end = a.getEnd();
			if (begin<end && begin>b && begin<=e) ++ret;
		}
		return ret;
	}
}
