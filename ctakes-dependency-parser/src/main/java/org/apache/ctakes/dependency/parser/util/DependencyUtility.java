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

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author m081914
 *
 */
public abstract class DependencyUtility {

	public static Logger logger = Logger.getLogger("org.apache.ctakes.dependency.parser.util.DependencyUtility");

	static private final Pattern N_DOT_PATTERN = Pattern.compile( "N..?" );

	public static void addToIndexes( JCas jcas, ArrayList<ConllDependencyNode> nodes ) {
		for (int i=0; i<nodes.size(); i++) {
			// Enter UIMA nodes into index
			nodes.get(i).addToIndexes(jcas);
		}
	}

	
	/** Equality expressions to aid in converting between DepNodes and CAS objects */
	public static boolean equalCoverage(Annotation annot1,Annotation annot2) {
		if (annot1==null || annot2==null) 
			return false;
		return annot1.getBegin()==annot2.getBegin() && 
			annot1.getEnd()==annot2.getEnd() && 
			annot1.getCoveredText().equals(annot2.getCoveredText());
	}

	/** Checks if one annotation subsumes another */
	public static boolean doesSubsume(Annotation annot1,Annotation annot2) {
		if (annot1==null || annot2==null) 
			return false;
		return annot1.getBegin()<=annot2.getBegin() && 
			annot1.getEnd()>=annot2.getEnd() && 
			annot1.getCoveredText().contains(annot2.getCoveredText());
	}
	
	/** Find the sentence in which an Annotation lives **/
	public static Sentence getSentence( JCas jCas, Annotation annot ) {
		FSIterator sentences = jCas.getAnnotationIndex(Sentence.type).iterator();
		while (sentences.hasNext()) {
			Sentence sentence = (Sentence) sentences.next();
			if (doesSubsume(sentence,annot)) {
				return sentence;
			}
		}
		return null;
	}


	/** Returns the first ConllDependencyNode in the CAS w/ same begin and end as the given Annotation **/	
	public static ConllDependencyNode getDependencyNode(JCas jCas, Annotation annot) {

		AnnotationIndex nodeIndex = jCas.getAnnotationIndex(ConllDependencyNode.type);
	    FSIterator nodeIterator = nodeIndex.iterator();
	    while (nodeIterator.hasNext()) {
	        ConllDependencyNode node = (ConllDependencyNode) nodeIterator.next();
	        if (equalCoverage(annot,node)) {
	        	return node;
	        }
	    }
    	return null;
	}
	
	/** Returns the ConllDependencyNodes in the CAS w/ subsumed begins and ends **/	
	public static List<ConllDependencyNode> getDependencyNodes(JCas jCas, Annotation annot) {
		ArrayList<ConllDependencyNode> output = new ArrayList<ConllDependencyNode>();
		AnnotationIndex nodeIndex = jCas.getAnnotationIndex(ConllDependencyNode.type);
	    FSIterator nodeIterator = nodeIndex.iterator();
	    while (nodeIterator.hasNext()) {
	        ConllDependencyNode node = (ConllDependencyNode) nodeIterator.next();
	        if (doesSubsume(annot,node)) {
	        	output.add(node);
	        }
	    }
    	return output;
	}

	/** Returns the first ConllDependencyNode in the CAS w/ same begin and end as the given Annotation **/	
	public static ConllDependencyNode getNominalHeadNode(JCas jCas, Annotation annot) {
		List<ConllDependencyNode> nodes = getDependencyNodes(jCas, annot);
		if (nodes==null || nodes.size()==0) {
			return null;
		}
		return getNominalHeadNode( nodes );
	}

