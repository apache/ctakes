package org.apache.ctakes.dictionary.lookup2.util;
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


import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

public class DummyAnnotator extends JCasAnnotator_ImplBase {

	public static final String PARAM_SAVE_ANN = "PARAM_SAVE_ANN";
	public static final String PARAM_PRINT_ANN = "PARAM_PRINT_ANN";

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// never run
	}

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
	}


	public AnalysisEngineDescription createAnnotatorDescription()
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				DummyAnnotator.class,
				DummyAnnotator.PARAM_SAVE_ANN, true,
				DummyAnnotator.PARAM_PRINT_ANN, true);
	}
}
