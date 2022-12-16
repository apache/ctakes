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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.typesystem.type.textsem.Predicate;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

public class FramesetCategoryExtractor implements FeatureExtractor1 {
  
  private Map<String, String> frameSetCategories;
  
  public FramesetCategoryExtractor() throws ResourceInitializationException {
    String path = "/org/apache/ctakes/temporal/propbank_noneventive_framesets.txt";
    URL uri = FramesetCategoryExtractor.class.getResource(path);
    this.frameSetCategories = Maps.newHashMap();
    try {
      for (String line : Resources.readLines(uri, Charsets.US_ASCII)) {
        String[] tagAndFrameset = line.split("\\s+");
        this.frameSetCategories.put(tagAndFrameset[1], tagAndFrameset[0]);
      }
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  @Override
  public List<Feature> extract(JCas view, Annotation focusAnnotation)
      throws CleartkExtractorException {
    List<Feature> features = Lists.newArrayList();
    for (Predicate predicate : JCasUtil.selectCovered(view, Predicate.class, focusAnnotation)) {
      String category = this.frameSetCategories.get(predicate.getFrameSet());
      if (category != null) {
        features.add(new Feature("FramesetCategory", category));
      }
    }
    return features;
  }

}
