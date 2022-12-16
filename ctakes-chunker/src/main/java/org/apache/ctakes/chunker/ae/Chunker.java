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
package org.apache.ctakes.chunker.ae;

import opennlp.tools.chunker.ChunkerModel;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

// import opennlp.tools.lang.english.TreebankChunker; // no longer part of OpenNLP as of 1.5

/**
 * This class provides a UIMA wrapper for the OpenNLP
 * opennlp.tools.chunker.Chunker class. This wrapper can generate chunks of any
 * kind as specified by the chunker model and the chunk creator.  
 */

@PipeBitInfo(
      name = "Chunker",
      description = "Annotator that generates chunks of any kind as specified by the chunker model and the chunk creator.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = { PipeBitInfo.TypeProduct.CHUNK }
)
public class Chunker extends JCasAnnotator_ImplBase {

	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());


	/**
	 * "ChunkerModel" is a required, single, string parameter that contains the
	 * file name of the chunker model. The model file name should
	 * end with ".bin.gz" or ".txt". If this is not the case, then please see
	 * resources/models/README.
	 */
	public static final String CHUNKER_MODEL_FILE_PARAM = "ChunkerModelFile"; // keep for backwards compatibility
	public static final String PARAM_CHUNKER_MODEL_FILE = CHUNKER_MODEL_FILE_PARAM;
	@ConfigurationParameter(
	    name = PARAM_CHUNKER_MODEL_FILE,
	    mandatory = false,
	    defaultValue = "org/apache/ctakes/chunker/models/chunker-model.zip",
	    description = "Model file for OpenNLP chunker"
	    )
  private String chunkerModelPath;

	/**
	 * "ChunkCreatorClass" is a required, single, string parameter that
	 * specifies the chunker creator class to instantiate. A chunker creator
	 * determines how chunk annotations are created.
	 * 
	 * @see ChunkCreator
	 * @see DefaultChunkCreator
	 * @see PhraseTypeChunkCreator
	 */
	public static final String CHUNKER_CREATOR_CLASS_PARAM = "ChunkCreatorClass"; // kept for backwards compatibility
	public static final String PARAM_CHUNKER_CREATOR_CLASS = CHUNKER_CREATOR_CLASS_PARAM;
	@ConfigurationParameter(
	    name = PARAM_CHUNKER_CREATOR_CLASS,
	    mandatory = false,
	    defaultValue = "org.apache.ctakes.chunker.ae.DefaultChunkCreator",
	    description = "The class that will create the chunks"
	    )
  String chunkerCreatorClassName;

	private opennlp.tools.chunker.Chunker chunker;

	ChunkCreator chunkerCreator;

	@Override
  public void initialize(UimaContext uimaContext) throws ResourceInitializationException {
		super.initialize(uimaContext);

    logger.info("Chunker model file: " + chunkerModelPath); 
		try (InputStream fis = FileLocator.getAsStream(chunkerModelPath)) {
			ChunkerModel model = new ChunkerModel(fis);
			chunker = new opennlp.tools.chunker.ChunkerME(model);

		} catch (IOException e) {
			logger.info("Chunker model: " + chunkerModelPath); 
			throw new ResourceInitializationException(e);
		}
		
    try {
      chunkerCreator = (ChunkCreator) Class.forName(chunkerCreatorClassName).newInstance();
    } catch (InstantiationException | IllegalAccessException
        | ClassNotFoundException e) {
      logger.error("Error creating chunkerCreator from classname: " + chunkerCreatorClassName);
      throw new ResourceInitializationException(e);
    }
    chunkerCreator.initialize(uimaContext);
	}

	@Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {

		logger.info(" process(JCas)");

		Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
		
		for(Sentence sentence : sentences){
	    List<BaseToken> tokens = JCasUtil.selectCovered(BaseToken.class, sentence);
      String[] words = new String[tokens.size()];
      String[] tags = new String[tokens.size()];
      for(int i = 0; i < tokens.size(); i++){
        words[i] = tokens.get(i).getCoveredText();
        tags[i] = tokens.get(i).getPartOfSpeech();
      }

			String[] chunks = chunker.chunk(words, tags);

			int chunkBegin = 0;
			String chunkType = "";
			int chunkEnd;

			// The logic below may seem to be oversimplified. For example, it
			// does not handle
			// cases where you might see a O I-NP O. However, such sequences
			// should never be
			// generated because they are restricted by
			// TreebankChunker.validOutcome()
			// This code was directly modified from TreebankChunker.main()
			for (int i = 0; i < chunks.length; i++) {

			  if (i > 0 && !chunks[i].startsWith("I-")) { // && !chunks[i - 1].equals("O")) {
			    chunkEnd = tokens.get(i - 1).getEnd();
			    chunkerCreator.createChunk(jCas, chunkBegin, chunkEnd, chunkType);
			  }

			  if (chunks[i].startsWith("B-")) {
			    chunkBegin = tokens.get(i).getBegin();
			    chunkType = chunks[i].substring(2);
			  } else if (chunks[i].equals("O")) { // O found  (her_PRP$ ear_O)
			    chunkBegin = tokens.get(i).getBegin();
			    chunkType = chunks[i];

			  }
			}
			if (chunks.length > 0 && !chunks[chunks.length - 1].equals("O")) {
			  chunkEnd = tokens.get(chunks.length - 1).getEnd();
			  chunkerCreator.createChunk(jCas, chunkBegin, chunkEnd, chunkType);
			}
		}
	}
	
	public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException{
	  return AnalysisEngineFactory.createEngineDescription(Chunker.class);
	}

	 public static AnalysisEngineDescription createAnnotatorDescription(String modelFile) throws ResourceInitializationException{
	    return AnalysisEngineFactory.createEngineDescription(Chunker.class,
	        Chunker.PARAM_CHUNKER_MODEL_FILE,
	        modelFile);
	  }
}
