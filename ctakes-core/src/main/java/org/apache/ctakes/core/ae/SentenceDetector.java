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
package org.apache.ctakes.core.ae;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.sentdetect.*;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.sentence.EndOfSentenceScannerImpl;
import org.apache.ctakes.core.sentence.SentenceDetectorCtakes;
import org.apache.ctakes.core.sentence.SentenceSpan;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Wraps the OpenNLP sentence detector in a UIMA annotator.
 *
 * <p>Note that this class is intended to address the idiosyncrasies of
 * clinical text, and the things it tags as "sentences" are more like
 * phrases in English prose text.  In particular, it will terminate
 * a sentences at a colon or semicolon (via EndOfSentenceScannerImpl)
 * or at a newline.
 *
 * <p>If you need sentence detection that suits prose text, see the
 * alternative mentioned below.
 * 
 * @author Mayo Clinic
 * @see org.apache.ctakes.core.ae.SentenceDetectorAnnotatorBIO
 */
@PipeBitInfo(
      name = "Sentence Detector",
      description = "Annotates Sentences based upon an OpenNLP model.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.SENTENCE }
)
public class SentenceDetector extends JCasAnnotator_ImplBase {
	/**
	 * Value is "SegmentsToSkip". This parameter specifies which sections to
	 * skip. The parameter should be of type String, should be multi-valued and
	 * optional.
	 */
	public static final String PARAM_SEGMENTS_TO_SKIP = "SegmentsToSkip";
	@ConfigurationParameter(
	    name = PARAM_SEGMENTS_TO_SKIP,
	    mandatory = false,
	    description = "Set of segments that can be skipped"
	    )
  private String[] skipSegmentsArray;
	private Set<String> skipSegmentsSet;
	
	public static final String PARAM_SD_MODEL_FILE = "SentenceModelFile";
	public static final String SD_MODEL_FILE_PARAM = PARAM_SD_MODEL_FILE; // backwards compatibility
	@ConfigurationParameter(
	    name = PARAM_SD_MODEL_FILE,
         description = "Path to sentence detector model file",
         defaultValue = "org/apache/ctakes/core/models/sentdetect/sd-med-model.zip"
   )
	private String sdModelPath;
	
	private opennlp.tools.sentdetect.SentenceModel sdmodel;

	private SentenceDetectorCtakes sentenceDetector;

	private String NEWLINE = "\n";

  // LOG4J logger based on class name
  static private final Logger LOGGER = LogManager.getLogger( "SentenceDetector" );

  @Override
  public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		try (InputStream is = FileLocator.getAsStream(sdModelPath)){
		  LOGGER.info("Sentence detector model file: " + sdModelPath);
		  sdmodel = new SentenceModel(is);
		  EndOfSentenceScannerImpl eoss = new EndOfSentenceScannerImpl();
		  DefaultSDContextGenerator cg = new DefaultSDContextGenerator(eoss.getEndOfSentenceCharacters());
		  sentenceDetector = new SentenceDetectorCtakes(sdmodel.getMaxentModel(), cg, eoss);

		  skipSegmentsSet = new HashSet<>();
		  if(skipSegmentsArray != null){
		    Collections.addAll(skipSegmentsSet, skipSegmentsArray);
		  }
    } catch (IOException e) {
      e.printStackTrace();
      throw new ResourceInitializationException(e);
    }
	}

	/**
	 * Entry point for processing.
	 */
	@Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

		LOGGER.info("Starting processing.");

		int sentenceCount = 0;

		String text = jcas.getDocumentText();

		Collection<Segment> segments = JCasUtil.select(jcas, Segment.class);
		for(Segment segment : segments){
			String sectionID = segment.getId();
			if (!skipSegmentsSet.contains(sectionID)) {
				sentenceCount = annotateRange(jcas, text, segment, sentenceCount);
			}
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
	protected int annotateRange(JCas jcas, String text, Segment section,
			int sentenceCount) {

		int b = section.getBegin();
		int e = section.getEnd();

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
		ArrayList<SentenceSpan> sentenceSpans = new ArrayList<>(0);
		for (int i = 0; i < potentialSentSpans.length; i++) {
			if (potentialSentSpans[i] != null) {
				sentenceSpans.addAll(potentialSentSpans[i]
						.splitAtLineBreaksAndTrim(NEWLINE)); // TODO Determine
																// line break
																// type
			}
		}

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
					LOGGER.error("Skipping sentence from " + span.getStart()
							+ " to " + span.getEnd());
					LOGGER.error("Overlap with previous sentence that ended at "
							+ previousEnd);
				}
			}
		}
		return sentenceCount;
	}

	public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException{
      return AnalysisEngineFactory.createEngineDescription( SentenceDetector.class );
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
		final Logger LOGGER = LogManager.getLogger(SentenceDetector.class.getName()
				+ ".main()");

		// Handle arguments
		if (args.length < 2 || args.length > 4) {
			usage( LOGGER );
			System.exit(-1);
		}

		File inFile = getReadableFile(args[0]);

		File outFile = getFileInExistingDir(args[1]);
		// File outFile = new File(args[1]);

		int iters = 100;
		if (args.length > 2) {
			iters = parseInt(args[2], LOGGER);
		}

		int cut = 5;
		if (args.length > 3) {
			cut = parseInt(args[3], LOGGER);
		}

		// Now, do the actual training
		EndOfSentenceScannerImpl scanner = new EndOfSentenceScannerImpl();
		int numEosc = scanner.getEndOfSentenceCharacters().length;

		LOGGER.info("Training new model from " + inFile.getAbsolutePath());
		LOGGER.info("Using " + numEosc + " end of sentence characters.");


		Charset charset = Charset.forName("UTF-8");
		SentenceModel mod = null;
    
		MarkableFileInputStreamFactory mfisf = new MarkableFileInputStreamFactory(inFile);
		try (ObjectStream<String> lineStream = new PlainTextByLineStream(mfisf, charset)) {
		  
		  ObjectStream<SentenceSample> sampleStream =  new SentenceSampleStream(lineStream);

		  // Training Parameters
		  TrainingParameters mlParams = new TrainingParameters();
		  mlParams.put(TrainingParameters.ALGORITHM_PARAM, "MAXENT");
		  mlParams.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(iters));
		  mlParams.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(cut));

		  // Abbreviations dictionary
		  // TODO: Actually import a Dictionary of abbreviations
		  Dictionary dict = new Dictionary();
		  SentenceDetectorFactory sdFactory = new SentenceDetectorFactory("en", true, dict, null);

		  try {
		    // TODO: language should be generic (i.e. params.getLang() )
		    mod = SentenceDetectorME.train("en", sampleStream, sdFactory, mlParams);
		  } finally {
			  sampleStream.close();
		  }
		}
		
		try(FileOutputStream outStream = new FileOutputStream(outFile)){
		  LOGGER.info("Saving the model as: " + outFile.getAbsolutePath());
		  mod.serialize(outStream);
		}
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
		File parent = f.getAbsoluteFile().getParentFile();
		if (!parent.isDirectory()) {
			throw new IOException("Directory not found: "
					+ f.getParentFile().getAbsolutePath());
		}
		return f;
	}

}
