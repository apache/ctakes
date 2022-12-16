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
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.test.TestUtil;

public class ContextAnnotatorTests {

	static String unexpectedParamValueMsg = "unexpected parameter value for descriptor file desc/test/ContextTestAnnotator.xml for param: ";

	/**
	 * This test tests ContextAnnotator.getLeftScopeTokens(JCas, Annotation,
	 * Annotation). The method takes in annotations for the focus and window so
	 * the descriptor file parameter settings for FocusAnnotationClass and
	 * WindowAnnotationClass are effectively ignored by the method. This means
	 * we can pass any annotation in as the focus and window test the results.
	 * 
	 * @see ContextAnnotator#getLeftScopeContextAnnotations(JCas, Annotation, Annotation)
	 * @throws ResourceInitializationException
	 * @throws AnalysisEngineProcessException
	 */
	@Test
	public void testLeftScopeTokens() throws ResourceInitializationException, AnalysisEngineProcessException {

		String descriptor = "desc/test/TestContextAnnotator.xml";
		AnalysisEngine contextAE = TestUtil.getAE(new File(descriptor));
		UimaContext uimaContext = contextAE.getUimaContext();
		ContextAnnotator contextAnnotator = new ContextAnnotator();
//		contextAnnotator.initialize(uimaContext);
//
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.MAX_LEFT_SCOPE_SIZE_PARAM, new Integer(8));
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.CONTEXT_ANNOTATION_CLASS_PARAM, "org.apache.ctakes.typesystem.type.syntax.BaseToken");
//
//		AnalysisEngine segmentTokenSentenceAE = TestUtil.getAE(new File("desc/test/SegmentTokenSentenceAggregate.xml"));
//		// this gives us a JCas that has segments, tokens, and sentences
//		JCas jCas = TestUtil
//				.processAE(
//						segmentTokenSentenceAE,
//						"A farmer went trotting upon his gray mare, Bumpety, bumpety, bump, With his daughter behind him, so rosy and fair, Lumpety, lumpety, lump.");
//
//		BaseToken firstToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
//		BaseToken secondToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
//		BaseToken thirdToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
//		BaseToken fifthToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 4);
//		BaseToken eighthToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 7);
//		BaseToken ninthToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 8);
//		BaseToken tenthToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 9);
//		BaseToken penultimateToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 30);
//		BaseToken lastToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 31);
//		Sentence sentence = TestUtil.getFeatureStructureAtIndex(jCas, Sentence.class, 0);
//
//		// perform some sanity checks to make sure that we have the annotations
//		// we think we have.
//		assertEquals("A", firstToken.getCoveredText());
//		assertEquals("farmer", secondToken.getCoveredText());
//		assertEquals("went", thirdToken.getCoveredText());
//		assertEquals("upon", fifthToken.getCoveredText());
//		assertEquals("mare", eighthToken.getCoveredText());
//		assertEquals(",", ninthToken.getCoveredText());
//		assertEquals("Bumpety", tenthToken.getCoveredText());
//		assertEquals("lump", penultimateToken.getCoveredText());
//		assertEquals(".", lastToken.getCoveredText());
//		assertEquals(0, sentence.getBegin());
//		assertEquals(138, sentence.getEnd());
//
//		// if focus is the first token then the left tokens should be empty
//		Annotation focus = firstToken;
//		List<Annotation> leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(0, leftScopeTokens.size());
//
//		// if focus is the second token then the left tokens should contain the
//		// first token
//		focus = secondToken;
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(1, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//
//		// if focus is the third token then the left tokens should contain the
//		// first two tokens
//		focus = thirdToken;
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(2, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//		assertEquals(secondToken, leftScopeTokens.get(1));
//
//		// if focus is the ninth token then the left tokens should contain the
//		// first eight tokens
//		focus = ninthToken;
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(8, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//		assertEquals(secondToken, leftScopeTokens.get(1));
//		assertEquals(thirdToken, leftScopeTokens.get(2));
//		assertEquals(eighthToken, leftScopeTokens.get(7));
//
//		// if focus is the tenth token then the left tokens should contain the
//		// eight tokens beginning with the second
//		focus = tenthToken;
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(8, leftScopeTokens.size());
//		assertEquals(secondToken, leftScopeTokens.get(0));
//		assertEquals(thirdToken, leftScopeTokens.get(1));
//		assertEquals(eighthToken, leftScopeTokens.get(6));
//		assertEquals(ninthToken, leftScopeTokens.get(7));
//
//		focus = new PunctuationToken(jCas, 2, 12);
//		assertEquals("farmer wen", focus.getCoveredText());
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(1, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//		focus = new PunctuationToken(jCas, 1, 13);
//		assertEquals(" farmer went", focus.getCoveredText());
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(1, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//
//		// if focus is a token that overlaps on the left with other tokens, the
//		// leftScopeTokens
//		// should not include the partially overlapping tokens on the left
//		focus = new PunctuationToken(jCas, 3, 11);
//		assertEquals("armer we", focus.getCoveredText());
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(1, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//		// it should be the same if the focus type is different
//		focus = new IdentifiedAnnotation(jCas, 3, 11);
//		assertEquals("armer we", focus.getCoveredText());
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(1, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//
//		// if focus annotation is the last token then there should be 8 tokens
//		// returned.
//		focus = lastToken;
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(8, leftScopeTokens.size());
//
//		// "A farmer went trotting upon his gray mare"
//		focus = new IdentifiedAnnotation(jCas, 9, 27);
//		assertEquals("went trotting upon", focus.getCoveredText());
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(2, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//		assertEquals(secondToken, leftScopeTokens.get(1));
//
//		focus = new IdentifiedAnnotation(jCas, 7, 27);
//		assertEquals("r went trotting upon", focus.getCoveredText());
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(1, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//
//		focus = new IdentifiedAnnotation(jCas, 11, 27);
//		assertEquals("nt trotting upon", focus.getCoveredText());
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(2, leftScopeTokens.size());
//		assertEquals(firstToken, leftScopeTokens.get(0));
//		assertEquals(secondToken, leftScopeTokens.get(1));
//
//		// If there are no complete words to left of focus because the window
//		// cuts off the token - e.g. farmer, then
//		// the token farmer should not be returned as part of left tokens
//		sentence = new Sentence(jCas, 5, 27);
//		assertEquals("mer went trotting upon", sentence.getCoveredText());
//		focus = new IdentifiedAnnotation(jCas, 9, 27);
//		assertEquals("went trotting upon", focus.getCoveredText());
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(0, leftScopeTokens.size());
//
//		// there are no complete tokens to the left of "went" inside the new
//		// window
//		focus = thirdToken;
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(0, leftScopeTokens.size());
//
//		// there are two complete tokens to the left of "upon" inside the new
//		// window
//		focus = fifthToken;
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(2, leftScopeTokens.size());
//		assertEquals(thirdToken, leftScopeTokens.get(0));
//		assertEquals("trotting", leftScopeTokens.get(1).getCoveredText());
//
//		/*
//		 * there are three complete tokens to the left of the first comma (the
//		 * tenth token) inside the new window but the tenth token itself is not
//		 * inside the window and so an empty array is returned.
//		 */
//		focus = tenthToken;
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(0, leftScopeTokens.size());
//
//		focus = new BaseToken(jCas, 13,14);
//		leftScopeTokens = contextAnnotator.getLeftScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(1, leftScopeTokens.size());

	}

	
	/**
	 * This test tests ContextAnnotator.getRightScopeTokens(JCas, Annotation,
	 * Annotation). The method takes in annotations for the focus and window so
	 * the descriptor file parameter settings for FocusAnnotationClass and
	 * WindowAnnotationClass are effectively ignored by the method. This means
	 * we can pass any annotation in as the focus and window test the results.
	 * 
	 * @see ContextAnnotator#getRightScopeContextAnnotations(JCas, Annotation, Annotation)
	 * @throws ResourceInitializationException
	 * @throws AnalysisEngineProcessException
	 */
	@Test
	public void testRightScopeTokens() throws ResourceInitializationException, AnalysisEngineProcessException {

		String descriptor = "desc/test/TestContextAnnotator.xml";
		AnalysisEngine contextAE = TestUtil.getAE(new File(descriptor));
		UimaContext uimaContext = contextAE.getUimaContext();
		ContextAnnotator contextAnnotator = new ContextAnnotator();
//		contextAnnotator.initialize(uimaContext);
//
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.MAX_RIGHT_SCOPE_SIZE_PARAM, new Integer(4));
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.CONTEXT_ANNOTATION_CLASS_PARAM, "org.apache.ctakes.typesystem.type.syntax.BaseToken");
//
//		AnalysisEngine segmentTokenSentenceAE = TestUtil.getAE(new File("desc/test/SegmentTokenSentenceAggregate.xml"));
//		// this gives us a JCas that has segments, tokens, and sentences
//		JCas jCas = TestUtil
//				.processAE(
//						segmentTokenSentenceAE,
//						"A farmer went trotting upon his gray mare, Bumpety, bumpety, bump, With his daughter behind him, so rosy and fair, Lumpety, lumpety, lump.");
//
//		BaseToken firstToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
//		BaseToken secondToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
//		BaseToken thirdToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
//		BaseToken fifthToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 4);
//		BaseToken eighthToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 7);
//		BaseToken ninthToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 8);
//		BaseToken tenthToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 9);
//		BaseToken fifthFromEndToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 27);
//		BaseToken fourthFromEndToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 28);
//		BaseToken thirdFromEndToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 29);
//		BaseToken penultimateToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 30);
//		BaseToken lastToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 31);
//		Sentence sentence = TestUtil.getFeatureStructureAtIndex(jCas, Sentence.class, 0);
//
//		// perform some sanity checks to make sure that we have the annotations
//		// we think we have.
//		assertEquals("A", firstToken.getCoveredText());
//		assertEquals("farmer", secondToken.getCoveredText());
//		assertEquals("went", thirdToken.getCoveredText());
//		assertEquals("upon", fifthToken.getCoveredText());
//		assertEquals("mare", eighthToken.getCoveredText());
//		assertEquals(",", ninthToken.getCoveredText());
//		assertEquals("Bumpety", tenthToken.getCoveredText());
//		assertEquals(",", fifthFromEndToken.getCoveredText());
//		assertEquals("lumpety", fourthFromEndToken.getCoveredText());
//		assertEquals(",", thirdFromEndToken.getCoveredText());
//		assertEquals("lump", penultimateToken.getCoveredText());
//		assertEquals(".", lastToken.getCoveredText());
//		assertEquals(0, sentence.getBegin());
//		assertEquals(138, sentence.getEnd());
//
//		// if focus is the first token then the right tokens should number 4
//		Annotation focus = firstToken;
//		List<Annotation> rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(4, rightScopeTokens.size());
//		assertEquals(secondToken, rightScopeTokens.get(0));
//		assertEquals(thirdToken, rightScopeTokens.get(1));
//		assertEquals("trotting", rightScopeTokens.get(2).getCoveredText());
//		assertEquals(fifthToken, rightScopeTokens.get(3));
//		
//		// if focus is the second token then the right tokens should number 4
//		focus = secondToken;
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(4, rightScopeTokens.size());
//		assertEquals(thirdToken, rightScopeTokens.get(0));
//		assertEquals("trotting", rightScopeTokens.get(1).getCoveredText());
//		assertEquals(fifthToken, rightScopeTokens.get(2));
//		assertEquals("his", rightScopeTokens.get(3).getCoveredText());
//
//		// if focus is the ninth token then the left tokens should number 4
//		focus = ninthToken;
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(4, rightScopeTokens.size());
//		assertEquals("Bumpety", rightScopeTokens.get(0).getCoveredText());
//		assertEquals(",", rightScopeTokens.get(1).getCoveredText());
//		assertEquals("bumpety", rightScopeTokens.get(2).getCoveredText());
//		assertEquals(",", rightScopeTokens.get(3).getCoveredText());
//
//		focus = new PunctuationToken(jCas, 2, 12);
//		assertEquals("farmer wen", focus.getCoveredText());
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(4, rightScopeTokens.size());
//		assertEquals("trotting", rightScopeTokens.get(0).getCoveredText());
//		assertEquals("upon", rightScopeTokens.get(1).getCoveredText());
//		assertEquals("his", rightScopeTokens.get(2).getCoveredText());
//		assertEquals("gray", rightScopeTokens.get(3).getCoveredText());
//		
//		// if focus annotation is the last token then the right tokens should number 0
//		focus = lastToken;
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(0, rightScopeTokens.size());
//
//		// if focus annotation is the fifth-from-end token then the right tokens should number 4
//		focus = fifthFromEndToken;
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(4, rightScopeTokens.size());
//		assertEquals(fourthFromEndToken, rightScopeTokens.get(0));
//		assertEquals(thirdFromEndToken, rightScopeTokens.get(1));
//		assertEquals(penultimateToken, rightScopeTokens.get(2));
//		assertEquals(lastToken, rightScopeTokens.get(3));
//
//		// if focus annotation is the fourth-from-end token then the right tokens should number 3
//		focus = fourthFromEndToken;
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(3, rightScopeTokens.size());
//		assertEquals(thirdFromEndToken, rightScopeTokens.get(0));
//		assertEquals(penultimateToken, rightScopeTokens.get(1));
//		assertEquals(lastToken, rightScopeTokens.get(2));
//
//		// if focus annotation is the third-from-end token then the right tokens should number 2
//		focus = thirdFromEndToken;
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(2, rightScopeTokens.size());
//		assertEquals(penultimateToken, rightScopeTokens.get(0));
//		assertEquals(lastToken, rightScopeTokens.get(1));
//
//		focus = new IdentifiedAnnotation(jCas, 9, 27);
//		assertEquals("went trotting upon", focus.getCoveredText());
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(4, rightScopeTokens.size());
//		assertEquals("his", rightScopeTokens.get(0).getCoveredText());
//		assertEquals("gray", rightScopeTokens.get(1).getCoveredText());
//		assertEquals("mare", rightScopeTokens.get(2).getCoveredText());
//		assertEquals(",", rightScopeTokens.get(3).getCoveredText());
//
//		focus = new IdentifiedAnnotation(jCas, 9, 29);
//		assertEquals("went trotting upon h", focus.getCoveredText());
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals("gray", rightScopeTokens.get(0).getCoveredText());
//		assertEquals("mare", rightScopeTokens.get(1).getCoveredText());
//		assertEquals(ninthToken, rightScopeTokens.get(2));
//		assertEquals(tenthToken, rightScopeTokens.get(3));
//
//		focus = new BaseToken(jCas, 27, 28);
//		assertEquals(" ", focus.getCoveredText());
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals("his", rightScopeTokens.get(0).getCoveredText());
//		assertEquals("gray", rightScopeTokens.get(1).getCoveredText());
//		assertEquals("mare", rightScopeTokens.get(2).getCoveredText());
//		assertEquals(",", rightScopeTokens.get(3).getCoveredText());
//
//		
//		sentence = new Sentence(jCas, 120, 136);
//		System.out.println(sentence.getCoveredText());
//		assertEquals("ty, lumpety, lum", sentence.getCoveredText());
//		focus = new IdentifiedAnnotation(jCas, 124, 131);
//		assertEquals("lumpety", focus.getCoveredText());
//		rightScopeTokens = contextAnnotator.getRightScopeContextAnnotations(jCas, focus, sentence);
//		assertEquals(1, rightScopeTokens.size());

	}

	
	/**
	 * The test tests ContextAnnotator.getMiddleScopeTokens(JCas, Annotation).
	 * The method takes in an annotation for the focus and so the descriptor
	 * file parameter setting for FocusAnnotationClass is effectively ignored by
	 * the method. This means we can pass any annotation in as the focus and
	 * test the results. Similarly, the WindowAnnotationClass can be ignored as
	 * this is used to determine the focus annotations before this method is
	 * called. However, the list of returned annotations will be of the type
	 * specified in the parameter TokenAnnotationClass.
	 * 
	 * @see ContextAnnotator#getMiddleScopeContextAnnotations(JCas, Annotation)
	 * @throws ResourceInitializationException
	 * @throws AnalysisEngineProcessException
	 */
	@Test
	public void testMiddleScopeTokens() throws ResourceInitializationException, AnalysisEngineProcessException {
		//TODO: Pei- For unit tests, we should wire up the pipeline programmatically.
		//We can use uimafit instead of xml descriptor files.
		
		String descriptor = "desc/test/TestContextAnnotator.xml";
		AnalysisEngine contextAE = TestUtil.getAE(new File(descriptor));
		UimaContext uimaContext = contextAE.getUimaContext();
		ContextAnnotator contextAnnotator = new ContextAnnotator();
//		contextAnnotator.initialize(uimaContext);
//
//		TestUtil.testConfigParam(uimaContext, descriptor, ContextAnnotator.CONTEXT_ANNOTATION_CLASS_PARAM, "org.apache.ctakes.typesystem.type.syntax.BaseToken");
//
//
//		AnalysisEngine segmentTokenSentenceAE = TestUtil.getAE(new File("desc/test/SegmentTokenSentenceAggregate.xml"));
//		// this gives us a JCas that has segments, tokens, and sentences
//		JCas jCas = TestUtil
//				.processAE(
//						segmentTokenSentenceAE,
//						"A farmer went trotting upon his gray mare, Bumpety, bumpety, bump, With his daughter behind him, so rosy and fair, Lumpety, lumpety, lump.");
//
//		BaseToken firstToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
//		BaseToken secondToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
//		BaseToken thirdToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
//		BaseToken penultimateToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 30);
//		BaseToken lastToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 31);
//		Sentence sentence = TestUtil.getFeatureStructureAtIndex(jCas, Sentence.class, 0);
//
//		// some sanity checks to verify that we have the tokens and sentence we
//		// think we have.
//		assertEquals("A", firstToken.getCoveredText());
//		assertEquals("farmer", secondToken.getCoveredText());
//		assertEquals("went", thirdToken.getCoveredText());
//		assertEquals("lump", penultimateToken.getCoveredText());
//		assertEquals(".", lastToken.getCoveredText());
//		assertEquals(0, sentence.getBegin());
//		assertEquals(138, sentence.getEnd());
//
//		// if focus is the first token then the middle tokens should consist of
//		// that token
//		Annotation focus = firstToken;
//		List<Annotation> middleScopeTokens = contextAnnotator.getMiddleScopeContextAnnotations(jCas, focus);
//		assertEquals(1, middleScopeTokens.size());
//		assertEquals(focus, middleScopeTokens.get(0));
//
//		// if focus is the sentence, then the middle tokens should consist of
//		// all of the tokens in the sentence
//		focus = sentence;
//		middleScopeTokens = contextAnnotator.getMiddleScopeContextAnnotations(jCas, focus);
//		assertEquals(32, middleScopeTokens.size());
//		assertEquals(firstToken, middleScopeTokens.get(0));
//		assertEquals(secondToken, middleScopeTokens.get(1));
//		assertEquals(penultimateToken, middleScopeTokens.get(30));
//		assertEquals(lastToken, middleScopeTokens.get(31));
//
//		// if focus is a token that contains within it other tokens, then the
//		// middle tokens should consist of only the tokens that are completely
//		// inside the token and not tokens that are partially inside and not the
//		// large token itself.
//		focus = new PunctuationToken(jCas, 2, 12);
//		assertEquals("farmer wen", focus.getCoveredText());
//		middleScopeTokens = contextAnnotator.getMiddleScopeContextAnnotations(jCas, focus);
//		assertEquals(1, middleScopeTokens.size());
//		assertEquals(secondToken, middleScopeTokens.get(0));
//		focus = new PunctuationToken(jCas, 1, 13);
//		assertEquals(" farmer went", focus.getCoveredText());
//		middleScopeTokens = contextAnnotator.getMiddleScopeContextAnnotations(jCas, focus);
//		assertEquals(2, middleScopeTokens.size());
//		assertEquals(secondToken, middleScopeTokens.get(0));
//		assertEquals("went", middleScopeTokens.get(1).getCoveredText());
//
//		// if focus is a token overlaps with other tokens, but does not
//		// completely contain other tokens, then middle tokens should contain
//		// that one token
//		focus = new PunctuationToken(jCas, 3, 11);
//		assertEquals("armer we", focus.getCoveredText());
//		middleScopeTokens = contextAnnotator.getMiddleScopeContextAnnotations(jCas, focus);
//		assertEquals(1, middleScopeTokens.size());
//
//		// if focus is a named entity that overlaps with other tokens, but does
//		// not completely contain other tokens, then middle tokens should should
//		// be empty
//		focus = new IdentifiedAnnotation(jCas, 3, 11);
//		assertEquals("armer we", focus.getCoveredText());
//		middleScopeTokens = contextAnnotator.getMiddleScopeContextAnnotations(jCas, focus);
//		assertEquals(0, middleScopeTokens.size());
//
//		focus = new IdentifiedAnnotation(jCas, 0, 13);
//		assertEquals("A farmer went", focus.getCoveredText());
//		middleScopeTokens = contextAnnotator.getMiddleScopeContextAnnotations(jCas, focus);
//		assertEquals(3, middleScopeTokens.size());
//		assertEquals(firstToken, middleScopeTokens.get(0));
//		assertEquals(secondToken, middleScopeTokens.get(1));
//		assertEquals(thirdToken, middleScopeTokens.get(2));

	}

	@Test
	public void testParseScopeOrder() throws AnnotatorConfigurationException {
		ContextAnnotator ca = new ContextAnnotator();
		ca.parseScopeOrder(new String[] { "MIDDLE", "LEFT", "RIGHT", "ALL" });
		assertEquals(4, ca.scopes.size());
		assertEquals(ContextAnnotator.MIDDLE_SCOPE, (int)ca.scopes.get(0));
		assertEquals(ContextAnnotator.LEFT_SCOPE, (int)ca.scopes.get(1));
		assertEquals(ContextAnnotator.RIGHT_SCOPE, (int)ca.scopes.get(2));
		assertEquals(ContextAnnotator.ALL_SCOPE, (int)ca.scopes.get(3));
	}
}
