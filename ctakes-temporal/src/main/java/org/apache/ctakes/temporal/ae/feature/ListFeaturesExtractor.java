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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

public class ListFeaturesExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  /*
   * (non-Javadoc)
   * @see org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor#extract(org.apache.uima.jcas.JCas, org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation, org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation)
   * This feature extractor checks if each argument is part of a list, by looking
   * in a parse tree at sibling categories, for commas, coordinators given an NP parent.
   * Features include whether either arg is in a list and what list position (start, middle, end),
   * and whether the left sibling in the list is part of an existing relation, and if so, whether
   * that relation has the same other argument as the current proposed relation.
   */
  public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
    List<Feature> feats = Lists.newArrayList();
    
    feats.addAll(getArgFeats(jCas, arg1, arg2, "Arg1"));
    feats.addAll(getArgFeats(jCas, arg2, arg2, "Arg2"));
    return feats;
  }

  private static Collection<? extends Feature> getArgFeats(JCas jCas, IdentifiedAnnotation primeArg,
      IdentifiedAnnotation secondArg, String prefix) {
    List<Feature> feats = Lists.newArrayList();
    
    List<TreebankNode> nodes = JCasUtil.selectCovered(TreebankNode.class, primeArg);
    if(nodes.size() > 0){
      TreebankNode node = nodes.get(0);
      if(node.getBegin() == primeArg.getBegin() && node.getEnd() == primeArg.getEnd()){
        HashSet<String> priorNPs = new HashSet<String>();
        // we have a node with the exact span as the argument
        // now check if it is an element of a list
        // first move NNs up to their constituent
        if(node.getNodeType().startsWith("NN")){
          node = node.getParent();
        }
        TreebankNode parent = node.getParent();
        if(parent == null) return feats;
        int childIndex = -1;
        for(int i = 0; i < parent.getChildren().size(); i++){
          if(parent.getChildren(i) == node){
            childIndex = i;
            break;
          }
          priorNPs.add(getKey(parent.getChildren(i)));
        }
           
        // cnoditions for this arg being an element of a list:
        // 1) is NP
        // 2) Parent is NP
        // 3) left neighbor is , or right neighbor is , or both neigbors are ,
        boolean lcComma=false, rcComma=false, lcAnd=false;
        if(node.getNodeType().equals("NP") && parent.getNodeType().equals("NP")){
          if(childIndex > 0 && parent.getChildren(childIndex-1).getNodeType().equals(",")){
            // left child is ","
            lcComma = true;
          }
          if(childIndex+1 < parent.getChildren().size() && parent.getChildren(childIndex+1).getNodeType().equals(",")){
            rcComma = true;
          }
          if(childIndex+1 == parent.getChildren().size() && childIndex > 0 && parent.getChildren(childIndex-1).getNodeType().equals("CC")){
            lcAnd = true;
          }
        }
        if(lcComma && rcComma){
          feats.add(new Feature(prefix + "_midlist", true));
        }else if(childIndex==0 && rcComma){
          feats.add(new Feature(prefix + "_startlist", true));
        }else if(lcAnd){
          feats.add(new Feature(prefix + "_endlist", true));
        }
        
        if(lcComma || rcComma || lcAnd){
          // somehow in a list
          // check to see if any element of the list is already part of a relation
          for(BinaryTextRelation otherRel : JCasUtil.select(jCas, BinaryTextRelation.class)){
            Annotation a1 = otherRel.getArg1().getArgument();
            Annotation a2 = otherRel.getArg2().getArgument();
            if(a1 instanceof TimeMention || a2 instanceof TimeMention) continue; // covered by another feature
            if(priorNPs.contains(getKey(a1))){
              // one of the left children is already in another relation!
              feats.add(new Feature(prefix + "_leftSiblingInRelation", true));
              
              // check if the other argument in that relation is the secondary arg
              if(secondArg.getBegin() == a2.getBegin() && secondArg.getEnd() == a2.getEnd()){
                // the other proposed arg of this relation is already in a relation with another element of this list!
                feats.add(new Feature(prefix + "_leftSiblingInRelationWithCurArg"));
              }
            }
            
            if(priorNPs.contains(getKey(a2))){
              feats.add(new Feature(prefix + "_leftSiblingInRelation", true));
              
              if(secondArg.getBegin() == a1.getBegin() && secondArg.getEnd() == a1.getEnd()){
                // the other proposed arg of this relation is already in a relation with another element of this list!
                feats.add(new Feature(prefix + "_leftSiblingInRelationWithCurArg"));
              }
            }
          }
        }
      }
      
      
    }
    
    return feats;
  }

  private static String getKey(Annotation annot){
    return annot.getBegin() + "-" + annot.getEnd();
  }
}
