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
package org.apache.ctakes.temporal.ae;

import java.io.File;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.Predicate;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

@PipeBitInfo(
		name = "NE Predicate Annotator",
		description = "Creates Events from Identified Annotations and Predicates.",
		dependencies = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
		products = { PipeBitInfo.TypeProduct.EVENT }
)
public class NEPredicateEventAnnotator extends TemporalEntityAnnotator_ImplBase {

	public NEPredicateEventAnnotator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process(JCas jCas, Segment segment)
			throws AnalysisEngineProcessException {
		for (EntityMention neMention : JCasUtil.select(jCas, EntityMention.class)) {
			// create the event object
			Event event = new Event(jCas);
			event.setConfidence(1.0f);

			// create the event mention
			EventMention eventMention = new EventMention(jCas, neMention.getBegin(), neMention.getEnd());
			eventMention.setConfidence(1.0f);

			// add the links between event, mention and properties
			//  		  event.setProperties(eventProperties);
			//  		  event.setMentions(neMention.getOntologyConceptArr());
			eventMention.setEvent(event);
			eventMention.setOntologyConceptArr(neMention.getOntologyConceptArr());

			// add the annotations to the indexes
			//  		  eventProperties.addToIndexes();
			event.addToIndexes();
			eventMention.addToIndexes();
		}

		//add predicates
		for (Predicate predicate : JCasUtil.select(jCas, Predicate.class)) {
			// create the event object
			Event event = new Event(jCas);
			event.setConfidence(1.0f);

			// create the event mention
			EventMention eventMention = new EventMention(jCas, predicate.getBegin(), predicate.getEnd());
			eventMention.setConfidence(1.0f);

			// add the links between event, mention and properties
			//	  		  event.setProperties(eventProperties);
			//	  		  event.setMentions(neMention.getOntologyConceptArr());
			eventMention.setEvent(event);

			// add the annotations to the indexes
			//	  		  eventProperties.addToIndexes();
			event.addToIndexes();
			eventMention.addToIndexes();
		}

	}

	public static AnalysisEngineDescription createAnnotatorDescription(String modelPath)
		      throws ResourceInitializationException {
		    return AnalysisEngineFactory.createEngineDescription(
		    	NEPredicateEventAnnotator.class,
		    	CleartkAnnotator.PARAM_IS_TRAINING,
		        false,
		        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
		        modelPath);
	}
	
	  /**
	   * @deprecated use String path instead of File.
	   * ClearTK will automatically Resolve the String to an InputStream.
	   * This will allow resources to be read within from a jar as well as File.  
	   */	 
	public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
		      throws ResourceInitializationException {
		    return AnalysisEngineFactory.createEngineDescription(
		    	NEPredicateEventAnnotator.class,
		    	CleartkAnnotator.PARAM_IS_TRAINING,
		        false,
		        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
		        new File(modelDirectory, "model.jar"));
	}

}
