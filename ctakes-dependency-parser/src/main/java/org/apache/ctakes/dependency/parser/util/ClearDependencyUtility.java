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

import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m081914
 *
 */
public class ClearDependencyUtility extends DependencyUtility {

//	public static UimaDepNode DUMMY_PARENT = new UimaDepNode();
	
	// LOG4J logger based on class name
	public Logger logger = Logger.getLogger(getClass().getName());
	
//	public static UimaDepTree getDepTreeFromArray(ArrayList<ConllDependencyNode> nodes) {
//	    UimaDepTree tree = new UimaDepTree();
//	    
//	    for (ConllDependencyNode cNode : nodes)
//	    {
//	        UimaDepNode node = new UimaDepNode();
//	        node.id     = cNode.getID();
//	        node.form   = cNode.getFORM();
//	        node.lemma  = cNode.getLEMMA();
//	        node.pos    = cNode.getPOSTAG();
//	        node.headId = cNode.getHEAD().getID();
//	        node.deprel = cNode.getDEPREL();
//	        node.begin  = cNode.getBegin();
//	        node.end    = cNode.getEnd();
//	        node.hasHead= (cNode.getHEAD()!=null);
//	        
//	        tree.add(node);
//	    }
//	    
//	    return tree;
//	}


//	public static UimaDepTree getDepTreeFromCas(JCas jCas, Sentence sentence) {    
//		return getDepTreeFromArray( getConllNodesFromCas(jCas,sentence) );
//	}
//
//
	public static ArrayList<ConllDependencyNode> convert(JCas jcas, DEPTree clearTree, Sentence sentence, List<BaseToken> tokens)  {
        
        ArrayList<ConllDependencyNode> uimaNodes = new ArrayList<ConllDependencyNode>(tokens.size()+1);

      // Create the root node first
        int rootBegin = tokens.get(0).getBegin();
        int rootEnd = tokens.get(tokens.size()-1).getEnd();
        uimaNodes.add( 0, new ConllDependencyNode(jcas, rootBegin, rootEnd));

        // Create all the other nodes
        for (int i=1; i<clearTree.size(); i++) {
            int nodeBegin = tokens.get(i-1).getBegin(); // assumes that tokens are off 1 from clearTree
            int nodeEnd = tokens.get(i-1).getEnd();
            ConllDependencyNode uimaNode = new ConllDependencyNode(jcas, nodeBegin, nodeEnd);
            uimaNodes.add(i,uimaNode);
        }
        
        // Set values in all the other nodes
        for (int i=1; i<clearTree.size(); i++) {
            

            DEPNode clearNode = clearTree.get(i);
            ConllDependencyNode uimaNode = uimaNodes.get(i);

            uimaNode.setId(clearNode.id);
            uimaNode.setForm(clearNode.form);
            uimaNode.setLemma(clearNode.lemma);
            uimaNode.setCpostag(clearNode.pos);
            uimaNode.setPostag(clearNode.pos);
            uimaNode.setFeats("_");
            uimaNode.setHead(uimaNodes.get(clearNode.getHead().id));
            uimaNode.setDeprel(clearNode.getLabel());
            uimaNode.setPhead(null);
            uimaNode.setPdeprel("_");

             
       }
        
        return uimaNodes;//uimaNodes.get(0); //return the root node
    }
    	
	
	/** Equality expressions to aid in converting between DepNodes and CAS objects */
	public static boolean equalCoverage(Annotation annot1,Annotation annot2) {
		return annot1.getBegin()==annot2.getBegin() && 
			annot1.getEnd()==annot2.getEnd() && 
			annot1.getCoveredText()==annot2.getCoveredText();
	}
//	public static boolean equalCoverage(Annotation annot,UimaDepNode udNode) {
//		return annot.getBegin()==udNode.begin && 
//			annot.getEnd()==udNode.end && 
//			annot.getCoveredText()==udNode.form;
//	}
//	public static boolean equalCoverage(UimaDepNode udNode, Annotation annot) {
//		return annot.getBegin()==udNode.begin && 
//			annot.getEnd()==udNode.end && 
//			annot.getCoveredText()==udNode.form;
//	}
//	public static boolean equalCoverage(UimaDepNode udNode, UimaDepNode udNode2) {
//		return udNode2.begin==udNode.begin && 
//			udNode2.end==udNode.end && 
//			udNode2.form==udNode.form;
//	}
	

	
}
