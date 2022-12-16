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
package org.apache.ctakes.temporal.ae.feature.duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
//import org.threeten.bp.temporal.TemporalUnit;
import java.time.temporal.TemporalUnit;

import scala.collection.immutable.Set;

/**
 * Assumes all relations whose argument have no duration data have been deleted.
 */
public class DurationTimeUnitFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2)
      throws AnalysisEngineProcessException {
    
    List<Feature> features = new ArrayList<>();
    String timeText = arg2.getCoveredText().toLowerCase();  // arg2 is a time mention

    Set<TemporalUnit> units = Utils.runTimexParser(timeText);
    if(units == null) {
      features.add(new Feature("failed_normalization", true));
      return features;
    }
    
    scala.collection.Iterator<TemporalUnit> iterator = units.iterator();
    while(iterator.hasNext()) {
      TemporalUnit unit = iterator.next();
      String coarseTimeUnit = Utils.putInBin(unit.toString());
      
      if(coarseTimeUnit == null) {
        features.add(new Feature("failed_normalization", true));
        return features;
      }
      
      Map<String, Float> distribution = Utils.convertToDistribution(coarseTimeUnit);
      float timeExpectedDuration = Utils.expectedDuration(distribution);
      features.add(new Feature("time_duration", timeExpectedDuration));
    } 

    return features; 
  }
}
