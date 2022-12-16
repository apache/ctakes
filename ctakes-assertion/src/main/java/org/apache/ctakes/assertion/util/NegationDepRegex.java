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

import org.apache.ctakes.dependency.parser.util.DependencyRegex;
import static org.apache.ctakes.dependency.parser.util.DependencyRegex.*;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import com.googlecode.clearnlp.dependency.DEPLib;
import static com.googlecode.clearnlp.dependency.DEPLib.*;
import com.googlecode.clearnlp.dependency.DEPLibEn;
import static com.googlecode.clearnlp.dependency.DEPLibEn.*;
import com.googlecode.clearnlp.dependency.DEPNode;


/**
 * Uses one or more regular expressions to detect patterns in dependency paths.
 * 
 * @author Mayo Clinic
 */
/**
 * @author m081914
 *
 */
public class NegationDepRegex {

	// regular modal verb
	public Set<String> iv_modalVerbsSet = new HashSet<String>();
	// negative particle
	public Set<String> iv_negParticlesSet = new HashSet<String>();
	// regular verbs requiring negation particle
	public Set<String> iv_regVerbsSet = new HashSet<String>();
	// neagive verbs that contain negation in them
	public Set<String> iv_negVerbsSet = new HashSet<String>();
	// negation preposition
	public Set<String> iv_negPrepositionsSet = new HashSet<String>();
	// negatively charged determiners
	public Set<String> iv_negDeterminersSet = new HashSet<String>();
	// regular nouns - indicators
	public Set<String> iv_regNounsSet = new HashSet<String>();
	// regular prepositions
	public Set<String> iv_regPrepositionsSet = new HashSet<String>();
	// negative adjectives
	public Set<String> iv_negAdjectivesSet = new HashSet<String>();
	// negative collocations
	public Set<String> iv_negCollocSet = new HashSet<String>();
	// NEGATIVE COLLOCATION PARTICLE
	public Set<String> iv_negColPartSet = new HashSet<String>();
	// conjunctions as POS
	public Set<String> rel_conjunctSet = new HashSet<String>();
	// copula
	public Set<String> iv_copulaSet = new HashSet<String>();
	// Union of negative determiners and prepositions
	public Set<String> iv_negDetPlusPrepSet = new HashSet<String>(); 
	
	public Set<String> _boundaryWordSet;

	// The regexes to be used
	public List<DependencyRegex> regexSet = new ArrayList<DependencyRegex>();

	
	/**
	 * Constructor
	 * @throws ResourceInitializationException 
	 */
	public NegationDepRegex(){
		initialize();
	}

	public void initialize(){
		// Define some boundary conditions (consistent with old negation annotator)
		initBoundaryData();

		// Add all the sets of special words
		initWordSets();
		
		// Add all the regexes possible
		initRegexes();
	}

	private void initBoundaryData() {
		_boundaryWordSet = new HashSet<String>();
		_boundaryWordSet.add("but");
		_boundaryWordSet.add("however");
		_boundaryWordSet.add("nevertheless");
		_boundaryWordSet.add("notwithstanding");
		_boundaryWordSet.add("though");
		_boundaryWordSet.add("although");
		_boundaryWordSet.add("if");
		_boundaryWordSet.add("when");
		_boundaryWordSet.add("how");
		_boundaryWordSet.add("what");
		_boundaryWordSet.add("which");
		_boundaryWordSet.add("while");
		_boundaryWordSet.add("since");
		_boundaryWordSet.add("then");
		_boundaryWordSet.add("i");
		_boundaryWordSet.add("he");
		_boundaryWordSet.add("she");
		_boundaryWordSet.add("they");
		_boundaryWordSet.add("we");
	
		_boundaryWordSet.add(";");
		_boundaryWordSet.add(":");
		_boundaryWordSet.add(".");
		_boundaryWordSet.add(")");
	}

	public boolean isBoundary(Annotation contextAnnotation, int scopeOrientation) throws AnalysisEngineProcessException {
		String lcText = contextAnnotation.getCoveredText().toLowerCase();
		return _boundaryWordSet.contains(lcText);
	}

