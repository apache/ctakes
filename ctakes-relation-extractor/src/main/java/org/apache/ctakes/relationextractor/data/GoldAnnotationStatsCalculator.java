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
package org.apache.ctakes.relationextractor.data;

import java.util.Collection;
import java.util.List;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.DegreeOfRelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.LocationOfRelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator.IdentifiedAnnotationPair;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Count various stats such as token and relation counts 
 * based on the gold standard data. 
 * 
 * Make sure relationType constant is set to the appropriate relation ("location_of" vs. "degree_of")
 * to make sure the relation-specific statisitics are calculated correctly. 
 *  
 * @author dmitriy dligach
 *
 */
@PipeBitInfo(
		name = "Gold Stats Calculator",
		description = "Count various stats such as token and relation counts based on the gold standard data.",
		role = PipeBitInfo.Role.SPECIAL,
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN,
				PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION,
				PipeBitInfo.TypeProduct.LOCATION_RELATION, PipeBitInfo.TypeProduct.DEGREE_RELATION }
)
public class GoldAnnotationStatsCalculator extends JCasAnnotator_ImplBase {

	public static final String goldViewName = "GoldView";
	public static final String systemViewName = CAS.NAME_DEFAULT_SOFA;
	public static final String targetRelationType = "location_of"; 
	
	public int tokenCount;
	public int sentenceCount;
	public int entityMentionCount;
	public int entityMentionPairCount;
	public int relationArgumentDistance;
	public Multiset<String> relationTypes;
	public Multiset<String> entityMentionPairTypes;
	
	@Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
	  
