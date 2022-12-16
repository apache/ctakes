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
package org.apache.ctakes.necontexts;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.test.TestUtil;

public class StatusAnnotatorTests {

	@Test
	public void testNegationExamples() throws ResourceInitializationException, AnalysisEngineProcessException {
		//TODO: Pei- For unit tests, we should wire up the pipeline programmatically.
		//We can use uimafit instead of xml descriptor files.
		String descriptor = "desc/test/StatusAnnotator.xml";
		AnalysisEngine contextAE = TestUtil.getAE(new File(descriptor));
		UimaContext uimaContext = contextAE.getUimaContext();
		ContextAnnotator statusAnnotator = new ContextAnnotator();
//		statusAnnotator.initialize(uimaContext);
//
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.MAX_LEFT_SCOPE_SIZE_PARAM, new Integer(7));
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.MAX_RIGHT_SCOPE_SIZE_PARAM, new Integer(7));
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.SCOPE_ORDER_PARAM, "LEFT", 0);
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.SCOPE_ORDER_PARAM, "RIGHT", 1);
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.CONTEXT_ANALYZER_CLASS_PARAM, "org.apache.ctakes.necontexts.status.StatusContextAnalyzer");
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.CONTEXT_HIT_CONSUMER_CLASS_PARAM, "org.apache.ctakes.necontexts.status.StatusContextHitConsumer");
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.WINDOW_ANNOTATION_CLASS_PARAM, "org.apache.ctakes.typesystem.type.textspan.Sentence");
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.FOCUS_ANNOTATION_CLASS_PARAM, "org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation");
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.CONTEXT_ANNOTATION_CLASS_PARAM, "org.apache.ctakes.typesystem.type.syntax.BaseToken");
//
//		AnalysisEngine segmentTokenSentenceAE = TestUtil.getAE(new File("desc/test/SegmentTokenSentenceAggregate.xml"));
//
//		String text;
//		JCas jCas;
//
//		text = "Electrocardiogram taken earlier today was read by Dr. Xxxxx X. Xxxxxx, cardiologist at Mayo, as \"marked sinus bradycardia, left atrial enlargement, possible inferior infarct, T-wave abnormality, consider lateral ischemia.";
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		IdentifiedAnnotation ne1 = addNE(jCas, 104, 121, "sinus bradycardia");
//		IdentifiedAnnotation ne2 = addNE(jCas, 104, 109, "sinus");
//		IdentifiedAnnotation ne3 = addNE(jCas, 110, 121, "bradycardia");
//		IdentifiedAnnotation ne4 = addNE(jCas, 123, 146, "left atrial enlargement");
//		IdentifiedAnnotation ne5 = addNE(jCas, 135, 146, "enlargement");
//		IdentifiedAnnotation ne6 = addNE(jCas, 166, 173, "infarct");
//		IdentifiedAnnotation ne7 = addNE(jCas, 175, 176, "T");
//		IdentifiedAnnotation ne8 = addNE(jCas, 182, 193, "abnormality");
//		IdentifiedAnnotation ne9 = addNE(jCas, 212, 220, "ischemia");
//		statusAnnotator.process(jCas);
//		assertEquals(3, ne1.getUncertainty()); // 3 means probable, possible, etc
//		assertEquals(3, ne2.getUncertainty());
//		assertEquals(3, ne3.getUncertainty());
//		assertEquals(3, ne4.getUncertainty());
//		assertEquals(3, ne5.getUncertainty());
//		assertEquals(3, ne6.getUncertainty());
//		assertEquals(3, ne7.getUncertainty());
//		assertEquals(3, ne8.getUncertainty());
//		assertEquals(0, ne9.getUncertainty()); // 0 since window size prevents "possible" from being applied to "ischemia"
//
//		text = "It is my impression that Mr. Xxxxxx presents with acute-on-chronic hypercapnic respiratory failure which likely precipitated his underlying lung disease which is multifactorial."; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 67, 98, "hypercapnic respiratory failure");
//		ne2 = addNE(jCas, 79, 98 , "respiratory failure");
//		ne3 = addNE(jCas, 140, 152, "lung disease");
//		ne4 = addNE(jCas, 145, 152, "disease");
//		statusAnnotator.process(jCas);
//		assertEquals(0, ne1.getUncertainty());
//		assertEquals(0, ne2.getUncertainty());
//		assertEquals(3, ne3.getUncertainty());
//		assertEquals(3, ne4.getUncertainty());
//
//		text = "#4 Obesity and hyperventilation with probable sleep-disordered breathing"; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 3, 10, "Obesity");
//		ne2 = addNE(jCas, 15, 31 , "hyperventilation");
//		statusAnnotator.process(jCas);
//		assertEquals(3, ne1.getUncertainty());
//		assertEquals(3, ne2.getUncertainty());
//
//		text = "He has suffered from excessive daytime sleepiness since adolescence, and he was first diagnosed with narcolepsy in 2000, based on history and pupillography."; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 101, 111, "narcolepsy");
//		statusAnnotator.process(jCas);
//		assertEquals(1, ne1.getUncertainty()); // 1 means History of
//
//		text = "She has a known history of atrial fibrillation."; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 27, 46, "atrial fibrillation");
//		ne2 = addNE(jCas, 34, 46, "fibrillation");
//		statusAnnotator.process(jCas);
//		assertEquals(1, ne1.getUncertainty());
//		assertEquals(1, ne2.getUncertainty());
//
//		text = "No history of stroke."; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 14, 20, "stroke");
//		statusAnnotator.process(jCas);
//		assertEquals(1, ne1.getUncertainty());
//
//		text = "History of hypertension."; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 11, 23, "hypertension");
//		statusAnnotator.process(jCas);
//		assertEquals(1, ne1.getUncertainty());
//
//		text = "(5) History of atrial fibrillation."; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 15, 34, "atrial fibrillation");
//		ne2 = addNE(jCas, 22, 34, "fibrillation");
//		statusAnnotator.process(jCas);
//		assertEquals(1, ne1.getUncertainty());
//		assertEquals(1, ne2.getUncertainty());
//
//		text = "Family history is largely unremarkable save a strong family history of cancer of uncertain primary."; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 71, 77, "cancer");
//		statusAnnotator.process(jCas);
//		assertEquals(2, ne1.getUncertainty()); // 2 means Family history of
//
//		text = "He has no history of cardiac-related problems and no family history of coronary artery disease."; 
//		jCas = TestUtil.processAE(segmentTokenSentenceAE, text);
//		ne1 = addNE(jCas, 37, 45, "problems");
//		ne2 = addNE(jCas, 71, 94 , "coronary artery disease");
//		ne3 = addNE(jCas, 87, 94, "disease");
//		statusAnnotator.process(jCas);
//		assertEquals(2, ne1.getUncertainty());
//		assertEquals(2, ne2.getUncertainty());
//		assertEquals(2, ne3.getUncertainty());

	}

	private IdentifiedAnnotation addNE(JCas jCas, int neBegin, int neEnd, String neText) throws ResourceInitializationException, AnalysisEngineProcessException {
		IdentifiedAnnotation IdentifiedAnnotation = new IdentifiedAnnotation(jCas, neBegin, neEnd);
		IdentifiedAnnotation.addToIndexes();
		assertEquals(neText, IdentifiedAnnotation.getCoveredText());
		return IdentifiedAnnotation;
	}
}
