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

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.assertion.pipelines.GenerateTreeRepresentation;
import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.util.CleartkInitializationException;

public class AboveLeftFragmentExtractor extends TreeFragmentFeatureExtractor {

  public AboveLeftFragmentExtractor(String prefix, String resourceFilename)
      throws CleartkInitializationException {
    super(prefix, resourceFilename);
  }

  @Override
  public List<Feature> extract(JCas jcas, IdentifiedAnnotation annotation) {
    List<Feature> features = new ArrayList<>();
//    SimpleTree tree = AssertionTreeUtils.extractAboveLeftConceptTree(jcas, annotation, sems);
    SimpleTree tree = GenerateTreeRepresentation.getNegationTree(jcas, annotation, sems);
    
    for(SimpleTree frag : frags){
      if(TreeUtils.containsIgnoreCase(tree, frag)){
        features.add(new Feature("TreeFrag_" + prefix, frag.toString()));
      }
      
//      features.add(new Feature("TreeFrag_" + prefix + "_" + frag.toString(), TreeUtils.countFrags(tree, frag)));
    }
  
    return features;
  }

}
