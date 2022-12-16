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
package org.apache.ctakes.core.sentence;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.ml.maxent.GISTrainer;
import opennlp.tools.ml.maxent.GISModel;
import opennlp.model.EventStream;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.sentdetect.DefaultSDContextGenerator;
import opennlp.tools.sentdetect.EndOfSentenceScanner;
import opennlp.tools.sentdetect.SDContextGenerator;
import opennlp.tools.sentdetect.SDEventStream;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.sentdetect.SentenceSample;
import opennlp.tools.sentdetect.SentenceSampleStream;
import opennlp.tools.sentdetect.lang.Factory;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.HashSumEventStream;
import opennlp.tools.util.AbstractObjectStream;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.StringUtil;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.ModelUtil;

/**
 * A sentence detector for splitting up raw text into sentences.
 * <p>
 * A maximum entropy model is used to evaluate the characters ".", "!", and "?" in a
 * string to determine if they signify the end of a sentence.
 * 
 * @see  opennlp.tools.sentdetect.SentenceDetectorME in OpenNLP 1.5
 */
public class SentenceDetectorCtakes {

	  /**
	   * Constant indicates a sentence split.
	   */
	  public static final String SPLIT ="s";

	  /**
	   * Constant indicates no sentence split.
	   */
	  public static final String NO_SPLIT ="n";
	  
	  private static final Double ONE = new Double(1);

	  /**
	   * The maximum entropy model to use to evaluate contexts.
	   */
	  private MaxentModel model;

	  /**
	   * The feature context generator.
	   */
	  private final SDContextGenerator cgen;

	  /**
	   * The {@link EndOfSentenceScanner} to use when scanning for end of sentence offsets.
	   */
	  private final EndOfSentenceScanner scanner;

	  /**
	   * The list of probabilities associated with each decision.
	   */
	  private List<Double> sentProbs = new ArrayList<Double>();

	  protected boolean useTokenEnd;

	  /**
	   * Initializes the current instance.
	   *
	   * @param model the {@link SentenceModel}
	   */
	  public SentenceDetectorCtakes(MaxentModel model, DefaultSDContextGenerator cg, EndOfSentenceScanner eoss) {
		  this.model = model;
		  cgen = cg;
		  scanner = eoss;
		  useTokenEnd = false; // TODO
	  }


	/**
	   * Detect sentences in a String.
	   *
	   * @param s  The string to be processed.
	   *
	   * @return   A string array containing individual sentences as elements.
	   */
	  public String[] sentDetect(String s) {
	    int[] endsOfSentences = sentPosDetect(s);
	    String sentences[];
	    if (endsOfSentences.length != 0) {

	      sentences = new String[endsOfSentences.length];

	      int begin = 0;
	      for (int si = 0; si < endsOfSentences.length; si++) {
	        sentences[si] = s.substring(begin, endsOfSentences[si]+1);
	        begin = endsOfSentences[si]+1;
	      }
	    }
	    else {
	      sentences = new String[] {};
	    }
	    return sentences;
	  }

	  private int getFirstWS(String s, int pos) {
	    while (pos < s.length() && !StringUtil.isWhitespace(s.charAt(pos)))
	      pos++;
	    return pos;
	  }

	  private int getFirstNonWS(String s, int pos) {
	    while (pos < s.length() && StringUtil.isWhitespace(s.charAt(pos)))
	      pos++;
	    return pos;
	  }

	  /**
	   * Detect the position of the first words of sentences in a String.
	   *
	   * @param s  The string to be processed.
	   * @return   A integer array containing the positions of the end index of
	   *          every sentence
	   *
	   * @see SentenceDetectorME#sentPosDetect(String)  
	   */
	  public int[] sentPosDetect(String s) { // return int[] to be line OpenNLP 1.4
	    double sentProb = 1;
	    sentProbs.clear();
	    StringBuffer sb = new StringBuffer(s);
	    List<Integer> enders = scanner.getPositions(s);
	    List<Integer> positions = new ArrayList<Integer>(enders.size());

	    for (int i = 0, end = enders.size(), index = 0; i < end; i++) {
	      Integer candidate = enders.get(i);
	      int cint = candidate;
	      // skip over the leading parts of non-token final delimiters
	      int fws = getFirstWS(s,cint + 1);
	      if (i + 1 < end && enders.get(i + 1) < fws) {
	        continue;
	      }

	      double[] probs = model.eval(cgen.getContext(sb, cint));
	      String bestOutcome = model.getBestOutcome(probs);
	      sentProb *= probs[model.getIndex(bestOutcome)];

	      if (bestOutcome.equals(SPLIT) && isAcceptableBreak(s, index, cint)) {
	        if (index != cint) {
	          if (useTokenEnd) {
	            positions.add(getFirstNonWS(s, getFirstWS(s,cint + 1)));
	          }
	          else {
	            positions.add(getFirstNonWS(s,cint));
	          }
	          sentProbs.add(new Double(probs[model.getIndex(bestOutcome)]));
	        }
	        index = cint + 1;
	      }
	    }

	    int[] sentenceBreaks = new int[positions.size()];
	    for (int i = 0; i < sentenceBreaks.length; i++) {
	      sentenceBreaks[i] = positions.get(i)+1;
	    }

	    return sentenceBreaks;
	    
	  }

