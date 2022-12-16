/**
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
package org.apache.ctakes.relationextractor.ae.features;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.JCasUtil;

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;

public class DependencyParseUtils {

	/**
	 * Returns the paths from each node to the common ancestor between them
	 */
	public static List<LinkedList<ConllDependencyNode>> getPathsToCommonAncestor(ConllDependencyNode node1, ConllDependencyNode node2) {
		List<LinkedList<ConllDependencyNode>> paths = new ArrayList<LinkedList<ConllDependencyNode>>(2);
		LinkedList<ConllDependencyNode> node1ToHeadPath = DependencyParseUtils.getPathToSentenceHead(node1);
		LinkedList<ConllDependencyNode> node2ToHeadPath = DependencyParseUtils.getPathToSentenceHead(node2);
		
		// We will remove the last item in each path until they diverge
		ConllDependencyNode ancestor = null;
		while (!node1ToHeadPath.isEmpty() && !node2ToHeadPath.isEmpty()) {
			if (node1ToHeadPath.getLast() == node2ToHeadPath.getLast()) {
				node1ToHeadPath.removeLast();
				ancestor = node2ToHeadPath.removeLast();
			} else {
				break;
			}
		}
		
		// Put the common ancestor back on both paths
		if (ancestor != null) {
	    	 node1ToHeadPath.add(ancestor);
	    	 node2ToHeadPath.add(ancestor);
		}
	     
		paths.add(node1ToHeadPath);
		paths.add(node2ToHeadPath);
		return paths;
	}

	/**
	 * Finds the head word within a given annotation span
	 */
	public static ConllDependencyNode findAnnotationHead(JCas jcas, Annotation annotation) {
	
	    for (ConllDependencyNode depNode : JCasUtil.selectCovered(jcas, ConllDependencyNode.class, annotation)) {
	    	
	    	ConllDependencyNode head = depNode.getHead();
	    	if (head == null || head.getEnd() <= annotation.getBegin() || head.getBegin() > annotation.getEnd()) {
	    		// The head is outside the bounds of the annotation, so this node must be the annotation's head
	    		return depNode;
	    	}
	    }
	    // Can this happen?
	    return null;
	}

	public static LinkedList<ConllDependencyNode> getPathToSentenceHead(ConllDependencyNode node) {
	     LinkedList<ConllDependencyNode> path = new LinkedList<ConllDependencyNode>();
	     ConllDependencyNode currNode = node;
	     while (currNode.getHead() != null) { 
	         path.add(currNode);
	         currNode = currNode.getHead();
	     }
	     return path;
	}

	/**
	 * Finds the path between two dependency nodes
	 */
	public static LinkedList<ConllDependencyNode> getPathBetweenNodes(ConllDependencyNode srcNode, ConllDependencyNode tgtNode) {
		LinkedList<ConllDependencyNode> path = new LinkedList<ConllDependencyNode>();
		List<LinkedList<ConllDependencyNode>> paths = getPathsToCommonAncestor(srcNode, tgtNode);
		LinkedList<ConllDependencyNode> srcToAncestorPath = paths.get(0);
		LinkedList<ConllDependencyNode> tgtToAncestorPath = paths.get(1);
		
		if (srcNode == tgtNode) {
			return path;
		}
		
		// Join the two paths
		if (!srcToAncestorPath.isEmpty()) {
			srcToAncestorPath.removeLast();
		}
		path = srcToAncestorPath;
		while (!tgtToAncestorPath.isEmpty()) {
			path.add(tgtToAncestorPath.removeLast());
		}
		
		return path;
	}
	

	/**
	 * This will convert a path into a string lexicalized at the end points with arc labels and POS tags in between
	 */
	
	public static String pathToString(LinkedList<ConllDependencyNode> path) {
		
		StringBuilder builder = new StringBuilder();
		for (ConllDependencyNode node : path) {
			if (node == path.getFirst() || node == path.getLast()) {
				builder.append(node.getCoveredText());
			} else {
				builder.append(node.getPostag());
			}
			
			builder.append("-");
			builder.append(node.getDeprel());
			if (node != path.getLast()) {
				builder.append("/");
			}
		}
		return builder.toString();
	}
	

	

	public static String dumpDependencyRelations(JCas jcas, Annotation annotation) {
		StringBuilder builder = new StringBuilder();
	    for (ConllDependencyNode depNode : JCasUtil.selectCovered(jcas, ConllDependencyNode.class, annotation)) {
	    	if (depNode.getHead() != null) {
	    		builder.append(String.format("%s(%s,%s)\n", depNode.getDeprel(), depNode.getCoveredText(), depNode.getHead().getCoveredText()));
	    	}
	    }
	    return builder.toString();
		
	}

}
