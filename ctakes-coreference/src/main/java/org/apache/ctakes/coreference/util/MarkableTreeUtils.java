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

import java.util.Iterator;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.coreference.type.Markable;

public class MarkableTreeUtils {

	public static TreebankNode markableNode(JCas jcas, int a, int b){
		TreebankNode lowestDom = null;
		int overage = Integer.MAX_VALUE;
		FSIterator<Annotation> iter = jcas.getJFSIndexRepository().getAnnotationIndex(TreebankNode.type).iterator();
		while(iter.hasNext()){
			TreebankNode node = (TreebankNode) iter.next();
			if(node.getBegin() == a && node.getEnd() == b){
				// this code will drill down -- actually want to go other way
//				while(node.getChildren() != null && node.getChildren().size() == 1){
//					node = node.getChildren(0);
//				}
				
				// this code will head up as long as parent has the same span
				try{
					while(node.getParent() != null && node.getParent().getChildren().size() == 1 && !node.getParent().getNodeType().equals("TOP")){
						node = node.getParent();
					}
				}catch(NullPointerException e){
					System.err.println("Null pointer exception in AttributeCalculator::markableNode()");
				}
				return node;
			}else if(node.getBegin() <= a && node.getEnd() >= b){
				int tempOver = (a-node.getBegin()) + (node.getEnd()-b);
				if(tempOver < overage){
					lowestDom = node;
					overage = tempOver;
				}
			}
		}
		// There are lots of reasons to get this far -- error in the parse, personal pronoun in an NP (not annotated so not a markable),
		// unrecognized NML structure, etc.
		// Some other work will add any such nodes to the parse tree as in haghighi klein 09 (simple synt sem ...)
		// In contrast, we don't add node to the parse tree, just find the lowest node dominating the markable range
		// TODO test this
		return lowestDom;
	}
	
	public static Markable nodeMarkable(JCas jcas, int a, int b){
		Markable mark = null;
		Markable best = null;
		Iterator<Annotation> iter = jcas.getAnnotationIndex(Markable.type).iterator();
		while(iter.hasNext()){
			Markable temp = (Markable) iter.next();
			if(temp.getBegin() == a && temp.getEnd() == b){
				mark = temp;
				break;
			}else if(temp.getEnd() == b && temp.getBegin() > a){
				// keep track of a best markable in case there is not an exact match - the biggest markable that ends in the same spot
				// and begins after the node.
				if(best == null || temp.getBegin() < best.getBegin()){
					best = temp;
				}
			}
		}
		return (mark == null ? best : mark);
	}
	
	public static TerminalTreebankNode getHead(TreebankNode n){
		TerminalTreebankNode ttn = null;
		int headIndex = n.getHeadIndex();
		ttn = getRoot(n).getTerminals(headIndex);
		return ttn;
	}
	
	public static TopTreebankNode getRoot(TreebankNode n){
		TopTreebankNode ret = null;
		while(!(n instanceof TopTreebankNode)){
			n = n.getParent();
		}
		ret = (TopTreebankNode) n;
		return ret;
	}

}
