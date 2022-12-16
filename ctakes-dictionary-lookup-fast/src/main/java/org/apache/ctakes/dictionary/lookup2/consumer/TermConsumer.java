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
package org.apache.ctakes.dictionary.lookup2.consumer;

import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * Stores terms in the cas
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 12/5/13
 */
public interface TermConsumer {


   /**
    * @param jcas         -
    * @param dictionary   the dictionary: Anatomical Site, Disease/Disorder, Drug, combination, etc.
    * @param textSpanCuis collection of discovered terms
    * @param cuiConcepts  map of cuis to concepts
    * @throws AnalysisEngineProcessException
    */
   void consumeHits( final JCas jcas,
                     final RareWordDictionary dictionary,
                     final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                     final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException;

   /**
    * @param jcas           -
    * @param codingScheme   - name of the coding scheme to use if no secondary (snomed, rxnorm) schemes exist
    * @param cTakesSemantic cTakes IdentifiedAnnotation only accepts an integer as a cTakesSemantic
    * @throws org.apache.uima.analysis_engine.AnalysisEngineProcessException
    */
   void consumeTypeIdHits( final JCas jcas, final String codingScheme, final int cTakesSemantic,
                           final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                           final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException;

}
