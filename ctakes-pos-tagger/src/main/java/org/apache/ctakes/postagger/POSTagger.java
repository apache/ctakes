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

/**
 * This class was derived directly from the example annotator provided by the Apache
 * UIMA distribution 2.2.1 in the opennlp_wrappers directory of the uimaj-examples project.
 * 
 * The following changes have been made:
 * - import of different sentence and token types.
 * - removed original comments
 * - typed the collections used in process
 * - throws an exception instead of printing out an error message.
 * 
 * Please read the README in the top-level directory of this project for further details.  
 */

package org.apache.ctakes.postagger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import opennlp.tools.postag.POSModel;

@PipeBitInfo(
		name = "Part of Speech Tagger",
		description = "Annotate Parts of Speech.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.BASE_TOKEN, }
)
public class POSTagger extends JCasAnnotator_ImplBase {

	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * "PosModelFile" is a required, single, string parameter that contains the
	 * file name of the part of speech tagger model. The model file name should
	 * end with ".bin.gz" or ".txt". If this is not the case, then please see
	 * resources/models/README.
	 */
	public static final String POS_MODEL_FILE_PARAM = "PosModelFile";
	public static final String PARAM_POS_MODEL_FILE = POS_MODEL_FILE_PARAM;
	@ConfigurationParameter(name = POS_MODEL_FILE_PARAM, mandatory = false, defaultValue = "org/apache/ctakes/postagger/models/mayo-pos.zip", description = "Model file for OpenNLP POS tagger")
	private String posModelPath;
	private opennlp.tools.postag.POSTaggerME tagger;

	@Override
	public void initialize(UimaContext uimaContext)
			throws ResourceInitializationException {
		super.initialize(uimaContext);

		logger.info("POS tagger model file: " + posModelPath);

		try (InputStream fis = FileLocator.getAsStream(posModelPath)) {
			POSModel modelFile = new POSModel(fis);
			tagger = new opennlp.tools.postag.POSTaggerME(modelFile);
		} catch (Exception e) {
			logger.info("Error loading POS tagger model: " + posModelPath);
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		logger.info("process(JCas)");

		Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
		for (Sentence sentence : sentences) {

			List<BaseToken> printableTokens = new ArrayList<>();
			
			for(BaseToken token : JCasUtil.selectCovered(BaseToken.class,	sentence)){
			  if(token instanceof NewlineToken) continue;
			  printableTokens.add(token);
			}
			
			String[] words = new String[printableTokens.size()];
			for (int i = 0; i < words.length; i++) {
				words[i] = printableTokens.get(i).getCoveredText();
			}

			if (words.length > 0) {
				String[] wordTagList = tagger.tag(words);

				try {
					for (int i = 0; i < printableTokens.size(); i++) {
						BaseToken token = printableTokens.get(i);
						String posTag = wordTagList[i];
						token.setPartOfSpeech(posTag);
					}
				} catch (IndexOutOfBoundsException e) {
					throw new AnalysisEngineProcessException(
							"sentence being tagged is: '"
									+ sentence.getCoveredText() + "'", null, e);
				}
			}
		}
	}

	public static AnalysisEngineDescription createAnnotatorDescription()
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				POSTagger.class, TypeSystemDescriptionFactory
						.createTypeSystemDescription(), TypePrioritiesFactory
						.createTypePriorities(Segment.class, Sentence.class,
								BaseToken.class));
	}

	public static AnalysisEngineDescription createAnnotatorDescription(
			String model) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				POSTagger.class, TypeSystemDescriptionFactory
						.createTypeSystemDescription(), TypePrioritiesFactory
						.createTypePriorities(Segment.class, Sentence.class,
								BaseToken.class),
				POSTagger.PARAM_POS_MODEL_FILE, model);
	}
}
