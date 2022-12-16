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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.apache.ctakes.constituency.parser.treekernel.TreeExtractor;
import org.apache.ctakes.constituency.parser.util.AnnotationTreeUtils;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

public class AssertionTreeUtils {

  public static final SimpleTree NULL_TREE = SimpleTree.fromString("(S (TOK nullparse))");
  
  public static SimpleTree extractFeatureTree(JCas jcas, Annotation mention, SemanticClasses sems){
    SimpleTree tree = null;
    TopTreebankNode annotationTree = AnnotationTreeUtils.getAnnotationTree(jcas, mention);
    if(annotationTree != null){
      TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(jcas, annotationTree);
      AnnotationTreeUtils.insertAnnotationNode(jcas, root, mention, "CONCEPT");
      tree = TreeExtractor.getSimpleClone(root);
    }else{
      tree = NULL_TREE;
    }

    TreeExtractor.lowercaseWords(tree);
    if(sems != null){
      replaceWordsWithSemanticClasses(tree, sems);
    }
    
    return tree;    
  }
  
	public static SimpleTree extractAboveLeftConceptTree(JCas jcas, Annotation mention, SemanticClasses sems){
		SimpleTree tree = null;
		TopTreebankNode annotationTree = AnnotationTreeUtils.getAnnotationTree(jcas, mention);
		if(annotationTree != null){
			TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(jcas, annotationTree);
			TreebankNode conceptNode = AnnotationTreeUtils.insertAnnotationNode(jcas, root, mention, "CONCEPT");
			// navigate up the tree to retrieve the first "S" above this node.
			TreebankNode node = conceptNode;
			while(node.getParent() != null && !node.getNodeType().startsWith("S")){
				node =  node.getParent();
			}

//	    elevateListConcepts(jcas, node);
	    
			// remove nodes to the right of the CONCEPT node
			AnnotationTreeUtils.removeRightOfAnnotation(jcas, node, conceptNode);
			
			tree = TreeExtractor.getSimpleClone(node);
		}else{
			tree = NULL_TREE;
		}

		TreeExtractor.lowercaseWords(tree);
		if(sems != null){
			replaceWordsWithSemanticClasses(tree, sems);
		}
		
		return tree;
	}
	
	public static void elevateListConcepts(JCas jcas, TreebankNode tree) {
	  if(tree.getLeaf()) return;
	  
	  int conceptIndex = -1;
	  for(int i = 0; i < tree.getChildren().size(); i++){
	    if(tree.getChildren(i).getNodeType().equals("CONCEPT")){
	      conceptIndex = i;
	      break;
	    }
	  }
	  
	  if(conceptIndex == -1){
	    // explore children
//	    for(SimpleTree child : tree.children){
	    for(int i = 0; i < tree.getChildren().size(); i++){
	      elevateListConcepts(jcas, tree.getChildren(i));
	    }
	  }else{
	    // check 3 conditions:
	    // 1) First node under tree, with at least one node to the right, with category CC or ,
	    // 2) last node under tree, with at least one node to the left, with category CC or ,
	    // 3) node in the middle with node to the left category , and node to the right category CC or ,
	    if(conceptIndex == 0 && tree.getChildren().size() > 1 && tree.getChildren(1).getNodeType().matches("CC|,")
	        || conceptIndex == tree.getChildren().size()-1 && tree.getChildren().size() > 1 && tree.getChildren(conceptIndex-1).getNodeType().matches("CC|,") 
	        || conceptIndex > 0 && conceptIndex < tree.getChildren().size()-1 && tree.getChildren().size() > 2 && tree.getChildren(conceptIndex-1).getNodeType().equals(",") && tree.getChildren(conceptIndex+1).getNodeType().matches("CC|,")){
	      // if we meet this simple condition we raise the CONCEPT node!
	      // remove old concept node:
	      TreebankNode entityRoot = tree.getChildren(conceptIndex).getChildren(0);
	      tree.setChildren(conceptIndex, entityRoot);
	      entityRoot.setParent(tree);
	      
	      // insert new concept node:
//	      SimpleTree replacementNode = new SimpleTree(tree.cat);
	      TreebankNode replacementNode = new TreebankNode(jcas);
	      replacementNode.setNodeType(tree.getNodeType());
	      replacementNode.setChildren(tree.getChildren());
	      for(int i = 0; i < replacementNode.getChildren().size(); i++){
	        replacementNode.getChildren(i).setParent(replacementNode);
	      }
	      replacementNode.setParent(tree);
	      
	      tree.setNodeType("CONCEPT");
//	      tree.children = new ArrayList<SimpleTree>();
	      FSArray children = new FSArray(jcas, 1);
	      children.set(0, replacementNode);
	      tree.setChildren(children);
//	      tree.addChild(replacementNode);
	    }
	  }
  }

