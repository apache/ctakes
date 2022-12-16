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
package org.apache.ctakes.smokingstatus.ae;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;

/**
 * UIMA annotator that uses some patterns and some rules about those patterns to
 * adjust certain annotations.
 * 
 * This annotator was extended to handle sentence
 * boundaries for the Smoking status classification: Example: "Tobacco: none"
 * has two sentences as detected by the sentence boundary detector. This
 * annotator merges them into one sentence to enable correct negation detection.
 * 
 */
public class SentenceAdjuster extends JCasAnnotator_ImplBase {
	/**
	 * The list of words ("and" "&") to ignore in pattern matching.
	 */
	public static final String PARAM_IGNORE_WORDS = "WordsToIgnore";

	/**
	 * The list of words ("none", "no", etc) used in the pattern
	 */
	public static final String PARAM_WORDS_IN_PATTERN = "WordsInPattern";

	// LOG4J logger based on class name
	public Logger iv_logger = Logger.getLogger(getClass().getName());



	/**
	 * Performs initialization logic. This implementation just reads values for
	 * the configuration parameters. This method is not invoked for every
	 * document processed.
	 * 
	 * @see com.ibm.uima.analysis_engine.annotator.BaseAnnotator#initialize(UimaContext)
	 */
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException

	{

		super.initialize(aContext);

		context = aContext;
		try {
			configInit();
		} catch (AnnotatorContextException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Sets configuration parameters with values from the descriptor.
	 */
	private void configInit() throws AnnotatorContextException {
		// populate the HashSet of words that we will ignore when pattern
		// matching
		String[] ignoreWords = (String[]) context
				.getConfigParameterValue(PARAM_IGNORE_WORDS);

		wordsToIgnore = new HashSet<String>();
		for (int i = 0; i < ignoreWords.length; i++)
			wordsToIgnore.add(ignoreWords[i]);

		if (iv_logger.isInfoEnabled())
			iv_logger.info("Loaded list of " + ignoreWords.length
					+ " words to ignore during adjustment.");

		// populate the HashSet of the laterality or tobacco-related words
		String[] patternWords = (String[]) context
				.getConfigParameterValue(PARAM_WORDS_IN_PATTERN);
		wordsInPattern = new HashSet<String>();
		for (int i = 0; i < patternWords.length; i++)
			wordsInPattern.add(patternWords[i]);

		if (iv_logger.isInfoEnabled())
			iv_logger.info("Loaded list of " + patternWords.length
					+ " pattern words for adjustment.");

		useSegments = ((Boolean) context.getConfigParameterValue("UseSegments"))
				.booleanValue();
		String[] skipSegmentIDs = (String[]) context
				.getConfigParameterValue("SegmentsToSkip");
		skipSegmentsSet = new HashSet<String>();
		for (int i = 0; i < skipSegmentIDs.length; i++)
			skipSegmentsSet.add(skipSegmentIDs[i]);

		if (iv_logger.isInfoEnabled())
			iv_logger.info("List of words to ignore during adjustment:");

		Object[] o = wordsToIgnore.toArray();
		// String [] ignoreTheseWords = (String []) wordsToIgnore.toArray();

		if (iv_logger.isInfoEnabled()) {
			for (int i = 0; i < ignoreWords.length; i++)
				iv_logger.info("  " + o[i]);
		}

	}

	/**
	 * Invokes this annotator's analysis logic. Invoked for each document
	 * processed.
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String text = jcas.getDocumentText();
		try {
			// just one sentence
			iv_logger.info(" jcas "+ jcas.getViewName());
			if (!useSegments) {
				// annotate over full doc text
				annotateRange(jcas, text, 0, text.length());
			} else {
				JFSIndexRepository indexes = jcas.getJFSIndexRepository();
				Iterator<?> segmentItr = indexes.getAnnotationIndex(
						Segment.type).iterator();
				while (segmentItr.hasNext()) {
					Segment segmentAnnotation = (Segment) segmentItr.next();
					String segmentID = segmentAnnotation.getId();

					if (!skipSegmentsSet.contains(segmentID)) {
						int start = segmentAnnotation.getBegin();
						int end = segmentAnnotation.getEnd();
						annotateRange(jcas, text, start, end);
					}
				}
			}
		} catch (AnnotatorContextException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A utility method that annotates a given range.
	 */
	protected void annotateRange(JCas jcas, String text, int rangeBegin,
			int rangeEnd) throws AnnotatorContextException {
		if (iv_logger.isInfoEnabled())
			iv_logger.info("started Sentence merging process.");

		JFSIndexRepository indexes = jcas.getJFSIndexRepository();

		Iterator<?> sentItr = indexes.getAnnotationIndex(Sentence.type)
				.iterator();
		Sentence prevSent = null;
		Sentence currSent = null;

		Map<Integer, Sentence> sentences = getSentencesOrderById(sentItr);

		// find the first Sentence in the specified range
		for (int i = 0; i < sentences.size(); i++) {
			prevSent = currSent;
			currSent = (Sentence) sentences.get(new Integer(i));

			if (currSent == null)
				iv_logger.error("Wow! some sentence is null");

			if (prevSent == null)
				continue; // got to have 2 sentences
// Added a check for the currSent being null which should not be happening, but apparently is for some reason 10/7/2011
			if (prevSent.getCoveredText().endsWith(":") && currSent != null) {
				int newEnd = -1;
				if ((currSent.getSentenceNumber() - 1) == prevSent
						.getSentenceNumber()) {
					// System.out.println("Found adjecent sentence: " +
					// prevSent.getSentenceNumber() + " is next to " +
					// currSent.getSentenceNumber());
					String textSent2 = currSent.getCoveredText().toLowerCase();
					Iterator<String> itWordsInPattern = wordsInPattern
							.iterator();
					while (itWordsInPattern.hasNext()) {
						String word = (String) itWordsInPattern.next();
						// System.out.println("Checking for word: " + word);
						if (textSent2.startsWith(word)) {
							// System.out.println("Sentence begins with word: "
							// + word);
							newEnd = currSent.getEnd();
							// System.out.println("Old Sentence ["+text.substring(prevSent.getBegin(),
							// prevSent.getEnd())+"]");
							prevSent.setEnd(newEnd);
							// System.out.println("New Sentence ["+text.substring(prevSent.getBegin(),
							// prevSent.getEnd())+"]");
							currSent.removeFromIndexes();
						}
					}
				}
			}
		}
	}

	private Map<Integer, Sentence> getSentencesOrderById(Iterator<?> sentItr) {
		Map<Integer, Sentence> sentences = new HashMap<Integer, Sentence>();
		while (sentItr.hasNext()) {
			Sentence sa = (Sentence) sentItr.next();
			int snum = sa.getSentenceNumber();

			sentences.put((new Integer(snum)), sa);
		}

		return sentences;
	}
	private UimaContext context;

	private HashSet<String> wordsToIgnore;
	private HashSet<String> wordsInPattern;

	private boolean useSegments;
	private Set<String> skipSegmentsSet;
}