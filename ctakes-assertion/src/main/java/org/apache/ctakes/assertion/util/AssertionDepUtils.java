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
package org.apache.ctakes.assertion.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.JCasUtil;

public class AssertionDepUtils {

  public static SimpleTree getTokenTreeString(JCas jCas, List<ConllDependencyNode> nodes, Annotation annotation){
    return getTokenTreeString(jCas, nodes, annotation, 1);
  }
  
  public static SimpleTree getTokenTreeString(JCas jCas, List<ConllDependencyNode> nodes, Annotation annotation, int upNodes){
    Map<ConllDependencyNode, SimpleTree> node2tree = new HashMap<ConllDependencyNode, SimpleTree>();
    for(ConllDependencyNode node : nodes){
      if(node.getHead() == null){
        // do absolutely nothing with this -- it covers the whole sentence and has no useful info
        continue;
      }
      
      SimpleTree curTree = null;
      SimpleTree headTree = null;
      if(!node2tree.containsKey(node)){
        curTree = new SimpleTree(node.getCoveredText().toLowerCase());
        node2tree.put(node, curTree);
      }else{
        curTree = node2tree.get(node);
      }
      
      
          
      if(curTree.parent == null && node.getHead().getHead() != null){
        if(node2tree.containsKey(node.getHead())){
          headTree = node2tree.get(node.getHead());
        }else{
          headTree = new SimpleTree(node.getHead().getCoveredText().toLowerCase());
          node2tree.put(node.getHead(), headTree);
        }

        curTree.parent = headTree;
        headTree.addChild(curTree);
      }
    }

    List<ConllDependencyNode> coveredNodes = JCasUtil.selectCovered(jCas, ConllDependencyNode.class, annotation);
    if(coveredNodes == null || coveredNodes.size() == 0) return null;
    ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(coveredNodes);
    SimpleTree localTree = node2tree.get(headNode.getHead().getHead() == null ? headNode : headNode.getHead());
    String realCat = node2tree.get(headNode).cat;
    // have to do this so that we have a placeholder so we can lowercase tokens, then insert the upper-case CONCEPT signifier token later.
    node2tree.get(headNode).cat = "CONCEPT";
    if(annotation instanceof IdentifiedAnnotation){
      node2tree.get(headNode).cat += ((IdentifiedAnnotation)annotation).getTypeID();
    }

//    String treeStr = localTree.toString();
//    treeStr = "(TOP " + treeStr.replaceAll("\\(([^\\(]+) \\)", "($1 nil)").toLowerCase().replace("conceptplaceholder", "CONCEPT") + ")";
//    treeStr = "(TOP " + treeStr.toLowerCase().replace("conceptplaceholder", "CONCEPT") + ")";
//    node2tree.get(headNode).cat = realCat;
//    return localTree;
    SimpleTree returnTree = null;
    
    int steps = 0;
    ConllDependencyNode returnNode = headNode;
    while(steps < upNodes && returnNode.getHead().getHead() != null){
      returnNode = returnNode.getHead();
      steps++;
    }
    returnTree = node2tree.get(returnNode);
    
    return returnTree;
  }
  
  public static String getTokenRelTreeString(JCas jCas, List<ConllDependencyNode> nodes, Annotation annotation, String label){
    return getTokenRelTreeString(jCas, nodes, new Annotation[]{annotation}, new String[]{label});
  }
  
  public static String getTokenRelTreeString(JCas jCas, List<ConllDependencyNode> nodes, Annotation[] annotations, String[] labels){
    Map<ConllDependencyNode, SimpleTree> node2tree = new HashMap<ConllDependencyNode, SimpleTree>();
    // create a SimpleTree object that corresponds to this dependency tree, where the
    // root is the head of the sentence and the children are all the words such that the parent
    // is their head. In this case every word is represented by its relationship as well as
    // its word
    for(ConllDependencyNode node : nodes){
      if(node.getHead() == null){
        // do absolutely nothing with this -- it covers the whole sentence and has no useful info
        continue;
      }
      
      SimpleTree curTree = null;
      SimpleTree headTree = null;
      if(!node2tree.containsKey(node)){
        curTree = SimpleTree.fromString(String.format("(%s %s)", node.getDeprel(), node.getCoveredText()));
        node2tree.put(node, curTree);
      }else{
        curTree = node2tree.get(node);
      }


      if(curTree.parent == null && node.getHead().getHead() != null){
        if(node2tree.containsKey(node.getHead())){
          headTree = node2tree.get(node.getHead());
        }else{
          headTree = SimpleTree.fromString(String.format("(%s %s)", node.getHead().getDeprel(), node.getHead().getCoveredText()));
          node2tree.put(node.getHead(), headTree);
        }

        curTree.parent = headTree.children.get(0);
        headTree.children.get(0).addChild(curTree);
      }
    }
    
    ConllDependencyNode highestHead = null;
    // take the set of input annotations and the corresponding labels and insert them into the SimpleTree
    for(int i = 0; i < annotations.length; i++){
      // get the node representing the head of this annotation
      List<ConllDependencyNode> coveredNodes = JCasUtil.selectCovered(jCas, ConllDependencyNode.class, annotations[i]);
      if(coveredNodes == null || coveredNodes.size() == 0) continue;
      ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(coveredNodes);
      
      // is this the highest node of all the annotations we're looking at?
      if(highestHead == null || (distanceFromRoot(headNode) < distanceFromRoot(highestHead))){
        highestHead = headNode;
      }
      
//      String realCat = node2tree.get(headNode).children.get(0).cat;
      //  have to do this so that we have a placeholder so we can lowercase tokens, then insert the upper-case CONCEPT signifier token later.
//      node2tree.get(headNode).children.get(0).cat = "conceptplaceholder";
      SimpleTree insertionPoint = node2tree.get(headNode);
      SimpleTree insertingTree = new SimpleTree(insertionPoint.cat);
      insertionPoint.cat = labels[i];
      insertingTree.children = insertionPoint.children;
      insertingTree.children.get(0).parent = insertingTree;
      insertionPoint.children = new ArrayList<SimpleTree>();
      insertionPoint.addChild(insertingTree);
      insertingTree.parent = insertionPoint;

//      node2tree.get(headNode).children.get(0).cat = realCat;
    }
    SimpleTree localTree = node2tree.get(highestHead.getHead().getHead() == null ? highestHead : highestHead.getHead());
    String treeStr = localTree.toString();
    treeStr = treeStr.replaceAll("\\(([^\\(]+) \\)", "($1 nil)").toLowerCase();
    
    return treeStr;

  }
  
  private static int distanceFromRoot(ConllDependencyNode node){
    int dist = 0;
    while(node.getHead() != null){
      dist++;
      node = node.getHead();
    }
    return dist;
  }
}
