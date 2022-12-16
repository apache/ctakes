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
package org.apache.ctakes.assertion.medfacts;

import org.apache.ctakes.assertion.medfacts.types.Concept;
import org.apache.ctakes.assertion.stub.ConceptType;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

@PipeBitInfo(
      name = "Concept Converter",
      description = "Creates a simple Concept for each Identified Annotation.",
      dependencies = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class ConceptConverterAnalysisEngine extends JCasAnnotator_ImplBase
{
  public static final Logger logger = Logger
      .getLogger(ConceptConverterAnalysisEngine.class.getName());

  public ConceptConverterAnalysisEngine()
  {
  }

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException
  {
    logger.info("beginning of ConceptConverterAnalysisEngine.process()");
    String contents = jcas.getDocumentText();

    processForEntityType(jcas, EntityMention.type, EntityMention.class);

    processForEntityType(jcas, EventMention.type, EventMention.class);

    logger.info("end of ConceptConverterAnalysisEngine.process()");
  }

  public void processForEntityType(JCas jcas, int annotationType, Class<? extends IdentifiedAnnotation> annotationClass)
  {
    AnnotationIndex<Annotation> annotationIndex = jcas
        .getAnnotationIndex(annotationType);

    int totalAnnotationCount = jcas.getAnnotationIndex().size();
    int typeSpecificAnnotationCount = annotationIndex.size();

    logger.info(String.format("    total annotation count %d",
        totalAnnotationCount));
    logger.info(String.format("    %s annotation count %d",
        annotationClass.getName(),
        typeSpecificAnnotationCount));

    //logger.info("    before iterating over named entities...");
    for (FeatureStructure featureStructure : annotationIndex)
    {
      //logger.info("    begin single named entity");
      IdentifiedAnnotation annotation = (IdentifiedAnnotation) featureStructure;

      int begin = annotation.getBegin();
      int end = annotation.getEnd();
      String conceptText = annotation.getCoveredText();

      //logger.info(String.format("NAMED ENTITY: \"%s\" [%d-%d]", conceptText,
      //    begin, end));

      Concept concept = new Concept(jcas, begin, end);
      concept.setConceptText(conceptText);
      concept.setConceptType(null);

      concept.setOriginalEntityExternalId(annotation.getAddress());

      FSArray ontologyConceptArray = annotation
          .getOntologyConceptArr();

      ConceptType conceptType = ConceptLookup
          .lookupConceptType(ontologyConceptArray);

      //logger.info(String.format("got concept type: %s", conceptType));

      // now always generating a concept annotation whether or not the
      // conceptType is null (previously, we only generated a concept
      // annotation if the conceptType was not null)
      if (conceptType != null)
      {
        concept.setConceptType(conceptType.toString());
      }
      concept.addToIndexes();

      //logger.info("finished adding new Concept annotation. " + concept);

    }
    //logger.info("    after iterating over named entities.");
  }

}
