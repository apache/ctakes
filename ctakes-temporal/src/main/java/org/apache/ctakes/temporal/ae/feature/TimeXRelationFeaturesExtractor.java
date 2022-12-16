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
package org.apache.ctakes.temporal.ae.feature;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;
/**
 * extract the nearby nearest time, date information for any event 
 * @author CH151862
 *
 */
public class TimeXRelationFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	private String name="TimeXRelatioinFeature";

	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = Lists.newArrayList();

		List<EventMention> events = Lists.newArrayList();
		if(arg1 instanceof EventMention) events.add((EventMention)arg1);
		if(arg2 instanceof EventMention) events.add((EventMention)arg2);

		Map<EventMention, Collection<Sentence>> coveringMap =
				JCasUtil.indexCovering(jCas, EventMention.class, Sentence.class);

		for(EventMention event : events){
			Collection<Sentence> sentList = coveringMap.get(event);

			//2 get TimeX
			Map<Integer, IdentifiedAnnotation> timeDistMap = new TreeMap<>();

			for(Sentence sent : sentList) {
				for (TimeMention time : JCasUtil.selectCovered(jCas, TimeMention.class, sent)) {
					timeDistMap.put(Math.abs(time.getBegin() - event.getBegin()), time);
				}
				for (TimeAnnotation time : JCasUtil.selectCovered(jCas, TimeAnnotation.class, sent)) {
					timeDistMap.put(Math.abs(time.getBegin() - event.getBegin()), time);
				}
				for (DateAnnotation time : JCasUtil.selectCovered(jCas, DateAnnotation.class, sent)) {
					timeDistMap.put(Math.abs(time.getBegin() - event.getBegin()), time);
				}
			}

			//get the closest Time Expression feature
			for (Map.Entry<Integer, IdentifiedAnnotation> entry : timeDistMap.entrySet()) {
				Feature feature = new Feature(this.name, entry.getValue().getCoveredText());
				features.add(feature);
				//			  logger.info("add time feature: "+ entry.getValue().getCoveredText() + entry.getValue().getTimeClass());
				Feature indicator = new Feature("TimeXNearby", this.name);
				features.add(indicator);
				Feature type = new Feature("TimeXType", entry.getValue().getClass());
				features.add(type);

				//add PP get Heading preposition
				for(TreebankNode treebankNode : JCasUtil.selectCovering(
						jCas, 
						TreebankNode.class, 
						entry.getValue().getBegin(), 
						entry.getValue().getEnd())) {

					if(treebankNode.getNodeType().equals("PP")) {
						Feature PPNodeType = new Feature("Timex_PPNodeType", treebankNode.getNodeType());
						features.add(PPNodeType);
						String value = treebankNode.getNodeValue();
						if(value != null)
							features.add(new Feature("Timex_PPNodeValue_", value));
						features.add(new Feature("PP_Timex_", entry.getValue().getClass().getCanonicalName()));
						break;
					}
				}

				break;

			}
		}

		return features;
	}

}
