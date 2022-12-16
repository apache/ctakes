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
import java.util.List;

import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import com.google.common.collect.Lists;

public class UmlsSingleFeatureExtractor implements FeatureExtractor1 {

  @Override
  public List<Feature> extract(JCas jCas, Annotation focusAnnotation)
			throws CleartkExtractorException {

    List<Feature> features = new ArrayList<>();
    
    JCas systemView = jCas;
//    try {
//      systemView = jCas.getView("_InitialView");
//    } catch (CASException e) {
//      throw new AnalysisEngineProcessException(e);
//    }
    List<String> eventTypes = Lists.newArrayList();
    
    if(focusAnnotation instanceof EventMention) {
//      List<EntityMention> entityMentions = JCasUtil.selectCovering(systemView, EntityMention.class, arg1.getBegin(), arg1.getEnd());

      CounterMap<String> typeCounts = 
          getMentionTypes(JCasUtil.selectCovering(systemView, EventMention.class, focusAnnotation.getBegin(), focusAnnotation.getEnd()));
      
      // print out totals:
      for(String typeId : typeCounts.keySet()){
        String featName = "eventTypeID_"+typeId;
        eventTypes.add(featName);
        features.add(new Feature(featName, typeCounts.get(typeId)));        
      }
      
      // TO print out just the types without counts:
//      for(String typeId : typeCounts.keySet()){
//        features.add(new Feature("arg1EntityTypeID_", typeId));
//      }
      
      // TODO: this is the correct implementatino, but it does not perform as well 
//      for(int typeID : uniqueTypeIDs) {
//        features.add(new Feature("arg1EntityTypeID", String.valueOf(typeID)));
//      }

    }

    
    return features;
  }
  
  private static CounterMap<String> getMentionTypes(List<EventMention> entities){
    CounterMap<String> typeCounts = new CounterMap<>();
    for(EventMention entityMention : entities) {
      if(entityMention.getDiscoveryTechnique() == CONST.NE_DISCOVERY_TECH_DICT_LOOKUP)
        typeCounts.add(String.valueOf(entityMention.getTypeID()));
    }
    return typeCounts;
    
  }
}
