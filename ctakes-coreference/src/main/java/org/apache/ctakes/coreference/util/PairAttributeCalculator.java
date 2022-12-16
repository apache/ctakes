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

import org.apache.ctakes.coreference.type.Markable;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

public class PairAttributeCalculator extends AttributeCalculator {

	protected Markable m1, m2;
    protected String ms1, ms2; // markable strings
    protected String es1, es2; // entity strings
//	protected String s1, s2;
	protected Annotation a1, a2;
	boolean alias;
	
	public PairAttributeCalculator (JCas jcas, Markable m1, Markable m2) {
		super(jcas);
		this.m1 = m1;
		this.m2 = m2;
		this.a1 = m1.getContent();
		this.a2 = m2.getContent();
		ms1 = m1.getCoveredText();
		ms2 = m2.getCoveredText();
		es1 = a1.getCoveredText();
		es2 = a2.getCoveredText();
		alias = isAlias();
	}
	
	/**
	 * Determine whether the markables are located within the same section
	 * @author andreea bodnari
	 * @return
	 */
	public String calcSameSection(){
//		ArrayList<Segment> ret = new ArrayList<Segment>();
//		FSIterator iter = jcas.getJFSIndexRepository().getAnnotationIndex(Segment.type).iterator();
//		while (iter.hasNext())
//			ret.add((Segment)iter.next());
//		java.util.Collections.sort(ret, new AnnotOffsetComparator());
//		
//		Segment seg1 = null;
//		Segment seg2 = null;
//		
//		for (Segment a : ret){
//			if(a.getStart() <= m1.getStart() && a.getEnd() >= m1.getEnd())
//				seg1 = a;
//			if(a.getStart() <= m2.getStart() && a.getEnd() >= m2.getEnd())
//				seg2 = a;	
//		}
		boolean sameSection = false;
		
//		if(seg1 != null && seg2 != null)
//			sameSection = seg1.getId().equals(seg2.getId());
			
		return sameSection ? "yes" : "no";
	}

	public double calcTokenDistance () {
		return AnnotationCounter.countInterval(AnnotationSelector.selectBaseToken(jcas), m1.getEnd(), m2.getBegin()) / (double) CorefConsts.TOKDIST;
	}

	public double calcSentenceDistance () {
		return getSentenceDistance() / (double) CorefConsts.SENTDIST;
	}

	public int getSentenceDistance() {
		return AnnotationCounter.countPoint(AnnotationSelector.selectSentence(jcas), m1.getEnd(), m2.getBegin());
	}

	public boolean calcExactMatch () {
		return ms1.equalsIgnoreCase(ms2);
	}

	public boolean calcStartMatch () {
		return TextMatch.startMatch(ms1, ms2);
	}

	public boolean calcMidMatch () {
		return false;
	}

	public boolean calcEndMatch () {
		return TextMatch.endMatch(ms1, ms2);
	}

	public boolean calcStringMatch() {
		return (calcExactMatch() || calcStartMatch() || calcEndMatch());
	}
	
    public boolean calcEntityExactMatch() {
        return es1.equalsIgnoreCase(es2);
    }

    public boolean calcEntityStartMatch() {
    	return TextMatch.startMatch(es1, es2);
    }

    public boolean calcEntityEndMatch(){
    	return TextMatch.endMatch(es1, es2);
    }

	public boolean calcSoonStr () {
		String sl1 = ms1.toLowerCase();
		String sl2 = ms2.toLowerCase();
//		if (sl1.startsWith("the ")) sl1 = sl1.substring(4);
//		if (sl1.startsWith("a ")) sl1 = sl1.substring(2);
//		if (sl2.startsWith("the ")) sl2 = sl2.substring(4);
//		if (sl2.startsWith("a ")) sl2 = sl2.substring(2);
		sl1 = nonDetSubstr(sl1);
		sl2 = nonDetSubstr(sl2);
		return sl1.equals(sl2);
	}
	
	private static String nonDetSubstr (String s) {
		if(s.startsWith("the ")) return s.substring(4);
		if(s.startsWith("a ")) return s.substring(2);
		if(s.startsWith("this ")) return s.substring(5);
		if(s.startsWith("that ")) return s.substring(5);
		return s;
	}

	public boolean calcPronoun1 () {
		return isPronoun(m1);
	}

	public boolean calcPronoun2 () {
		return isPronoun(m2);
	}

	public boolean calcDefinite2 () {
		return isDefinite(ms2);
	}

	public boolean calcDemonstrative2 () {
		return isDemonstrative(ms2);
	}

