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
//import java.util.Iterator;
import java.util.Set;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

// TODO: This class hardcoded all the criteria,
// which should be replaced by a parser of
// the inclusionCondition resource (use parameter?) MarkableCreator
public class AnnotationSelector {

	public static ArrayList<Annotation> selectNE (JCas jcas) {
		ArrayList<Annotation> ret = new ArrayList<Annotation>();
		FSIterator<Annotation> iter = jcas.getJFSIndexRepository().getAnnotationIndex(IdentifiedAnnotation.type).iterator();
		while (iter.hasNext()) {
		  IdentifiedAnnotation a = (IdentifiedAnnotation) iter.next();
		  if (a instanceof EntityMention || a instanceof EventMention) {
			if(a.getOntologyConceptArr() != null) {
//			int tid = a.getTypeID();
//			if (tid == TypeSystemConst.NE_TYPE_ID_ANATOMICAL_SITE ||
//				tid == TypeSystemConst.NE_TYPE_ID_DISORDER ||
//				tid == TypeSystemConst.NE_TYPE_ID_PROCEDURE ||
//				tid == TypeSystemConst.NE_TYPE_ID_FINDING)
				ret.add(a);
			}
		  }
		}
		java.util.Collections.sort(ret, new AnnotOffsetComparator());
		return ret;
	}

	public static ArrayList<BaseToken> selectBaseToken (JCas jcas) {
		ArrayList<BaseToken> ret = new ArrayList<BaseToken>();
		FSIterator<?> iter = jcas.getJFSIndexRepository().getAnnotationIndex(BaseToken.type).iterator();
		while (iter.hasNext())
			ret.add((BaseToken)iter.next());
		java.util.Collections.sort(ret, new AnnotOffsetComparator());
		return ret;
	}

	public static ArrayList<Sentence> selectSentence (JCas jcas) {
		ArrayList<Sentence> ret = new ArrayList<Sentence>();
		FSIterator<Annotation> iter = jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type).iterator();
		while (iter.hasNext())
			ret.add((Sentence)iter.next());
		java.util.Collections.sort(ret, new AnnotOffsetComparator());
		return ret;
	}

	public static ArrayList<WordToken> selectPronoun (JCas jcas,
			Set<String> modalAdj, Set<String> cogved, Set<String> othervb,
			Logger logger) {
		Hashtable<String, WordToken> offset2token = new Hashtable<String, WordToken>();
		ArrayList<WordToken> ret = new ArrayList<WordToken>();
		FSIterator<Annotation> iter = jcas.getJFSIndexRepository().getAnnotationIndex(WordToken.type).iterator();
		while (iter.hasNext()) {
			WordToken t = (WordToken)iter.next();
			String s = t.getCoveredText();
			if (//s.equalsIgnoreCase("it") ||
				s.equalsIgnoreCase("its") ||
				s.equalsIgnoreCase("they") ||
				s.equalsIgnoreCase("their") ||
				s.equalsIgnoreCase("them") ||
				s.equalsIgnoreCase("theirs"))
				ret.add(t);
			if (s.equalsIgnoreCase("it"))
				offset2token.put(t.getBegin()+"-"+t.getEnd(), t);
		}

		iter = jcas.getJFSIndexRepository().getAnnotationIndex(TerminalTreebankNode.type).iterator();
		while (iter.hasNext()) {
			TerminalTreebankNode ttn = (TerminalTreebankNode) iter.next();
			if (ttn.getCoveredText().equalsIgnoreCase("it"))
				if (isPleonastic(ttn, modalAdj, cogved, othervb))
					logger.info("Pleonastic \"it\" at position "+
							ttn.getIndex()+" of \""+
							MarkableTreeUtils.getRoot(ttn).getCoveredText()+"\"");
				else
					ret.add(offset2token.get(ttn.getBegin()+"-"+ttn.getEnd()));
		}
		java.util.Collections.sort(ret, new AnnotOffsetComparator());
		return ret;
	}

	private static boolean isPleonastic (TerminalTreebankNode ttn,
			Set<String> modalAdj, Set<String> cogved, Set<String> othervb) {
		if (!ttn.getCoveredText().equalsIgnoreCase("it")) return false;

		if (ttn.getNodeType().equals("PRP")) {
			TreebankNode tn = ttn.getParent();
			while (tn.getNodeType().startsWith("NP"))
				tn = tn.getParent();
			if (tn.getNodeType().equals("S")) {
				TreebankNode par = tn;
				TreebankNode vp = findP(tn, "VP", 0);
				while (vp!=null) vp = findP(par = vp, "VP", 0);
				vp = par;
				par = vp.getParent();

				FSArray c = vp.getChildren();
				TreebankNode firstChild = (TreebankNode) c.get(0);
				if (isBe(firstChild)) {
					TreebankNode adjP = findP(vp, "ADJP", 1);
					if (adjP!=null && modalAdj.contains(adjP.getCoveredText()) &&
							(findP(vp, "SBAR", 1)!=null ||
							findP(vp, "S", 1)!=null ||
							findP(adjP, "SBAR", 1)!=null ||
							findP(adjP, "S", 1)!=null))
						return true;
				} else if (isBe(par.getChildren(0))) {
					firstChild = vp.getChildren(0);
					if (firstChild!=null && cogved.contains(firstChild.getCoveredText()) &&
							(findP(vp, "SBAR", 1)!=null ||
							findP(vp, "S", 1)!=null))
						return true;
				} else {
					// hacky way to get base form of the verbs in otherVerb list,
					// should use lvg to get the base form of the word
					String word = firstChild.getCoveredText().replaceAll("s$", "").replaceAll("ed$", "").replaceAll("t$", "");
					if (othervb.contains(word) &&
							(findP(vp, "SBAR", 1)!=null ||
							findP(vp, "S", 1)!=null))
						return true;
				}
			}
		}
		return false;
	}

	private static TreebankNode findP (TreebankNode n, String phraseTag, int startingChild) {
		FSArray c = n.getChildren();
		int i = startingChild;
		while (i < c.size()) {
			TreebankNode tn = (TreebankNode) c.get(i++);
			if (tn.getNodeType().equals(phraseTag) ||
					tn.getNodeType().startsWith(phraseTag+"-"))
				return tn;
		}
		return null;
	}

	private static boolean isBe (TreebankNode n) {
		String phCat = n.getNodeType();
		String txt = n.getCoveredText();
		if ((phCat.equals("VB") ||
				phCat.equals("VBZ") ||
				phCat.equals("VBD") ||
				phCat.equals("VBN")) &&
				(txt.equalsIgnoreCase("is") ||
						txt.equalsIgnoreCase("was") ||
						txt.equalsIgnoreCase("been") ||
						txt.equalsIgnoreCase("be")))
			return true;
		
		return false;
	}

	public static ArrayList<Chunk> selectDemonAndRelative (JCas jcas) {
		ArrayList<Chunk> ret = new ArrayList<Chunk>();
		FSIterator<Annotation> iter = jcas.getJFSIndexRepository().getAnnotationIndex(Chunk.type).iterator();
		while (iter.hasNext()) {
			Chunk c = (Chunk)iter.next();
			if (c.getChunkType().equals("NP")) {
				String s = c.getCoveredText().toLowerCase();
				if (s.startsWith("these") ||
					s.startsWith("those") ||
					s.startsWith("this") ||
					s.startsWith("that") ||
					s.startsWith("which"))
				ret.add(c);
			}
		}
		java.util.Collections.sort(ret, new AnnotOffsetComparator());
		return ret;
	}
}
