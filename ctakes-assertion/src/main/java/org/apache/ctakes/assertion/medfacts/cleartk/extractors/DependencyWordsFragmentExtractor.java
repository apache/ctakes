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

import org.apache.ctakes.assertion.pipelines.GenerateDependencyRepresentation;
import org.apache.ctakes.assertion.util.AssertionDepUtils;
import org.apache.ctakes.assertion.util.AssertionTreeUtils;
import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.util.CleartkInitializationException;

public class DependencyWordsFragmentExtractor extends TreeFragmentFeatureExtractor {

  public DependencyWordsFragmentExtractor(String prefix, String fragsPath) throws CleartkInitializationException {
    super(prefix, fragsPath);
  }

  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation mention) {
    List<Feature> features = new ArrayList<Feature>();

    List<Sentence> sents = JCasUtil.selectCovering(jCas, Sentence.class, mention.getBegin(), mention.getEnd());
    if(sents != null && sents.size() > 0){

      Sentence sent = sents.get(0);
      List<ConllDependencyNode> nodes = JCasUtil.selectCovered(ConllDependencyNode.class, sent);

      SimpleTree tree = AssertionDepUtils.getTokenTreeString(jCas, nodes, mention, GenerateDependencyRepresentation.UP_NODES);
      if(tree == null){
        System.err.println("Tree is null!");
      }else{
        AssertionTreeUtils.replaceDependencyWordsWithSemanticClasses(tree, sems);
        for(SimpleTree frag : frags){
          if(TreeUtils.containsDepFragIgnoreCase(tree, frag)){
            features.add(new Feature("TreeFrag_" + prefix, frag.toString()));
          }
        }
      }

    }
    return features;
  }
}