	public boolean calcNumberMatchC () {
		String n1 = number(m1);
		String n2 = number(m2);
		if (!n1.equals("U") && !n2.equals("U") && n1.equals(n2)){
			return true;
		}
		return false;
	}

	public boolean calcNumberMatchI () {
		String n1 = number(m1);
		String n2 = number(m2);
		if (!n1.equals("U") && !n2.equals("U") && !n1.equals(n2)){
			return true;
		}
		return false;
	}

	public boolean calcNumberMatchNA () {
		String n1 = number(m1);
		String n2 = number(m2);
		if (n1.equals("U") || n2.equals("U")){
			return true;
		}
		return false;
	}

//	public String calcNumberMatch () {
//		String n1 = number(m1);
//		String n2 = number(m2);
//		if (n1.equals("U") || n2.equals("U"))
//			return "NA";
//		else if (n1.equals(n2))
//			return "C";
//		else
//			return "I";
//	}

	// heuristics
	//	public String calcAppositive () {
	//		if (jcas.getDocumentText().substring(m1.getBegin(), m2.getEnd())
	//				.equals(s1 + ", " + s2))
	//			return "yes";
	//		else return "no";
	//	}

	public boolean calcWnClassC () {
		if (m1.getContent() instanceof IdentifiedAnnotation &&
				m2.getContent() instanceof IdentifiedAnnotation) {
			IdentifiedAnnotation ne1 = (IdentifiedAnnotation) m1.getContent();
			IdentifiedAnnotation ne2 = (IdentifiedAnnotation) m2.getContent();
			if (ne1.getTypeID() == ne2.getTypeID()){
				return true;
			}
			return false;
		}
		return false;
	}

	public boolean calcWnClassI () {
		if (m1.getContent() instanceof IdentifiedAnnotation &&
				m2.getContent() instanceof IdentifiedAnnotation) {
			IdentifiedAnnotation ne1 = (IdentifiedAnnotation) m1.getContent();
			IdentifiedAnnotation ne2 = (IdentifiedAnnotation) m2.getContent();
			if (ne1.getTypeID() != ne2.getTypeID()){
				return true;
			}
			return false;
		}
		return false;
	}

	public boolean calcWnClassNA () {
		if (!(m1.getContent() instanceof IdentifiedAnnotation) ||
				!(m2.getContent() instanceof IdentifiedAnnotation)){
			return true;
		}
		return false;
	}

	public boolean calcWnClass () {
		if (m1.getContent() instanceof IdentifiedAnnotation &&
				m2.getContent() instanceof IdentifiedAnnotation) {
			IdentifiedAnnotation ne1 = (IdentifiedAnnotation) m1.getContent();
			IdentifiedAnnotation ne2 = (IdentifiedAnnotation) m2.getContent();
			if (ne1.getTypeID() == ne2.getTypeID()){
				return true;
			}
		}
		return false;
	}

	public boolean calcAlias () {
		return alias;
	}
	
	public boolean isAlias(){
		try{
		if (m1.getContent() instanceof IdentifiedAnnotation &&
			m2.getContent() instanceof IdentifiedAnnotation) {
			IdentifiedAnnotation ne1 = (IdentifiedAnnotation) m1.getContent();
			IdentifiedAnnotation ne2 = (IdentifiedAnnotation) m2.getContent();

			ArrayList<String> l = new ArrayList<String>();
			FSArray fsa = ne1.getOntologyConceptArr();
			for (int i = 0; i < fsa.size(); ++i)
				if (fsa.get(i) instanceof UmlsConcept)
					l.add(((UmlsConcept)fsa.get(i)).getCui());

			fsa = ne2.getOntologyConceptArr();
			for (int i = 0; i < fsa.size(); ++i)
				if (fsa.get(i) instanceof UmlsConcept &&
					l.contains(((UmlsConcept)fsa.get(i)).getCui()))
					return true;
		}
		}catch(Exception e){
			System.err.println("Error here!");
		}
		return false;
	}
	
	// PRO_STR in Ng and Cardie
	public boolean calcProStr () {
		if (isPronominal(m1) &&
			isPronominal(m2) &&
			ms1.equalsIgnoreCase(ms2)){
			return true;
		}
		return false;
	}

//	public String calcPnStr () {
//		if (s1.equalsIgnoreCase(s2)) {
//			ArrayList<BaseToken> toks = containedTokens(m1.getBegin(), m2.getEnd());
//			for (BaseToken t : toks)
//				if (t.getPartOfSpeech().startsWith("NNP"))
//					return "yes";
//		}
//		return "no";
//	}

