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
package org.apache.ctakes.examples.ae;

import java.util.Collection;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

public class ExampleHelloWorldAnnotator extends JCasAnnotator_ImplBase {

	public static final String PARAM_SAVE_ANN = "PARAM_SAVE_ANN";
	public static final String PARAM_PRINT_ANN = "PARAM_PRINT_ANN";
	private Logger LOG = Logger.getLogger(getClass().getName());

	@ConfigurationParameter(name = PARAM_SAVE_ANN, mandatory = false, description = "Example of Options/Parameters Save Annotation?")
	protected boolean saveAnnotation = true;

	@ConfigurationParameter(name = PARAM_PRINT_ANN, mandatory = false, description = "Example of Options/Parameters Print Annotation?")
	protected boolean printAnnotation = true;

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// Create a dummy IdentifiedAnnotation in the type system
		// If the BaseToken Part Of Speech is a Noun
		Collection<BaseToken> tokens = JCasUtil.select(jcas, BaseToken.class);
		for (BaseToken token : tokens) {
			if (saveAnnotation && token.getPartOfSpeech() != null
					&& token.getPartOfSpeech().startsWith("N")) {
				IdentifiedAnnotation ann = new IdentifiedAnnotation(jcas);
				ann.setBegin(token.getBegin());
				ann.setEnd(token.getEnd());
				ann.addToIndexes();

				if (printAnnotation) {
					LOG.info("Token:" + token.getCoveredText() + " POS:"
							+ token.getPartOfSpeech());
				}
			}
		}
	}

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(
			boolean saveAnn, boolean printAnn)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				ExampleHelloWorldAnnotator.class,
				ExampleHelloWorldAnnotator.PARAM_SAVE_ANN, saveAnn,
				ExampleHelloWorldAnnotator.PARAM_PRINT_ANN, printAnn);
	}

	public static AnalysisEngineDescription createAnnotatorDescription()
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				ExampleHelloWorldAnnotator.class,
				ExampleHelloWorldAnnotator.PARAM_SAVE_ANN, true,
				ExampleHelloWorldAnnotator.PARAM_PRINT_ANN, true);
	}
}
