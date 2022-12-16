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
package org.apache.ctakes.postagger.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Iterator;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.utils.test.TestUtil;

public class POSTaggerTests {

	@Test
    public void testTagger() throws ResourceInitializationException {
		
		//this tests the tagging model with no tag dictionary
//		AnalysisEngine analysisEngine = TestUtil.getAE(new File("desc/test/POSTaggerAggregate.xml"));
//		JCas jCas = TestUtil.processAE(analysisEngine, "A farmer went trotting upon his gray mare, Bumpety, bumpety, bump, With his daughter behind him, so rosy and fair, Lumpety, lumpety, lump.");
//
//		BaseToken baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
//		assertEquals(0, baseToken.getBegin());
//		assertEquals(1, baseToken.getEnd());
//		assertEquals("A", baseToken.getCoveredText());
//		assertEquals("DT", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
//		assertEquals(2, baseToken.getBegin());
//		assertEquals(8, baseToken.getEnd());
//		assertEquals("farmer", baseToken.getCoveredText());
//		assertEquals("JJ", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
//		assertEquals("went", baseToken.getCoveredText());
//		assertEquals("JJ", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 3);
//		assertEquals("trotting", baseToken.getCoveredText());
//		assertEquals("NN", baseToken.getPartOfSpeech());
//
//		Iterator<?> baseTokenItr = jCas.getJFSIndexRepository().getAnnotationIndex(BaseToken.type).iterator();
//		while(baseTokenItr.hasNext()) {
//			baseToken = (BaseToken) baseTokenItr.next();
//			assertNotNull(baseToken.getPartOfSpeech());
//		}
	}

	/*
	 * For aiding debug
	 */
	private void printPosTags(JCas jCas) {
		BaseToken baseToken;
		// Print the pos tag assigned to each token
		for (int i=0; i < TestUtil.getFeatureStructureSize(jCas, BaseToken.class) ; i++) {
			baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, i);
			System.out.println(baseToken.getCoveredText() + " part of speech = " + baseToken.getPartOfSpeech());
		}
	}
	/**
	 * The first test uses a tag dictionary as case sensitive.  So, all the words should be constrained to have the tag "IN"<br>
	 * The second test uses the same tag dictionary as case sensitive.  However, all the words in the input string have had their
	 * case modified so that the tags should not be constrained to be "IN" - but instead simply be whatever the model chooses 
	 * for them.<br>
	 * The third test uses the same tag dictionary as case *insensitive* with the same input string used in the second test.  Because
	 * the dictionary is case insensitive the tags should be constrained to be "IN".  
	 * @throws ResourceInitializationException
	 */
	@Test
    public void testTagDictionary() throws ResourceInitializationException {
		//TODO: Pei- For unit tests, we should wire up the pipeline programmatically.
		//We can use uimafit instead of xml descriptor files.
//		
//		AnalysisEngine analysisEngine = TestUtil.getAE(new File("desc/test/POSTaggerAggregate2.xml"));
//		JCas jCas = TestUtil.processAE(analysisEngine, "Use of new biologic markers in the ovulation induction.");
//
//		//TEST1
//		BaseToken baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
//		assertEquals("Use", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
//		assertEquals("of", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
//		assertEquals("new", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 3);
//		assertEquals("biologic", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 4);
//		assertEquals("markers", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 5);
//		assertEquals("in", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 6);
//		assertEquals("the", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 7);
//		assertEquals("ovulation", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 8);
//		assertEquals("induction", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 9);
//		assertEquals(".", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		Iterator<?> baseTokenItr = jCas.getJFSIndexRepository().getAnnotationIndex(BaseToken.type).iterator();
//		while(baseTokenItr.hasNext()) {
//			baseToken = (BaseToken) baseTokenItr.next();
//			assertNotNull(baseToken.getPartOfSpeech());
//		}
//
//		//TEST2
//		analysisEngine = TestUtil.getAE(new File("desc/test/POSTaggerAggregate2.xml"));
//		jCas = TestUtil.processAE(analysisEngine, "use Of neW Biologic Markers IN The oVULation inductiOn.");
//
//		// printPosTags(jCas); // output all the tags so if one of the early ones fail, you still get to see the others
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
//		assertEquals("use", baseToken.getCoveredText());
//		assertEquals("NN", baseToken.getPartOfSpeech());
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
//		assertEquals("Of", baseToken.getCoveredText());
//		assertEquals("CD", baseToken.getPartOfSpeech()); // NN
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
//		assertEquals("neW", baseToken.getCoveredText());
//		assertEquals("JJ", baseToken.getPartOfSpeech());
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 3);
//		assertEquals("Biologic", baseToken.getCoveredText());
//		assertEquals("NN", baseToken.getPartOfSpeech());
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 4);
//		assertEquals("Markers", baseToken.getCoveredText());
//		assertEquals("NNS", baseToken.getPartOfSpeech());
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 5);
//		assertEquals("IN", baseToken.getCoveredText());
//		assertEquals("VBP", baseToken.getPartOfSpeech()); // IN
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 6);
//		assertEquals("The", baseToken.getCoveredText());
//		assertEquals("DT", baseToken.getPartOfSpeech());
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 7);
//		assertEquals("oVULation", baseToken.getCoveredText());
//		assertEquals("NN", baseToken.getPartOfSpeech());
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 8);
//		assertEquals("inductiOn", baseToken.getCoveredText());
//		assertEquals("NN", baseToken.getPartOfSpeech());
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 9);
//		assertEquals(".", baseToken.getCoveredText());
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//
//		
//		//TEST3
//		analysisEngine = TestUtil.getAE(new File("desc/test/POSTaggerAggregate3.xml"));
//		jCas = TestUtil.processAE(analysisEngine, "use Of neW Biologic Markers IN The oVULation inductiOn.");
//
//		// This assertion should really expect "IN" - but the POSDictionary class does not read in the 
//		// dictionary in a case insensitive way.  The word "Use" is the only word in the tag dictionary 
//		// that is not all lower case and so "use" effectively doesn't exist in the dictionary.  
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
//		assertEquals("NN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 3);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 4);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 5);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 6);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 7);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 8);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
//		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 9);
//		assertEquals("IN", baseToken.getPartOfSpeech());
//
	}

}