	// WORDS_STR in Ng and Cardie - currently not used
	public boolean calcWordsStr () {
		if (!isPronominal(m1) && !isPronominal(m2) &&
			ms1.equalsIgnoreCase(ms2)){
			return true;
		}
		return false;
	}

	private static String removeArticleAndDemon(String s){
		if (s.toLowerCase().startsWith("a "))
			return s.substring(2);
		else if (s.toLowerCase().startsWith("an "))
			return s.substring(3);
		else if (s.toLowerCase().startsWith("the "))
			return s.substring(4);
		else if (s.toLowerCase().startsWith("this "))
			return s.substring(5);
		else if (s.toLowerCase().startsWith("that "))
			return s.substring(5);
		else if (s.toLowerCase().startsWith("these "))
			return s.substring(6);
		else if (s.toLowerCase().startsWith("those "))
			return s.substring(6);
		else
			return s;
	}

	// SOON_STR_NONPRO from Ng and Cardie
	public boolean calcSoonStrNonpro () {
		if (!isPronominal(m1) && !isPronominal(m2)) {
			String str1 = removeArticleAndDemon(ms1);
			String str2 = removeArticleAndDemon(ms2);
			if (str1.toLowerCase().indexOf(str2.toLowerCase()) >= 0 ||
				str2.toLowerCase().indexOf(str1.toLowerCase()) >= 0){
				return true;
			}
		}
		return false;
	}


	// WORD_OVERLAP from Ng and Cardie 02
	public boolean calcWordOverlap () {
		ArrayList<String> t1 = contentWords(m1);
		ArrayList<String> t2 = contentWords(m2);
		for (String s : t2){
			if (t1.contains(s)){
				return true;
			}
		}
		return false;
	}

	// TODO with syntax
	// MODIFIER from Ng and Cardie 02
	public boolean calcModifier () {
		return true; 
	}

//	public String calcPnSubstr () {
//		
//	}

	// is l1 a proper substring of l2?
	// TODO optimize with Stringbuffer instead of concatenation
	private static boolean isProperSubstring (ArrayList<String> l1, ArrayList<String> l2) {
		String str1 = "";
		String str2 = "";
		for (String s : l1)
			str1 += " " + s;
		for (String s: l2)
			str2 += " " + s;
		// FIXME This should be an AND ?
		if (str1.length()!=str2.length() || str2.indexOf(str1)>=0){
			return true;
		}
		return false;
	}

	public boolean calcWordsSubstr () {
		if (!isPronominal(m1) && !isPronominal(m2)) {
			ArrayList<String> t1 = contentWords(m1);
			ArrayList<String> t2 = contentWords(m2);
			if (isProperSubstring(t1, t2) || isProperSubstring(t2, t1)){
				return true;
			}
		}
		return false;
	}

	public boolean calcBothDefinitesC () {
		return (isDefinite(ms1) && isDefinite(ms2));
	}

	public boolean calcBothDefinitesI () {
		return (!isDefinite(ms1) && !isDefinite(ms2));
	}

	public boolean calcBothDefinitesNA () {
		boolean b1 = isDefinite(ms1);
		boolean b2 = isDefinite(ms2);
		return (!(b1&&b2) && (b1||b2));
	}

//	public String calcBothDefinites () {
//		boolean b1 = isDefinite(ms1);
//		boolean b2 = isDefinite(ms2);
//		if (b1 && b2) return "C";
//		if (b1 || b2) return "NA";
//		return "I";
//	}

//	public String calcBothEmbeddedC () {
//		return "N"; //TODO: sketch
//	}
//
//	public String calcBothEmbeddedI () {
//		return "N"; //TODO: sketch
//	}
//
//	public String calcBothEmbeddedNA () {
//		return "N"; //TODO: sketch
//	}
//
//	public String calcBothEmbedded () {
//		return "NA"; //TODO: sketch
//	}

	public boolean calcBothPronounsC () {
		boolean b1 = isPronoun(m1);
		boolean b2 = isPronoun(m2);
		return (b1 && b2);
	}

	public boolean calcBothPronounsI () {
		boolean b1 = isPronoun(m1);
		boolean b2 = isPronoun(m2);
		return (!b1 && !b2);
	}

	public boolean calcBothPronounsNA () {
		boolean b1 = isPronoun(m1);
		boolean b2 = isPronoun(m2);
		return (!(b1&&b2) && (b1||b2));
	}

