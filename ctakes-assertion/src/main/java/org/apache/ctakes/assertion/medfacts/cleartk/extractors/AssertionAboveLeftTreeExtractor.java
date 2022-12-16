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
package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import static org.apache.ctakes.assertion.util.AssertionTreeUtils.extractAboveLeftConceptTree;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.assertion.util.SemanticClasses;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.TreeFeature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.util.CleartkInitializationException;

public class AssertionAboveLeftTreeExtractor implements FeatureExtractor1<IdentifiedAnnotation> {
  protected SemanticClasses sems = null;

  public AssertionAboveLeftTreeExtractor() throws CleartkInitializationException{
    try{
      sems = new SemanticClasses(FileLocator.getAsStream("org/apache/ctakes/assertion/all_cues.txt"));
    }catch(Exception e){
      throw new CleartkInitializationException(e, "org/apache/ctakes/assertion/semantic_classes", "Could not find semantic classes resource.", new Object[]{});
    }
  }
  
  @Override
  public List<Feature> extract(JCas jcas, IdentifiedAnnotation annotation)
      throws CleartkExtractorException {
    List<Feature> features = new ArrayList<>();
    SimpleTree tree = extractAboveLeftConceptTree(jcas, annotation, sems);
    features.add(new TreeFeature("TK_AboveLeftTree", tree.toString()));
    return features;
  }
}