  public static SimpleTree extractAboveRightConceptTree(JCas jcas, Annotation mention, SemanticClasses sems){
		SimpleTree tree = null;
		TopTreebankNode annotationTree = AnnotationTreeUtils.getAnnotationTree(jcas, mention);
		if(annotationTree != null){
			TopTreebankNode root = AnnotationTreeUtils.getTreeCopy(jcas, annotationTree);
			TreebankNode conceptNode = AnnotationTreeUtils.insertAnnotationNode(jcas, root, mention, "CONCEPT");
			//						SimpleTree tree = null;
			//						tree = TreeExtractor.getSurroundingTreeWithAnnotation(node, "CONCEPT");
			// navigate up the tree to retrieve the first "S" above this node.
			TreebankNode node = conceptNode;
			while(node.getParent() != null && !node.getNodeType().startsWith("S")){
				node =  node.getParent();
			}

			// get the VP node (clause) or S that most closely dominates the concept, and remove everything after that
			// should smallen the tree while also permitting post-mention negation like "problem resolved" or "problem ruled out"
			
			// remove nodes to the right of the CONCEPT node
			AnnotationTreeUtils.removeLeftOfAnnotation(jcas, node, conceptNode);
		
			tree = TreeExtractor.getSimpleClone(node);
		}else{
			tree = SimpleTree.fromString("(S noparse)");
		}

		TreeExtractor.lowercaseWords(tree);
		if(sems != null){
			replaceWordsWithSemanticClasses(tree, sems);
		}
		return tree;
	}
	
	public static void replaceWordsWithSemanticClasses(SimpleTree tree, SemanticClasses sems){
		// recursion base case... actually apply semantic classes...
		if(tree.isLeaf()){
			for(Map.Entry<String,HashSet<String>> semClass : sems.entrySet()){
				if(semClass.getValue().contains(tree.cat)){
					tree.cat = "semclass_" + semClass.getKey();
				}
			}
		}else{
			// iterate over children
			for(SimpleTree child : tree.children){
				replaceWordsWithSemanticClasses(child, sems);
			}
		}
	}
	
  public static void replaceDependencyWordsWithSemanticClasses(SimpleTree tree,
      SemanticClasses sems) {
    
    // same but don't need to check for leaf node
    for(Map.Entry<String, HashSet<String>> semClass : sems.entrySet()){
      if(semClass.getValue().contains(tree.cat)){
        tree.cat = "semclass_" + semClass.getKey();
      }
    }
    
    // now iterate over children
    for(SimpleTree child : tree.children){
      replaceDependencyWordsWithSemanticClasses(child, sems);
    }
  }

	static HashMap<String,String> wordMap = new HashMap<String,String>();
    static Random random = new Random();
	public void randomizeWords(SimpleTree tree, boolean dep) {
		if(!tree.cat.equals("CONCEPT") && !tree.cat.equals("TOP") && (dep || tree.children.size() == 0)){
			if(wordMap.containsKey(tree.cat)){
				tree.cat = wordMap.get(tree.cat);
			}else{
				// generate new random word... (from http://stackoverflow.com/a/4952066)
				String oldWord = tree.cat;
				char[] word = new char[random.nextInt(8)+3]; // words of length 3 through 10. (1 and 2 letter words are boring.)
				for(int j = 0; j < word.length; j++)
				{
					word[j] = (char)('a' + random.nextInt(26));
				}
				tree.cat = new String(word);
				wordMap.put(oldWord, tree.cat);
			}
		}
		if(tree.children.size() > 0){
			for(SimpleTree child : tree.children){
				randomizeWords(child, dep);
			}
		}
	}


}
