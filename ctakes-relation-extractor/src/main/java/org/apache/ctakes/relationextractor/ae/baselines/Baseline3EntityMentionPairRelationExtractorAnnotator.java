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
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

import java.util.*;

/**
 * This baseline links each anatomical site with the closest entity of a type  
 * that's suitable for location_of, as long as there is no intervening anatomical site. 
 */
@PipeBitInfo(
		name = "Location of Annotator 3",
		description = "Links each anatomical site with the closest entity of a type that's suitable for location_of," +
						  " as long as there is no intervening anatomical site.",
		role = PipeBitInfo.Role.ANNOTATOR,
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
		products = { PipeBitInfo.TypeProduct.LOCATION_RELATION }
)
public class Baseline3EntityMentionPairRelationExtractorAnnotator extends RelationExtractorAnnotator {
	
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
		
		List<EntityMention> anatomicalSites = getAnatomicalSites(entityMentions);
		List<EntityMention> entitiesSuitableForLocationOf = getEntitiesSuitableForLocationOf(entityMentions);
		
		if((anatomicalSites.size() < 1) || (entitiesSuitableForLocationOf.size() < 1)) {
		  return new ArrayList<IdentifiedAnnotationPair>();
		}
		
		List<IdentifiedAnnotationPair> result = new ArrayList<IdentifiedAnnotationPair>();
		Set<EntityMention> alreadyLinked = new HashSet<EntityMention>();
		
		for(EntityMention anatomicalSite : anatomicalSites) {
		  EntityMention nearestEntity = getNearestEntity(identifiedAnnotationView, anatomicalSite, entitiesSuitableForLocationOf);
		  
		  // don't link if there's an another anatomical site between this one and its nearest entity
		  if(checkForAnatomicalSiteBetween(identifiedAnnotationView, anatomicalSite, nearestEntity)) {
		    continue;
		  }
		  
		  // make sure this entity isn't already linked to an anatomical site
		  if(! alreadyLinked.contains(nearestEntity)) {
		    result.add(new IdentifiedAnnotationPair(anatomicalSite, nearestEntity));
		    alreadyLinked.add(nearestEntity);
		  }
		}
		
    return result;
	}

	/*
	 * Return entity mentions that are anatomical types (i.e. typeId is 6)
	 */
	private static List<EntityMention> getAnatomicalSites(List<EntityMention> entityMentions) {
	  
	  List<EntityMention> anatomicalSites = new ArrayList<EntityMention>();
	  
	  for(EntityMention entityMention : entityMentions) {
	    if(entityMention.getTypeID() == 6) {
	      anatomicalSites.add(entityMention);
	    }
	  }
	  
	  return anatomicalSites;
	}
	
  /*
   * Return entity mentions that qualityf to be the arg2 of location_of relation (i.e. 2, 3, or 5)
   */
  private static List<EntityMention> getEntitiesSuitableForLocationOf(List<EntityMention> entityMentions) {
    
    HashSet<Integer> okArg2Types = new HashSet<Integer>(Arrays.asList(2, 3, 5));
    List<EntityMention> suitableEntities = new ArrayList<EntityMention>();
    
    for(EntityMention entityMention : entityMentions) {
      if(okArg2Types.contains(entityMention.getTypeID())) {
        suitableEntities.add(entityMention);
      }
    }
    
    return suitableEntities;
  }
	
  /*
   * Find the entity nearest to the anatomical site
   */
	private static EntityMention getNearestEntity(JCas jCas, EntityMention anatomicalSite, List<EntityMention> entityMentions) {

	  // token distance from anatomical site to other entity mentions
	  Map<EntityMention, Integer> distanceToEntities = new HashMap<EntityMention, Integer>();

	  for(EntityMention entityMention : entityMentions) {
	    List<BaseToken> baseTokens = JCasUtil.selectBetween(jCas, BaseToken.class, anatomicalSite, entityMention);
	    distanceToEntities.put(entityMention, baseTokens.size());
	  }
	  
    List<EntityMention> sortedEntityMentions = new ArrayList<EntityMention>(distanceToEntities.keySet());
    Function<EntityMention, Integer> getValue = Functions.forMap(distanceToEntities);
    Collections.sort(sortedEntityMentions, Ordering.natural().onResultOf(getValue));
    
    return sortedEntityMentions.get(0);
	}
  
  /*
   * Return true if there's an anatomical site entity mention between two entities.
   */
  private static boolean checkForAnatomicalSiteBetween(JCas jCas, EntityMention entity1, EntityMention entity2) {
    
    for(EntityMention entityMention : JCasUtil.selectBetween(jCas, EntityMention.class, entity1, entity2)) {
      if(entityMention.getTypeID() == 6) {
        return true;
      }
    }
    
    return false;
  }
  
  @Override
  public String classify(List<Feature> features) {
    return "location_of";
  }
}
