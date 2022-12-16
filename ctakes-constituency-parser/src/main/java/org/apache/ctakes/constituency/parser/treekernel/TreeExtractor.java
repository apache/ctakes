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
package org.apache.ctakes.constituency.parser.treekernel;

import java.util.ArrayList;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.utils.tree.SimpleTree;

/*
 * This class extracts tree relations between two nodes in a tree (or from 2 separate trees).
 * This has been used to great effect in relation extraction with the use of tree kernels.
 * Two are implemented here, path trees used in coreference (a tree whose nodes are simply the nodes in the
 * path between the anaphor and antecedent) and the Path-enclosed Tree from Moschitti 2004 (acl) on SRL and
 * Zhang, Zhang, and Su 2006 (Naacl-hlt) for relex, which in that paper got the best performance, though
 * they also implemented several other tree types which might be worth pursuign for other
 * tasks. 
 */
public class TreeExtractor {

//	public static TopTreebankNode extractPathTree(TreebankNode t1, TreebankNode t2, JCas jcas){
//		TopTreebankNode node = new TopTreebankNode(jcas);
//
//		// edge cases that don't really make sense....
//		// 1) Same tree
//		// 2) overlapping trees
//		if(t1 == t2 || (t1.getBegin() >= t2.getBegin() && t1.getEnd() <= t2.getEnd()) || (t2.getBegin() >= t1.getBegin() && t2.getEnd() <= t1.getEnd())){
//			return sameTree(t1, t2, jcas);
//		}
//		TreebankNode lca = getLCA(t1, t2);
//
//		node.setNodeType(lca == null ? "TOP" : lca.getNodeType());
//		FSArray children = new FSArray(jcas,2);
//		node.setChildren(children);
//
//		ArrayList<TreebankNode> antePath = getAnaphoraPath(lca, t1);
//		TreebankNode parent = node;
//		for(TreebankNode child : antePath){
//			TreebankNode newChild = new TreebankNode(jcas);
//			newChild.setNodeType(child.getNodeType());
//			if(parent != node){
//				FSArray tempChildren = new FSArray(jcas, 1);
//				parent.setChildren(tempChildren);
//			}
//			parent.setChildren(0,newChild);
//			parent = newChild;	
//		}
//		TerminalTreebankNode fakeWord = new TerminalTreebankNode(jcas);
//		fakeWord.setNodeType("antecedent");
//		fakeWord.setLeaf(true);
//		fakeWord.setChildren(new FSArray(jcas, 0));
//		if(parent != node){
//			FSArray termChildren = new FSArray(jcas, 1);
//			parent.setChildren(termChildren);
//		}
//		parent.setChildren(0, fakeWord);
//
//		parent = node;
//		ArrayList<TreebankNode>	anaphPath = getAnaphoraPath(lca, t2);
//		for(TreebankNode child : anaphPath){
//			TreebankNode newChild = new TreebankNode(jcas);
//			newChild.setNodeType(child.getNodeType());
//			if(parent != node){
//				FSArray tempChildren = new FSArray(jcas, 1);
//				parent.setChildren(tempChildren);
//				parent.setChildren(0,newChild);
//			}else{
//				parent.setChildren(1,newChild);
//			}
//			parent = newChild;
//		}
//		fakeWord = new TerminalTreebankNode(jcas);
//		fakeWord.setNodeType("anaphor");
//		fakeWord.setLeaf(true);
//		fakeWord.setChildren(new FSArray(jcas, 0));
//		if(parent != node){
//			FSArray termChildren = new FSArray(jcas, 1);
//			parent.setChildren(termChildren);
//			parent.setChildren(0, fakeWord);
//		}else{
//			parent.setChildren(1, fakeWord);
//		}
//		return node;
//	}

