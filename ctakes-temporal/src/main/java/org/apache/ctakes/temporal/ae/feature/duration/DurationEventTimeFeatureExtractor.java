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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Calculate probability that CONTAINS relation can exist between two arguments.
 */
public class DurationEventTimeFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2)
      throws AnalysisEngineProcessException {

    List<Feature> features = new ArrayList<>();

    String eventText = Utils.normalizeEventText(jCas, arg1); // arg1 is an event
    String timeText = arg2.getCoveredText().toLowerCase();  // arg2 is a time mention

    File durationLookup = new File(Utils.durationDistributionPath);
    Map<String, Map<String, Float>> textToDistribution = null;
    try {
      textToDistribution = Files.readLines(durationLookup, Charsets.UTF_8, new Utils.Callback());
    } catch(IOException e) {
      e.printStackTrace();
      return features;
    }
    Map<String, Float> eventDistribution = textToDistribution.get(eventText);

    HashSet<String> timeUnits = Utils.getTimeUnits(timeText);
    String timeUnit = timeUnits.iterator().next();
    float cumulativeProbability = 0f;
    for(String bin : Utils.bins) { 
      if(bin.equals(timeUnit)) {
        cumulativeProbability = cumulativeProbability + eventDistribution.get(bin); 
        break;
      }
      cumulativeProbability = cumulativeProbability + eventDistribution.get(bin); 
    }
    features.add(new Feature("cumulative_probability", cumulativeProbability));
    
    return features; 
  }
}
