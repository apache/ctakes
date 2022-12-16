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
package org.apache.ctakes.assertion.attributes.subject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.ctakes.assertion.eval.AssertionEvaluation;
import org.apache.ctakes.dependency.parser.util.DependencyPath;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Predicate;
import org.apache.ctakes.typesystem.type.textsem.SemanticArgument;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.JCasUtil;


/**
 * @author stephenwu
 *
 */
public class SubjectAttributeClassifier {

	public static final String DONOR_TOKEN = "donor_token"; 
	public static final String DONOR_SRLARG = "donor_srlarg";
	public static final String DONOR_DEPPATH = "donor_deppath";
	public static final String DONOR_DEPTOK = "donor_depsrl";
	public static final String DONOR_OR = "donor_or";
	public static final String FAMILY_TOKEN = "family_token"; 
	public static final String FAMILY_SRLARG = "family_srlarg";
	public static final String FAMILY_DEPPATH = "family_deppath";
	public static final String FAMILY_DEPTOK = "family_depsrl";
	public static final String FAMILY_OR = "family_or";
	public static final String OTHER_TOKEN = "other_token"; 
	public static final String OTHER_SRLARG = "other_srlarg"; 
	public static final String OTHER_DEPPATH = "other_deppath"; 
	public static final String OTHER_DEPTOK = "other_depsrl";
	public static final String OTHER_OR = "other_or";
    public static ArrayList<String> FeatureIndex = new ArrayList<String>();
    private static Logger logger = Logger.getLogger(SubjectAttributeClassifier.class); 

    static{
            FeatureIndex.add(DONOR_TOKEN);
            FeatureIndex.add(DONOR_SRLARG);
            FeatureIndex.add(DONOR_DEPPATH);
            FeatureIndex.add(DONOR_DEPTOK);
            FeatureIndex.add(DONOR_OR);
            FeatureIndex.add(FAMILY_TOKEN);
            FeatureIndex.add(FAMILY_SRLARG);
            FeatureIndex.add(FAMILY_DEPPATH);
            FeatureIndex.add(FAMILY_DEPTOK);
            FeatureIndex.add(FAMILY_OR);
            FeatureIndex.add(OTHER_TOKEN);
            FeatureIndex.add(OTHER_SRLARG);
            FeatureIndex.add(OTHER_DEPPATH);
            FeatureIndex.add(OTHER_DEPTOK);
            FeatureIndex.add(OTHER_OR);
    }

	// currently goes from entityMention to Sentence to SemanticArgument
	public static String getSubject(JCas jCas, IdentifiedAnnotation mention) {
		
		// Extract the stuff into features
		HashMap<String, Boolean> vfeat = extract(jCas, mention);
		
		// Logic to identify cases, may be replaced by learned classification
		return classifyWithLogic(vfeat);
			
	}


	public static HashMap<String, Boolean> extract(JCas jCas,
			Annotation mention) {
		HashMap<String,Boolean> vfeat = new HashMap<String,Boolean>();
		for (String feat : FeatureIndex) {
			vfeat.put(feat, false);
		}
		
		// find the sentence that entityMention is in
		Sentence sEntity = null;
		Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
		for (Sentence s : sentences) {
			if ( s.getBegin()<=mention.getBegin() && s.getEnd()>=mention.getEnd()) {
				sEntity = s;
				break;
			}
		}
		
		// if there is no sentence, then all these features are null!
		if (sEntity==null) {
//			for ( String feat : FeatureIndex ) {
//				vfeat.put(feat, null);
//			}
//			return vfeat;
			return new HashMap<String,Boolean>();
		}
				
		// get any SRL arguments
		List<SemanticArgument> args = JCasUtil.selectCovered(jCas, SemanticArgument.class, sEntity);
		for (SemanticArgument arg : args) {
			
			// look in SRL arguments for a family or other subject 
			if (arg.getLabel().matches("A[01]")) {
				if ( isDonorTerm(arg) ) {
					vfeat.put(DONOR_SRLARG, true);
				}
				if ( isFamilyTerm(arg) ) {
					vfeat.put(FAMILY_SRLARG, true);
				}
				if ( isOtherTerm(arg) ) {
					vfeat.put(OTHER_SRLARG, true);
				}
			}

		}

		// get any SRL predicates
		List<Predicate> preds = JCasUtil.selectCovered(jCas, Predicate.class, sEntity);

		
		// search dependency paths for stuff
		List<ConllDependencyNode> depnodes = JCasUtil.selectCovered(jCas, ConllDependencyNode.class, mention);
		if (!depnodes.isEmpty()) {
			ConllDependencyNode depnode = DependencyUtility.getNominalHeadNode(depnodes);
			for (ConllDependencyNode dn : DependencyUtility.getPathToTop(jCas, depnode)) {
				if ( isDonorTerm(dn) ) {
					vfeat.put(DONOR_DEPPATH, true);
				}
				if ( isFamilyTerm(dn) ) {
					vfeat.put(FAMILY_DEPPATH, true);
				}
				if ( isOtherTerm(dn) ) {
					vfeat.put(OTHER_DEPPATH, true);
				}
				
			}
		}

		// look for mentions of "donor" in the tokens
		List<BaseToken> toks = JCasUtil.selectCovered(jCas, BaseToken.class, sEntity);
		for (BaseToken tok : toks) {
			
			if ( isDonorTerm(tok) ) {
				vfeat.put(DONOR_TOKEN, true);
				
				// check if there are one-removed dependencies on the dependency path
				DependencyPath path = DependencyUtility.getPath(jCas, DependencyUtility.getNominalHeadNode(jCas,tok), 
						DependencyUtility.getNominalHeadNode(jCas,mention));
				int commonInd = path.indexOf(path.getCommonNode());
				if (commonInd==1 || commonInd==path.size()-2) {
					vfeat.put(DONOR_DEPTOK, true);
				}
			}
			if ( isFamilyTerm(tok) ) {
				vfeat.put(FAMILY_TOKEN, true);

				// check if there are one-removed dependencies on the dependency path
				DependencyPath path = DependencyUtility.getPath(jCas, DependencyUtility.getNominalHeadNode(jCas,tok), 
						DependencyUtility.getNominalHeadNode(jCas,mention));
				
				// 6/28/13 srh fixing for null pointer exception
				if (path != null) {
					int commonInd = path.indexOf(path.getCommonNode());
					if (commonInd==1 || commonInd==path.size()-2) {
						vfeat.put(FAMILY_DEPTOK, true);
					}
				}
			}
			
			if ( isOtherTerm(tok) ) {
				vfeat.put(OTHER_TOKEN, true);

				// check if there are one-removed dependencies on the dependency path
				DependencyPath path = DependencyUtility.getPath(jCas, DependencyUtility.getNominalHeadNode(jCas,tok), 
						DependencyUtility.getNominalHeadNode(jCas,mention));
				int commonInd = path.indexOf(path.getCommonNode());
				if (commonInd==1 || commonInd==path.size()-2) {
					vfeat.put(OTHER_DEPTOK, true);
				}
			}
		}
		return vfeat;
	}
	
