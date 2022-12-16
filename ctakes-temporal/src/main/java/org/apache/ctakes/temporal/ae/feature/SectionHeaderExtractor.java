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
package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
//import java.util.logging.Logger;

import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class SectionHeaderExtractor implements FeatureExtractor1 {

	private String name;

	//  private Logger logger = Logger.getLogger(this.getClass().getName());

	public SectionHeaderExtractor() {
		super();
		this.name = "SectionHeader";

	}

	@Override
	public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
		List<Feature> features = new ArrayList<>();

		//1 get covering sentence:
		Map<EventMention, Collection<Segment>> coveringMap =
				JCasUtil.indexCovering(view, EventMention.class, Segment.class);
		EventMention targetTokenAnnotation = (EventMention)annotation;
		Collection<Segment> segList = coveringMap.get(targetTokenAnnotation);

		//2 get Verb Tense
		if (segList != null && !segList.isEmpty()){
			for(Segment seg : segList) {
				String segname = seg.getId();
				if (!segname.equals("SIMPLE_SEGMENT")){//remove simple segment
					Feature feature = new Feature(this.name, segname);
					features.add(feature);
				}else{
					continue;
				}
				//			  logger.info("found segment id: "+ segname);
			}

		}
		return features;
	}

}
