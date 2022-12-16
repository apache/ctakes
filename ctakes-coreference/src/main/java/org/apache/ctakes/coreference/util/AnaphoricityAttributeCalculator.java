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
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.coreference.type.Markable;

public class AnaphoricityAttributeCalculator extends AttributeCalculator {

	private Markable m;
	String s;
	String pos;
	

	public AnaphoricityAttributeCalculator (JCas jcas, Markable m) {
		super(jcas);
		this.m = m;
		s = m.getCoveredText();
		pos = getPOS();
	}

	private String getPOS(){
		try{
			TreebankNode node = MarkableTreeUtils.markableNode(jcas, m.getBegin(), m.getEnd());
//			TerminalTreebankNode wordNode = (TerminalTreebankNode) node.getRoot().getTerminals().get(node.getHeadIndex());
			TerminalTreebankNode wordNode = MarkableTreeUtils.getHead(node);
			return wordNode.getNodeType();
		}catch(Exception e){

			return null;
		}
	}
	
	public String calcmDefinite () {
		return isDefinite(s) ? "Y" : "N";
	}

	public String calcmDemonstrative () {
		return isDemonstrative(s) ? "Y" : "N";
	}

	public String calcmNumSing () {	
		// get the largest constituent covering this markable:
		if(pos == null) return "N";
		if(pos.equals("NN") || pos.equals("NNP")){
			return "Y";
		}
		return "N";
	}

	public String calcmNumPlu () {
		if(pos == null) return "N";
		if(pos.equals("NNS") || pos.equals("NNPS")){
			return "Y";
		}else{
			return "N";
		}
	}

	public String calcmNumUnk () {
//		ArrayList<BaseToken> l = containedTokens(m.getBegin(), m.getEnd());
//		for (int i = l.size()-1; i>=0; i--) {
//			String pos = l.get(i).getPartOfSpeech();
		if(pos == null) return "N";
			if (pos.startsWith("NN"))
				return "N";
//		}
		return "Y";
	}

	// FIX**ME - use result of constituency parser to get rightmost N* of NP head.
	// FIXED - not used, so the fixes above work fine.
	public String calcmNumber () {
		// use the underlying NE, instead of the expanded markable to find the number
		//ArrayList<BaseToken> l = containedTokens(m.getContent().getBegin(), m.getContent().getEnd());
		ArrayList<BaseToken> l = containedTokens(m.getBegin(), m.getEnd());
		for (BaseToken t : l) {
			String pos = t.getPartOfSpeech();
			if (pos.equals("NN") || pos.equals("NNP"))
				return "S";
			else if (pos.equals("NNS") || pos.equals("NNPS"))
				return "P";
		}
		return "U";
	}

	public String calcmIsDrug () {
		if (m.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m.getContent()).getTypeID() == CONST.NE_TYPE_ID_DRUG)
			return "Y";
		else
			return "N";
	}

	public String calcmIsDisorder () {
		if (m.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m.getContent()).getTypeID() == CONST.NE_TYPE_ID_DISORDER)
			return "Y";
		else
			return "N";
	}

	public String calcmIsFinding () {
		if (m.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m.getContent()).getTypeID() == CONST.NE_TYPE_ID_FINDING)
			return "Y";
		else
			return "N";
	}

	public String calcmIsProcedure () {
		if (m.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m.getContent()).getTypeID() == CONST.NE_TYPE_ID_PROCEDURE)
			return "Y";
		else
			return "N";
	}

	public String calcmIsAnatomicalSite () {
		if (m.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m.getContent()).getTypeID() == CONST.NE_TYPE_ID_ANATOMICAL_SITE)
			return "Y";
		else
			return "N";
	}

	public String calcmWnClass () {
		if (m.getContent() instanceof IdentifiedAnnotation) {
			IdentifiedAnnotation ne = (IdentifiedAnnotation) m.getContent();
			return String.valueOf(ne.getTypeID());
		} else
			return "0";
	}

	public String calcmProStr () {
		return isPronominal(m) ? "Y" : "N";
	}

	public String calcmIndefinite () {
		if (s.toLowerCase().startsWith("a ") ||
			s.toLowerCase().startsWith("an "))
			return "Y";
		else
			return "N";
	}

	// FIXME use parser output instead of LWA
	public String calcmNPHead () {
		Annotation a = m.getContent();
		FSIterator iter = jcas.getJFSIndexRepository().getAnnotationIndex(LookupWindowAnnotation.type).iterator();
		while (iter.hasNext()) {
			LookupWindowAnnotation lwa = (LookupWindowAnnotation) iter.next();
			if (lwa.getBegin()<=a.getBegin() && lwa.getEnd()==a.getEnd())
				return "yes";
		}
		return "no";
	}

	public String calcmSimilarStr () {
		List<Annotation> lm = FSIteratorToList.convert(
				jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type).iterator());

		List<String> contentTokens = contentWords(m.getBegin(), m.getEnd());

		int window = 3;
		Sentence[] sent = new Sentence[window+1];
		for (int i = 0; i <= window; i++) sent[i] = null;

		for (int i = 0; i < lm.size(); i++) {
			for (int j = 1; j <= window; j++) sent[j-1] = sent[j];
			Sentence ss = (Sentence) lm.get(i);
			sent[window] = ss;
			if (ss.getBegin() <= m.getBegin() && ss.getEnd() >= m.getEnd()) {
				for (int k = window-1; k >= 0 && sent[k] != null; k--) {
					List<String> lt = contentWords(sent[k].getBegin(), sent[k].getEnd());
					for (String s : lt)
						if (contentTokens.contains(s))
							return "Y";
				}
			} else if (ss.getBegin() > m.getBegin())
				return "N";
		}
		return "N";
	}

}