	  /**
	   * Returns the probabilities associated with the most recent
	   * calls to sentDetect().
	   *
	   * @return probability for each sentence returned for the most recent
	   * call to sentDetect.  If not applicable an empty array is
	   * returned.
	   */
	  public double[] getSentenceProbabilities() {
	    double[] sentProbArray = new double[sentProbs.size()];
	    for (int i = 0; i < sentProbArray.length; i++) {
	      sentProbArray[i] = ((Double) sentProbs.get(i)).doubleValue();
	    }
	    return sentProbArray;
	  }

	  /**
	   * Allows subclasses to check an overzealous (read: poorly
	   * trained) model from flagging obvious non-breaks as breaks based
	   * on some boolean determination of a break's acceptability.
	   *
	   * <p>The implementation here always returns true, which means
	   * that the MaxentModel's outcome is taken as is.</p>
	   *
	   * @param s the string in which the break occurred.
	   * @param fromIndex the start of the segment currently being evaluated
	   * @param candidateIndex the index of the candidate sentence ending
	   * @return true if the break is acceptable
	   */
	  protected boolean isAcceptableBreak(String s, int fromIndex, int candidateIndex) {
	    return true;
	  }
	  
	  public static SentenceModel train(String languageCode, ObjectStream<SentenceSample> samples,
	      boolean useTokenEnd, Dictionary abbreviations) throws IOException {
	    return train(languageCode, samples, useTokenEnd, abbreviations,5,100);
	  }
	  
	  public static SentenceModel train(String languageCode, ObjectStream<SentenceSample> samples,
	      boolean useTokenEnd, Dictionary abbreviations, int cutoff, int iterations) throws IOException {

	    Map<String, String> manifestInfoEntries = new HashMap<String, String>();
	    ModelUtil.addCutoffAndIterations(manifestInfoEntries, cutoff, iterations);
	    
	    Factory factory = new Factory();

	    // TODO: Fix the EventStream to throw exceptions when training goes wrong
	    SDEventStream eventStream = new SDEventStream(samples,
	        factory.createSentenceContextGenerator(languageCode),
	        factory.createEndOfSentenceScanner(languageCode));
	    
	    HashSumEventStream hses = new HashSumEventStream(eventStream); // AbstractObjectStream<Event>
	    GISTrainer trainer = new GISTrainer();
	    MaxentModel sentModel = trainer.trainModel(hses, iterations, cutoff);

	    manifestInfoEntries.put(BaseModel.TRAINING_EVENTHASH_PROPERTY, 
	        hses.calculateHashSum().toString(16));
	    
	    return new SentenceModel(languageCode, sentModel, useTokenEnd, abbreviations, manifestInfoEntries);
	  }

	  private static void usage() {
	    System.err.println("Usage: SentenceDetectorME -encoding charset -lang language trainData modelName [cutoff iterations]");
	    System.err.println("-encoding charset specifies the encoding which should be used ");
	    System.err.println("                  for reading and writing text.");
	    System.err.println("-lang language    specifies the language which ");
	    System.err.println("                  is being processed.");
	    System.err.println("trainData         specifies the name of the input training file");
	    System.err.println("                  to train the resulting model.");
	    System.err.println("modelName         specifies the resulting saved model after");
	    System.err.println("                  training.");
	    System.exit(1);
	  }

	  /**
	   * <p>Trains a new sentence detection model.</p>
	   *
	   * <p>Usage: opennlp.tools.sentdetect.SentenceDetectorME data_file new_model_name (iterations cutoff)?</p>
	   *
	   * @param args
	   * @throws IOException
	   */
	  public static void main(String[] args) throws IOException {
	    int ai=0;
	    String encoding = null;
	    String lang = null;
	    if (args.length == 0) {
	      usage();
	    }
	    while (args[ai].startsWith("-")) {
	      if (args[ai].equals("-encoding")) {
	        ai++;
	        if (ai < args.length) {
	          encoding = args[ai];
	          ai++;
	        }
	        else {
	          usage();
	        }
	      }
	      else if (args[ai].equals("-lang")) {
	        ai++;
	        if (ai < args.length) {
	          lang = args[ai];
	          ai++;
	        }
	        else {
	          usage();
	        }
	      }
	      else {
	        usage();
	      }
	    }

	    File inFile = new File(args[ai++]);
	    File outFile = new File(args[ai++]);

	    int numberOfArgs = args.length;
	    int iters = (ai < numberOfArgs ? convertToInt(args[ai++]) : 100);
	    int cutoff = (ai < numberOfArgs ? convertToInt(args[ai++]) : 4);


	    try {
	      if ((lang == null) || (encoding == null)) {
	        usage();
	      }
	      
	      MarkableFileInputStreamFactory mfisf = new MarkableFileInputStreamFactory(inFile);
		  ObjectStream<String> lineStream = null;
		  lineStream = new PlainTextByLineStream(mfisf, encoding);
		  ObjectStream<SentenceSample> sampleStream = new SentenceSampleStream(lineStream);
		  
	      //new PlainTextByLineStream(new InputStreamReader(new FileInputStream(inFile), encoding))
		  SentenceModel model = train(lang, sampleStream, true, null, cutoff, iters);

	      // TODO: add support for iterations and cutoff settings

//	      if (args.length > ai)
//	        mod = train(es, Integer.parseInt(args[ai++]), Integer.parseInt(args[ai++]));
//	      else
//	        mod = train(es, 100, 5);

	      System.out.println("Saving the model as: " + outFile);
	      model.serialize(new FileOutputStream(outFile));
	    }
	    catch (Exception e) {
	      e.printStackTrace();
	    }
	  }


	private static int convertToInt(String s) {

		int i = Integer.parseInt(s); 
		return i;
	}
	
}