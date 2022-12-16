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

import java.util.LinkedList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.cas.FSIterator;


/**
 * Converts all Annotations that are retrievable from an FSIterator
 * to a sorted LinkedList.
 * Non-Annotation type objects are ignored.
 * Sort order is determined by the character offsets:
 * a1.getBegin() - a2.getBegin() || a1.getEnd - a2.getEnd();
 * @author Mayo Clinic
 *
 */
/* @author Jiaping Zheng
 * 
 */
public class FSIteratorToList {

	public static LinkedList<Annotation> convert (FSIterator iter) {
		LinkedList<Annotation> ret = new LinkedList<Annotation>();
		while (iter.hasNext()) {
			Object o = iter.next();
			if (o instanceof Annotation) ret.add((Annotation)o);
		}
//		java.util.Collections.sort(ret, new java.util.Comparator<Annotation>() {
//			public int compare (Annotation a1, Annotation a2) {
//				int r = a1.getBegin() - a2.getBegin();
//				return r==0 ? a1.getEnd()-a2.getEnd() : r;
//			};
//		});
		java.util.Collections.sort(ret, new AnnotOffsetComparator());
		return ret;
	}
}
