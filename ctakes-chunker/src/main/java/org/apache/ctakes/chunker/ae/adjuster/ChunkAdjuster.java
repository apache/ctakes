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
package org.apache.ctakes.chunker.ae.adjuster;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * UIMA annotator that uses a pattern and a rule about that pattern to adjust
 * certain annotations.
 * 
 * The original reason for this annotator is to extend NP annotations to include
 * prepositional phrases so that for the pattern NP PP NP, named entities that
 * includes a word(s) from each of those NPs is found.
 * 
 * Searches for the pattern within each Sentence. The end offset of the first
 * chunk in the pattern is extended to match the end offset of the last chunk in
 * the pattern.
 * 
 * Note the pattern is applied repeatedly so that a sentence of NP PP NP PP NP
 * results in only the first NP being extended all the way to the last NP in
 * that sentence. This prevents NP annotations from only partially overlapping
 * other NP annotations.
 * 
 * This annotator is written to be able to handle more general cases than NP PP
 * NP.
 * 
 * 
 */
@PipeBitInfo(
		name = "Chunk Adjuster",
		description = "Annotator that uses a pattern and a rule about that pattern to adjust certain annotations.",
		role = PipeBitInfo.Role.SPECIAL,
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.CHUNK }
)
public class ChunkAdjuster extends JCasAnnotator_ImplBase {
	/**
	 * The pattern of chunks that trigger an adjustment.
	 * 
	 */
  public static final String PARAM_CHUNK_PATTERN = "ChunkPattern";
  @ConfigurationParameter(
      name = PARAM_CHUNK_PATTERN,
      mandatory = true,
      description = "The pattern of chunks that trigger an adjustment"
      )
  private String[] chunksTypesInPattern;

	/**
	 * The index of the token (within the pattern) to extend the end offset to
	 * include. E.g. is 2 to extend the first NP to include the last NP in NP PP
	 * NP.
	 */
  public static final String PARAM_EXTEND_TO_INCLUDE_TOKEN = "IndexOfTokenToInclude";
  @ConfigurationParameter(
      name = PARAM_EXTEND_TO_INCLUDE_TOKEN,
      mandatory = true,
      description = "The index of the token in the pattern to extend to the end offset"
      )
  private int indexOfTokenToInclude;

	// TODO Consider adding a parameter for the type of annotation to look for
	// pattern within, instead of always Sentence

	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Performs initialization logic. This implementation just reads values for
	 * the configuration parameters. This method is not invoked for every
	 * document processed.
	 *
	 * @see org.apache.uima.fit.component.JCasAnnotator_ImplBase#initialize(UimaContext)
	 */
	@Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {

		super.initialize(aContext);

		configInit();
	}

	/**
	 * Sets configuration parameters with values from the descriptor.
	 */
	private void configInit() throws ResourceInitializationException {
		// TODO Consider validating values in pattern to type system

		if (indexOfTokenToInclude < 0
				|| indexOfTokenToInclude >= chunksTypesInPattern.length) {
			// "The value "{0}" is not valid for the {1} parameter."
			String msgArgs[] = { Integer.toString(indexOfTokenToInclude),
					PARAM_EXTEND_TO_INCLUDE_TOKEN };
			throw new ResourceInitializationException(
					AnnotatorConfigurationException.PARAMETER_NOT_VALID,
					msgArgs);
		}

	}