	public static SimpleTree extractPathTree(TreebankNode t1, TreebankNode t2){
		// swap the ordering if necessary
		if(t1.getBegin() > t2.getBegin()){
			TreebankNode temp = t2;
			t2 = t1;
			t1 = temp;
		}

		// edge cases that don't really make sense....
		// 1) Same tree
		// 2) overlapping trees
		if(t1 == t2 || (t1.getBegin() >= t2.getBegin() && t1.getEnd() <= t2.getEnd()) || (t2.getBegin() >= t1.getBegin() && t2.getEnd() <= t1.getEnd())){
			return sameTree(t1, t2);
		}
		
		SimpleTree node = null;
		TreebankNode lca = getLCA(t1, t2);
		if(lca == null) node = new SimpleTree("TOP");
		else node = new SimpleTree(lca.getNodeType());
		
		ArrayList<TreebankNode> antePath = getUpwardPath(lca, t1);
		SimpleTree parent = node;
		for(TreebankNode child : antePath){
			SimpleTree newChild = new SimpleTree(child.getNodeType());
			parent.addChild(newChild);
			parent = newChild;		
		}
		parent.addChild(new SimpleTree("arg1"));
		
		ArrayList<TreebankNode> anaPath = getUpwardPath(lca, t2);
		parent = node;
		for(TreebankNode child : anaPath){
			SimpleTree newChild = new SimpleTree(child.getNodeType());
			parent.addChild(newChild);
			parent = newChild;
		}
		parent.addChild(new SimpleTree("arg2"));
		
		return node;
	}

	public static SimpleTree extractPathEnclosedTree(TreebankNode t1, TreebankNode t2, JCas jcas){
		SimpleTree node = null;
		// swap them if wrong order:
		if(t1.getBegin() > t2.getBegin()){
			TreebankNode temp = t1;
			t1 = t2;
			t2 = temp;
		}
		if(t1 == t2 || (t1.getBegin() >= t2.getBegin() && t1.getEnd() <= t2.getEnd()) || (t2.getBegin() >= t1.getBegin() && t2.getEnd() <= t1.getEnd())){
			node = sameTree(t1,t2);
		}else{
			TreebankNode lca = getLCA(t1,t2);
			ArrayList<TreebankNode> l1 = getUpwardPath(lca, t1);
			ArrayList<TreebankNode> l2 = getUpwardPath(lca, t2);
			if(lca == null){
				lca = new TopTreebankNode(jcas);
				lca.setNodeType("TOP");
				lca.setChildren(new FSArray(jcas,2));
				if(l1.size()==0){
					l1.add(t1);
				}
				if(l2.size() == 0){
					l2.add(t2);
				}
				lca.setChildren(0, l1.get(0));
				lca.setChildren(1, l2.get(0));
			}
			node = buildSimpleClonePET(lca, t1, t2);
		}
		return node;
	}

	private static SimpleTree buildSimpleClonePET(TreebankNode lca, TreebankNode t1, TreebankNode t2){
		SimpleTree t = new SimpleTree(lca.getNodeType());
		if(lca instanceof TerminalTreebankNode){
			t.addChild(new SimpleTree(lca.getNodeValue()));
		}else{
			for(int i = 0; i < lca.getChildren().size(); i++){
				TreebankNode tn = lca.getChildren(i);
				if(tn.getEnd() > t1.getBegin() && tn.getBegin() < t2.getEnd()){
					t.addChild(buildSimpleClonePET(lca.getChildren(i), t1, t2));
				}
			}
		}
		return t;
	}
	
	// Find the least common ancestor of two other nodes, or null (top node) if they are in different sentences
	public static TreebankNode getLCA(TreebankNode t1, TreebankNode t2){
		TreebankNode temp = null;
		if(t2.getBegin() < t1.getBegin()){
			temp = t1;
			t1 = t2;
			t2 = temp;
		}
		
		TreebankNode lca = t2;
		while(lca != null && (lca.getBegin() > t1.getBegin() || lca.getEnd() < t1.getEnd())){
			lca = lca.getParent();
		}
		return lca;
	}

//	private static ArrayList<TreebankNode> getAnaphoraPath(TreebankNode lca, TreebankNode t){
//		return getUpwardPath(lca,t);
//	}
//
//	private static ArrayList<TreebankNode> getAntecedentPath(TreebankNode lca, TreebankNode t){
//		return getUpwardPath(lca,t);
//	}

