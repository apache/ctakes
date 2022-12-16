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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.utils.test.TestUtil;

public class OpenNLPPOSCollectionReaderTests {

	@Test
	public void testReader() throws ResourceInitializationException, IOException, CollectionException {
		CollectionReader collectionReader = TestUtil.getCR(new File("desc/test/OpenNLPPOSCollectionReader.xml"));
		AnalysisEngine analysisEngine = TestUtil.getAE(new File("desc/test/NullAnnotator.xml"));
		JCas jCas = analysisEngine.newJCas();
		collectionReader.getNext(jCas.getCas());
		
		BaseToken baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
		assertEquals(0, baseToken.getBegin());
		assertEquals(1, baseToken.getEnd());
		assertEquals("A", baseToken.getCoveredText());
		assertEquals("A", baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
		assertEquals("farmer", baseToken.getCoveredText());
		assertEquals("B", baseToken.getPartOfSpeech());
		
		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
		assertEquals("went", baseToken.getCoveredText());
		assertEquals("CC", baseToken.getPartOfSpeech());
		
		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 3);
		assertEquals("trotting", baseToken.getCoveredText());
		assertEquals("DDD", baseToken.getPartOfSpeech());
		
		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 4);
		assertEquals("upon_A", baseToken.getCoveredText());
		assertEquals("E", baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 5);
		assertEquals("his", baseToken.getCoveredText());
		assertEquals("EE", baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 6);
		assertEquals(".", baseToken.getCoveredText());
		assertEquals(".", baseToken.getPartOfSpeech());

		CollectionException ce = null;
		jCas = analysisEngine.newJCas();
		try {
			collectionReader.getNext(jCas.getCas());
		} catch(CollectionException e) {
			ce = e;
		}
		assertNotNull(ce);
		
		jCas = analysisEngine.newJCas();
		collectionReader.getNext(jCas.getCas());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
		assertEquals("A_", baseToken.getCoveredText());
		assertEquals("A", baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
		assertEquals("_", baseToken.getCoveredText());
		assertEquals("B", baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
		assertEquals("_B_", baseToken.getCoveredText());
		assertEquals("C", baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 3);
		assertEquals("B", baseToken.getCoveredText());
		assertEquals("_", baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 4);
		assertEquals("__", baseToken.getCoveredText());
		assertEquals("_", baseToken.getPartOfSpeech());
	}
	
	@Test
	public void testLoadWordsOnly() throws ResourceInitializationException, IOException, CollectionException {
		CollectionReader collectionReader = TestUtil.getCR(new File("desc/test/OpenNLPPOSCollectionReader2.xml"));
		AnalysisEngine analysisEngine = TestUtil.getAE(new File("desc/test/NullAnnotator.xml"));
		JCas jCas = analysisEngine.newJCas();
		collectionReader.getNext(jCas.getCas());
		
		BaseToken baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 0);
		assertEquals(0, baseToken.getBegin());
		assertEquals(1, baseToken.getEnd());
		assertEquals("A", baseToken.getCoveredText());
		assertNull(baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 1);
		assertEquals("farmer", baseToken.getCoveredText());
		assertNull(baseToken.getPartOfSpeech());
		
		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 2);
		assertEquals("went", baseToken.getCoveredText());
		assertNull(baseToken.getPartOfSpeech());
		
		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 3);
		assertEquals("trotting", baseToken.getCoveredText());
		assertNull(baseToken.getPartOfSpeech());
		
		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 4);
		assertEquals("upon_A", baseToken.getCoveredText());
		assertNull(baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 5);
		assertEquals("his", baseToken.getCoveredText());
		assertNull(baseToken.getPartOfSpeech());

		baseToken = TestUtil.getFeatureStructureAtIndex(jCas, BaseToken.class, 6);
		assertEquals(".", baseToken.getCoveredText());
		assertNull(baseToken.getPartOfSpeech());

		CollectionException ce = null;
		jCas = analysisEngine.newJCas();
		try {
			collectionReader.getNext(jCas.getCas());
		} catch(CollectionException e) {
			ce = e;
		}
		assertNotNull(ce);
		
	}

}