	/** Finds the head node out of a few ConllDependencyNodes. Biased toward nouns. **/
	public static ConllDependencyNode getNominalHeadNode(
			List<ConllDependencyNode> nodes) {
		ArrayList<ConllDependencyNode> anodes = new ArrayList<ConllDependencyNode>(nodes);
		Boolean[][] matrixofheads = new Boolean[anodes.size()][anodes.size()];
		List<ConllDependencyNode> outnodes = new ArrayList<ConllDependencyNode>();

      // Remove root from consideration
		for (int i=0; i<anodes.size(); i++) {
			if (anodes.get(i).getId()==0) {
				anodes.remove(i);
			}
		}

      // Create a dependency matrix
		for (int id1=0; id1<anodes.size(); id1++) {
			for (int id2=0; id2<anodes.size(); id2++) {
				// no head-dependency relationship between id1 and id2
				matrixofheads[ id2 ][ id1 ]
						= id1 != id2
						  && anodes.get( id2 ).getHead() != null
						  && anodes.get( id1 ).getId() == anodes.get( id2 ).getHead().getId();
//				if (id1==id2 || anodes.get(id1).getId()!=anodes.get(id2).getHead().getId()) {
//					matrixofheads[id2][id1]=false;
//				}
//				// a match
//				else {
//					matrixofheads[id2][id1]=true;
//				}
			}
		}
			
		// Search the dependency matrix for the head
		for (int idhd=0; idhd<anodes.size(); idhd++) {
			boolean occupiedCol = false;
			for (int row=0; row<anodes.size(); row++) {
				if ( matrixofheads[row][idhd] ) {
					occupiedCol = true;
				}
			}
			if (occupiedCol) {
				boolean occupiedRow = false;
				for (int col=0; col<anodes.size(); col++) {
					if ( matrixofheads[idhd][col] ) {
						occupiedRow = true;
					}
				}
				if (!occupiedRow) {
					outnodes.add(anodes.get(idhd));
				}
			}
		}

      // Unheaded phrases
		if (outnodes.isEmpty()) {
			// pick a noun from the left, if there is one
			for (int i=0; i<anodes.size(); i++) {
				if ( anodes.get( i ) != null && anodes.get( i ).getPostag() != null
					  && N_DOT_PATTERN.matcher( anodes.get( i ).getPostag() ).matches() ) {
					return anodes.get(i);
				}
			}
			// default to picking the rightmost node
			return anodes.get(anodes.size()-1);
		}
		// Headed phrases
		else {
			// pick a noun from the left, if there is one
			for (int i=0; i<outnodes.size(); i++) {
				if ( outnodes.get( i ) != null && outnodes.get( i ).getPostag() != null
					  && N_DOT_PATTERN.matcher( outnodes.get( i ).getPostag() ).matches() ) {
					return outnodes.get(i);
				}
			}
			// otherwise, pick the rightmost node with dependencies
			return outnodes.get(outnodes.size()-1);		
		}
	}


	/** Given a set of ConllDependencyNodes, find the path between two nodes **/
	public static DependencyPath getPath(List<ConllDependencyNode> nodes, ConllDependencyNode node1, ConllDependencyNode node2) {

		DependencyPath pathUp1 = new DependencyPath();
		DependencyPath pathUp2 = new DependencyPath();
		DependencyPath pathLtoR = new DependencyPath();
		DependencyPath pathRtoL = new DependencyPath();

		if (node1==null || node2==null) {
			System.err.println(" WARNING: looking for path between null nodes.");
			return null;
		}

		// Trace each node up to the ROOT
		pathUp1.add(node1);
		while (node1.getHead()!=null) {
			node1 = node1.getHead();
			pathUp1.add(node1);
		}
		pathUp2.add(node2);
		while (node2.getHead()!=null) {
			node2 = node2.getHead();
			pathUp2.add(node2);
		}
		
		// Give the path from node1 to node2
		pathLtoR.clear();
		boolean foundMatch = false;
		for (int i=0; i<pathUp1.size(); i++ ) {
			
			// Assemble the part of the path from node1 to common ancestor
			ConllDependencyNode nodeUp1 = pathUp1.get(i);
			pathLtoR.add(nodeUp1);

			// Check each node above node2 for a match 
			pathRtoL.clear();
			for (int j=0; j<pathUp2.size(); j++ ) {
				ConllDependencyNode nodeUp2 = pathUp2.get(j);				
				if (DependencyUtility.equalCoverage(nodeUp1, nodeUp2)) {
					// word-rel<word-rel<commonparent>rel-word>rel-word
					pathLtoR.setCommonNode(nodeUp1);
					
					// add the two halves of the path together
					pathLtoR.addAll(pathRtoL);
					foundMatch = true;
					break;
				} else {
					pathRtoL.addFirst(nodeUp2);
				}
			}

			if (foundMatch) 
				return pathLtoR;
		}

		return null;

	}

