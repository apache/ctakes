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
package org.apache.ctakes.ytex.uima.annotators;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import opennlp.tools.sentdetect.DefaultSDContextGenerator;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.sentence.EndOfSentenceScannerImpl;
import org.apache.ctakes.core.sentence.SentenceDetectorCtakes;
import org.apache.ctakes.core.util.ParamUtil;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.common.base.Strings;

/**
 * Wraps the OpenNLP sentence detector in a UIMA annotator.
 * 
 * Changes:
 * <ul>
 * <li>split on paragraphs before feeding into maximum entropy model
 * <li>don't split on newlines
 * <li>split on periods
 * <li>split on semi-structured text such as checkboxes
 * </ul>
 * 
 * Parameters (optional):
 * <ul>
 * <li>paragraphPattern: regex to split paragraphs. default PARAGRAPH_PATTERN
 * <li>acronymPattern: default ACRONYM_PATTERN. If the text preceding period
 * matches this pattern, we do not split at the period
 * <li>periodPattern: default PERIOD_PATTERN. If the text following period
 * matches this pattern, we split it.
 * <li>splitPattern: regex to split at semi-structured fields. default
 * SPLIT_PATTERN
 * </ul>
 * 
 * 
 * 
 * @author Mayo Clinic
 * @author vijay
 */
public class SentenceDetector extends JCasAnnotator_ImplBase {
	/**
	 * Value is "SegmentsToSkip". This parameter specifies which sections to
	 * skip. The parameter should be of type String, should be multi-valued and
	 * optional.
	 */
	public static final String PARAM_SEGMENTS_TO_SKIP = "SegmentsToSkip";

	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	public static final String SD_MODEL_FILE_PARAM = "SentenceModelFile";

	private opennlp.tools.sentdetect.SentenceModel sdmodel;
	/**
	 * vng change split paragraphs on this pattern
	 */
	public static final String PARAGRAPH_PATTERN = "(?m):\\r{0,1}\\n|\\r{0,1}\\n\\r{0,1}\\n";
	/**
	 * vng change split sentences periods that do not have this acronym
	 * preceding it
	 */
	public static final String ACRONYM_PATTERN = "(?m)Dr\\z|Ms\\z|Mr\\z|Mrs\\z|Ms\\z|\\p{Upper}\\z";
	/**
	 * vng change split sentences periods after which this pattern is seen
	 */
	public static final String PERIOD_PATTERN = "(?m)\\A\\s+\\p{Upper}|\\A\\s+\\d\\.";
	/**
	 * vng change split sentences on these patterns
	 */
	public static final String SPLIT_PATTERN = "(?im)\\n[\\(\\[]\\s*[yesxno]{0,3}\\s*[\\)\\]]|[\\(\\[]\\s*[yesxno]{0,3}\\s*[\\)\\]]\\s*\\r{0,1}\\n|^[^:\\r\\n]{3,20}\\:[^\\r\\n]{3,20}$";
	/**
	 * vng change
	 */
	private Pattern paragraphPattern;
	/**
	 * vng change
	 */
	private Pattern splitPattern;
	/**
	 * vng change
	 */
	private Pattern periodPattern;
	/**
	 * vng change
	 */
	private Pattern acronymPattern;

	private UimaContext context;

	private Set<?> skipSegmentsSet;

	private SentenceDetectorCtakes sentenceDetector;

	private String NEWLINE = "\n";

	private int sentenceCount = 0;

	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		super.initialize(aContext);
		logger.info(Arrays.asList(aContext.getConfigParameterNames()));

