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
package org.apache.ctakes.ytex.uima.annotators;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.Assert;
import org.junit.Test;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.JCasFactory;

/**
 * TODO get rid of hard-coded path to Types.xml - load from classpath
 * 
 * @author vgarla
 * 
 */
public class DictionaryLookupAnnotatorTest {

	@Test
	public void testDictionaryLookupSimple() throws UIMAException, IOException {
	    JCas jCas = JCasFactory.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
		String text = "acetaminophen";
	    jCas.setDocumentText(text);
	    WordToken tok = new WordToken(jCas);
	    tok.setBegin(0);
	    tok.setEnd(text.length());
	    tok.addToIndexes();
	    LookupWindowAnnotation lwa = new LookupWindowAnnotation(jCas);
	    lwa.setBegin(0);
	    lwa.setEnd(text.length());
	    lwa.addToIndexes();
		AggregateBuilder builder = new AggregateBuilder();
		addDescriptor(builder,
				"desc/analysis_engine/DictionaryLookupAnnotator.xml");
		AnalysisEngine engine = builder.createAggregate();
		engine.process(jCas);
		Assert.assertTrue(printAnnos(jCas, IdentifiedAnnotation.type) > 0);
	}
	/**
	 * integration test
	 * 
	 * @throws UIMAException
	 * @throws Exception
	 */
	@Test
	public void testDictionaryLookupIntegrated() throws UIMAException, IOException {
		// JCas jCas =
		// JCasFactory.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
		String text = "Dr. Doolitle asked patient\nto take a deep breath\nand exhale slowly.  Patient coughed and complained of abdominal pain.  Patient was administered acetaminophen.";
		AggregateBuilder builder = new AggregateBuilder();
		addDescriptor(builder,
				"desc/analysis_engine/AggregatePlaintextUMLSProcessorMinimal.xml");
		// addDescriptor(builder,
		// "desc/analysis_engine/SentenceDetectorAnnotator.xml");
		// addDescriptor(builder,
		// "../ctakes-core/desc/analysis_engine/TokenizerAnnotator.xml");
		// addDescriptor(builder,
		// "../ctakes-context-tokenizer/desc/analysis_engine/ContextDependentTokenizerAnnotator.xml");
		// addDescriptor(builder,
		// "../ctakes-pos-tagger/desc/POSTagger.xml");
		// addDescriptor(builder,
		// "../ctakes-chunker/desc/Chunker.xml");
		// addDescriptor(builder,
		// "../ctakes-chunker/desc/AdjustNounPhraseToIncludeFollowingNP.xml");
		// addDescriptor(builder,
		// "../ctakes-chunker/desc/AdjustNounPhraseToIncludeFollowingPPNP.xml");
		// addDescriptor(builder,
		// "../ctakes-clinical-pipeline/desc/analysis_engine/LookupWindowAnnotator.xml");
		// addDescriptor(builder,
		// "desc/analysis_engine/DictionaryLookupAnnotator.xml");
		AnalysisEngine engine = builder.createAggregate();
		JCas jCas = engine.newJCas();
		jCas.setDocumentText(text);
//		Segment s = new Segment(jCas);
//		s.setBegin(0);
//		s.setEnd(text.length());
//		s.setId("DEFAULT");
//		s.addToIndexes();
		// run the analysis engine
		engine.process(jCas);
		Assert.assertTrue(printAnnos(jCas, Chunk.type) > 0);
		Assert.assertTrue(printAnnos(jCas, LookupWindowAnnotation.type) > 0);
		Assert.assertTrue(printAnnos(jCas, IdentifiedAnnotation.type) > 0);
	}

	private int printAnnos(JCas jCas, int type) {
		AnnotationIndex<Annotation> sentences = jCas.getAnnotationIndex(type);
		Iterator<Annotation> iter = sentences.iterator();
		int sentCount = 0;
		while (iter.hasNext()) {
			Annotation sent = (Annotation) iter.next();
			System.out.println(sent.getClass().getSimpleName() + " ["
					+ sent.getCoveredText() + "]");
			sentCount++;
		}
		return sentCount;
	}

	private void addDescriptor(AggregateBuilder builder, String path)
			throws IOException, InvalidXMLException {
		File fileCtakes = new File(path);
		XMLParser parser = UIMAFramework.getXMLParser();
		XMLInputSource source = new XMLInputSource(fileCtakes);
		builder.add(parser.parseAnalysisEngineDescription(source));
	}
}
