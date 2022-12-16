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

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;

public class MarkableDepUtils {

	public static ConllDependencyNode markableNode(JCas jcas, int begin, int end, TreebankNode n) {
		// Find head of markable using treebanknode
		TerminalTreebankNode term = MarkableTreeUtils.getHead(n);
		FSIterator<Annotation> iter = jcas.getAnnotationIndex(ConllDependencyNode.type).iterator();
		ConllDependencyNode best = null;
		
		while(iter.hasNext()){
			Annotation a = iter.next();
			if(a.getBegin() == term.getBegin() && a.getEnd() == term.getEnd()){
				best = (ConllDependencyNode) a;
				break;
			}
		}
		return best;
	}

}
