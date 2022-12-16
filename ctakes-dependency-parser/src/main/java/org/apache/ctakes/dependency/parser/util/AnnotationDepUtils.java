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
package org.apache.ctakes.dependency.parser.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class AnnotationDepUtils {
  private static Logger logger = Logger.getLogger(AnnotationDepUtils.class);

  public static String getTokenRelTreeString(JCas jCas, List<ConllDependencyNode> nodes, Annotation[] annotations, String[] labels){
    return getTokenRelTreeString(jCas, nodes, annotations, labels, false);
  }
  
  public static String getTokenRelTreeString(JCas jCas, List<ConllDependencyNode> nodes, Annotation[] annotations, String[] labels, boolean getParent){
    Map<ConllDependencyNode, SimpleTree> node2tree = new HashMap<ConllDependencyNode, SimpleTree>();
    ConllDependencyNode topNode = null;
    
    // create a SimpleTree object that corresponds to this dependency tree, where the
    // root is the head of the sentence and the children are all the words such that the parent
    // is their head. In this case every word is represented by its relationship as well as
    // its word
    for(ConllDependencyNode node : nodes){
      if(node.getHead() == null){
        topNode = node;
        continue;
        // do absolutely nothing with this -- it covers the whole sentence and has no useful info
//        continue;
//      }else if(node.getHead().getHead() == null){
//        topNode = node;
      }
           
      SimpleTree curTree = null;
      SimpleTree headTree = null;
      if(!node2tree.containsKey(node)){
        curTree = SimpleTree.fromString(String.format("(%s %s)", node.getDeprel(), node.getCoveredText()));
        node2tree.put(node, curTree);
      }else{
        curTree = node2tree.get(node);
      }


      if(curTree.parent == null && node.getHead() != null){
        if(node2tree.containsKey(node.getHead())){
          headTree = node2tree.get(node.getHead());
        }else{
          String token = node.getHead().getHead() == null ? "TOP" : node.getHead().getCoveredText();
          headTree = SimpleTree.fromString(String.format("(%s %s)", node.getHead().getDeprel(), SimpleTree.escapeCat(token)));
          node2tree.put(node.getHead(), headTree);
        }

        curTree.parent = headTree.children.get(0);
        headTree.children.get(0).addChild(curTree);
      }
    }
    
    ConllDependencyNode highestHead = null;
    ConllDependencyNode leftmostHead = null;
    ConllDependencyNode rightmostHead = null;
    List<SimpleTree> annotationNodes = Lists.newArrayList();
    
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
      if(leftmostHead == null || headNode.getBegin() < leftmostHead.getBegin()){
        leftmostHead = headNode;
      }
      if(rightmostHead == null || headNode.getEnd() > rightmostHead.getEnd()){
        rightmostHead = headNode;
      }
      
      SimpleTree insertionPoint = node2tree.get(headNode);
      SimpleTree insertingTree = new SimpleTree(insertionPoint.cat);
      insertionPoint.cat = labels[i];
      insertingTree.children = insertionPoint.children;
      insertingTree.children.get(0).parent = insertingTree;
      insertionPoint.children = new ArrayList<SimpleTree>();
      insertionPoint.addChild(insertingTree);
      insertingTree.parent = insertionPoint;
      annotationNodes.add(insertionPoint);
    }
    if(highestHead == null) return null;
    
    SimpleTree root = node2tree.get(topNode);
    SimpleTree leftmostNode = getLeftmostNode(root, Sets.newHashSet(labels));
    SimpleTree rightmostNode = getRightmostNode(root, Sets.newHashSet(labels));
    SimpleTree pet = getPathEnclosedTree(root, annotationNodes, leftmostNode, rightmostNode);
    if(getParent && pet.parent != null) pet = pet.parent;
    
    String treeStr = pet.toString();
    treeStr = treeStr.replaceAll("\\(([^\\(]+) \\)", "($1 nil)").toLowerCase();
    
    return treeStr;
  }
  
  private static SimpleTree getRightmostNode(SimpleTree root, Set<String> labels) {
    SimpleTree node = null;
    
    for(int i = root.children.size()-1; i >= 0; i--){
      if(labels.contains(root.children.get(i).cat)){
        node = root.children.get(i);
        break;
      }
      node = getRightmostNode(root.children.get(i), labels);
      if(node != null) break;
    }
    
    return node;
  }

  private static SimpleTree getLeftmostNode(SimpleTree root, Set<String> labels) {
    SimpleTree node = null;
    
    for(int i = 0; i < root.children.size(); i++){
      if(labels.contains(root.children.get(i).cat)){
        node = root.children.get(i);
        break;
      }
      node = getLeftmostNode(root.children.get(i), labels);
      if(node != null) break;
    }
    
    return node;
  }
  
  public static String getTokenTreeString(JCas jCas, List<ConllDependencyNode> nodes, Annotation[] annotations, String[] labels, boolean getParent){
    Map<ConllDependencyNode, SimpleTree> node2tree = getNodeTreeMap(nodes);
    ConllDependencyNode topNode = getTopNode(nodes);
    
    ConllDependencyNode highestHead = null;
    ConllDependencyNode leftmostHead = null;
    ConllDependencyNode rightmostHead = null;
    List<SimpleTree> annotationNodes = Lists.newArrayList();
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
      if(leftmostHead == null || headNode.getBegin() < leftmostHead.getBegin()){
        leftmostHead = headNode;
      }
      if(rightmostHead == null || headNode.getEnd() > rightmostHead.getEnd()){
        rightmostHead = headNode;
      }
      
      SimpleTree insertionPoint = node2tree.get(headNode);
      SimpleTree insertingTree = new SimpleTree(insertionPoint.cat);
      insertionPoint.cat = labels[i];
      insertingTree.children = insertionPoint.children;
      insertingTree.children.get(0).parent = insertingTree;
      insertionPoint.children = new ArrayList<SimpleTree>();
      insertionPoint.addChild(insertingTree);
      insertingTree.parent = insertionPoint;
      annotationNodes.add(insertionPoint);
    }
    if(highestHead == null) return null;
    
    SimpleTree root = node2tree.get(topNode);
    SimpleTree leftmostNode = getLeftmostNode(root, Sets.newHashSet(labels));
    SimpleTree rightmostNode = getRightmostNode(root, Sets.newHashSet(labels));
    SimpleTree pet = getPathEnclosedTree(root, annotationNodes, leftmostNode, rightmostNode);
    if(getParent && pet.parent != null) pet = pet.parent;
    
    String treeStr = pet.toString();
    treeStr = treeStr.replaceAll("\\(([^\\(]+) \\)", "($1 nil)").toLowerCase();
    
    return treeStr;

  }
  
  public static SimpleTree getPathEnclosedTree(SimpleTree root, List<SimpleTree> annotationNodes, SimpleTree leftmostTree, SimpleTree rightmostTree){
    SimpleTree pet = null;
    // for the general case (>= 1 annotations) we need to first find the common set of ancestors
    Set<SimpleTree> commonAncestors = getAncestors(annotationNodes.get(0));
    for(int i = 1; i < annotationNodes.size(); i++){
      Set<SimpleTree> nodeAncestors = getAncestors(annotationNodes.get(i));
      commonAncestors = Sets.intersection(commonAncestors, nodeAncestors);
    }
    // of the common set, which is the lowest?
    SimpleTree lowestAncestor = null;
    for(SimpleTree ancestor : commonAncestors){
      if(lowestAncestor == null || distanceFromRoot(ancestor) > distanceFromRoot(lowestAncestor)){
        lowestAncestor = ancestor;
      }
    }
    // of the children of the lowest ancestors, which do not contain any of the annotations we are
    // interested in?
    root = lowestAncestor;
    SimpleTree curNode = leftmostTree;
    SimpleTree lastNode = null;
    while(curNode != root){
      lastNode = curNode;
      if(curNode == null || curNode.parent == null){
        logger.error("Something weird.");
      }
      curNode = curNode.parent;
      if(curNode == null || curNode.children == null){
        logger.error("Something is null on the left side of the tree in PET!");
        break;
      }
      while(curNode.children.get(0) != lastNode){
        curNode.children.remove(0);
      }
    }
    
    curNode = rightmostTree;
    lastNode = null;
    while(curNode != root){
      lastNode = curNode;
      curNode = curNode.parent;
      if(curNode == null){
        logger.error("Something is null on the right side of the tree in PET!");
        break;
      }
      if(curNode.children == null || curNode.children.size() == 0){
        System.err.println("Help");
      }
      while(curNode.children.get(curNode.children.size()-1) != lastNode){
        curNode.children.remove(curNode.children.size()-1);
        if(curNode.children == null || curNode.children.size() == 0){
          System.err.println("Help");
        }
      }
    }
    pet = lowestAncestor;
    return pet;
  }
  
  private static ConllDependencyNode getTopNode(List<ConllDependencyNode> nodes){
    ConllDependencyNode topNode = null;
    for(ConllDependencyNode node : nodes){
      if(node.getHead() == null){
        topNode = node;
        break;
      }
    }
    
    return topNode;
  }
  
  private static Map<ConllDependencyNode, SimpleTree> getNodeTreeMap(List<ConllDependencyNode> nodes){
    Map<ConllDependencyNode, SimpleTree> node2tree = Maps.newHashMap();
    for(ConllDependencyNode node : nodes){
      if(node.getHead() == null){
//        topNode = node;
        continue;
        // do absolutely nothing with this -- it covers the whole sentence and has no useful info
//        continue;
//      }else if(node.getHead().getHead() == null){
//        topNode = node;
      }
           
      SimpleTree curTree = null;
      SimpleTree headTree = null;
      if(!node2tree.containsKey(node)){
        curTree = SimpleTree.fromString(String.format("(%s %s)", node.getDeprel(), node.getCoveredText()));
        node2tree.put(node, curTree);
      }else{
        curTree = node2tree.get(node);
      }


      if(curTree.parent == null && node.getHead() != null){
        if(node2tree.containsKey(node.getHead())){
          headTree = node2tree.get(node.getHead());
        }else{
          String token = node.getHead().getHead() == null ? "TOP" : node.getHead().getCoveredText();
          headTree = SimpleTree.fromString(String.format("(%s %s)", node.getHead().getDeprel(), SimpleTree.escapeCat(token)));
          node2tree.put(node.getHead(), headTree);
        }

        curTree.parent = headTree.children.get(0);
        headTree.children.get(0).addChild(curTree);
      }
    }
    return node2tree;
  }
  
  private static Set<SimpleTree> getAncestors(SimpleTree tree){
    Set<SimpleTree> ancestors = Sets.newHashSet();
    while(tree != null){
      ancestors.add(tree);
      tree = tree.parent;
    }
    return ancestors;
  }
  
  public static int distanceFromRoot(ConllDependencyNode node){
    int dist = 0;
    while(node.getHead() != null){
      dist++;
      node = node.getHead();
    }
    return dist;
  }
  
  public static int distanceFromRoot(SimpleTree tree){
    int dist = 0;
    while(tree.parent != null){
      dist++;
      tree = tree.parent;
    }
    return dist;
  }
}