	/**
	 * Invokes this annotator's analysis logic. Invoked for each document
	 * processed. For each Sentence, look for the pattern, and adjust a chunk if
	 * the pattern is found.
	 */
	@Override
	public void process(JCas jcas)
	    throws AnalysisEngineProcessException {

	  logger.info(" process(JCas)");

	  try {
	    Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);
	    for(Sentence sentence : sentences){
	      annotateSentence(jcas, sentence);
	    }
	  } catch (Exception e) {
	    throw new AnalysisEngineProcessException(e);
	  }
	}

	protected void annotateSentence(JCas jcas, Sentence sent) throws AnalysisEngineProcessException{
    List<Chunk> chunkList = new ArrayList<>(JCasUtil.selectCovered(jcas, Chunk.class, sent));

    // For each chunk in the Sentence, see if the chunk is the start of a
    // matching pattern
    // If so, extend the end offset of the <code>i</code> +
    // <code>indexOfTokenToInclude</code>
    for (int i = 0; i < chunkList.size(); i++) {

      boolean matches = true;
      Chunk chunk = chunkList.get(i);

      while (matches == true) {
        matches = compareToPattern(chunkList, i);
        if (matches) {
          extendChunk(chunk, chunkList.get(i + indexOfTokenToInclude)
              .getEnd());
          removeEnvelopedChunks(chunkList, i); // to check again on next
                          // iteration of while loop
        }
      }
    }

  }
	
	/**
	 * A utility method that annotates a given range.
	 */
	protected void annotateRange(JCas jcas, int rangeBegin,
			int rangeEnd)
			throws AnalysisEngineProcessException {

		// logger.info("Adjuster: from " + rangeBegin + " to " + rangeEnd);

		// Find the Chunks in this Sentence
		// For each Chunk, there is a corresponding more specific such as NP,
		// PP, etc
		List<Chunk> chunkList = new ArrayList<>(JCasUtil.selectCovered(jcas, Chunk.class, rangeBegin, rangeEnd));

		// For each chunk in the Sentence, see if the chunk is the start of a
		// matching pattern
		// If so, extend the end offset of the <code>i</code> +
		// <code>indexOfTokenToInclude</code>
		for (int i = 0; i < chunkList.size(); i++) {

			boolean matches = true;
			Chunk chunk = chunkList.get(i);

			while (matches == true) {
				matches = compareToPattern(chunkList, i);
				if (matches) {
					extendChunk(chunk, chunkList.get(i + indexOfTokenToInclude)
							.getEnd());
					removeEnvelopedChunks(chunkList, i); // to check again on next
													// iteration of while loop
				}
			}
		}

	}

	/**
	 * Remove from our local list of chunks the chunks that have been enveloped.
	 * This allows the rule to be applied again.
	 * 
	 */
	private void removeEnvelopedChunks(List<Chunk> list, int i) {
		for (int j = 0; j < indexOfTokenToInclude; j++) {
			list.remove(i + 1);
//			if (false)
//				logger.info("removed '" + chunk.getCoveredText() + "'");
		}
	}

	/**
	 * Compares the chunks at index i to the 1st element on the pattern, i+1 to
	 * the 2nd element, etc and returns true if the chunks starting at i fit the
	 * pattern
	 * 
	 * @param list
	 *            the list of chunks
	 * @param i
	 *            the position within the list to compare to the pattern
	 * @return true if the pattern is matched by the chunks starting with
	 *         element <code>i</code> in the list. Note if there aren't enough
	 *         chunks in the list starting at i to match the pattern, returns
	 *         false.
	 * @throws AnnotatorProcessException
	 */
	private boolean compareToPattern(List<Chunk> list, int i)
			{

		boolean match = true;
		int len = list.size();
		for (int j = 0; j < chunksTypesInPattern.length; j++) {
			if (i + j >= len
					|| !list.get(i + j).getChunkType()
							.equals(chunksTypesInPattern[j])) {
				match = false; // some part of pattern doesn't match chunks
								// starting at i
				break;
			}
		}

		return match;

	}

	/**
	 * Update the end value for the chunk to have the new value
	 * 
	 * @param chunk
	 *            The chunk to update
	 * @param newEnd
	 *            The new end value for the chunk.
	 * @return The updated Chunk
	 * @throws AnnotatorProcessException
	 */
	private static Chunk extendChunk(Chunk chunk, int newEnd)
			throws AnalysisEngineProcessException {

		if (newEnd < chunk.getBegin()) {
			Exception e;
			e = new Exception("New end offset (" + newEnd
					+ ") < begin offset (" + chunk.getBegin() + ").");
			throw new AnalysisEngineProcessException(e);
		}
		// logger.info("Extending chunk end from " +chunk.getEnd()+ " to " +
		// newEnd + ".");
		// logger.info("For chunk " + chunk.getChunkType());
		// logger.info(" text =      '" + chunk.getCoveredText() + "'.");
		chunk.setEnd(newEnd);
		// logger.info(" new text =  '" + chunk.getCoveredText() + "'.");
		return chunk;

	}

	public static AnalysisEngineDescription createAnnotatorDescription(String[] chunkPattern, int patternIndex) throws ResourceInitializationException{
	  return AnalysisEngineFactory.createEngineDescription(ChunkAdjuster.class, 
	      ChunkAdjuster.PARAM_CHUNK_PATTERN,
	      chunkPattern,
	      ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN,
	      patternIndex);
	}
}
