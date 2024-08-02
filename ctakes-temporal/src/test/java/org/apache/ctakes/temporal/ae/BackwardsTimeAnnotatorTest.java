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
package org.apache.ctakes.temporal.ae;

import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BackwardsTimeAnnotatorTest extends TemporalTest_ImplBase {

	// LOG4J logger based on class name
	private Logger LOGGER = LoggerFactory.getLogger(getClass().getName());

	@Test
	public void testPipeline() throws UIMAException, IOException {
		if ( !canUseUmlsDictionary() ) {
			LOGGER.warn( "Running this test requires that UmlsKey is set in your environment." );
			return;
		}
		String note = "The patient is a 55-year-old man referred by Dr. Good for recently diagnosed colorectal cancer.  "
				+ "The patient was well until 6 months ago, when he started having a little blood with stool.";
		JCas jcas = JCasFactory.createJCas();
		jcas.setDocumentText(note);

		// Use piper files so that a module doesn't need to depend upon all of ctakes just to run a test.
//		// Get the default pipeline with umls dictionary lookup
//		AggregateBuilder builder = new AggregateBuilder();
//		builder.add(getTokenProcessingPipeline());
//		builder.add(AnalysisEngineFactory
//				.createEngineDescription(org.apache.ctakes.temporal.eval.Evaluation_ImplBase.CopyNPChunksToLookupWindowAnnotations.class));
//		builder.add(AnalysisEngineFactory
//				.createEngineDescription(org.apache.ctakes.temporal.eval.Evaluation_ImplBase.RemoveEnclosedLookupWindows.class));
//		// Commented out the Dictionary lookup for the test
//		// Uncomment and set -Dctakes.umlsuser and -Dctakes.umlspw env params if
//		// needed
//		// builder.add(UmlsDictionaryLookupAnnotator.createAnnotatorDescription());
//		builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
//
//		// Add BackwardsTimeAnnotator
//		builder.add(BackwardsTimeAnnotator
//				.createAnnotatorDescription("/org/apache/ctakes/temporal/models/timeannotator/model.jar"));
//
//		SimplePipeline.runPipeline(jcas, builder.createAggregateDescription());

		// Use piper files so that a module doesn't need to depend upon all of ctakes just to run a test.
		new PiperFileReader( "DefaultTemporalPipeline" ).getBuilder().run( jcas );

		Collection<TimeMention> mentions = JCasUtil.select(jcas,
				TimeMention.class);

		ArrayList<String> temp = new ArrayList<>();
		for (TimeMention mention : mentions) {
			LOGGER.info("Time: " + mention.getCoveredText());
			LOGGER.info("Time class: " + mention.getTimeClass());
			//LOGGER.info("Time: " + mention.getTime().getNormalizedForm());
			temp.add(mention.getCoveredText());
		}
		assertEquals(2, temp.size());
		assertTrue(temp.contains("recently"));
		assertTrue(temp.contains("until 6 months ago"));
	}

}
