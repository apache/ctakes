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

import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.MeasurementAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class DateAndMeasurementExtractor implements FeatureExtractor1<Annotation> {

  private String name;

  public DateAndMeasurementExtractor() {
    super();
    this.name = "DateXFeature";
    
  }

  @Override
  public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
	  List<Feature> features = new ArrayList<>();
	  
	  //1 get covering sentence:
	  Map<EventMention, Collection<Sentence>> coveringMap =
			  JCasUtil.indexCovering(view, EventMention.class, Sentence.class);
	  EventMention targetTokenAnnotation = (EventMention)annotation;
	  Collection<Sentence> sentList = coveringMap.get(targetTokenAnnotation);
	  
	  //2 get DateX
	  if (sentList != null && !sentList.isEmpty()){
		  for(Sentence sent : sentList) {
			  for (@SuppressWarnings("unused") DateAnnotation date : JCasUtil.selectCovered(view, DateAnnotation.class, sent)) {
				  Feature indicator = new Feature("DateXNearby", this.name);
				  features.add(indicator);
				  break;
			  }
		  }
	  }
	  
	//3 get Measurement
	  if (sentList != null && !sentList.isEmpty()){
		  for(Sentence sent : sentList) {
			  for (@SuppressWarnings("unused") MeasurementAnnotation date : JCasUtil.selectCovered(view, MeasurementAnnotation.class, sent)) {
				  Feature indicator = new Feature("MeasurementNearby", "measure");
				  features.add(indicator);
				  break;
			  }
		  }
	  }
	  
	//4 get number
	  if (sentList != null && !sentList.isEmpty()){
		  for(Sentence sent : sentList) {
			  for (@SuppressWarnings("unused") NumToken date : JCasUtil.selectCovered(view, NumToken.class, sent)) {
				  Feature indicator = new Feature("NumTokenNearby", "NumToken");
				  features.add(indicator);
				  break;
			  }
		  }
	  }
	  
	  return features;
  }

}
