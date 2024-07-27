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

import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NearbyVerbTenseXExtractor implements FeatureExtractor1 {

  private String name;

  //private Logger LOGGER = LoggerFactory.getLogger(this.getClass().getName());

  public NearbyVerbTenseXExtractor() {
    super();
    this.name = "VerbTenseFeature";
    
  }

  @Override
  public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
	  List<Feature> features = new ArrayList<>();

	  //1 get covering sentence:
	  Map<EventMention, List<Sentence>> coveringMap =
			  JCasUtil.indexCovering(view, EventMention.class, Sentence.class);
	  EventMention targetTokenAnnotation = (EventMention)annotation;
	  Collection<Sentence> sentList = coveringMap.get(targetTokenAnnotation);

	  //2 get Verb Tense
	  if (sentList != null && !sentList.isEmpty()){
		  for(Sentence sent : sentList) {
			  String verbTP ="";
			  for ( WordToken wt : JCasUtil.selectCovered(view, WordToken.class, sent)) {
				  if (wt != null){
					  String pos = wt.getPartOfSpeech();
					  if (pos.startsWith("VB")){
						  verbTP = verbTP + "_" + pos;
					  }
				  }
			  }
			  Feature feature = new Feature(this.name, verbTP);
			  features.add(feature);
			  //LOGGER.info("found nearby verb's pos tag: "+ verbTP);
		  }

	  }
	  return features;
  }


	public List<Feature> extract( final JCas view, final Annotation annotation, final Collection<WordToken> tokens )
			throws CleartkExtractorException {
		final List<Feature> features = new ArrayList<>();
		StringBuilder verbTP = new StringBuilder();
		for ( WordToken wt : tokens ) {
			if ( wt != null ) {
				String pos = wt.getPartOfSpeech();
				if ( pos.startsWith( "VB" ) ) {
					verbTP.append( "_" )
							.append( pos );
				}
			}
		}
		final Feature feature = new Feature( this.name, verbTP.toString() );
		features.add( feature );
		return features;
	}

	
}
