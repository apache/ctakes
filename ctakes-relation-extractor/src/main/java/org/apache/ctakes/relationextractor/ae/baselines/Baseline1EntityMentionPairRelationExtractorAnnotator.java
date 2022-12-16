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

import com.google.common.collect.Lists;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Annotate location_of relation between two entities in sentences containing
 * exactly two entities (where the entities are of the correct types).
 */
@PipeBitInfo(
      name = "Location of Annotator 1",
      description = "Annotates Location Of relations in sentences containing exactly two entities (where the entities are of the correct types).",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
      products = { PipeBitInfo.TypeProduct.LOCATION_RELATION }
)
public class Baseline1EntityMentionPairRelationExtractorAnnotator extends RelationExtractorAnnotator {
	
	@Override
	protected Class<? extends Annotation> getCoveringClass() {
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

		// look for sentence with two entities
		if (args.size() == 2) {
		  EntityMention arg1 = args.get(0);
		  EntityMention arg2 = args.get(1);
		  // there are two entities in this sentence
		  // are they of suitable types for location_of?
		  for (IdentifiedAnnotationPair pair : Lists.newArrayList(
		      new IdentifiedAnnotationPair(arg1, arg2),
		      new IdentifiedAnnotationPair(arg2, arg1))) {
		    if (Utils.validateLocationOfArgumentTypes(pair)) {
	        System.out.println(sentence.getCoveredText());
	        System.out.println("arg1: " + pair.getArg1().getCoveredText());
	        System.out.println("arg2: " + pair.getArg2().getCoveredText());
	        System.out.println();
		      return Lists.newArrayList(pair);
		    }
		  }
		}
		
		
		// for all other cases, return no entity pairs
		return new ArrayList<IdentifiedAnnotationPair>();
	}

	@Override
  public String classify(List<Feature> features) {
    return "location_of";
  }
}
