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
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

import java.util.*;

/**
 * Annotate location_of relation between two entities in sentences with multiple anatomical sites
 * and a single legitimate location_of arg2. Use the pair of arguments that are the closest to each other.
 */
@PipeBitInfo(
      name = "Location of Annotator 2",
      description = "Annotates Location Of relations in sentences containing with multiple anatomical sites.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
      products = { PipeBitInfo.TypeProduct.LOCATION_RELATION }
)
public class Baseline2EntityMentionPairRelationExtractorAnnotator extends RelationExtractorAnnotator {
	
	@Override
	public Class<? extends Annotation> getCoveringClass(){
		return Sentence.class;
	}
	
	@Override
	public List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas identifiedAnnotationView, Annotation sentence) {

		// collect all possible relation arguments from the sentence
		List<EntityMention> args = JCasUtil.selectCovered(
				identifiedAnnotationView,
				EntityMention.class,
				sentence);

		// Create pairings
		List<IdentifiedAnnotationPair> pairs = new ArrayList<IdentifiedAnnotationPair>();
		for (EntityMention arg1 : args) {
			for (EntityMention arg2 : args) {
				if (arg1.getBegin() == arg2.getBegin() && arg1.getEnd() == arg2.getEnd()) {
				  continue;
				}
				pairs.add(new IdentifiedAnnotationPair(arg1, arg2));
			}
		}

		// look for sentences with one legitimate arg2 and multiple anatomical sties (arg1)
		int legitimateArg1Count = 0;
		int legitimateArg2Count = 0;
		for(EntityMention entityMention : args) {
		  if(entityMention.getTypeID() == 6) {
		    legitimateArg1Count++;
		  }
		  HashSet<Integer> okArg2Types = new HashSet<Integer>(Arrays.asList(2, 3, 5));
		  if(okArg2Types.contains(entityMention.getTypeID())) {
		    legitimateArg2Count++;
		  }
		}
		if(! (legitimateArg1Count >= 1 && legitimateArg2Count == 1)) {
		  return new ArrayList<IdentifiedAnnotationPair>();
		}
		
		// compute distance between entities for the pairs where entity types are correct
		HashMap<IdentifiedAnnotationPair, Integer> distanceLookup = new HashMap<IdentifiedAnnotationPair, Integer>();
		for(IdentifiedAnnotationPair pair : pairs) {
		  if(Utils.validateLocationOfArgumentTypes(pair)) {
		    try {
          int distance = Utils.getDistance(identifiedAnnotationView.getView(CAS.NAME_DEFAULT_SOFA), pair);
          distanceLookup.put(pair, distance);
        } catch (CASException e) {
          System.out.println("couldn't get default sofa");
          break;
        }
		  } 
		}
		
		if(distanceLookup.isEmpty()) {
		  return new ArrayList<IdentifiedAnnotationPair>(); // no pairs with suitable argument types
		}
		
		// find the pair where the distance between entities is the smallest and return it
    List<IdentifiedAnnotationPair> rankedPairs = new ArrayList<IdentifiedAnnotationPair>(distanceLookup.keySet());
    Function<IdentifiedAnnotationPair, Integer> getValue = Functions.forMap(distanceLookup);
    Collections.sort(rankedPairs, Ordering.natural().onResultOf(getValue));

    List<IdentifiedAnnotationPair> result = new ArrayList<IdentifiedAnnotationPair>();
    result.add(rankedPairs.get(0));

    System.out.println(sentence.getCoveredText());
    System.out.println("arg1: " + result.get(0).getArg1().getCoveredText());
    System.out.println("arg2: " + result.get(0).getArg2().getCoveredText());
    System.out.println();
    
    return result;
	}

  @Override
  public String classify(List<Feature> features) {
    return "location_of";
  }
}
