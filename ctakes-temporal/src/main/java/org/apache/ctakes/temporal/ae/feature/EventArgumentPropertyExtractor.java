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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

public class EventArgumentPropertyExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<>();

		Set<Sentence> coveringSents = new HashSet<>();
		if( arg1 instanceof EventMention){
			coveringSents.addAll(JCasUtil.selectCovering(jCas, Sentence.class, arg1.getBegin(), arg1.getEnd()));
		}else if(arg2 instanceof EventMention){
			coveringSents.addAll(JCasUtil.selectCovering(jCas, Sentence.class, arg2.getBegin(), arg2.getEnd()));
		}else{
			return feats;
		}

		for(Sentence coveringSent : coveringSents){
			List<EventMention> events = JCasUtil.selectCovered(EventMention.class, coveringSent);
			List<EventMention> realEvents = new ArrayList<>();
			for(EventMention event : events){
				// filter out ctakes events
				if(event.getClass().equals(EventMention.class)){
					realEvents.add(event);
				}
			}
			events = realEvents;
			if( events.size()>0){
				EventMention anchor = events.get(0);
				if(arg1 == anchor){
					feats.add(new Feature("Arg1LeftmostEvent"));
				}else if(arg2 == anchor){
					feats.add(new Feature("Arg2LeftmostEvent"));
				}
			}
		}
		
		if(arg1 instanceof EventMention){
			feats.addAll(getEventFeats("mention1property", (EventMention)arg1));
		}
		if(arg2 instanceof EventMention){
			feats.addAll(getEventFeats("mention2property", (EventMention)arg2));
		}

		return feats;
	}

	private static Collection<? extends Feature> getEventFeats(String name, EventMention mention) {
		List<Feature> feats = new ArrayList<>();
		//add contextual modality as a feature
		if(mention.getEvent()==null || mention.getEvent().getProperties() == null){
			return feats;
		}
		String contextualModality = mention.getEvent().getProperties().getContextualModality();
		if (contextualModality != null)
			feats.add(new Feature(name + "_modality", contextualModality));

		//    feats.add(new Feature(name + "_aspect", mention.getEvent().getProperties().getContextualAspect()));//null
		//    feats.add(new Feature(name + "_permanence", mention.getEvent().getProperties().getPermanence()));//null
		Integer polarity = mention.getEvent().getProperties().getPolarity();
		if(polarity!=null )
			feats.add(new Feature(name + "_polarity", polarity));
		//    feats.add(new Feature(name + "_category", mention.getEvent().getProperties().getCategory()));//null
		//    feats.add(new Feature(name + "_degree", mention.getEvent().getProperties().getDegree()));//null
		String docTimeRel = mention.getEvent().getProperties().getDocTimeRel();
		if(docTimeRel!=null)
			feats.add(new Feature(name + "_doctimerel", docTimeRel));

		Integer typeId = mention.getEvent().getProperties().getTypeIndexID();
		if(typeId != null)
			feats.add(new Feature(name + "_typeId"));

		return feats;
	}

}
