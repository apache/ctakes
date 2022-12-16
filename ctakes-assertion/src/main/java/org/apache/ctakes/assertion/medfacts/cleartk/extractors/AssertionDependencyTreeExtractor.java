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

import java.util.List;

import org.apache.ctakes.assertion.pipelines.GenerateDependencyRepresentation;
import org.apache.ctakes.assertion.util.AssertionDepUtils;
import org.apache.ctakes.assertion.util.AssertionTreeUtils;
import org.apache.ctakes.assertion.util.SemanticClasses;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.TreeFeature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.util.CleartkInitializationException;

import com.google.common.collect.Lists;

public class AssertionDependencyTreeExtractor implements FeatureExtractor1<IdentifiedAnnotation> {
  protected SemanticClasses sems = null;

  public AssertionDependencyTreeExtractor() throws CleartkInitializationException {
    try{
      sems = new SemanticClasses(FileLocator.getAsStream("org/apache/ctakes/assertion/all_cues.txt"));
    }catch(Exception e){
      throw new CleartkInitializationException(e, "org/apache/ctakes/assertion/semantic_classes", "Could not find semantic classes resource.", new Object[]{});
    }
  }
  
  @Override
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1)
      throws CleartkExtractorException {
    List<Feature> feats = Lists.newArrayList();
    TreeFeature f1 = null;
    String treeString = null;
    
    List<Sentence> sents = JCasUtil.selectCovering(jCas, Sentence.class, arg1.getBegin(), arg1.getEnd());
    if(sents == null || sents.size() == 0){
      treeString = "(S (no parse))";
    }else{
      Sentence sent = sents.get(0);
      List<ConllDependencyNode> nodes = JCasUtil.selectCovered(ConllDependencyNode.class, sent);
    
      //treeString = AnnotationDepUtils.getTokenRelTreeString(jCas, nodes, new Annotation[]{arg1}, new String[]{"CONCEPT"}, true);
//      treeString = AssertionDepUtils.getTokenRelTreeString(jCas, nodes, arg1, "CONCEPT");
      SimpleTree tree = AssertionDepUtils.getTokenTreeString(jCas, nodes, arg1, GenerateDependencyRepresentation.UP_NODES);
      
      if(tree == null){
        treeString = "(S (no parse))";
      }else{
        AssertionTreeUtils.replaceDependencyWordsWithSemanticClasses(tree, sems);
        treeString = tree.toString();
//        treeString = treeString.replaceAll("\\(([^ ]+) \\)", "$1");
      }
    }

    f1 = new TreeFeature("TK_DW", treeString);   
    feats.add(f1);
    return feats;
  }

}
