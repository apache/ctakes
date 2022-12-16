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
package org.apache.ctakes.relationextractor.ae;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.ManagesTreatsTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Identifies manages/treats relation.
 * 
 * arg1: drug, procedure
 * arg2: disease/disorder, sign/symptom, anatomical site
 */
@PipeBitInfo(
      name = "Manages / Treats Annotator",
      description = "Annotates Manages / Treats relations.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
      products = { PipeBitInfo.TypeProduct.GENERIC_RELATION }
)
public class ManagesTreatsRelationExtractorAnnotator extends RelationExtractorAnnotator {

  @Override
  protected Class<? extends BinaryTextRelation> getRelationClass() {
    return ManagesTreatsTextRelation.class;
  }

  @Override
  public List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
      JCas identifiedAnnotationView,
      Annotation sentence) {

    List<IdentifiedAnnotation> arg1s = new ArrayList<>();
    List<MedicationMention> medications = 
        JCasUtil.selectCovered(identifiedAnnotationView, MedicationMention.class, sentence); 
    List<ProcedureMention> procedures = 
        JCasUtil.selectCovered(identifiedAnnotationView, ProcedureMention.class, sentence);
    arg1s.addAll(medications);
    arg1s.addAll(procedures);
    
    List<IdentifiedAnnotation> arg2s = new ArrayList<>();
    List<DiseaseDisorderMention> diseaseDisorders = 
        JCasUtil.selectCovered(identifiedAnnotationView, DiseaseDisorderMention.class, sentence);
    List<SignSymptomMention> signSymptoms = 
        JCasUtil.selectCovered(identifiedAnnotationView, SignSymptomMention.class, sentence);
    List<AnatomicalSiteMention> anatomicalSites =  
        JCasUtil.selectCovered(identifiedAnnotationView, AnatomicalSiteMention.class, sentence);
    arg2s.addAll(diseaseDisorders);
    arg2s.addAll(signSymptoms);
    arg2s.addAll(anatomicalSites);
    
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    for(IdentifiedAnnotation arg1 : arg1s) {
      for(IdentifiedAnnotation arg2 : arg2s) {
        pairs.add(new IdentifiedAnnotationPair(arg1, arg2));
      }
    }
    
    return pairs;
  }

  @Override
  protected void createRelation(
      JCas jCas,
      IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2,
      String predictedCategory) {
    RelationArgument relArg1 = new RelationArgument(jCas);
    relArg1.setArgument(arg1);
    relArg1.setRole("Argument");
    relArg1.addToIndexes();
    RelationArgument relArg2 = new RelationArgument(jCas);
    relArg2.setArgument(arg2);
    relArg2.setRole("Related_to");
    relArg2.addToIndexes();
    ManagesTreatsTextRelation relation = new ManagesTreatsTextRelation(jCas);
    relation.setArg1(relArg1);
    relation.setArg2(relArg2);
    relation.setCategory(predictedCategory);
    relation.addToIndexes();
  }

  @Override
  protected Class<? extends Annotation> getCoveringClass() {
    return Sentence.class;
  }
}
