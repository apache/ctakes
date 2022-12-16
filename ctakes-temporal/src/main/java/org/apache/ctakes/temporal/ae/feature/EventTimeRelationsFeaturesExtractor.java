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

import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

public class EventTimeRelationsFeaturesExtractor implements
    RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  /*
   * (non-Javadoc)
   * @see org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor#extract(org.apache.uima.jcas.JCas, org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation, org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation)
   * This feature extractor uses existing event-time relations as features, e.g., for an event-event classifier downstream
   * At this point, it only does narrative container-based features -- arg1 is in a narrative contrainer, arg2 is in a narrative container 
   */
  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
    List<Feature> feats = Lists.newArrayList();
    
    for(BinaryTextRelation etc : JCasUtil.select(jCas, BinaryTextRelation.class)){
      Annotation etcA1 = etc.getArg1().getArgument();
      Annotation etcA2 = etc.getArg2().getArgument();
      if(etcA1 instanceof TimeMention || etcA2 instanceof TimeMention){
        if(etc.getCategory().equalsIgnoreCase("CONTAINS")){
          if(etcA1.getBegin() == arg1.getBegin() && etcA1.getEnd() == arg1.getEnd() ||
              etcA2.getBegin() == arg1.getBegin() && etcA2.getEnd() == arg1.getEnd()){
            feats.add(new Feature("ARG1_IN_NC"));
          }
          if(etcA1.getBegin() == arg2.getBegin() && etcA1.getEnd() == arg2.getEnd() ||
              etcA2.getBegin() == arg2.getBegin() && etcA2.getEnd() == arg2.getEnd()){
            feats.add(new Feature("ARG2_IN_NC"));
          }
        }
      }
    }
    
    return feats;
  }

}
