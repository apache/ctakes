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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Ordering;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.Modifier;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

import java.util.*;

/**
 * This baseline links each modifier with the closest entity of a type  
 * that's suitable for degree_of, as long as there is no intervening modifier. 
 */
@PipeBitInfo(
		name = "Degree of Annotator 3",
		description = "Annotates Degree Of relations between two shortest-distance entities in sentences as long as there is no intervening modifier.",
		role = PipeBitInfo.Role.ANNOTATOR,
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
		products = { PipeBitInfo.TypeProduct.DEGREE_RELATION }
)
public class Baseline3DegreeOfRelationExtractorAnnotator extends RelationExtractorAnnotator {
	
	@Override
	public Class<? extends Annotation> getCoveringClass(){
		return Sentence.class;
	}
	
	@Override
	public List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas identifiedAnnotationView, Annotation sentence) {

		List<EntityMention> entityMentions = JCasUtil.selectCovered(
				identifiedAnnotationView,
				EntityMention.class,
				sentence);

		List<Modifier> modifiers = JCasUtil.selectCovered(
		    identifiedAnnotationView,
		    Modifier.class,
		    sentence);
		
		List<EntityMention> entitiesSuitableForDegreeOf = getEntitiesSuitableForDegreeOf(entityMentions);
		
		if((entitiesSuitableForDegreeOf.size() < 1) || (modifiers.size() < 1)) {
		  return new ArrayList<IdentifiedAnnotationPair>();
		}
		
		List<IdentifiedAnnotationPair> result = new ArrayList<IdentifiedAnnotationPair>();
		Set<EntityMention> alreadyLinked = new HashSet<EntityMention>();
		
		for(Modifier modifier : modifiers) {
		  EntityMention nearestEntity = getNearestEntity(identifiedAnnotationView, modifier, entitiesSuitableForDegreeOf);
		  
		  // don't link if there's an another modifier between this one and its nearest entity
		  if(checkForModifierBetween(identifiedAnnotationView, modifier, nearestEntity)) {
		    continue;
		  }
		  
		  // make sure this entity isn't already linked to an anatomical site
		  if(! alreadyLinked.contains(nearestEntity)) {
		    result.add(new IdentifiedAnnotationPair(nearestEntity, modifier));
		    alreadyLinked.add(nearestEntity);
		  }
		}

    return result;
	}
	
  /*
   * Return entity mentions that qualityf to be the arg1 of degree_of relation (i.e. 2, 3)
   */
  private static List<EntityMention> getEntitiesSuitableForDegreeOf(List<EntityMention> entityMentions) {
    
    HashSet<Integer> okArg1Types = new HashSet<Integer>(Arrays.asList(2, 3));
    List<EntityMention> suitableEntities = new ArrayList<EntityMention>();
    
    for(EntityMention entityMention : entityMentions) {
      if(okArg1Types.contains(entityMention.getTypeID())) {
        suitableEntities.add(entityMention);
      }
    }
    
    return suitableEntities;
  }
	
  /*
   * Find the entity nearest to the modifier
   */
	private static EntityMention getNearestEntity(JCas jCas, Modifier modifier, List<EntityMention> entityMentions) {

	  // token distance from modifier to other entity mentions
	  Map<EntityMention, Integer> distanceToEntities = new HashMap<EntityMention, Integer>();

	  for(EntityMention entityMention : entityMentions) {
	    List<BaseToken> baseTokens = JCasUtil.selectBetween(jCas, BaseToken.class, modifier, entityMention);
	    distanceToEntities.put(entityMention, baseTokens.size());
	  }
	  
    List<EntityMention> sortedEntityMentions = new ArrayList<EntityMention>(distanceToEntities.keySet());
    Function<EntityMention, Integer> getValue = Functions.forMap(distanceToEntities);
    Collections.sort(sortedEntityMentions, Ordering.natural().onResultOf(getValue));
    
    return sortedEntityMentions.get(0);
	}
  
  /*
   * Return true if there's a modifier between the given modifier and an entity.
   */
  private static boolean checkForModifierBetween(JCas jCas, Modifier modifier, EntityMention entity) {
    
    List<Modifier> modifiers = JCasUtil.selectBetween(jCas, Modifier.class, modifier, entity);
    if(modifiers.size() > 0) {
      return true;
    } 

    return false;
  }
  
  @Override
  public String classify(List<Feature> features) {
    return "degree_of";
  }
}
