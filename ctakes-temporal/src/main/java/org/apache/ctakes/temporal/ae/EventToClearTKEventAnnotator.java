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

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.timeml.type.Event;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

/**
 * for every cTAKES EventMention annotation, create a ClearTK Event Annotation
 * 
 * @author Chen Lin
 * 
 */
@PipeBitInfo(
		name = "ClearTK Event Creator",
		description = "Creates ClearTK Events from cTAKES Events.",
		dependencies = { PipeBitInfo.TypeProduct.EVENT }
)
public class EventToClearTKEventAnnotator extends JCasAnnotator_ImplBase {

	public EventToClearTKEventAnnotator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (EventMention eMention : JCasUtil.select(jCas, EventMention.class)) {
			// needed because SignSymptomMention, etc. do not have EventProperties
			if (eMention.getClass().equals(EventMention.class)) {
				// create a cleartk event object
				Event event = new Event(jCas);
				event.setBegin(eMention.getBegin());
				event.setEnd(eMention.getEnd());

				event.addToIndexes();
			}
		}

	}

	public static AnalysisEngineDescription getAnnotatorDescription()
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(EventToClearTKEventAnnotator.class);
	}

}