	private void initWordSets() {
		iv_modalVerbsSet.add("can");
		iv_modalVerbsSet.add("ca");
		iv_modalVerbsSet.add("will");
		iv_modalVerbsSet.add("must");
		iv_modalVerbsSet.add("could");
		iv_modalVerbsSet.add("would");
		iv_modalVerbsSet.add("should");
		iv_modalVerbsSet.add("shall");
		iv_modalVerbsSet.add("did");
//		iv_modalVerbsSet.add("is");     // added for dependencyNeg
//		iv_modalVerbsSet.add("was");    // added for dependencyNeg
//		iv_modalVerbsSet.add("has");    // added for dependencyNeg
//		iv_modalVerbsSet.add("had");    // added for dependencyNeg

		iv_copulaSet.add("is");     // added for dependencyNeg
		iv_copulaSet.add("was");    // added for dependencyNeg
		iv_copulaSet.add("be");    // added for dependencyNeg
		iv_copulaSet.add("are");     // added for dependencyNeg
		iv_copulaSet.add("were");    // added for dependencyNeg
		iv_copulaSet.add("be");    // added for dependencyNeg
		
		iv_negParticlesSet.add("not");
		iv_negColPartSet.add("out");
		iv_negParticlesSet.add("n't");
		iv_negParticlesSet.add("'t");
	
		iv_negCollocSet.add("rule");
		iv_negCollocSet.add("rules");
		iv_negCollocSet.add("ruled");
		iv_negCollocSet.add("ruling");
		iv_negCollocSet.add("rule-out");
	
		iv_regVerbsSet.add("reveal");
		iv_regVerbsSet.add("reveals");
		iv_regVerbsSet.add("revealed");
		iv_regVerbsSet.add("revealing");
		iv_regVerbsSet.add("have");
		iv_regVerbsSet.add("had");
		iv_regVerbsSet.add("has");
		iv_regVerbsSet.add("feel");
		iv_regVerbsSet.add("feels");
		iv_regVerbsSet.add("felt");
		iv_regVerbsSet.add("feeling");
		iv_regVerbsSet.add("complain");
		iv_regVerbsSet.add("complains");
		iv_regVerbsSet.add("complained");
		iv_regVerbsSet.add("complaining");
		iv_regVerbsSet.add("demonstrate");
		iv_regVerbsSet.add("demonstrates");
		iv_regVerbsSet.add("demonstrated");
		iv_regVerbsSet.add("demonstrating");
		iv_regVerbsSet.add("appear");
		iv_regVerbsSet.add("appears");
		iv_regVerbsSet.add("appeared");
		iv_regVerbsSet.add("appearing");
		iv_regVerbsSet.add("caused");
		iv_regVerbsSet.add("cause");
		iv_regVerbsSet.add("causing");
		iv_regVerbsSet.add("causes");
		iv_regVerbsSet.add("find");
		iv_regVerbsSet.add("finds");
		iv_regVerbsSet.add("found");
		iv_regVerbsSet.add("discover");
		iv_regVerbsSet.add("discovered");
		iv_regVerbsSet.add("discovers");
//		iv_regVerbsSet.add("show");     // added for dependencyNeg
//		iv_regVerbsSet.add("shows");	// added for dependencyNeg
//		iv_regVerbsSet.add("showed");	// added for dependencyNeg
//		iv_regVerbsSet.add("showwing");	// added for dependencyNeg
//		iv_regVerbsSet.add("indicate");     // added for dependencyNeg
//		iv_regVerbsSet.add("indicates");	// added for dependencyNeg
//		iv_regVerbsSet.add("indicated");	// added for dependencyNeg
//		iv_regVerbsSet.add("indicating");	// added for dependencyNeg
//		iv_regVerbsSet.add("include");		// added for dependencyNeg
//		iv_regVerbsSet.add("includes");		// added for dependencyNeg
//		iv_regVerbsSet.add("included");		// added for dependencyNeg
//		iv_regVerbsSet.add("including");	// added for dependencyNeg
	
		iv_negVerbsSet.add("deny");
		iv_negVerbsSet.add("denies");
		iv_negVerbsSet.add("denied");
		iv_negVerbsSet.add("denying");
		iv_negVerbsSet.add("fail");
		iv_negVerbsSet.add("fails");
		iv_negVerbsSet.add("failed");
		iv_negVerbsSet.add("failing");
		iv_negVerbsSet.add("decline");
		iv_negVerbsSet.add("declines");
		iv_negVerbsSet.add("declined");
		iv_negVerbsSet.add("declining");
		iv_negVerbsSet.add("exclude");
		iv_negVerbsSet.add("excludes");
		iv_negVerbsSet.add("excluding");
		iv_negVerbsSet.add("excluded");
//		iv_regVerbsSet.add("contraindicate");     // added for dependencyNeg
//		iv_regVerbsSet.add("contraindicates");	// added for dependencyNeg
//		iv_regVerbsSet.add("contraindicated");	// added for dependencyNeg
//		iv_regVerbsSet.add("contraindicating");	// added for dependencyNeg
//		iv_regVerbsSet.add("contra-indicate");     // added for dependencyNeg
//		iv_regVerbsSet.add("contra-indicates");	// added for dependencyNeg
//		iv_regVerbsSet.add("contra-indicated");	// added for dependencyNeg
//		iv_regVerbsSet.add("contra-indicating");	// added for dependencyNeg
	
		iv_negPrepositionsSet.add("without");
		iv_negPrepositionsSet.add("absent"); //removed for dependencyNeg
		iv_negPrepositionsSet.add("none");   //removed for dependencyNeg
	
		iv_negDeterminersSet.add("no");
		iv_negDeterminersSet.add("any");  //removed for dependencyNeg
		iv_negDeterminersSet.add("neither");
		iv_negDeterminersSet.add("nor");
		iv_negDeterminersSet.add("never");
	
		iv_regNounsSet.add("evidence");
		iv_regNounsSet.add("indication");
		iv_regNounsSet.add("indications");
		iv_regNounsSet.add("sign");
		iv_regNounsSet.add("signs");
		iv_regNounsSet.add("symptoms");
		iv_regNounsSet.add("symptom");
		iv_regNounsSet.add("sx");
		iv_regNounsSet.add("dx");
		iv_regNounsSet.add("diagnosis");
		iv_regNounsSet.add("history");
		iv_regNounsSet.add("hx");
		iv_regNounsSet.add("findings");
//		iv_regNounsSet.add("finding");		// added for dependencyNeg
//		iv_regNounsSet.add("recurrence");	// added for dependencyNeg
//		iv_regNounsSet.add("recurrences");	// added for dependencyNeg
//		iv_regNounsSet.add("occurrence");	// added for dependencyNeg
//		iv_regNounsSet.add("occurrences");	// added for dependencyNeg
		     
		iv_regPrepositionsSet.add("of");
		iv_regPrepositionsSet.add("in");
		iv_regPrepositionsSet.add("for");
		iv_regPrepositionsSet.add("with");
	
		iv_negAdjectivesSet.add("unremarkable");
		iv_negAdjectivesSet.add("unlikely");
		iv_negAdjectivesSet.add("negative");
//		iv_negAdjectivesSet.add("absent"); // added for dependencyNeg
//		iv_negAdjectivesSet.add("none");   // added for dependencyNeg
	
//		rel_conjunctSet.add("CONJ");		// added for dependencyNeg
		rel_conjunctSet.add(DEPLibEn.DEP_CONJ);
//		rel_conjunctSet.add("COORD");		// added for dependencyNeg
		rel_conjunctSet.add(DEPLibEn.DEP_CC);
//		rel_conjunctSet.add("APPO");		// added for dependencyNeg
		rel_conjunctSet.add(DEPLibEn.DEP_APPOS);
//		rel_conjunctSet.add("P");			// added for dependencyNeg
		rel_conjunctSet.add(DEPLibEn.DEP_PREP);
		
		iv_negDetPlusPrepSet = iv_negDeterminersSet;
		iv_negDetPlusPrepSet.addAll(iv_negPrepositionsSet);
	}