	public boolean calcBothPronouns () {
		boolean b1 = isPronoun(m1);
		boolean b2 = isPronoun(m2);
		if (b1 && b2) return true;
		return false;
	}

//	public String calcSpan () {
//		if (m1.getBegin()<=m2.getBegin()) {
//			if (m1.getEnd()>=m2.getEnd()) return "C";
//			else return "I";
//		} else {
//			if (m1.getEnd()<=m2.getEnd()) return "C";
//			else return "I";
//		}
//	}

	public boolean calcIndefinite () {
		if (ms2.toLowerCase().startsWith("a ") ||
			ms2.toLowerCase().startsWith("an ")){
			return false;
		}
		return true;
	}

	public boolean calcPronoun () {
		 return !(isPronoun(m1) && !isPronoun(m2));
	}

//	public String calcContainsPn () {
//		
//	}

	public boolean calcDefinite1 () {
		return isDefinite(ms1);
	}

//	public String calcProperNoun () {
//		
//	}

	public boolean calcIsDrug () {
		if (m1.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m1.getContent()).getTypeID() == CONST.NE_TYPE_ID_DRUG){
			return true;
		}
		return false;
	}

	public boolean calcIsDisorder () {
		if (m1.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m1.getContent()).getTypeID() == CONST.NE_TYPE_ID_DISORDER){
			return true;
		}
		return false;
	}

	public boolean calcIsFinding () {
		if (m1.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m1.getContent()).getTypeID() == CONST.NE_TYPE_ID_FINDING){
			return true;
		}
		return false;
	}

	public boolean calcIsProcedure () {
		if (m1.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m1.getContent()).getTypeID() == CONST.NE_TYPE_ID_PROCEDURE){
			return true;
		}
		return false;
	}

	public boolean calcIsAnatomicalSite () {
		if (m1.getContent() instanceof IdentifiedAnnotation &&
				((IdentifiedAnnotation)m1.getContent()).getTypeID() == CONST.NE_TYPE_ID_ANATOMICAL_SITE){
			return true;
		}
		return false;
	}

	public boolean calcNegatedBoth(){
		if((a1 instanceof EntityMention && a2 instanceof EntityMention) || (a1 instanceof EventMention && a2 instanceof EventMention)){
			if(((IdentifiedAnnotation)a1).getPolarity() == -1 &&
			   ((IdentifiedAnnotation)a2).getPolarity() == -1){
				return true;
			}
		}
		return false;
	}
	
	public boolean calcNonNegatedBoth(){
		if(a1 instanceof EntityMention && a2 instanceof EntityMention || (a1 instanceof EventMention && a2 instanceof EventMention)){
			if(((IdentifiedAnnotation)a1).getPolarity() == 1.0 &&
			   ((IdentifiedAnnotation)a2).getPolarity() == 1.0){
				return true;
			}
		}
		return false;
	}
	
	public boolean calcClosestComp () {
		if (calcWnClass()) {
			ArrayList<Annotation> l = AnnotationSelector.selectNE(jcas);
			int m2type = ((IdentifiedAnnotation)m2.getContent()).getTypeID();
			for (Annotation a : l) {
				if (((IdentifiedAnnotation)a).getTypeID()==m2type &&
					a.getBegin()>=m1.getEnd() &&
					a.getEnd()<=m2.getBegin())
					return false;
			}
			return true;
		}
		return false;
	}

	public boolean calcNPHead () {
		Annotation a = m1.getContent();
//		return (a.getEnd()==m1.getEnd() && a.getBegin()>m1.getBegin()) ? "yes" : "no";
		FSIterator iter = jcas.getJFSIndexRepository().getAnnotationIndex(LookupWindowAnnotation.type).iterator();
		while (iter.hasNext()) {
			LookupWindowAnnotation lwa = (LookupWindowAnnotation) iter.next();
			if (lwa.getBegin()<=a.getBegin() && lwa.getEnd()==a.getEnd())
				return true;
		}
		return false;
	}

	
	// FIXME - Based on gpl'd code so can't be released (marginal to no effect on performance)
	public double calcPermStrDist () {
//		StringSim ss = new StringSim(s1, s2);
//		ss.setStopWords(stopwords);
//		return ss.calc();
		return 0.0;
	}

	public boolean calcAliasDrug (){
		return (alias && calcIsDrug());
	}

	public boolean calcAliasDisorder(){
		return (alias && calcIsDisorder());
	}

	public boolean calcAliasFinding(){
		return (alias && calcIsFinding());
	}

	public boolean calcAliasProcedure(){
		return (alias && calcIsProcedure());
	}

	public boolean calcAliasAnatomy(){
		return (alias && calcIsAnatomicalSite());
	}

}