		context = aContext;
		try {
			configInit();
		} catch (Exception ace) {
			throw new ResourceInitializationException(ace);
		}
	}

	/**
	 * Reads configuration parameters.
	 * 
	 * @throws ResourceAccessException
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	private void configInit() throws ResourceAccessException, InvalidFormatException, IOException {

		String sdModelPath = (String) context
				.getConfigParameterValue(SD_MODEL_FILE_PARAM);
			InputStream is = FileLocator.getAsStream(sdModelPath);
			logger.info("Sentence detector model file: " + sdModelPath);
			sdmodel = new SentenceModel(is);
			is.close();
			EndOfSentenceScannerImpl eoss = new EndOfSentenceScannerImpl();
			char[] eosc = eoss.getEndOfSentenceCharacters();
			// SentenceDContextGenerator cg = new SentenceDContextGenerator();
			DefaultSDContextGenerator cg = new DefaultSDContextGenerator(eosc);
			sentenceDetector = new SentenceDetectorCtakes(sdmodel.getMaxentModel(), cg, eoss);

			skipSegmentsSet = ParamUtil.getStringParameterValuesSet(
					PARAM_SEGMENTS_TO_SKIP, context);
			// vng change begin
			paragraphPattern = compilePatternCheck("paragraphPattern",
					PARAGRAPH_PATTERN);
			splitPattern = compilePatternCheck("splitPattern", SPLIT_PATTERN);
			periodPattern = compilePatternCheck("periodPattern", PERIOD_PATTERN);
			acronymPattern = compilePatternCheck("acronymPattern", ACRONYM_PATTERN);
			// vng change end
	}
	/**
	 * vng change
	 */
	private Pattern compilePatternCheck(String patternKey, String patternDefault) {
		String strPattern = (String) context
				.getConfigParameterValue(patternKey);
		if (strPattern == null)
			strPattern = patternDefault;
		Pattern pat = null;
		try {
			pat = Strings.isNullOrEmpty(strPattern) ? null : Pattern
					.compile(strPattern);
		} catch (PatternSyntaxException pse) {
			logger.warn("ignoring bad pattern, reverting to default: "
					+ strPattern, pse);
			pat = Pattern.compile(patternDefault);
		}
		return pat;
	}

	/**
	 * Entry point for processing.
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		logger.info("Starting processing.");

		sentenceCount = 0;

		String text = jcas.getDocumentText();

		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> sectionItr = indexes.getAnnotationIndex(Segment.type)
				.iterator();
		while (sectionItr.hasNext()) {
			Segment sa = (Segment) sectionItr.next();
			String sectionID = sa.getId();
			if (!skipSegmentsSet.contains(sectionID)) {
				sentenceCount = annotateParagraph(jcas, text, sa.getBegin(),
						sa.getEnd(), sentenceCount);
			}
		}
	}

	/**
	 * split paragraphs. Arc v1.0 had a paragraph splitter, and sentences never
	 * crossed paragraph boundaries. paragraph splitter was lost in upgrade to
	 * ctakes 1.3.2. Now split paragraphs before running through maximum entropy
	 * model - this resolves situations where the model would split after a
	 * period, e.g.:
	 * 
	 * <pre>
	 * Clinical History:
	 * Mr. So and so
	 * </pre>
	 * 
	 * Without the paragraph splitter, the model splits after Mr. With the
	 * paragraph splitter, the model doesn't split after Mr.
	 * 
	 * @param jcas
	 * @param text
	 * @param b
	 * @param e
	 * @param sentenceCount
	 * @return
	 * @throws AnalysisEngineProcessException 
	 * @throws AnnotatorProcessException
	 */
	protected int annotateParagraph(JCas jcas, String text, int b, int e,
			int sentenceCount) throws AnalysisEngineProcessException {
		if (this.paragraphPattern == null) {
			return this.annotateRange(jcas, text, b, e, sentenceCount);
		} else {
			int lastEnd = b;
			Matcher m = paragraphPattern.matcher(text);
			while (m.find()) {
				if (m.end() > b && m.end() < e) {
					sentenceCount = annotateRange(jcas, text, lastEnd, m.end(),
							sentenceCount);
					lastEnd = m.end();
				} else if (m.end() >= e) {
					break;
				}
			}
			sentenceCount = annotateRange(jcas, text, lastEnd, e, sentenceCount);
			return sentenceCount;
		}
	}

	/**
	 * Detect sentences within a section of the text and add annotations to the
	 * CAS. Uses OpenNLP sentence detector, and then additionally forces
	 * sentences to end at end-of-line characters (splitting into multiple
	 * sentences). Also trims sentences. And if the sentence detector does
	 * happen to form a sentence that is just white space, it will be ignored.
	 * 
	 * @param jcas
	 *            view of the CAS containing the text to run sentence detector
	 *            against
	 * @param text
	 *            the document text
	 * @param section
	 *            the section this sentence is in
	 * @param sentenceCount
	 *            the number of sentences added already to the CAS (if
	 *            processing one section at a time)
	 * @return count The sum of <code>sentenceCount</code> and the number of
	 *         Sentence annotations added to the CAS for this section
	 * @throws AnnotatorProcessException
	 */
	protected int annotateRange(JCas jcas, String text, int b, int e,
			int sentenceCount) throws AnalysisEngineProcessException {

		// vng change begin
		// int b = section.getBegin();
		// int e = section.getEnd();
		// vng chang end

		// Use OpenNLP tools to split text into sentences
		// The sentence detector returns the offsets of the sentence-endings it
		// detects
		// within the string
		int[] sentenceBreaks = sentenceDetector.sentPosDetect(text.substring(b,
				e)); // OpenNLP tools 1.5 returns Spans rather than offsets that
						// 1.4 did
		int numSentences = sentenceBreaks.length;
		// There might be text after the last sentence-ending found by detector,
		// so +1
		SentenceSpan[] potentialSentSpans = new SentenceSpan[numSentences + 1];

		int sentStart = b;
		int sentEnd = b;
		// Start by filling in sentence spans from what OpenNLP tools detected
		// Will trim leading or trailing whitespace when check for end-of-line
		// characters
		for (int i = 0; i < numSentences; i++) {
			sentEnd = sentenceBreaks[i] + b; // OpenNLP tools 1.5 returns Spans
												// rather than offsets that 1.4
												// did
			String coveredText = text.substring(sentStart, sentEnd);
			potentialSentSpans[i] = new SentenceSpan(sentStart, sentEnd,
					coveredText);
			sentStart = sentEnd;
		}

		// If detector didn't find any sentence-endings,
		// or there was text after the last sentence-ending found,
		// create a sentence from what's left, as long as it's not all
		// whitespace.
		// Will trim leading or trailing whitespace when check for end-of-line
		// characters
		if (sentEnd < e) {
			String coveredText = text.substring(sentEnd, e);
			if (coveredText.trim() != "") {
				potentialSentSpans[numSentences] = new SentenceSpan(sentEnd, e,
						coveredText);
				numSentences++;
			}
		}

		// Copy potentialSentSpans into sentenceSpans,
		// ignoring any that are entirely whitespace,
		// trimming the rest,
		// and splitting any of those that contain an end-of-line character.
		// Then trim any leading or trailing whitespace of ones that were split.
		ArrayList<SentenceSpan> sentenceSpans1 = new ArrayList<SentenceSpan>(0);
		for (int i = 0; i < potentialSentSpans.length; i++) {
			if (potentialSentSpans[i] != null) {
				sentenceSpans1.addAll(potentialSentSpans[i]
						.splitAtLineBreaksAndTrim(NEWLINE)); // TODO Determine
																// line break
																// type
			}
		}
		// vng change begin
		// split at ".  "
		ArrayList<SentenceSpan> sentenceSpans = new ArrayList<SentenceSpan>(
				sentenceSpans1.size());
		for (SentenceSpan span : sentenceSpans1) {
			if (span != null) {
				sentenceSpans.addAll(span.splitAtPeriodAndTrim(acronymPattern,
						periodPattern, splitPattern));
			}
		}
		// vng change end

		// Add sentence annotations to the CAS
		int previousEnd = -1;
		for (int i = 0; i < sentenceSpans.size(); i++) {
			SentenceSpan span = sentenceSpans.get(i);
			if (span.getStart() != span.getEnd()) { // skip empty lines
				Sentence sa = new Sentence(jcas);
				sa.setBegin(span.getStart());
				sa.setEnd(span.getEnd());
				if (previousEnd <= sa.getBegin()) {
					// System.out.println("Adding Sentence Annotation for " +
					// span.toString());
					sa.setSentenceNumber(sentenceCount);
					sa.addToIndexes();
					sentenceCount++;
					previousEnd = span.getEnd();
				} else {
					logger.error("Skipping sentence from " + span.getStart()
							+ " to " + span.getEnd());
					logger.error("Overlap with previous sentence that ended at "
							+ previousEnd);
				}
			}
		}
		return sentenceCount;
	}

	/**
	 * Train a new sentence detector from the training data in the first file
	 * and write the model to the second file.<br>
	 * The training data file is expected to have one sentence per line.
	 * 
	 * @param args
	 *            training_data_filename name_of_model_to_create iters? cutoff?
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final Logger logger = Logger.getLogger(SentenceDetector.class.getName()
				+ ".main()");

		// Handle arguments
		if (args.length < 2 || args.length > 4) {
			usage(logger);
			System.exit(-1);
		}

		File inFile = getReadableFile(args[0]);

		File outFile = getFileInExistingDir(args[1]);
		// File outFile = new File(args[1]);

		int iters = 100;
		if (args.length > 2) {
			iters = parseInt(args[2], logger);
		}

		int cut = 5;
		if (args.length > 3) {
			cut = parseInt(args[3], logger);
		}

		// Now, do the actual training
		EndOfSentenceScannerImpl scanner = new EndOfSentenceScannerImpl();
		int numEosc = scanner.getEndOfSentenceCharacters().length;

		logger.info("Training new model from " + inFile.getAbsolutePath());
		logger.info("Using " + numEosc + " end of sentence characters.");

		logger.error("----------------------------------------------------------------------------------");
		logger.error("Need to update yet for OpenNLP changes "); // TODO
		logger.error("Commented out code that no longer compiles due to OpenNLP API incompatible changes"); // TODO
		logger.error("----------------------------------------------------------------------------------");
		// GISModel mod = SentenceDetectorME.train(inFile, iters, cut, scanner);
		// SuffixSensitiveGISModelWriter ssgmw = new
		// SuffixSensitiveGISModelWriter(
		// mod, outFile);
		// logger.info("Saving the model as: " + outFile.getAbsolutePath());
		// ssgmw.persist();

	}

	public static void usage(Logger log) {
		log.info("Usage: java "
				+ SentenceDetector.class.getName()
				+ " training_data_filename name_of_model_to_create <iters> <cut>");
	}

	public static int parseInt(String s, Logger log) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			log.error("Unable to parse '" + s + "' as an integer.");
			throw (nfe);
		}
	}

	public static File getReadableFile(String fn) throws IOException {
		File f = new File(fn);
		if (!f.canRead()) {
			throw new IOException("Unable to read from file "
					+ f.getAbsolutePath());
		}
		return f;
	}

	public static File getFileInExistingDir(String fn) throws IOException {
		File f = new File(fn);
		if (!f.getParentFile().isDirectory()) {
			throw new IOException("Directory not found: "
					+ f.getParentFile().getAbsolutePath());
		}
		return f;
	}

}
