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

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Annotate location_of relation between two entities whenever 
 * they are enclosed within the same noun phrse.
 */
@PipeBitInfo(
		name = "Location of Annotator 4",
		description = "Annotates Location Of relations between two entities whenever they are enclosed within the same noun phrase.",
		role = PipeBitInfo.Role.ANNOTATOR,
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
		products = { PipeBitInfo.TypeProduct.LOCATION_RELATION }
)
public class Baseline4EntityMentionPairRelationExtractorAnnotator extends RelationExtractorAnnotator {
	
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

		// find pairs enclosed inside a noun phrase
		List<IdentifiedAnnotationPair> result = new ArrayList<IdentifiedAnnotationPair>();
		for(IdentifiedAnnotationPair pair : pairs) {
		  if(Utils.validateLocationOfArgumentTypes(pair)) {
		    for(TreebankNode nounPhrase : Utils.getNounPhrases(identifiedAnnotationView, (Sentence) sentence)) {
		      if(Utils.isEnclosed(pair, nounPhrase)) {
		        IdentifiedAnnotation arg1 = pair.getArg1();
		        IdentifiedAnnotation arg2 = pair.getArg2();
		        result.add(new IdentifiedAnnotationPair(arg1, arg2));
		        System.out.println("NP: " + nounPhrase.getCoveredText() + ", " + nounPhrase.getBegin() + ", " + nounPhrase.getEnd());
		        System.out.println("arg1: " + arg1.getCoveredText() + ", " + arg1.getBegin() + ", " + arg1.getEnd());
		        System.out.println("arg2: " + arg2.getCoveredText() + ", " + arg2.getBegin() + ", " + arg2.getEnd());
		        System.out.println();
		        break; // don't check other NPs
		      }
		    }
		  }
		}
		
		return result;
	}
		
  @Override
  public String classify(List<Feature> features) {
    return "location_of";
  }
}