	/** Given a CAS, find the path between two nodes in a sentence **/
	public static DependencyPath getPath( JCas jCas, ConllDependencyNode node1, ConllDependencyNode node2) {
		
		Sentence sent1 = getSentence( jCas, node1);
		Sentence sent2 = getSentence( jCas, node2);
		if (sent1.equals(sent2)) {
			return getPath( getDependencyNodes(jCas, sent1), node1, node2);
		} else {
			
			// 6/28/13 shalgrim
			// nodes can be null so check before calling getCoveredText
			String node1txt, node2txt;
			
			if (node1 == null) {
				node1txt = "null";
			} else {
				node1txt = node1.getCoveredText();
			}
			
			if (node2 == null)
			{
				node2txt = "null";
			} else {
				node2txt = node2.getCoveredText();
			}
			
			logger.debug(String.format("Cannot find path between nodes in different sentences. Node1: %s  Node2: %s",
					node1txt, node2txt));			
		}
		return null;
	}

	
	public static DependencyPath getPathToTop(JCas jCas, ConllDependencyNode node1) {

		DependencyPath pathUp1 = new DependencyPath();

		if (node1==null) {
			System.err.println(" WARNING: looking for path between null nodes.");
			return null;
		}

		pathUp1.add(node1);
		while (node1.getHead()!=null) {
			node1 = node1.getHead();
			pathUp1.add(node1);
		}
		return pathUp1;
	}

	public static List<ConllDependencyNode> getRightSibs( ConllDependencyNode refNode, List<ConllDependencyNode> tree ) {
		
		ConllDependencyNode parent = refNode.getHead();
		List<ConllDependencyNode> out = new ArrayList<ConllDependencyNode>();
		
		for ( ConllDependencyNode node : tree.subList( tree.indexOf(refNode)+1, tree.size() ) ) {
			if ( node.getHead().equals(parent) ) {
				out.add(node);
			}
		}
		return out;
	}
	
	public static List<ConllDependencyNode> getLeftSibs( ConllDependencyNode refNode, List<ConllDependencyNode> tree ) {
		
		ConllDependencyNode parent = refNode.getHead();
		List<ConllDependencyNode> out = new ArrayList<ConllDependencyNode>();
		
		List<ConllDependencyNode> lSide = tree.subList(0,tree.indexOf(refNode));
		for ( int i=tree.indexOf(refNode)-1; i>=0; i-- ) {
			ConllDependencyNode node = lSide.get(i);
			if ( node.getHead().equals(parent) ) {
				out.add(node);
			}
		}
		return out;
	}
	
	public static List<ConllDependencyNode> getProgeny( ConllDependencyNode refNode, List<ConllDependencyNode> tree) {

		List<ConllDependencyNode> out = new ArrayList<ConllDependencyNode>();
		
		// Find the path to root for every node
		for ( ConllDependencyNode node : tree ) {
			
			// Progeny includes the reference node itself
			if ( node.equals(refNode) ) {
				out.add(node);
			} else {
				
				// Anything with refNode on its path to root is progeny.  Requires acyclicity
				ConllDependencyNode upNode = node;
				while (upNode.getHead()!=null) {
					upNode = upNode.getHead();
					if (upNode.equals(refNode)) {
						out.add(node);
						break;
					}
				}
				
			}
			
		}
		
		return out;
	}
	
	public static List<ConllDependencyNode> getProgeny( List<ConllDependencyNode> refNodes, List<ConllDependencyNode> tree) {

		List<ConllDependencyNode> out = new ArrayList<ConllDependencyNode>();
		
		// Find the path to root for every node
		for ( ConllDependencyNode node : tree ) {
			
			// Progeny includes the reference nodes themselves
			if ( refNodes.contains(node) ) {
				out.add(node);
			} else {
				
				// Anything with refNode on its path to root is progeny.  Requires acyclicity
				ConllDependencyNode upNode = node;
				while (upNode.getHead()!=null) {
					upNode = upNode.getHead();
					
					if (refNodes.contains(upNode)) {
						out.add(node);
						break;
					}
				}
				
			}
			
		}
		
		return out;
	}

	public static List<ConllDependencyNode> getRightSibProgeny( ConllDependencyNode refNode, List<ConllDependencyNode> tree) {
		return getProgeny( getRightSibs(refNode,tree), tree );
	}
	
	public static List<ConllDependencyNode> getLeftSibProgeny( ConllDependencyNode refNode, List<ConllDependencyNode> tree) {
		return getProgeny( getLeftSibs(refNode,tree), tree );
	}


	public static String dumpDependencyGraph(Annotation annotation) {
		StringBuilder builder = new StringBuilder();
		for (ConllDependencyNode depNode : JCasUtil.selectCovered(ConllDependencyNode.class, annotation)) {
			ConllDependencyNode head = depNode.getHead();
			String headStr = (head != null) ? head.getCoveredText() : "TOP";
			builder.append(String.format("%s(%s, %s)\n", depNode.getDeprel(), depNode.getCoveredText(), headStr));
		}
		return builder.toString();
	}
}