	private void initRegexes() {
		// Recognizes phrases like "<disease1>, <disease2>, or FOCUS".  For appending for any FOCUS.
		DEPNode[] regnodes_NN_CONJ_NN = new DEPNode[1];
		regnodes_NN_CONJ_NN[0]		= new DEPNode(DEPLib.NULL_ID, ANY_TOKEN);
		regnodes_NN_CONJ_NN[0].setLabel(DependencyRegex.fromSet(rel_conjunctSet));
		//regnodes_NN_CONJ_NN[1]        = new DEPNode();
		//regnodes_NN_CONJ_NN[1].deprel = DependencyRegex.fromSet(rel_conjunctSet);

		// Recognizes phrases like "denies <symptom1>"
		DEPNode[] regnodes_VBNEG_OBJ = new DEPNode[2];
		regnodes_VBNEG_OBJ[0]        = new DEPNode(DEPLib.NULL_ID, fromSet(iv_negVerbsSet));
//		regnodes_VBNEG_OBJ[0].form   = DependencyRegex.fromSet(iv_negVerbsSet);
		regnodes_VBNEG_OBJ[1]		 = new DEPNode(DEPLib.NULL_ID, ANY_TOKEN);
		regnodes_VBNEG_OBJ[1].setLabel(DEPLibEn.DEP_DOBJ);  /* was "OBJ" in clearparser */
		int cVBNEG_OBJ = 0;
		
		// Recognizes phrases like "was not FOCUS" -- in dependency order: not was 
		DEPNode[] regnodes_PRT_VB_PRD = new DEPNode[3];
		regnodes_PRT_VB_PRD[0]          = new DEPNode(DEPLib.NULL_ID, fromSet(iv_negParticlesSet));
//		regnodes_PRT_VB_PRD[0].form     = DependencyRegex.fromSet(iv_negParticlesSet);
		regnodes_PRT_VB_PRD[0].setLabel(DEPLibEn.DEP_NEG); // was: ("ADV");
		regnodes_PRT_VB_PRD[1]          = new DEPNode(DEPLib.NULL_ID, fromSet(iv_copulaSet));
//		regnodes_PRT_VB_PRD[1].form     = DependencyRegex.fromSet(iv_copulaSet);
		regnodes_PRT_VB_PRD[2]          = new DEPNode(DEPLib.NULL_ID, ANY_TOKEN);
		regnodes_PRT_VB_PRD[2].setLabel(DEPLibEn.DEP_ACOMP); // was: ("PRD");
		regnodes_PRT_VB_PRD[2].pos      = DependencyRegex.ANY_ADJECTIVE;
		int cPRT_VB_PRD = 1;
		
		// Recognizes phrases like "did not find FOCUS"  (don't think the modal verb will be on the path?)
		DEPNode[] regnodes_PRT_MOD_VB_OBJ = new DEPNode[3];
		regnodes_PRT_MOD_VB_OBJ[0]          = new DEPNode(DEPLib.NULL_ID,fromSet(iv_negParticlesSet));
//		regnodes_PRT_MOD_VB_OBJ[0].form     = DependencyRegex.fromSet(iv_negParticlesSet);
		regnodes_PRT_MOD_VB_OBJ[0].setLabel(DEPLibEn.DEP_NEG); // was: ("ADV");
		regnodes_PRT_MOD_VB_OBJ[1]          = new DEPNode(DEPLib.NULL_ID, fromSet(iv_regVerbsSet));
		regnodes_PRT_MOD_VB_OBJ[1].pos      = ANY_VERB;
		regnodes_PRT_MOD_VB_OBJ[2]          = new DEPNode(DEPLib.NULL_ID,ANY_TOKEN);
		regnodes_PRT_MOD_VB_OBJ[2].setLabel(DEPLibEn.DEP_DOBJ);	
		// everything below here was replaced for this path:
/*		// 
		regnodes_PRT_MOD_VB_OBJ[1]          = new DEPNode(DEPLib.NULL_ID,fromSet(iv_modalVerbsSet));
//		regnodes_PRT_MOD_VB_OBJ[1].form     = DependencyRegex.fromSet(iv_modalVerbsSet);
		regnodes_PRT_MOD_VB_OBJ[2]          = new DEPNode(DEPLib.NULL_ID,fromSet(iv_regVerbsSet));
//		regnodes_PRT_MOD_VB_OBJ[2].form     = DependencyRegex.fromSet(iv_regVerbsSet);
//		regnodes_PRT_MOD_VB_OBJ[2].setLabel(DEPLibEn.DEP_CCOMP); // was: ("VC"); really a shot in the dark
		regnodes_PRT_MOD_VB_OBJ[2].pos      = ANY_VERB;
		regnodes_PRT_MOD_VB_OBJ[3]          = new DEPNode(DEPLib.NULL_ID,ANY_TOKEN);
		regnodes_PRT_MOD_VB_OBJ[3].setLabel(DEPLibEn.DEP_DOBJ);  // was:("OBJ"); */
		int cPRT_MOD_VB_OBJ = 1;
		
		// Recognizes phrases like "did not find evidence of FOCUS"
		DEPNode[] regnodes_PRT_MOD_VB_OBJ_IN_PMOD = new DEPNode[6];
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[0]          = new DEPNode(DEPLib.NULL_ID,fromSet(iv_negParticlesSet));
//		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[0].form     = DependencyRegex.fromSet(iv_negParticlesSet);
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[0].setLabel(DEPLibEn.DEP_NEG); // was:("ADV");
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[1]          = new DEPNode(DEPLib.NULL_ID,fromSet(iv_modalVerbsSet));
//		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[1].form     = DependencyRegex.fromSet(iv_modalVerbsSet);
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[1].setLabel(DEPLibEn.DEP_CCOMP); // was: ("VC");
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[2]          = new DEPNode(DEPLib.NULL_ID,fromSet(iv_regVerbsSet));
//		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[2].form     = DependencyRegex.fromSet(iv_regVerbsSet);
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[3]          = new DEPNode(DEPLib.NULL_ID,fromSet(iv_regNounsSet));
//		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[3].form     = DependencyRegex.fromSet(iv_regNounsSet);
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[3].setLabel(DEPLibEn.DEP_DOBJ); //("OBJ");
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[4]        = new DEPNode(DEPLib.NULL_ID,fromSet(iv_regPrepositionsSet));
//		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[4].form   = DependencyRegex.fromSet(iv_regPrepositionsSet);
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[4].pos    = "IN";
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[5]        = new DEPNode(DEPLib.NULL_ID,ANY_TOKEN);
		regnodes_PRT_MOD_VB_OBJ_IN_PMOD[5].setLabel(DEPLibEn.DEP_POBJ); // was: ("PMOD");		
		int cPRT_MOD_VB_OBJ_IN_PMOD = 1;
		
		// Recognizes phrases like "excluding FOCUS"
		DEPNode[] regnodes_VBN_SBJ = new DEPNode[2];
		regnodes_VBN_SBJ[0]        = new DEPNode(DEPLib.NULL_ID, fromSet(iv_negVerbsSet));
//		regnodes_VBN_SBJ[0].form   = DependencyRegex.fromSet(iv_negVerbsSet);
		regnodes_VBN_SBJ[0].pos    = "VBG";  // was VBN
		regnodes_VBN_SBJ[1]		   = new DEPNode(DEPLib.NULL_ID, ANY_TOKEN);
		regnodes_VBN_SBJ[1].setLabel(DEPLibEn.DEP_DOBJ); // was: ("SBJ");
		int cVBN_SBJ = 0;
		
		// Recognizes phrases like "rules out FOCUS"
		DEPNode[] regnodes_PRT_rule_OBJ = new DEPNode[3];
		regnodes_PRT_rule_OBJ[0]          = new DEPNode(DEPLib.NULL_ID, fromSet(iv_negColPartSet));
//		regnodes_PRT_rule_OBJ[0].form     = DependencyRegex.fromSet(iv_negColPartSet);
		regnodes_PRT_rule_OBJ[0].setLabel(DEPLibEn.DEP_PRT); // was: ("PRT");
		regnodes_PRT_rule_OBJ[1]          = new DEPNode(DEPLib.NULL_ID, fromSet(iv_negCollocSet));
//		regnodes_PRT_rule_OBJ[1].form     = DependencyRegex.fromSet(iv_negCollocSet);
		regnodes_PRT_rule_OBJ[2]          = new DEPNode(DEPLib.NULL_ID, ANY_TOKEN);
		regnodes_PRT_rule_OBJ[2].setLabel(DEPLibEn.DEP_DOBJ); //("OBJ");
		int cPRT_rule_OBJ = 1;
		
		//// Noun-ish rules
		// Recognizes phrases like "no pain"
		DEPNode[] regnodes_DT_NMOD   = new DEPNode[2];
		regnodes_DT_NMOD[0]          = new DEPNode(NULL_ID, fromSet(iv_negDetPlusPrepSet));
//		regnodes_DT_NMOD[0].form     = DependencyRegex.fromSet(iv_negDetPlusPrepSet);
		regnodes_DT_NMOD[0].setLabel(DEP_DET); // was: ("NMOD");
		regnodes_DT_NMOD[1]          = new DEPNode(NULL_ID, ANY_TOKEN);
		regnodes_DT_NMOD[1].pos      = DependencyRegex.ANY_NOUN;
		int cDT_NMOD = 1;

		// Recognizes phrases like "without pain"
		DEPNode[] regnodes_IN_PMOD   = new DEPNode[2];
		regnodes_IN_PMOD[0]          = new DEPNode(NULL_ID, fromSet(iv_negDetPlusPrepSet));
//		regnodes_IN_PMOD[0].form     = DependencyRegex.fromSet(iv_negDetPlusPrepSet);
		regnodes_IN_PMOD[0].setLabel(DEP_PMOD); // was: ("PMOD");
		regnodes_IN_PMOD[1]          = new DEPNode(NULL_ID, ANY_TOKEN);
		regnodes_IN_PMOD[1].pos      = DependencyRegex.ANY_NOUN;
		int cIN_PMOD = 0;
		
		// Recognizes phrases like "no history of FOCUS"
		DEPNode[] regnodes_DT_NN_IN_PMOD = new DEPNode[4];
		regnodes_DT_NN_IN_PMOD[0]        = new DEPNode(NULL_ID, fromSet(iv_negDetPlusPrepSet));
//		regnodes_DT_NN_IN_PMOD[0].form   = DependencyRegex.fromSet(iv_negDetPlusPrepSet);
//		regnodes_DT_NN_IN_PMOD[0].setLabel(DEP_PMOD + "|" + DEP_NMOD); // was: ("[NP]MOD");  // no convincing evidence that this can be restricted among the set of words listed above. (no would be det i think)
		regnodes_DT_NN_IN_PMOD[1]        = new DEPNode(NULL_ID, fromSet(iv_regNounsSet));
//		regnodes_DT_NN_IN_PMOD[1].form   = DependencyRegex.fromSet(iv_regNounsSet);
		regnodes_DT_NN_IN_PMOD[2]        = new DEPNode(NULL_ID, fromSet(iv_regPrepositionsSet));
//		regnodes_DT_NN_IN_PMOD[2].form   = DependencyRegex.fromSet(iv_regPrepositionsSet);
		regnodes_DT_NN_IN_PMOD[2].pos    = "IN";
		regnodes_DT_NN_IN_PMOD[3]        = new DEPNode(NULL_ID, ANY_TOKEN);
		regnodes_DT_NN_IN_PMOD[3].setLabel(DEP_POBJ); // was: ("PMOD");
		int cDT_NN_IN_PMOD = 1;
		
//		// Recognizes negative-adjective phrases like "is negative for carcinoma"
//		DEPNode[] regnodes_JJNEG_COP_IN_PMOD = new DEPNode[4];
//		regnodes_JJNEG_COP_IN_PMOD[0]        = new DEPNode();
//		regnodes_JJNEG_COP_IN_PMOD[0].form   = DependencyRegex.fromSet(iv_negAdjectivesSet);
//		regnodes_JJNEG_COP_IN_PMOD[1]        = new DEPNode();
//		regnodes_JJNEG_COP_IN_PMOD[1].pos    = DependencyRegex.ANY_VERB; // banking that people don't use double negatives
//		regnodes_JJNEG_COP_IN_PMOD[2]        = new DEPNode();
//		regnodes_JJNEG_COP_IN_PMOD[2].form   = DependencyRegex.fromSet(iv_regPrepositionsSet);
//		regnodes_JJNEG_COP_IN_PMOD[2].pos    = "IN";
//		regnodes_JJNEG_COP_IN_PMOD[3]        = new DEPNode();
//		regnodes_JJNEG_COP_IN_PMOD[3].deprel = "PMOD";
//		int cJJNEG_COP_IN_PMOD = 1;

		// Recognizes negative-adjective phrases like "negative for carcinoma"
		DEPNode[] regnodes_JJNEG_AMOD_PMOD = new DEPNode[3];
		regnodes_JJNEG_AMOD_PMOD[0]        = new DEPNode(NULL_ID, fromSet(iv_negAdjectivesSet));
//		regnodes_JJNEG_AMOD_PMOD[0].form   = DependencyRegex.fromSet(iv_negAdjectivesSet);
		regnodes_JJNEG_AMOD_PMOD[1]        = new DEPNode(NULL_ID, fromSet(iv_regPrepositionsSet));
//		regnodes_JJNEG_AMOD_PMOD[1].form   = DependencyRegex.fromSet(iv_regPrepositionsSet);
		regnodes_JJNEG_AMOD_PMOD[1].pos    = "IN";
		regnodes_JJNEG_AMOD_PMOD[2]        = new DEPNode(NULL_ID, ANY_TOKEN);
		regnodes_JJNEG_AMOD_PMOD[2].setLabel(DEP_POBJ); // was: ("PMOD");
		int cJJNEG_AMOD_PMOD = 0;
		
		// Recognizes negative-adjective phrases like "unlikely to have carcinoma"
		// FIXME - not sure this one works correctly -- this example parses weirdly in CVD.
		DEPNode[] regnodes_JJNEG_AMOD_IM_OBJ = new DEPNode[4];
		regnodes_JJNEG_AMOD_IM_OBJ[0]        = new DEPNode(NULL_ID, fromSet(iv_negAdjectivesSet));
//		regnodes_JJNEG_AMOD_IM_OBJ[0].form   = DependencyRegex.fromSet(iv_negAdjectivesSet);
		regnodes_JJNEG_AMOD_IM_OBJ[1]        = new DEPNode(NULL_ID, ANY_TOKEN);
		regnodes_JJNEG_AMOD_IM_OBJ[1].setLabel(DEP_AMOD); // was: ("AMOD");
		regnodes_JJNEG_AMOD_IM_OBJ[2]        = new DEPNode(NULL_ID, ANY_TOKEN);
		regnodes_JJNEG_AMOD_IM_OBJ[2].setLabel(DEP_INFMOD); // was: ("IM");
		regnodes_JJNEG_AMOD_IM_OBJ[3]        = new DEPNode(NULL_ID, ANY_TOKEN);
		regnodes_JJNEG_AMOD_IM_OBJ[3].setLabel(DEP_DOBJ); // was: ("OBJ");
		int cJJNEG_AMOD_IM_OBJ = 0;
		
		// Recognizes negative-adjective phrases like "unlikely carcinoma"
		DEPNode[] regnodes_JJNEG_NN = new DEPNode[2];
		regnodes_JJNEG_NN[0]        = new DEPNode(NULL_ID, fromSet(iv_negAdjectivesSet));
//		regnodes_JJNEG_NN[0].form   = DependencyRegex.fromSet(iv_negAdjectivesSet);
		regnodes_JJNEG_NN[1]        = new DEPNode(NULL_ID, ANY_TOKEN);
		regnodes_JJNEG_NN[1].pos    = DependencyRegex.ANY_NOUN;
		int cJJNEG_NN = 1;
		
		
		// Add the verb-ish rules to the set of regexes to search
		DependencyRegex regex_VBNEG_OBJ = 
				(new DependencyRegex(regnodes_VBNEG_OBJ, cVBNEG_OBJ+1, "NegVerb->Dobj"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_VBNEG_OBJ);
		DependencyRegex regex_PRT_VB_PRD = 
				(new DependencyRegex(regnodes_PRT_VB_PRD, cPRT_VB_PRD+1, "PRT_VB_PRD"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_PRT_VB_PRD);
		DependencyRegex regex_PRT_MOD_VB_OBJ = 
				(new DependencyRegex(regnodes_PRT_MOD_VB_OBJ, cPRT_MOD_VB_OBJ+1, "PRT_MOD_VB_OBJ"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_PRT_MOD_VB_OBJ);
		DependencyRegex regex_PRT_MOD_VB_OBJ_IN_PMOD = 
				(new DependencyRegex(regnodes_PRT_MOD_VB_OBJ_IN_PMOD, cPRT_MOD_VB_OBJ_IN_PMOD+1, "cPRT_MOD_VB_OBJ_IN_PMOD"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_PRT_MOD_VB_OBJ_IN_PMOD);
		DependencyRegex regex_VBN_SBJ = 
				(new DependencyRegex(regnodes_VBN_SBJ, cVBN_SBJ+1, "VBN_SBJ"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_VBN_SBJ);
		
		DependencyRegex regex_PRT_rule_OBJ = 
				(new DependencyRegex(regnodes_PRT_rule_OBJ, cPRT_rule_OBJ+1, "PRT_rule_OBJ"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_PRT_rule_OBJ);

		// Add the noun-ish rules to the set of regexes to search
		DependencyRegex regex_DT_NMOD = 
				(new DependencyRegex(regnodes_DT_NMOD, cDT_NMOD+1, "DT_NMOD"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_DT_NMOD);
		DependencyRegex regex_IN_PMOD = 
				(new DependencyRegex(regnodes_IN_PMOD, cIN_PMOD+1, "IN_PMOD"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_IN_PMOD);
		DependencyRegex regex_DT_NN_IN_PMOD = 
				(new DependencyRegex(regnodes_DT_NN_IN_PMOD, cDT_NN_IN_PMOD+1, "DT_NN_IN_PMOD"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_DT_NN_IN_PMOD);
		DependencyRegex regex_JJNEG_AMOD_PMOD = 
				(new DependencyRegex(regnodes_JJNEG_AMOD_PMOD, cJJNEG_AMOD_PMOD+1, "JJNEG_AMOD_PMOD"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_JJNEG_AMOD_PMOD);
		DependencyRegex regex_JJNEG_AMOD_IM_OBJ = 
				(new DependencyRegex(regnodes_JJNEG_AMOD_IM_OBJ, cJJNEG_AMOD_IM_OBJ+1, "JJNEG_AMOD_IM_OBJ"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_JJNEG_AMOD_IM_OBJ);
		DependencyRegex regex_JJNEG_NN = 
				(new DependencyRegex(regnodes_JJNEG_NN, cJJNEG_NN+1, "JJNEG_NN"))
					.appendOptional( regnodes_NN_CONJ_NN );
		regexSet.add(regex_JJNEG_NN);
//		DependencyRegex regex_INNEG_NN = (
//				new DependencyRegex(regnodes_INNEG_NN, cINNEG_NN+1)).appendOptional( regnodes_NN_CONJ_NN );
//		regexSet.add(regex_INNEG_NN);
	
		// Print out the regexSet for the fun of it!
//		System.out.println("### here are the regexes");
//		for (DependencyRegex dreg : regexSet) {
//			System.out.println(dreg.getName() + " :: " + dreg.toString());
//		}
		
	}


}