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
package org.apache.ctakes.constituency.parser.util;

import java.util.Collection;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.uima.fit.util.JCasUtil;


public class AnnotationTreeUtils {
	
	public static TopTreebankNode getAnnotationTree(JCas jcas, Annotation annot){
		TopTreebankNode tree = null;
		Collection<TopTreebankNode> roots = JCasUtil.select(jcas, TopTreebankNode.class);
		for(TopTreebankNode root : roots){
			if(root.getBegin() <= annot.getBegin() && root.getEnd() >= annot.getEnd()){
				tree = root;
				break;
			}
		}
		return tree;
	}

	public static TopTreebankNode getTreeCopy(JCas jcas, TopTreebankNode orig){
		if(orig == null) return null;
		TopTreebankNode copy = new TopTreebankNode(jcas);
		copy.setNodeType(orig.getNodeType());
		copy.setBegin(orig.getBegin());
		copy.setEnd(orig.getEnd());
		copy.setParent(null);
		copy.setChildren(new FSArray(jcas,1));
		copy.setTreebankParse(orig.getTreebankParse());
		if(orig.getChildren() == null || orig.getChildren().size() == 0){
			System.err.println("WHAT?");
		}
		copy.setChildren(0, getTreeCopy(jcas, orig.getChildren(0)));
		copy.getChildren(0).setParent(copy);
		return copy;
	}

	public static TreebankNode getTreeCopy(JCas jcas, TreebankNode orig){
		TreebankNode copy = null;
		if(orig instanceof TerminalTreebankNode){
			copy = new TerminalTreebankNode(jcas);
			copy.setLeaf(true);
			copy.setChildren(null);
		}else{
			copy = new TreebankNode(jcas);
			copy.setChildren(new FSArray(jcas, orig.getChildren().size()));
			for(int i = 0; i < orig.getChildren().size(); i++){
				copy.setChildren(i, getTreeCopy(jcas, orig.getChildren(i)));
				copy.getChildren(i).setParent(copy);
			}
		}
		copy.setNodeType(orig.getNodeType());
		copy.setNodeValue(orig.getNodeValue());
		copy.setBegin(orig.getBegin());
		copy.setEnd(orig.getEnd());
		return copy;
	}
	
	public static TreebankNode annotationNode(JCas jcas, Annotation annot){
		return annotationNode(jcas, annot.getBegin(), annot.getEnd());
	}
	
	public static TreebankNode annotationNode(JCas jcas, int a, int b){
		TreebankNode lowestDom = null;
		int overage = Integer.MAX_VALUE;
		FSIterator<Annotation> iter = jcas.getJFSIndexRepository().getAnnotationIndex(TreebankNode.type).iterator();
		while(iter.hasNext()){
			TreebankNode node = (TreebankNode) iter.next();
			if(node.getBegin() == a && node.getEnd() == b){
				// this code will drill down -- actually want to go other way
//				while(node.getChildren() != null && node.getChildren().size() == 1){
//					node = node.getChildren(0);
//				}
				
				// this code will head up as long as parent has the same span
				try{
					while(node.getParent() != null && node.getParent().getChildren().size() == 1 && !node.getParent().getNodeType().equals("TOP")){
						node = node.getParent();
					}
				}catch(NullPointerException e){
					System.err.println("Null pointer exception in AttributeCalculator::markableNode()");
				}
				return node;
			}else if(node.getBegin() <= a && node.getEnd() >= b){
				int tempOver = (a-node.getBegin()) + (node.getEnd()-b);
				if(tempOver < overage){
					lowestDom = node;
					overage = tempOver;
				}
			}
		}
		// There are lots of reasons to get this far -- error in the parse, personal pronoun in an NP (not annotated so not a markable),
		// unrecognized NML structure, etc.
		// Some other work will add any such nodes to the parse tree as in haghighi klein 09 (simple synt sem ...)
		// In contrast, we don't add node to the parse tree, just find the lowest node dominating the markable range
		// TODO test this
		return lowestDom;
	}
	
