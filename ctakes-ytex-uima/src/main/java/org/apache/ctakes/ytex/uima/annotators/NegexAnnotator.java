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
package org.apache.ctakes.ytex.uima.annotators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textsem.ContextAnnotation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * Negex adapted to cTAKES. Checks negation status of named entities. 
 * `Loads negex triggers from classpath:
 * <tt>/org/apache/ctakes/ytex/uima/annotators/negex_triggers.txt</tt>
 * Loads negex ignore words from classpath:
 * <tt>/org/apache/ctakes/ytex/uima/annotators/negex_excluded_keys.txt</tt>
 * <p/>
 * The meaning of the certainty and confidence attributes is nowhere documented
 * for cTakes. There are several ways of handling 'maybes', see below. Default
 * behavior: certainty attribute for negated & possible terms set to -1
 * Configure with following uima initialization parameters:
 * <li>checkPossibilities : should we check for possibilities
 * <li>negatePossibilities : should possibilities be negated, default = true? if
 * true,
 * <ul>
 * <li>negated: polarity=-1, confidence=1
 * <li>possible: polarity=-1, confidence=-1
 * <li>affirmed: polarity=1, confidence=1
 * </ul
 * if false
 * <ul>
 * <li>negated: polarity=-1, confidence=1
 * <li>possible: polarity=1, confidence=-1
 * <li>affirmed: polarity=1, confidence=1
 * </ul>
 * <li>storeAsInterval
 * <ul>
 * <li>negated: polarity=-1, confidence = -1
 * <li>possible: polarity=1, confidence = 0.5
 * <li>affirmed: polarity=1, confidence = 1
 * </ul>
 * 
 * Added support for negating arbitrary annotations. Set the targetTypeName to
 * an annotation type. Will see if it is negated; if so will set the negated and
 * possible boolean values on the annotation.
 * 
 * @author vijay, heavily updated by Peter A.
 * 
 */
@PipeBitInfo(
		name = "Negation Annotator (Negex)",
		description = "Use negex to assign polarity to Named Entities.",
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class NegexAnnotator extends JCasAnnotator_ImplBase {
	private static final String NEGEX_EXCLUDED_KEYS = "/org/apache/ctakes/ytex/uima/annotators/negex_excluded_keys.txt";
	private static final String NEGEX_TRIGGERS = "/org/apache/ctakes/ytex/uima/annotators/negex_triggers.txt";
	private static final Log log = LogFactory.getLog(NegexAnnotator.class);
	private boolean negatePossibilities = true;
	private boolean checkPossibilities = true;
	private boolean storeAsInterval = false;
	private String targetTypeName = null;
	// only look for rules matching around the NE
	// by this window.  Long unpunctuated notes
	// may display tens of spurious matches which 
	// are thrown away for each NE.
	private final int MATCHER_WINDOW = 200;
	
	private final int STOP_INIT = Integer.MAX_VALUE;

	private HashMap<String,ArrayList<NegexRule>> wordCloud = new HashMap<String,ArrayList<NegexRule>>(300);
	
	// throwaway words used in negation expressions that would result in performing extra matches
	private List<String> excludedKeyWords = null;
	private int ruleCount;
	
	private final static String[] tagList = { "[CONJ]", "[PSEU]", "[PREN]", "[POST]", "[POSP]" };


	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		this.excludedKeyWords = this.initalizeExcludedKeyWords();
		this.initializeRules();
		
		negatePossibilities = getBooleanConfigParam(aContext,
				"negatePossibilities", negatePossibilities);
		if (negatePossibilities) {
			checkPossibilities = true;
		} else {
			checkPossibilities = getBooleanConfigParam(aContext,
					"checkPossibilities", checkPossibilities);
		}
		storeAsInterval = getBooleanConfigParam(aContext, "storeAsInterval",
				storeAsInterval);
		targetTypeName = (String) aContext
				.getConfigParameterValue("targetTypeName");
	}

	private boolean getBooleanConfigParam(UimaContext aContext, String param,
			boolean defaultVal) {
		Boolean paramValue = (Boolean) aContext.getConfigParameterValue(param);
		return paramValue == null ? defaultVal : paramValue;

	}
	
	private List<String> listReader(String path) throws ResourceInitializationException {
		List<String> list = new ArrayList<String>();
		BufferedReader reader = null;
		try {
			InputStream stream = this.getClass().getResourceAsStream(path);
			if (stream == null) {
				log.error("Unable to find resource: " + path);
				throw new ResourceInitializationException(path, null);
			}
			reader = new BufferedReader(new InputStreamReader(stream));
			String line = null;
			try {
				while ((line = reader.readLine()) != null)
					if (line.charAt(0) != '#') {
						list.add(line);
					}
			} catch (IOException e) {
				log.error("Error reading list: " + path, e);
			} 
			
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				log.error("Error closing list", e);
			}
		}
		return list;
	}
	
	private List<String> initalizeRuleList() throws ResourceInitializationException {
		List<String> rules = listReader(NEGEX_TRIGGERS);
		Collections.sort(rules, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				int l1 = o1.trim().length();
				int l2 = o2.trim().length();
				if (l1 < l2)
					return 1;
				else if (l1 > l2)
					return -1;
				else
					return 0;
			}

		});
		return rules;
	}
	
	private List<String> initalizeExcludedKeyWords() throws ResourceInitializationException {
		return listReader(NEGEX_EXCLUDED_KEYS);
	}

	private void initializeRules() throws ResourceInitializationException {
		List<String> listRules = this.initalizeRuleList();
		this.ruleCount = listRules.size();
		Iterator<String> iRule = listRules.iterator();
		while (iRule.hasNext()) {
			String rule = iRule.next();
			Pattern p = Pattern.compile("[\\t]+"); // Working.
			String[] ruleTokens = p.split(rule.trim());
			if (ruleTokens.length == 2) {
				// Add the regular expression characters to tokens and assemble
				// the rule again.
				String[] ruleMembers = ruleTokens[0].trim().split(" ");
				String rule2 = "";
				boolean punctRule = false;
				for (int i = 0; i <= ruleMembers.length - 1; i++) {
					if (!ruleMembers[i].equals("")) {
						if (ruleMembers.length == 1) {
							if (ruleMembers[0].length() == 1) {
								String chrRule = ruleMembers[0];
								if (chrRule.equals("\\")) {
									chrRule += "\\";
								}
								rule2 = "[" + chrRule + "]";
								punctRule = true;
							} else {
								rule2 = ruleMembers[i];
							}	
						} else {
							rule2 = rule2 + ruleMembers[i].trim() + "\\s+";
						}
					}
				}
				// Remove the last \\s+ (we will re-add it below)
				if (rule2.endsWith("\\s+")) {
					rule2 = rule2.substring(0, rule2.lastIndexOf("\\s+"));
				}
				
				String rule3 = null;

				if (punctRule) {
					rule3 = rule2 + "\\s+";
				} else {
					rule3 = "(?m)(?i)[[\\p{Punct}&&[^\\]\\[]]|\\s+]("
							+ rule2 + ")[[\\p{Punct}&&[^_]]|\\s+]";
				}

				Pattern p2 = Pattern.compile(rule3.trim());
				NegexRule aRule = new NegexRule(p2, rule2, ruleTokens[1].trim());
				populateWordCloud(ruleMembers, aRule);
				
			} else {
				log.warn("could not parse rule:" + rule);
			}
		}
		if (log.isDebugEnabled()) {
			// this helps to manualy populate the excluded words list
			for (Entry<String, ArrayList<NegexRule>> entry : wordCloud.entrySet()) {
				log.debug("key: " + entry.getKey() + "  expression count: " + entry.getValue().size());
			}
		}
		return;

	}

	/**
	 * Creates a hashtable of arrays where the keys are words in regexps that can predict
	 * a potential hit.  Each array is a bin of regex rules that are triggered if the key word is
	 * found in the incoming sentence text.  Note that rules can live in multiple bins, so at the moment
	 * of execution we also make sure that any single rule is only executed once for each NE.
	 * eg.  there is an entry with key 'no' and array of all rules containing "no" in their Regex.
	 * We will run these rules only on sentences that contain the string "no" 
	 * 
	 * It's not perfect, but it greatly reduces the number of match calls from the previous version
	 * which blindly executed  all of them all the time.
	 * 
	 * @param ruleMembers
	 * @param aRule
	 */
	private void populateWordCloud(String[] ruleMembers, NegexRule aRule) {
		for(String token : ruleMembers) {
			boolean skip = false;
			for(String stp : this.excludedKeyWords) {
				skip = (stp.equals(token));
				if(skip == true) break;
			}
			if (!skip) {
				ArrayList<NegexRule> entry = wordCloud.get(token);
				if(entry == null) {
					entry = new ArrayList<NegexRule>();
					wordCloud.put(token, entry);
					log.debug("Created a wordcloud bin for: " + token);
				}
				entry.add(aRule);
			}
		}
	}

	public static interface TargetAnnoFilter {
		public boolean filter(Annotation anno);
	}

	/**
	 * only bother with IdentifiedAnnotations that have concepts
	 * 
	 * @author vijay
	 * 
	 */
	public static class NamedEntityTargetAnnoFilter implements TargetAnnoFilter {
		@Override
		public boolean filter(Annotation anno) {
			if (!(anno instanceof IdentifiedAnnotation))
				return false;
			IdentifiedAnnotation ia = (IdentifiedAnnotation) anno;
			return ia.getOntologyConceptArr() != null
					&& ia.getOntologyConceptArr().size() > 0;
		}
	}

	@Override
	public void process(JCas aJCas) {
		AnnotationIndex<?> sentenceIdx = aJCas
				.getAnnotationIndex(Sentence.typeIndexID);
		AnnotationIndex<?> neIdx = aJCas
				.getAnnotationIndex(IdentifiedAnnotation.typeIndexID);
		negateAnnotations(aJCas, sentenceIdx, neIdx,
				new NamedEntityTargetAnnoFilter());
		if (targetTypeName != null) {
			try {
				negateAnnotations(
						aJCas,
						sentenceIdx,
						aJCas.getAnnotationIndex(aJCas.getTypeSystem().getType(
								targetTypeName)), null);
			} catch (Exception e) {
				log.error("error getting typeSystemId for " + targetTypeName, e);
			}
		}
	}

	private void negateAnnotations(JCas aJCas, AnnotationIndex<?> sentenceIdx,
			AnnotationIndex<?> targetIdx, TargetAnnoFilter filter) {
		FSIterator<?> sentenceIter = sentenceIdx.iterator();
		// initialize to beyond end of sentence
		int lastStop = STOP_INIT;
		while (sentenceIter.hasNext()) {
			Sentence s = (Sentence) sentenceIter.next();
			String sText = "." + s.getCoveredText().toLowerCase() + ".";
			FSIterator<?> neIter = targetIdx.subiterator(s);
			while (neIter.hasNext()) {
				Annotation ne = (Annotation) neIter.next();
				if (filter == null || filter.filter(ne)) {
					int thisStop = checkNegation(aJCas, sText, s,  ne, lastStop);
					// pick up from the last 
					// CONJ tag and move forward.
					// [preneg?] NE NE [postneg?] [CONJ] (next possible negations)
					// reduces the number of matcher calls in complex sentences.
					if (thisStop > lastStop || lastStop == STOP_INIT) {
						lastStop = thisStop;
					}
					log.debug("LastStop:" + lastStop);
				}
			}
		}
	}

	public static class NegexRule {

		@Override
		public String toString() {
			return "NegexRule [rule=" + rule + ", tag=" + tag + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((rule == null) ? 0 : rule.hashCode());
			result = prime * result + ((tag == null) ? 0 : tag.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NegexRule other = (NegexRule) obj;
			if (rule == null) {
				if (other.rule != null)
					return false;
			} else if (!rule.equals(other.rule))
				return false;
			if (tag == null) {
				if (other.tag != null)
					return false;
			} else if (!tag.equals(other.tag))
				return false;
			return true;
		}

		private Pattern pattern;
		private String tag;
		private String rule;

		public Pattern getPattern() {
			return pattern;
		}

		public void setPattern(Pattern pattern) {
			this.pattern = pattern;
		}

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public String getRule() {
			return rule;
		}

		public void setRule(String rule) {
			this.rule = rule;
		}

		public NegexRule() {
			super();
		}

		public NegexRule(Pattern pattern, String rule, String tag) {
			super();
			this.pattern = pattern;
			this.tag = tag;
			this.rule = rule;
		}
	}

	public static class NegexToken implements Comparable<NegexToken> {
		private int start;
		private int end;
		private NegexRule rule;

		@Override
		public String toString() {
			return "NegexToken [start=" + start + ", end=" + end + ", rule="
					+ rule + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + end;
			result = prime * result + ((rule == null) ? 0 : rule.hashCode());
			result = prime * result + start;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NegexToken other = (NegexToken) obj;
			if (end != other.end)
				return false;
			if (rule == null) {
				if (other.rule != null)
					return false;
			} else if (!rule.equals(other.rule))
				return false;
			if (start != other.start)
				return false;
			return true;
		}

		public NegexToken(int start, int end, NegexRule rule) {
			super();
			this.start = start;
			this.end = end;
			this.rule = rule;
		}

		@Override
		public int compareTo(NegexToken o) {
			return new Integer(this.start).compareTo(o.start);
		}

		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		public String getTag() {
			return rule.getTag();
		}

	}

	private NegexToken findTokenByTag(String tag, String stopTags[],
			boolean before, int neRelStart, int neRelEnd, NegexToken tokens[]) {
		Set<String> stopTagSet = new HashSet<String>(stopTags.length);
		stopTagSet.addAll(Arrays.asList(stopTags));
		if (before) {
			for (int i = neRelStart - 1; i > 0; i--) {
				if (tokens[i] != null) {
					if (tokens[i].getTag().equals(tag)) {
						return tokens[i];
					} else if (stopTagSet.contains(tokens[i].getTag()))
						break;
				}
			}
		} else {
			for (int i = neRelEnd; i < tokens.length; i++) {
				if (tokens[i] != null) {
					if (tokens[i].getTag().equals(tag)) {
						return tokens[i];
					} else if (stopTagSet.contains(tokens[i].getTag()))
						break;
				}
			}
		}
		return null;
	}

	/**
	 * check the negation status of the specfied term in the specified sentence
	 * 
	 * @param aJCas
	 *            for adding annotations
	 * @param sText
	 * 				the covered text bracketed by . so it doesn't have to be re-done for each annotation in a sentence
	 * @param s
	 *            the sentence in which we will look
	 * @param ne
	 *            the named entity whose negation status will be checked.
	 * @param lastpos
	 * 			In the latter parts of compound sentences where the negation mode shifts, we can use this 
	 * 			to ignore what we've already processed, to reduce the number of unnecessary regex matches.
	 * @return endIndex for a possible [CONJ] after the current annotation.  This helps
	 			  reset the start for subsequent regex scans to after the [CONJ]
	 */
	private int checkNegation(JCas aJCas, String sText, Sentence s, Annotation ne, int lastStop) {
		if (storeAsInterval && ne instanceof IdentifiedAnnotation) {
			// default is affirmed, which is coded as confidence = 1
			((IdentifiedAnnotation) ne).setConfidence(1);
		}
		// allocate array of tokens
		// this maps each character of the sentence to a token
		NegexToken[] tokens = new NegexToken[sText.length()];
		// char buffer for modify the sentence
		// we want to 'black out' trigger words already found and the phrase we
		// were looking for
		CharBuffer buf = CharBuffer.wrap(sText.toCharArray());
		// calculate location of the ne relative to the sentence
		int neRelStart = ne.getBegin() - s.getBegin() + 1;
		int neRelEnd = ne.getEnd() - s.getBegin() + 1;
		
		for (int i = neRelStart; i < neRelEnd; i++) {
			// black out the named entity from the char buffer
			buf.put(i, '_');
		}

		// but if there was a stop clause from a previous phrase, blank up until then too.
		// because we may have a new negation 
		if (lastStop != 0 && lastStop < neRelStart) {
			for (int i = 0; i < lastStop ; i++) {
				buf.put(i, '_');
			}
		}
		
		if(log.isDebugEnabled()) {
			// look for negex rules in the sentence
			log.debug("Negex sentence: " + s.getCoveredText());
			log.debug("Negex NE: ("+neRelStart+","+neRelEnd+")" + ne.getCoveredText());
		}
		
		populateHits(tokens, buf, neRelStart, neRelEnd);
		// pre-negation
		// look for a PREN rule before the ne, without any intervening stop tags
		NegexToken t = this.findTokenByTag("[PREN]", tagList, true, neRelStart,
				neRelEnd, tokens);
		if (t != null) {
			// hit - negate the ne
			annotateNegation(aJCas, s, ne, t, true, false);
		} else {
			// look for POST rule after the ne, without any intervening stop
			// tags
			t = this.findTokenByTag("[POST]", tagList, false,
					neRelStart, neRelEnd, tokens);
			if (t != null) {
				annotateNegation(aJCas, s, ne, t, true, false);
			} else if (this.checkPossibilities || this.negatePossibilities) {
				// check possibles
				t = this.findTokenByTag("[PREP]", tagList, true,
						neRelStart, neRelEnd, tokens);
				if (t != null) {
					annotateNegation(aJCas, s, ne, t, false, true);
				} else {
					t = this.findTokenByTag("[POSP]", tagList, false,
							neRelStart, neRelEnd, tokens);
					if (t != null)
						annotateNegation(aJCas, s, ne, t, true, true);
				}
			}
		}

		// now see if we found the first stop clause (CONJ) only after our current entity
		// if not actions need to proceed from sentence start

		if (findTokenByTag("[CONJ]", tagList, false, neRelEnd,
				neRelEnd, tokens) != null) {
			return 0;
		}
		
		// now see if we found a stop clause (CONJ) before our current entity
		// if so, set up a blanking index we can use in the next iteration to ignore
		// any possible negations that came before the semantic break.
		//    "no headache but reports rash"  
		//     "rash but not headache"
		// should bother work

		// now look for one before current annotation
		t = this.findTokenByTag("[CONJ]", tagList, true, neRelStart,
				neRelStart, tokens);
		if (t != null) 
			return t.getEnd();

		return 0;
	}

	/**
	 * Peter A. 12-2021
	 * Try to execute as few Regex operations as possible
	 * We do this by only testing rules that have at least one word in common with the current sentence frag
	 * with the words in sentence buf.  We are eliminating the rules we know would fail.
	 * and matcher is much less efficient than indexOf
	 * @param tokens
	 * @param buf
	 * @param neEnd 
	 * @param neStart 
	 */
	private void populateHits(NegexToken[] tokens, CharBuffer buf, int neStart, int neEnd) {
		String bText = buf.toString();
		int count = 0;
		HashMap<Integer,Integer> deDupe = new HashMap<Integer,Integer>(this.ruleCount);
		for (Entry<String, ArrayList<NegexRule>> ent : this.wordCloud.entrySet()) {
			if (bText.indexOf(ent.getKey()) >= 0) {
				for (NegexRule rule : ent.getValue()) {
					Integer iH = new Integer(rule.hashCode());
					if (deDupe.containsKey(iH)) {
						// do not execute the same rule twice
						// this is because in the wordCloud, same rules may occur in different bins
						continue;
					}
					deDupe.put(iH, iH);
					Matcher m = rule.getPattern().matcher(buf);
					count++;
					while (m.find() == true) {
						if (log.isDebugEnabled()) {
							log.debug("Regex buf before: " + buf.toString());
							log.debug("rule: \'" + rule.getRule() + "\' match at :" + m.start() + "," + m.end() );
						}
						// in poorly punctuated notes ignore matches which occur far from the NE we
						// are judging.
						if ((m.start() < Math.max(0, neStart - MATCHER_WINDOW)) ||
								m.end() > (neEnd + MATCHER_WINDOW)) {
							break;
						}
						deDupe.clear();
						boolean bUnoccupied = true;
						// When two adjacent rules share the same punctuation or space
						// code must allow for overlap of one character e.g. in the phrase  "A but no B" 
						// " but " is CONJ while " no " is PREN.  The space between then shows up on both
						// regex matches!!!
						for (int i = m.start(); i < m.end() && bUnoccupied; i++)
							bUnoccupied = (tokens[i] == null || (tokens[i].getEnd() - 1) == i);
						
						if (bUnoccupied) {
							// mark the range in the sentence with this token
							NegexToken t = new NegexToken(m.start(), m.end(), rule);
							for (int i = m.start(); i < m.end() && bUnoccupied; i++) {
								// blank out this range from the char buffer
								buf.put(i, '_');
								// add the token to the array
								tokens[i] = t;
							}
							// sync text with new buf;
							log.debug("Regex buf after: " + buf.toString());
							bText = buf.toString();
						}
					}
				}
			}
		}
		log.debug("Rules tried: " + count);
	}

	/**
	 * set the certainty/confidence flag on a named entity, and add a negation
	 * context annotation.
	 * 
	 * @param aJCas
	 * @param s
	 *            used to figure out text span
	 * @param ne
	 *            the certainty/confidence will be set to -1
	 * @param t
	 *            the token
	 * @param fSetCertainty
	 *            should we set the certainty (true) or confidence (false)
	 */
	private void annotateNegation(JCas aJCas, Sentence s, Annotation anno,
			NegexToken t, boolean negated, boolean possible) {
		if (anno instanceof IdentifiedAnnotation) {
			IdentifiedAnnotation ne = (IdentifiedAnnotation) anno;
			if (!storeAsInterval) {
				if (possible)
					ne.setConfidence(-1);
				if (negated || (this.negatePossibilities && possible))
					ne.setPolarity(-1);
			} else {
				ne.setPolarity(negated || possible ? -1 : 0);
				float confidence = negated ? -1 : 1;
				if (possible)
					confidence *= 0.5;
				ne.setConfidence(confidence);
			}
		} else {
			try {
				BeanUtils.setProperty(anno, "negated", negated);
				BeanUtils.setProperty(anno, "possible", possible);
			} catch (IllegalAccessException iae) {
				log.error("error negating annotation", iae);
			} catch (InvocationTargetException e) {
				log.error("error negating annotation", e);
			}
		}
		ContextAnnotation nec = new ContextAnnotation(aJCas);
		// There is a bug way back in UIMA source which occasionally
		// returns a begin of -1 when certain POS types begin a sentence.
		int begin = Math.max(0, (s.getBegin() + t.getStart() - 1));
		nec.setBegin(begin);
		nec.setEnd(s.getBegin() + t.getEnd() - 1);
		nec.setScope(t.getTag());
		nec.setFocusText(anno.getCoveredText());
		nec.addToIndexes();
	}

}
