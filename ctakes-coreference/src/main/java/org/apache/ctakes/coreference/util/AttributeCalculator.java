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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;

import org.apache.ctakes.coreference.type.Markable;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
//import org.apache.ctakes.typesystem.type.NamedEntity;

public class AttributeCalculator {

	static HashSet<String> stopwords;
	JCas jcas;

	Hashtable<Integer, BaseToken> hbs;
	Hashtable<Integer, BaseToken> hbe;
//	Hashtable<Integer, NamedEntity> hns;
//	Hashtable<Integer, NamedEntity> hne;

	public AttributeCalculator (JCas jcas) {
		this.jcas = jcas;

		// index the base tokens and NEs by their offsets
		hbs = new Hashtable<Integer, BaseToken>();
		hbe = new Hashtable<Integer, BaseToken>();
		FSIterator iter = jcas.getJFSIndexRepository().getAnnotationIndex(BaseToken.type).iterator();
		while (iter.hasNext()) {
			BaseToken t = (BaseToken) iter.next();
			hbs.put(t.getBegin(), t);
			hbe.put(t.getEnd(), t);
		}
	}

	public void setStopWordsList (HashSet<String> l) {
		stopwords = l;
	}

	static boolean isPronoun (Markable m) {
		if (m.getContent() instanceof BaseToken) {
			BaseToken t = (BaseToken) m.getContent();
			if (t.getPartOfSpeech().startsWith("PRP")) // TODO: since only 3rd person pronouns are added as markables, no need to check
				return true;
		}
		return false;
	}

	static boolean isDemonstrative (String s) {
		if (s.startsWith("this") ||
				s.startsWith("that") ||
				s.startsWith("these") ||
				s.startsWith("those"))
				return true;
		else return false;
	}

	static boolean isPronominal (Markable m) {
		return (isDemonstrative(m.getCoveredText()) || isPronoun(m));
	}

	static boolean isDefinite (String s) {
		return s.toLowerCase().startsWith("the ");
	}

	public String number (Markable m){
		TreebankNode node = MarkableTreeUtils.markableNode(jcas, m.getBegin(), m.getEnd());
		if(node == null) return basicNumber(m);
		try{
			TerminalTreebankNode termNode = MarkableTreeUtils.getHead(node);
			String pos = termNode.getNodeType();
			if(pos.equals("NN") || pos.equals("NNP")) return "S";
			else if(pos.equals("NNS") || pos.equals("NNPS")) return "P";
			else{
				// obviously there are many other pronouns but we don't cover personal pronouns and so 
				// these are all we need.
				String word = termNode.getCoveredText();
				if(word.equalsIgnoreCase("it")) return "S";
				else if(word.equalsIgnoreCase("its")) return "S";
				else if(word.equalsIgnoreCase("they")) return "P";
				else if(word.equalsIgnoreCase("their")) return "P";
				else return "U";
			}
		}catch(NullPointerException e){
			return basicNumber(m);
		}
	}
	
	// FIXME, looks at the POS of the first NN type in the Markable, should look at the head
	// (Being fixed above in newNumber)
	String basicNumber (Markable m) {
		ArrayList<BaseToken> l = containedTokens(m.getContent().getBegin(), m.getContent().getEnd());
		for (BaseToken t : l) {
			String pos = t.getPartOfSpeech();
			if (pos.equals("NN") || pos.equals("NNP"))
				return "S";
			else if (pos.equals("NNS") || pos.equals("NNPS"))
				return "P";
//			else if (pos.equals("PRP")) {
//				if (m.getCoveredText().equalsIgnoreCase("we") || m.getCoveredText().equalsIgnoreCase("they"))
//					return "P";
//				else
//					return "S";
//			}
		}
		return "U";
	}

	public ArrayList<BaseToken> containedTokens (int a, int b) {
		ArrayList<BaseToken> ret = new ArrayList<BaseToken>();
		BaseToken t1 = hbs.get(a);
		BaseToken t2 = hbe.get(b);
		if (t1!=null && t2!=null) {
			int begin = t1.getTokenNumber();
			int end = t2.getTokenNumber();
			LinkedList<Annotation> l = FSIteratorToList.convert(jcas.getJFSIndexRepository().getAnnotationIndex(BaseToken.type).iterator());
			for (int i = 0; i < l.size(); i++) {
				BaseToken t = (BaseToken) l.get(i);
				if (t.getTokenNumber()>=begin && t.getTokenNumber()<=end)
					ret.add(t);
			}
		}
//		int e;
//		while (t!=null && (e=t.getEnd())<=b) {
//			ret.add(t);
//			t = hbs.get(e);
//		}
		return ret;
	}
	
	/*
	public TreebankNode markableNode(int a, int b){
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
					while(node.getParent() != null && node.getParent().getChildren().size() == 1){
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
//		return null;
	}
*/
	
	public ArrayList<String> contentWords (int begin, int end) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<BaseToken> l = containedTokens(begin, end);
		for (BaseToken t : l) {
			String s = t.getCoveredText().toLowerCase();
			if (!stopwords.contains(s))
				ret.add(s);
		}
		return ret;
	}

	ArrayList<String> contentWords (Markable m) {
		return contentWords(m.getBegin(), m.getEnd());
	}

}