	public static TerminalTreebankNode getHead(TreebankNode n){
		TerminalTreebankNode ttn = null;
		int headIndex = n.getHeadIndex();
		ttn = getRoot(n).getTerminals(headIndex);
		return ttn;
	}
	
	public static TopTreebankNode getRoot(TreebankNode n){
		TopTreebankNode ret = null;
		while(!(n instanceof TopTreebankNode)){
			n = n.getParent();
		}
		ret = (TopTreebankNode) n;
		return ret;
	}

	public static TreebankNode insertAnnotationNode(JCas jcas, TopTreebankNode root, Annotation arg1, String nodeType) {
		// tree did not match the arg exactly, so if possible we'll insert a node in the tree here that
		// is under tree but above its children.  So we'll try to find the start and end child that this
		// arg covers if possible.
		TreebankNode tree = root;
		TreebankNode lastTree = null; //tree;
		do{
			lastTree = tree;
			// only continue downward traversal if we are not at a POS node...
//			if(tree.getChildren().size() > 1 || tree.getChildren(0).getChildren() != null){
			if(!tree.getLeaf()){
				for(int i = 0; i < tree.getChildren().size(); i++){
					TreebankNode child = tree.getChildren(i);
					if(child.getBegin() <= arg1.getBegin() && child.getEnd() >= arg1.getEnd()){
						tree = child;
						break;  // break out of inner for-loop
					}
				}
			}
		}while(tree != lastTree);

		TreebankNode newTree = null;
		if(tree.getBegin() == arg1.getBegin() && tree.getEnd() == arg1.getEnd()){
			while(tree.getParent() != null && tree.getParent().getBegin() == arg1.getBegin() && tree.getParent().getEnd() == arg1.getEnd()){
				tree = tree.getParent();
			}
			// matches a node in tree, just insert one above it
			newTree = new TreebankNode(jcas, tree.getBegin(), tree.getEnd());
			newTree.setNodeType(nodeType);
			newTree.setChildren(new FSArray(jcas, 1));
			newTree.setChildren(0, tree);
			newTree.setParent(tree.getParent());
			TreeUtils.replaceChild(tree.getParent(), tree, newTree);
			tree.setParent(newTree);
//			newTree.setNodeType(tree.getNodeType());
//			newTree.setChildren(tree.getChildren());
//			newTree.setParent(tree);
//			tree.setNodeType(nodeType);
//			tree.setChildren(new FSArray(jcas, 1));
//			tree.setChildren(0,newTree);
//			newTree = tree;
		}else{
			// mismatch

			int startChild = -1;
			int endChild = -1;
			
			if(!tree.getLeaf()){
				// it can happen that the tree here is a terminal (pos tag:word) and thus has no children, in the case that the gold
				// standard entities are tokenized correctly and the tokenizer is wrong. With automatic tokens and entities this shouldn't happen.
				for(int i = 0; i < tree.getChildren().size(); i++){
					if(startChild == -1){
						if(tree.getChildren(i).getBegin() == arg1.getBegin()){
							startChild = i;
						}
					}else if(tree.getChildren(i).getEnd() == arg1.getEnd()){
						endChild = i;
						break;
					}
				}
			}
			
			// here is where we insert if possible
			if(startChild >= 0 && endChild >= 0){
				newTree = new TreebankNode(jcas, tree.getChildren(startChild).getBegin(), tree.getChildren(endChild).getEnd());
				newTree.setNodeType(nodeType);
				newTree.setParent(tree);
				int numStolenChildren = endChild-startChild+1;
				newTree.setChildren(new FSArray(jcas, numStolenChildren));
				// add new children to new intermediate node
				for(int i = startChild; i <= endChild; i++){
					newTree.setChildren(i-startChild, tree.getChildren(i));
				}
				// create new children array for top node (tree)
				FSArray children = new FSArray(jcas, tree.getChildren().size() - numStolenChildren + 1);
				for(int i = 0; i < startChild; i++){
					children.set(i, tree.getChildren(i));
				}
				children.set(startChild, newTree);
				for(int i = endChild+1; i < tree.getChildren().size(); i++){
					children.set(i-numStolenChildren+1, tree.getChildren(i));
				}
				tree.setChildren(children);
			}else{
				// just put above here...
				newTree = new TreebankNode(jcas, tree.getBegin(), tree.getEnd());
				newTree.setNodeType(nodeType);
				newTree.setChildren(new FSArray(jcas, 1));
				newTree.setChildren(0, tree);
				newTree.setParent(tree.getParent());
				TreeUtils.replaceChild(tree.getParent(), tree, newTree);
				tree.setParent(newTree);
//				newTree.setNodeType(tree.getNodeType());
//				newTree.setChildren(tree.getChildren());
//				newTree.setParent(tree);
//				tree.setNodeType(nodeType);
//				tree.setChildren(new FSArray(jcas, 1));
//				tree.setChildren(0,newTree);
//				newTree = tree;
			}
		}
		return newTree;
	}

