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

import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class ParseSpanFeatureExtractor  {

  public List<Feature> extract(JCas jcas, int begin, int end)
      {
    List<Feature> feats = new ArrayList<Feature>();
    
    TreebankNode domNode = AnnotationTreeUtils.annotationNode(jcas, begin, end);
    if(domNode != null){
      feats.add(new Feature("DominatingTreeCat", domNode.getNodeType()));
      if(domNode.getNodeTags() != null){
        for(int ind = 0; ind < domNode.getNodeTags().size(); ind++){
          String tag = domNode.getNodeTags(ind);
          if(tag.equals("TMP")){
            feats.add(new Feature("DominatingTmpTag", tag));
          }
        }
      }
      TreebankNode parent = domNode.getParent();
      if(parent != null){
        feats.add(new Feature("DominatingTreeParent", parent.getNodeType()));
        do{
          if(parent.getNodeTags() != null){
            for(int ind = 0; ind < parent.getNodeTags().size(); ind++){
              String tag = parent.getNodeTags(ind);
//              if(tag.equals("TMP")){
                feats.add(new Feature("DominatingAncestorTmpTag", tag));
//              }
            }
          }
          parent = parent.getParent();
        }while(parent != null);
      }
      
      if(domNode.getLeaf()){
        feats.add(new Feature("DominatingIsLeaf"));
      }else{
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < domNode.getChildren().size(); i++){
          buffer.append(domNode.getChildren(i).getNodeType());
          buffer.append("_");
          feats.add(new Feature("DominatingChildBag" + domNode.getChildren(i).getNodeType()));
        }
        feats.add(new Feature("DominatingProduction", buffer.toString()));
      }
      if(domNode.getBegin() == begin && domNode.getEnd() == end){
        feats.add(new Feature("DominatingExactMatch"));
      }
    }
    return feats;
  }

}