	private static ArrayList<TreebankNode> getUpwardPath(TreebankNode lca, TreebankNode t){
		ArrayList<TreebankNode> list = new ArrayList<TreebankNode>();
		while(t != null && t != lca && t.getParent() != null){
			list.add(0,t);
			t = t.getParent();
		}
		return list;	

	}

//	private static TopTreebankNode sameTree(TreebankNode t1, TreebankNode t2, JCas jcas){
//		TopTreebankNode node = new TopTreebankNode(jcas);
//		node.setNodeType(t1.getNodeType());
//		node.setChildren(new FSArray(jcas,2));
//		TreebankNode pt1 = new TreebankNode(jcas);
//		pt1.setNodeType("ANTECEDENT");
//		pt1.setLeaf(false);
//		pt1.setChildren(new FSArray(jcas,1));
//		TerminalTreebankNode c1 = new TerminalTreebankNode(jcas);
//		c1.setNodeType("antecedent");
//		c1.setLeaf(true);
//		c1.setChildren(new FSArray(jcas,0));
//		pt1.setChildren(0, c1);
//		node.setChildren(0,pt1);
//
//		TreebankNode pt2 = new TreebankNode(jcas);
//		pt2.setNodeType("ANAPHOR");
//		pt2.setLeaf(false);
//		pt2.setChildren(new FSArray(jcas,1));
//		TerminalTreebankNode c2 = new TerminalTreebankNode(jcas);
//		c2.setNodeType("anaphor");
//		c2.setLeaf(true);
//		c2.setChildren(new FSArray(jcas,0));
//		pt2.setChildren(0,c2);
//		node.setChildren(1,pt2);
//		return node;
//	}
	
	private static SimpleTree sameTree(TreebankNode t1, TreebankNode t2){
		SimpleTree node = new SimpleTree(t1.getNodeType());
		node.addChild(new SimpleTree("ANTECEDENT"));
		node.children.get(0).addChild(new SimpleTree("antecedent"));
		node.addChild(new SimpleTree("ANAPHOR"));
		node.children.get(1).addChild(new SimpleTree("anaphor"));
		return node;
	}

	/* This method is used to extract trees for finding _properties_ rather than relations, or for
	 * finding relations where only one argument is known (and a label), and the other argument
	 * will be learned as an important tree fragment (e.g., relations like negation or uncertainty).
	 * The object returned is the largest subtree surrounding the context, with an extra node above it
	 * labeled with the argument in the variable string.
	 * For example, given the arguments:
	 * (NP (DT a) (NN dog))) from a tree representing the sentence "That is not a dog" 
	 * and the string "NEGATION"; this method will return the tree:
	 * (S (NP (DT that)) (VP (VBZ is) (RB not) (NEGATION (NP (DT a) (NN dog)))))
	 * 
	 * It uses the method getSurroundingTree in a somewhat clever way to do the additional annotation
	 * on the output string instead of the tree object.
	 */
	public static SimpleTree getSurroundingTreeWithAnnotation(TreebankNode node, String string) {
		SimpleTree inner = getSimpleClone(node);
		SimpleTree outer = getSurroundingTree(node);
		
		String innerString = inner.toString();
		String outerString = outer.toString();
		
		String fullString = outerString.replace(innerString, "(" + string + " " + innerString + ")");
		return SimpleTree.fromString(fullString);		
	}

	public static SimpleTree getSurroundingTree(TreebankNode node){
		SimpleTree tree = null;
		while(node.getParent() != null){
			node = node.getParent();
		}
		tree = getSimpleClone(node);
		return tree;
	}

	public static SimpleTree getSimpleClone(TreebankNode node) {
		SimpleTree t = new SimpleTree(node.getNodeType());
		if(node instanceof TerminalTreebankNode){
			t.addChild(new SimpleTree(node.getNodeValue()));
		}else{
			for(int i = 0; i < node.getChildren().size(); i++){
				t.addChild(getSimpleClone(node.getChildren(i)));
			}
		}
		return t;
	}
	
	public static void lowercaseWords(SimpleTree t){
		if(t.children.size() == 0){
			t.cat = t.cat.toLowerCase();
		}else{
			for(SimpleTree child : t.children){
				lowercaseWords(child);
			}
		}
	}
}