	public static void removeRightOfAnnotation(JCas jcas, TreebankNode node, Annotation annot) {
		// if the whole tree is to the left of the annotation then do nothing:
		if(node.getEnd() <= annot.getBegin() || node.getLeaf()) return;

		// if there is some overlap then iterate over trees, ignoring those to the left, recursing on those that overlap, and deleting those to the right
		for(int i = 0; i < node.getChildren().size(); i++){
			TreebankNode child = node.getChildren(i);
			if(child.getEnd() <= annot.getBegin()){
				// child is to the left of annotation completely
				continue;
			}else if(child.getBegin() > annot.getEnd()){
				// child is to the right of annotation completely -- remove it and all to the right
				FSArray newChildren = new FSArray(jcas, i);
				for(int j = 0; j < i; j++){
					newChildren.set(j, node.getChildren(j));
				}
				node.setChildren(newChildren);
				break;
			}else{
				removeRightOfAnnotation(jcas, child, annot);
			}
		}
	}

	public static void removeLeftOfAnnotation(JCas jcas, TreebankNode node, Annotation annot) {
		if(node.getEnd() <= annot.getBegin() || node.getLeaf()) return;

		// go through tree and create a list of children that are overalpping or to the right of the concept node:
		for(int i = 0; i < node.getChildren().size(); i++){
			TreebankNode child = node.getChildren(i);
			if(child.getEnd() < annot.getBegin()){
				// ignore for now but this will be removed later
				continue;
			}else if(child.getEnd() > annot.getBegin()){
				// if it has substructure to the left of the concept we have to recurse
				if(child.getBegin() < annot.getBegin()){
					removeLeftOfAnnotation(jcas, child, annot);
				}
				
				if(i > 0){
					// if we're leaving some out we need to rebuild the whole children array
					// now create a child array of children partially or completely to the right
					FSArray newChildren = new FSArray(jcas, node.getChildren().size()-i);
					for(int j = i; j < node.getChildren().size(); j++){
						newChildren.set(j-i, node.getChildren(j));
					}
					node.setChildren(newChildren);
				}
				break;
			}
		}
	}

	public static TreebankNode getCommonAncestor(TreebankNode node1,
			TreebankNode node2) {
		// check for easy cases:
		// 1 - an argument is null
		if(node1 == null || node2 == null){
			return null;
		}
		
		// 1 - one completely dominates the other...
		if(dominates(node1, node2)){
			return node1;
		}else if(dominates(node2, node1)){
			return node2;
		}
		
		// they were entered in the wrong order...
		TreebankNode temp;
		if(node1.getBegin() > node2.getBegin()){
			temp = node1;
			node1 = node2;
			node2 = temp;
		}
		
		TreebankNode ancestor = node2;
		
		while(true){
			if(ancestor == null || ancestor.getBegin() <= node1.getBegin()){
				break;
			}
			ancestor = ancestor.getParent();
		}
		
		return ancestor;
	}
	
	public static final boolean dominates(TreebankNode node1, TreebankNode node2){
		return(node1.getBegin() <= node2.getBegin() && node1.getEnd() >= node2.getEnd());
	}
}