	  tokenCount = 0;
	  sentenceCount = 0;
	  entityMentionCount = 0;
	  entityMentionPairCount = 0;
	  relationArgumentDistance = 0;
	  relationTypes = HashMultiset.create();
	  entityMentionPairTypes = HashMultiset.create();
	}
  
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {

	  System.out.println();
	  System.out.format("%-30s%d\n", "token count", tokenCount);
	  System.out.format("%-30s%d\n", "sentence count", sentenceCount);
	  System.out.format("%-30s%d\n", "entity mention count", entityMentionCount);
	  System.out.format("%-30s%d\n", "entity mention pair count", entityMentionPairCount);
	  System.out.format("%-30s%d\n", "location_of count", relationTypes.count("location_of"));
	  System.out.format("%-30s%d\n", "degree_of count", relationTypes.count("degree_of"));
	  
	  System.out.println();
	  System.out.format("%-40s%f\n", "average distance between arguments", 
	      (float) relationArgumentDistance / relationTypes.count(targetRelationType));
	  
	  System.out.println();
	  System.out.println("location_of:");
	  System.out.format("%-40s%d\n", "anatomical site - disease/disorder", 
	      entityMentionPairTypes.count("anatomical site - disease/disorder"));
	  System.out.format("%-40s%d\n", "anatomical site - sign/symptom", 
	      entityMentionPairTypes.count("anatomical site - sign/symptom"));
	  System.out.format("%-40s%d\n", "anatomical site - procedure", 
	      entityMentionPairTypes.count("anatomical site - procedure"));
	  
	  System.out.println();
	  System.out.println("degree_of:"); 
	  System.out.format("%-40s%d\n", "disorder - modifier", 
	      entityMentionPairTypes.count("disease/disorder - modifier"));
	  System.out.format("%-40s%d\n", "sign/symptom - modifier", 
	      entityMentionPairTypes.count("sign/symptom - modifier"));
  }
  
	@Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {

    JCas goldView;
    try {
      goldView = jCas.getView(goldViewName);
    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    }	  

    JCas systemView;
    try {
      systemView = jCas.getView(systemViewName);
    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    }
    
    countTokens(jCas); // tokens exist in system view (not in gold)
    countSentences(jCas);
    countEntities(goldView);
    countEntityMentionPairs(jCas, goldView); 
    countDistanceBetweenArguments(systemView, goldView);
    countEntityMentionPairTypes(jCas, goldView);
    countRelationTypes(goldView); 
  }
	
	private void countTokens(JCas jCas) {
    
	  Collection<BaseToken> baseTokens = JCasUtil.select(jCas, BaseToken.class);
	  tokenCount += baseTokens.size();
	}
	
	private void countSentences(JCas jCas) {
	  Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
	  sentenceCount += sentences.size();
	}
	
  private void countEntityMentionPairs(JCas jCas, JCas goldView) {
    
    for(Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
      if(targetRelationType.equals("location_of")) {
        LocationOfRelationExtractorAnnotator emPairAnnot = new LocationOfRelationExtractorAnnotator();
        List<IdentifiedAnnotationPair> pairs = emPairAnnot.getCandidateRelationArgumentPairs(goldView, sentence);
        entityMentionPairCount += pairs.size();
      } 
      if(targetRelationType.equals("degree_of")) {
        DegreeOfRelationExtractorAnnotator degreeOfAnnot = new DegreeOfRelationExtractorAnnotator();
        List<IdentifiedAnnotationPair> pairs = degreeOfAnnot.getCandidateRelationArgumentPairs(goldView, sentence);
        entityMentionPairCount += pairs.size();
      }
    }
  }

  private void countEntityMentionPairTypes(JCas jCas, JCas goldView) {
    
    for(Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
         
      if(targetRelationType.equals("location_of")) {
        LocationOfRelationExtractorAnnotator emPairAnnot = new LocationOfRelationExtractorAnnotator();
        List<IdentifiedAnnotationPair> pairs = emPairAnnot.getCandidateRelationArgumentPairs(goldView, sentence);
        for(IdentifiedAnnotationPair pair : pairs) {
          String type1 = getEntityType(pair.getArg1().getTypeID());
          String type2 = getEntityType(pair.getArg2().getTypeID());
          entityMentionPairTypes.add(type1 + " - " + type2);
        }
      } 
      if(targetRelationType.equals("degree_of")){
        DegreeOfRelationExtractorAnnotator degreeOfAnnot = new DegreeOfRelationExtractorAnnotator();
        List<IdentifiedAnnotationPair> pairs = degreeOfAnnot.getCandidateRelationArgumentPairs(goldView, sentence);
        for(IdentifiedAnnotationPair pair : pairs) {
          String type1 = getEntityType(pair.getArg1().getTypeID());
          entityMentionPairTypes.add(type1 + " - " + "modifier"); // type2 is always modifier for degree_of
        }
      }
    }
  }

	private void countRelationTypes(JCas jCas) {
	  
    for(BinaryTextRelation binaryTextRelation : JCasUtil.select(jCas, BinaryTextRelation.class)) {
      String category = binaryTextRelation.getCategory();
      relationTypes.add(category);
    }
	}

	private void countDistanceBetweenArguments(JCas systemView, JCas goldView) {

	  for(BinaryTextRelation binaryTextRelation : JCasUtil.select(goldView, BinaryTextRelation.class)) {
	    if(binaryTextRelation.getCategory().equals(targetRelationType)) {
	      IdentifiedAnnotation arg1 = (IdentifiedAnnotation) binaryTextRelation.getArg1().getArgument();
	      IdentifiedAnnotation arg2 = (IdentifiedAnnotation) binaryTextRelation.getArg2().getArgument();
	      relationArgumentDistance += getTokenDistance(systemView, arg1, arg2);
	    }
	  }
	}

	private void countEntities(JCas jCas) {
	  
	  Collection<EntityMention> entityMentions = JCasUtil.select(jCas, EntityMention.class);
	  entityMentionCount += entityMentions.size();
	}
	
  public static int getTokenDistance(JCas systemView, IdentifiedAnnotation arg1, IdentifiedAnnotation arg2)  {
    
    List<BaseToken> baseTokens = JCasUtil.selectBetween(systemView, BaseToken.class, arg1, arg2);
    return baseTokens.size();
  }
  
	private static String getEntityType(int typeId) {
	  
	  if(typeId == 0) {
      return "unknown";
    }
	  if(typeId == 1) {
	    return "drug";
	  }
	  if(typeId == 2) {
	    return "disease/disorder";
	  } 
	  if(typeId == 3) {
      return "sign/symptom";
    }
	  if(typeId == 4) {
      return "none";
    }
	  if(typeId == 5) {
      return "procedure";
    }
	  if(typeId == 6) {
      return "anatomical site";
    }
	  return "n/a";
	}
}
