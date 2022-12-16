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
package org.apache.ctakes.assertion.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.dependency.parser.util.DependencyPath;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.jcas.JCas;

//import edu.mayo.bmi.fsm.output.NegationIndicator;
//import edu.mayo.bmi.nlp.parser.util.ClearDependencyUtility;
//import edu.mayo.bmi.nlp.parser.util.DependencyPath;
//import edu.mayo.bmi.nlp.parser.util.DependencyRegex;
//import edu.mayo.bmi.nlp.parser.util.DependencyUtility;
//import edu.mayo.bmi.uima.context.ContextHit;
//import edu.mayo.bmi.uima.core.type.syntax.ConllDependencyNode;


/**
 * Uses one or more finite state machines to detect dates in the given input of
 * tokens.
 * 
 * @author Mayo Clinic
 */
public class NegationManualDepContextAnalyzer {

	private NegationDepRegex regexes;
	
	public NegationManualDepContextAnalyzer(){
		regexes = new NegationDepRegex();		
	}
	
/*	public void initialize(UimaContext annotatorContext) throws ResourceInitializationException {
		// Initialize all the regex that will be used

	}
*/
	public boolean isBoundary(Annotation contextAnnotation, int scopeOrientation) throws AnalysisEngineProcessException {
		String lcText = contextAnnotation.getCoveredText().toLowerCase();
		return regexes._boundaryWordSet.contains(lcText);
	}

	/**
	 * This method analyzes a sentence
	 * for negation context based dependency paths.
	 */
/*	public ContextHit analyzeContext(List<ConllDependencyNode> nodes, ConllDependencyNode focus)
			throws AnalysisEngineProcessException {
		
		try {
			Set<NegationIndicator> s = findNegationContext(nodes,focus); 
			
			if (s.size() > 0) {
				NegationIndicator neg = s.iterator().next();
				return new ContextHit(neg.getStartOffset(), neg.getEndOffset());
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
*/	
	/**
	 * Executes the regular expressions on paths.
	 * 
	 * @param nodes
	 * @param focus
	 * @return Set of DateToken objects.
	 * @throws Exception
	 */
	public boolean[] findNegationContext(List<ConllDependencyNode> nodes, ConllDependencyNode focus) throws Exception {
		List<ConllDependencyNode> hits = new ArrayList<ConllDependencyNode>();
//		List<String> feats = new ArrayList<String>();
		boolean[] feats = new boolean[regexes.regexSet.size()];
		
		// Print out the sentence for testing
//		System.out.print("*** in findNegationContext; sentence is: ");
//		for (ConllDependencyNode n : nodes) {
//			System.out.print(n.getCoveredText()+" ");
//		}
//		System.out.println();
		
		// Iterate through all nodes in the sentence to find the focus node
		for (int i=0; i<nodes.size(); i++) {
			ConllDependencyNode hypNegNode = nodes.get(i);
//			System.out.println("Node "+i+": {"+hypNegNode.getFORM()+"}"+hypNegNode.getPOSTAG()+">"+hypNegNode.getDEPREL());
			DependencyPath path = DependencyUtility.getPath(nodes, hypNegNode, focus);
			if(path == null) continue;
			int featMatchInd = findPathMatches(path);
			if ( featMatchInd != -1) {
//				hits.add(hypNegNode);
				feats[featMatchInd] = true;
			}
		}
		return feats;
//		System.out.println("=== in findNegationContext; found "+hits.size()+" negations in sentence");
		
		// Iterate through all the node hits and convert to NegationIndicators
//		Set<NegationIndicator> negHits = new HashSet<NegationIndicator>();
//		for (int i=0; i<hits.size(); i++) {
//			NegationIndicator negInd = new NegationIndicator(hits.get(i).getBegin(), hits.get(i).getEnd());
//			negHits.add(negInd);
//		}
//		return negHits;
	}

	private int findPathMatches(DependencyPath path) {
		// Check this path against all regexes
		// Test regexes on path
//		System.out.println(path.toString());
		for (int i=0; i<regexes.regexSet.size(); i++) {
			String pathString = path.toString();
			if (regexes.regexSet.get(i).matches( pathString )) {
//				System.out.println("  Regex: "+regexes.regexSet.get(i).toString()+"\n  "
//						+regexes.regexSet.get(i).matches( path.toString() ));
				return i;
			}
		}
		return -1;

	}

	public int getNumFeatures(){
		return regexes.regexSet.size();
	}
	
	public String getRegexName(int i){
	  return regexes.regexSet.get(i).getName();
	}
}