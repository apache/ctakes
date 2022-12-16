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
package org.apache.ctakes.relationextractor.ae.baselines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator.IdentifiedAnnotationPair;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.util.JCasUtil;

public class Utils {

  /**
   * Are entity types of the arguments valid for location_of relation? 
   * The following combinations are allowed:
   * 
   * location-of(anatomical site/6, disorder/2)
   * location-of(anatomical site/6, sign/symptom/3)
   * location-of(anatomical site/6, procedure/5)
   */
  public static boolean validateLocationOfArgumentTypes(IdentifiedAnnotationPair pair) {
    
    // allowable arg2 types for location_of
	// Per CTAKES-190, only link anatomical sites in LocationOf to EventMentions, but these three are EventMentions now, 
	// so this check of type ID now already handles if it is an EventMention. 
    HashSet<Integer> okArg2Types = new HashSet<Integer>(Arrays.asList(CONST.NE_TYPE_ID_DISORDER, CONST.NE_TYPE_ID_FINDING, CONST.NE_TYPE_ID_PROCEDURE));
    
    IdentifiedAnnotation arg1 = pair.getArg1(); // Argument (should be anatomical site)
    IdentifiedAnnotation arg2 = pair.getArg2(); // Related_to (should be either disorder, sign/symptom, or procedure)
    int type1 = arg1.getTypeID();
    int type2 = arg2.getTypeID();
    
    if(type1 == CONST.NE_TYPE_ID_ANATOMICAL_SITE && okArg2Types.contains(type2)) {
      return true;
    }
    
    return false;
  }

  /**
   * Are entity types of the arguments valid for degree_of relation? 
   * The following are the valid combinations:
   * 
   * degree-of(disease/disorder/2, modifier)
   * degree-of(sign/symptom/3, modifier)
   */
  public static boolean validateDegreeOfArgumentTypes(IdentifiedAnnotationPair pair) {

    // allowable arg1 types
	// Per CTAKES-190, only link SeverityModifiers to EventMention, but the 2 type IDs below are EventMentions now, 
    // so this check of type ID now already handles if it is an EventMention. 
    HashSet<Integer> okArg1Types = new HashSet<Integer>(Arrays.asList(CONST.NE_TYPE_ID_DISORDER, CONST.NE_TYPE_ID_FINDING));

    IdentifiedAnnotation arg1 = pair.getArg1(); // Argument (should be either disease/disorder or sign/symptom
    int type1 = arg1.getTypeID();

    if(okArg1Types.contains(type1)) {
      return true; // assume arg2 is a modifier
    }

    return false;
  }
  
  /** 
   * Calculate the distance (in tokens) between two identified annotations.
   */
  public static int getDistance(JCas jCas, IdentifiedAnnotationPair pair)  {
    
    List<BaseToken> baseTokens = JCasUtil.selectBetween(jCas, BaseToken.class, pair.getArg1(), pair.getArg2());
    return baseTokens.size();
  }
  
  /**
   * Is this pair of entities enclosed inside a noun phrase?
   */
  public static boolean isEnclosed(IdentifiedAnnotationPair pair, TreebankNode np) {
    
    IdentifiedAnnotation arg1 = pair.getArg1();
    IdentifiedAnnotation arg2 = pair.getArg2();

    if((np.getBegin() <= arg1.getBegin()) &&
        (np.getEnd() >= arg1.getEnd()) &&
        (np.getBegin() <= arg2.getBegin()) &&
        (np.getEnd() >= arg2.getEnd())) {
      return true;
    }
    
    return false;
  }
  
  /**
   * Get all noun phrases in a sentence.
   */
  public static List<TreebankNode> getNounPhrases(JCas identifiedAnnotationView, Sentence sentence) {
    
    List<TreebankNode> nounPhrases = new ArrayList<TreebankNode>();
    List<TreebankNode> treebankNodes;
    try {
      treebankNodes = JCasUtil.selectCovered(
          identifiedAnnotationView.getView(CAS.NAME_DEFAULT_SOFA), 
          TreebankNode.class,
          sentence);
    } catch (CASException e) {
      treebankNodes = new ArrayList<TreebankNode>();
      System.out.println("couldn't get default sofa");
    }
    
    for(TreebankNode treebankNode : treebankNodes) {
      if(treebankNode.getNodeType().equals("NP")) {
        nounPhrases.add(treebankNode);
      }
    }
    
    return nounPhrases;   
  }
}
