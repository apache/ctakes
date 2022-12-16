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

import com.google.common.collect.Maps;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.timeml.type.Anchor;
import org.cleartk.timeml.type.DocumentCreationTime;
import org.cleartk.timeml.type.TemporalLink;

import java.util.Map;

/**
 * for every cTAKES EventMention annotation, create a ClearTK Event Annotation
 * 
 * @author Chen Lin
 * 
 */
@PipeBitInfo(
      name = "DocTimeRel ClearTK Annotator",
      description = "Annotates event relativity to document creation time.",
      dependencies = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.EVENT }
)
public class ClearTKDocTimeRelAnnotator extends JCasAnnotator_ImplBase {

	public ClearTKDocTimeRelAnnotator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		DocumentCreationTime dct = JCasUtil.selectSingle(jCas, DocumentCreationTime.class);
		Map<EventMention, String> docTimeRels = Maps.newHashMap();
		for (TemporalLink tlink : JCasUtil.select(jCas, TemporalLink.class)) {
			Anchor source = tlink.getSource();
			Anchor target = tlink.getTarget();
			if (source instanceof org.cleartk.timeml.type.Event && target.equals(dct)) {
				for (EventMention event : JCasUtil.selectCovered(jCas, EventMention.class, source)) {
					docTimeRels.put(event, tlink.getRelationType());
				}
			}
		}

		for (EventMention eventMention : JCasUtil.select(jCas, EventMention.class)) {
			// needed because SignSymptomMention, etc. do not have EventProperties
			if (eventMention.getClass().equals(EventMention.class)) {
				String docTimeRel = docTimeRels.get(eventMention);
				
				// convert missing or INCLUDES to OVERLAP
				if (docTimeRel == null || "INCLUDES".equals(docTimeRel)) {
					docTimeRel = "OVERLAP";
				}
				eventMention.getEvent().getProperties().setDocTimeRel(docTimeRel);
			}
		}
	}

	public static AnalysisEngineDescription getAnnotatorDescription()
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(ClearTKDocTimeRelAnnotator.class);
	}

}
