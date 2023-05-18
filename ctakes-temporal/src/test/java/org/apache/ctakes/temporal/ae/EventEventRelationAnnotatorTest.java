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

import com.google.common.collect.Lists;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.CopyNPChunksToLookupWindowAnnotations;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.RemoveEnclosedLookupWindows;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

public class EventEventRelationAnnotatorTest extends TemporalTest_ImplBase {

	// LOG4J logger based on class name
	private Logger LOGGER = Logger.getLogger(getClass().getName());

	@Test
	public void testPipeline() throws UIMAException, IOException {

		String note = "The patient is a 55-year-old man referred by Dr. Good for recently diagnosed colorectal cancer.  "
				+ "The patient was well till 6 months ago, when he started having a little blood with stool.";
		JCas jcas = JCasFactory.createJCas();
		jcas.setDocumentText(note);

		// Get the default pipeline with umls dictionary lookup
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(getTokenProcessingPipeline());
		builder.add(AnalysisEngineFactory
				.createEngineDescription(CopyNPChunksToLookupWindowAnnotations.class));
		builder.add(AnalysisEngineFactory
				.createEngineDescription(RemoveEnclosedLookupWindows.class));
		// Commented out the Dictionary lookup for the test
		// Uncomment and set -Dctakes.umlsuser and -Dctakes.umlspw env params if
		// needed
		//builder.add(UmlsDictionaryLookupAnnotator.createAnnotatorDescription());
		builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());

		// Add BackwardsTimeAnnotator
		builder.add(BackwardsTimeAnnotator
				.createAnnotatorDescription("/org/apache/ctakes/temporal/models/timeannotator/model.jar"));
		// Add EventAnnotator
		builder.add(EventAnnotator
				.createAnnotatorDescription("/org/apache/ctakes/temporal/models/eventannotator/model.jar"));
		//link event to eventMention
		builder.add(AnalysisEngineFactory.createEngineDescription(AddEvent.class));
		// Add Event to Event Relation Annotator
		builder.add(EventEventRelationAnnotator
				.createAnnotatorDescription("/org/apache/ctakes/temporal/models/eventevent/model.jar"));

		SimplePipeline.runPipeline(jcas, builder.createAggregateDescription());

		Collection<TemporalTextRelation> relations = JCasUtil.select(jcas,
				TemporalTextRelation.class);

		for (TemporalTextRelation relation : relations) {
			LOGGER.info("Relation: " + relation.getArg1() + " " + relation.getArg2() + " Category: " + relation.getCategory());
			}
		// assertEquals(2, temp.size());
		// assertTrue(temp.contains("recently"));
		// assertTrue(temp.contains("6 months ago"));
	}

	public static class AddEvent extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (EventMention emention : Lists.newArrayList(JCasUtil.select(
					jCas,
					EventMention.class))) {
				EventProperties eventProperties = new org.apache.ctakes.typesystem.type.refsem.EventProperties(jCas);

				// create the event object
				Event event = new Event(jCas);

				// add the links between event, mention and properties
				event.setProperties(eventProperties);
				emention.setEvent(event);

				// add the annotations to the indexes
				eventProperties.addToIndexes();
				event.addToIndexes();
			}
		}
	}

}