	public static String classifyWithLogic(HashMap<String, Boolean> vfeat) {
		
		if (vfeat==null) {
			// if missing values, use default subject value
			logger.warn("Subject attribute classifier missing feature values, defaulting to 'patient'");
			return CONST.ATTR_SUBJECT_PATIENT;
		}

		Boolean donor_summary = new Boolean(vfeat.get(DONOR_TOKEN) || vfeat.get(DONOR_DEPPATH) || 
				vfeat.get(DONOR_DEPTOK) || vfeat.get(DONOR_SRLARG));
		Boolean family_summary = new Boolean(                         vfeat.get(FAMILY_DEPPATH) || 
				vfeat.get(FAMILY_DEPTOK) || vfeat.get(FAMILY_SRLARG));
		Boolean other_summary = new Boolean(                          vfeat.get(OTHER_DEPPATH) || 
				vfeat.get(OTHER_DEPTOK) || vfeat.get(OTHER_SRLARG));
		vfeat.put(DONOR_OR, donor_summary);
		vfeat.put(FAMILY_OR, family_summary);
		vfeat.put(OTHER_OR, other_summary);

		if (vfeat.get(DONOR_OR) && vfeat.get(FAMILY_OR)) {
			return CONST.ATTR_SUBJECT_DONOR_FAMILY_MEMBER;
		} else if (vfeat.get(DONOR_OR) && !vfeat.get(FAMILY_OR)) {
			return CONST.ATTR_SUBJECT_DONOR_OTHER;
		} else if (!vfeat.get(DONOR_OR) && !vfeat.get(FAMILY_OR) && vfeat.get(OTHER_OR)) {
			return CONST.ATTR_SUBJECT_OTHER;
		} else if (!vfeat.get(DONOR_OR) && vfeat.get(FAMILY_OR)) {
			return (CONST.ATTR_SUBJECT_FAMILY_MEMBER);
		} else {
			return CONST.ATTR_SUBJECT_PATIENT;
		}

	}


	public static boolean isDonorTerm(Annotation arg) {
		return arg.getCoveredText().toLowerCase()
		.matches("(donor).*");
	}

	
	public static boolean isFamilyTerm(Annotation arg) {
		return arg.getCoveredText().toLowerCase()
		.matches("(father|dad|mother|mom|bro|sis|sib|cousin|aunt|uncle|grandm|grandp|grandf|" +
				"wife|spouse|husband|child|offspring|progeny|son|daughter|nephew|niece|kin|family).*");
	}


	public static boolean isOtherTerm(Annotation arg) {
		return arg.getCoveredText().toLowerCase()
		.matches(".*(in-law|stepc|stepd|stepso|stepf|stepm|step-).*");
	}


	// a main method for regex testing
	public static void main(String[] args) {
		String s = "steps";
		if (s.toLowerCase().matches(".*(in-law|stepc|stepd|stepso|stepf|stepm|step-).*")) {
			System.out.println("match");
		} else {
			System.out.println("no match");
		}
	}
}
