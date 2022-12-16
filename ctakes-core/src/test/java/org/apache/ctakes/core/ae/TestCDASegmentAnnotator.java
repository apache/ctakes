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
package org.apache.ctakes.core.ae;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.cleartk.util.cr.FilesCollectionReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCDASegmentAnnotator {

	public static String INPUT_FILE = "../ctakes-regression-test/testdata/input/plaintext/doc2_07543210_sample_current.txt";

	@Test
	public void TestCDASegmentPipeLine() throws Exception {
		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
				.createTypeSystemDescription();

		CollectionReaderDescription reader = CollectionReaderFactory
				.createReaderDescription(FilesCollectionReader.class,
						typeSystem, FilesCollectionReader.PARAM_ROOT_FILE,
						INPUT_FILE);

		AnalysisEngineDescription sectionAnnotator = AnalysisEngineFactory
				.createEngineDescription(CDASegmentAnnotator.class, typeSystem);
		AnalysisEngineDescription dumpOutput = AnalysisEngineFactory.createEngineDescription(
				DumpOutputAE.class, typeSystem);
		// SimplePipeline.runPipeline(reader, sectionAnnotator, dumpOutput);
		JCasIterable casIter = new JCasIterable(reader, sectionAnnotator,
				dumpOutput);
		final String expected_hpi_section = "2.16.840.1.113883.10.20.22.2.20";
		final int expected_begin = 1634;
		final int expected_end = 1696;
		boolean section_exists = false;
		int section_begin = 0;
		int section_end = 0;

		for(JCas jCas : casIter){
			for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
				if (expected_hpi_section.equalsIgnoreCase(segment.getId())) {
					section_exists = true;
					section_begin = segment.getBegin();
					section_end = segment.getEnd();
					break;
				}
			}
		}

		assertEquals(section_exists, true);
		assertEquals(expected_begin, section_begin);
		assertEquals("", expected_end, section_end);
	}

	public static class DumpOutputAE extends JCasAnnotator_ImplBase {
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
				System.out.println("Segment:" + segment.getId() + " Begin:"
						+ segment.getBegin() + " End:" + segment.getEnd());
				// System.out.println("Text" + segment.getCoveredText());
			}
		}
	}
}
