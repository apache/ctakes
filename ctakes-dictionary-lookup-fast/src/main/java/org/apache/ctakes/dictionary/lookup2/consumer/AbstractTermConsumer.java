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
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 2/5/14
 */
abstract public class AbstractTermConsumer implements TermConsumer {

   static private final String CODING_SCHEME_PRP_KEY = "codingScheme";

   final private String _codingScheme;


   public AbstractTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      _codingScheme = properties.getProperty( CODING_SCHEME_PRP_KEY );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeHits( final JCas jcas,
                            final RareWordDictionary dictionary,
                            final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                            final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException {
      final String codingScheme = getCodingScheme();
      final Collection<Integer> usedcTakesSemantics = getUsedcTakesSemantics( cuiConcepts );
      // The dictionary may have more than one type, create a map of types to terms and use them all
      final CollectionMap<TextSpan, Long, ? extends Collection<Long>> semanticCuis = new HashSetMap<>();
      for ( Integer cTakesSemantic : usedcTakesSemantics ) {
         semanticCuis.clear();
         for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCuis : textSpanCuis ) {
            for ( Long cuiCode : spanCuis.getValue() ) {
               final Collection<Concept> concepts = cuiConcepts.getCollection( cuiCode );
               if ( hascTakesSemantic( cTakesSemantic, concepts ) ) {
                  semanticCuis.placeValue( spanCuis.getKey(), cuiCode );
               }
            }
         }
         consumeTypeIdHits( jcas, codingScheme, cTakesSemantic, semanticCuis, cuiConcepts );
      }
   }

   protected String getCodingScheme() {
      return _codingScheme;
   }


   static protected Collection<Integer> getUsedcTakesSemantics(
         final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts ) {
      final Collection<Integer> usedSemanticTypes = new HashSet<>();
      for ( Collection<Concept> concepts : cuiConcepts.getAllCollections() ) {
         for ( Concept concept : concepts ) {
            usedSemanticTypes.addAll( concept.getCtakesSemantics() );
         }
      }
      return usedSemanticTypes;
   }

   static protected boolean hascTakesSemantic( final Integer cTakesSemantic, final Iterable<Concept> concepts ) {
      for ( Concept concept : concepts ) {
         if ( concept.getCtakesSemantics().contains( cTakesSemantic ) ) {
            return true;
         }
      }
      return false;
   }


}
