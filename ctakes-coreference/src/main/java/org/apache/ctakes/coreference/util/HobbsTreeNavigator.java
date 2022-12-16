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
package org.apache.ctakes.coreference.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.coreference.type.Markable;

public class HobbsTreeNavigator {

	public static LinkedList<TreebankNode> bfs(Queue<TreebankNode> q, TreebankNode X, TreebankNode node){
		// now traverse the queue until it is empty, adding NPs
		LinkedList<TreebankNode> list = new LinkedList<TreebankNode>();
		HashSet<TreebankNode> path = new HashSet<TreebankNode>();
		TreebankNode cur = node;
		if(cur != null){
			path.add(cur);
			while(cur != X){
				cur = cur.getParent();
				path.add(cur);
			}
		}
		while(!q.isEmpty()){
			cur = q.remove();
			if(cur.getNodeType().equals("NP") && !path.contains(cur)){
				list.add(cur);
				// I think:
				// if cur is on the path then short circuit.
			}
			for(int i = 0; i < cur.getChildren().size(); i++){
				TreebankNode n = cur.getChildren(i);
				if(n == node) break;
				// BEFORE: did this because terminal node was word.  Now terminal is POS tag, which can go here,
				// shouldn't need special case to avoid adding terminals...
//				if(!(n instanceof TerminalTreebankNode)){
//					q.add(n);
//				}
				q.add(n);
				if(path.contains(n)) break;
			}
		}
		return list;
	}
	
	// find the next "X" in Hobbs algorithm.  "Y" is the child of X on the path up to X that we took.
	// it is needed because the search is constrained to only the children of new X to the left of the
	// path from whence we came.
	public static TreebankNode nextX(TreebankNode curX){
		TreebankNode nextX = curX.getParent();
//		Y = curX;
		while(!(nextX.getNodeType().equals("NP") || nextX.getNodeType().equals("S"))){
//			Y = nextX;
			nextX = nextX.getParent();
			if(nextX == null) break;
		}
		return nextX;
	}
	
	public static Queue<TreebankNode> initializeQueue(TreebankNode X, TreebankNode Y){
		Queue<TreebankNode> q = new LinkedList<TreebankNode>();
		
		while(Y.getParent() != X){
			Y = Y.getParent();
		}
		
		for(int i = 0; i < X.getChildren().size(); i++){
			TreebankNode n = X.getChildren(i);
			q.add(n);
			if(n == Y) break;
		}
		return q;
	}
	
	/* Use hobbs algorithm to reorder the list of markables in terms of where they are in the tree.
	 * Throughout this method "X" is used to refer to the current position in the tree (as in the Hobbs paper).
	 * Y is used to refer to the previous X.  node is the node in the tree where the markable sits.
	 */
	public static LinkedList<Markable> sort(Markable m, LinkedList<Markable> lm, JCas jcas){
		LinkedList<Markable> ll = new LinkedList<Markable>();
		HashMap<TreebankNode, Markable> node2mark = new HashMap<TreebankNode,Markable>();
		HashSet<Markable> allMarks = new HashSet<Markable>();
		FSIterator<Annotation> iterator = jcas.getAnnotationIndex(TopTreebankNode.type).iterator();
		LinkedList<Annotation> sentList = FSIteratorToList.convert(iterator);
		int sentInd = 0;
		
		// initialize the mapping of nodes to markables, as well as the set of all markables so we know
		// how many of them we've found
		for(Annotation a : lm){
			if(a.getBegin() > m.getBegin()) break;
//			if(sentDist(jcas,m,(Markable)a) > 3) continue;
			TreebankNode n = MarkableTreeUtils.markableNode(jcas, a.getBegin(), a.getEnd());
			node2mark.put(n, (Markable)a);
			allMarks.add((Markable)a);
		}
		
		// find out what sentence we're at to make it easier to go backwards:
		for(int i = 0; i < sentList.size(); i++){
			Annotation a = sentList.get(i);
			if(m.getBegin() >= a.getBegin() && m.getEnd() <= a.getEnd()){
				sentInd = i;
				break;
			}
		}
		
		TreebankNode node = MarkableTreeUtils.markableNode(jcas, m.getBegin(), m.getEnd());
		TreebankNode Y = node;
		TreebankNode X = HobbsTreeNavigator.nextX(node); 
			
		if(X != null){
			// Step 3: Traverse left side of X
			// First add the children to the left to the queue
//			Queue<TreebankNode> q = new LinkedList<TreebankNode>();
			Queue<TreebankNode> q = HobbsTreeNavigator.initializeQueue(X, Y);
			LinkedList<TreebankNode> tempList = HobbsTreeNavigator.bfs(q, X, Y);
			// before adding to actual queue (ll), need to make sure each of these has an NP node between it and X
			for(TreebankNode cur : tempList){
				TreebankNode n = cur.getParent();
				while(n != null && n != X){
					if(n.getNodeType().equals("NP") || n.getNodeType().equals("S")){
						if(node2mark.containsKey(n)){
							ll.add(node2mark.get(n));
							allMarks.remove(node2mark.get(n));
							break;
						}
					}
					n = n.getParent();
				}
			}
			
			if(allMarks.size() == 0) return ll;
			
			while(true){
				// step 4: if we're at the top of the tree:
				if(X == null || X.getParent().getNodeType().equals("TOP")){

					while(allMarks.size() > 0){
						// keep going back sentence by sentence
						sentInd--;
						if(sentInd < 0) break;
						TopTreebankNode ttn = (TopTreebankNode) sentList.get(sentInd);
						q = new LinkedList<TreebankNode>();
						q.add(ttn);
						tempList = HobbsTreeNavigator.bfs(q, null, null);
						for(TreebankNode cur : tempList){
							if(node2mark.containsKey(cur)){
								ll.add(node2mark.get(cur));
								allMarks.remove(node2mark.get(cur));
							}
						}
					}
					break;
				}
				
				// step 5:
				// find next NP or S above X
				Y = X;
				X = HobbsTreeNavigator.nextX(Y);
				if(X == null) continue;
				// step 6 (simplified):
				// if NP, propose X
				if(X.getNodeType().equals("NP")){
					if(node2mark.containsKey(X)){
						ll.add(node2mark.get(X));
						allMarks.remove(node2mark.get(X));
					}
				}
				// step 7:
				// traverse branches below X to the left of Y
				q = HobbsTreeNavigator.initializeQueue(X, Y);
				tempList = HobbsTreeNavigator.bfs(q, X, Y);
				// here we add them all (no checking for NPs in the path)
				for(TreebankNode n : tempList){
					if(node2mark.containsKey(n)){
						ll.add(node2mark.get(n));
						allMarks.remove(node2mark.get(n));
					}
				}
				
				// step 8 is for cataphora--ignoring now.
				// step 9: Go to step 4: (end of loop)
			}
		}
		// TODO : add code to work from p-1 backwards 3 sentences and add all those which have not been removed from allMarks yet?
		/*
		for(int q = p-1; q >= 0; q--){
			Markable a = (Markable) lm.get(q);
			if(allMarks.contains(a) && sentDist(jcas, m, a) <= 3){
				ll.add(a);
			}
		}
		*/
		return ll;
		
	}
	
//	private static int sentDist (JCas jcas, Markable m1, Markable m2) {
//		PairAttributeCalculator ac = new PairAttributeCalculator(jcas, m1, m2);
////		ac.setStopWordsList(stopwords);
//		return ac.getSentenceDistance();
//	}

}
