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
import java.util.TreeMap;
//import java.util.logging.Logger;

import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class ClosestVerbExtractor implements FeatureExtractor1 {

  private String name;

//  private Logger logger = Logger.getLogger(this.getClass().getName());

  public ClosestVerbExtractor() {
    super();
    this.name = "ClosestVerb";
    
  }

  @Override
  public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
	  List<Feature> features = new ArrayList<>();
	  
	  //1 get covering sentence:
	  Map<EventMention, Collection<Sentence>> coveringMap =
			  JCasUtil.indexCovering(view, EventMention.class, Sentence.class);
	  EventMention targetTokenAnnotation = (EventMention)annotation;
	  Collection<Sentence> sentList = coveringMap.get(targetTokenAnnotation);
	  
	  Map<Integer, WordToken> verbDistMap = null;
	  
	  //2 get all Verbs within the same sentence as target event lies
	  if (sentList != null && !sentList.isEmpty()){
		  for(Sentence sent : sentList) {
			  verbDistMap = new TreeMap<>();
			  for ( WordToken wt : JCasUtil.selectCovered(view, WordToken.class, sent)) {
				  if (wt != null){
					  String pos = wt.getPartOfSpeech();
					  if (pos.startsWith("VB")){
						  verbDistMap.put(Math.abs(wt.getBegin() - annotation.getBegin()), wt);
					  }
				  }
			  }
			  for (Map.Entry<Integer, WordToken> entry : verbDistMap.entrySet()) {
				  Feature feature = new Feature(this.name+"_token", entry.getValue().getCoveredText());
				  features.add(feature);
				  //logger.info("found nearby closest verb: "+ entry.getValue().getCoveredText() + " POS:" + entry.getValue().getPartOfSpeech());
				  Feature posfeature = new Feature(this.name, entry.getValue().getPartOfSpeech());
				  features.add(posfeature);
				  break;		          
			  }
		  }
		  
	  }
	  return features;
  }

}
