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
package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory;
import org.apache.ctakes.dictionary.lookup2.concept.DefaultConcept;
import org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * {@inheritDoc}
 */
@Immutable
final public class DefaultDictionarySpec implements DictionarySpec {

   static private final RareWordDictionary EMPTY_DICTIONARY = new RareWordDictionary() {
      public String getName() {
         return "Empty Dictionary";
      }

      public Collection<RareWordTerm> getRareWordHits( final FastLookupToken fastLookupToken ) {
         return Collections.emptySet();
      }

      public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
         return Collections.emptySet();
      }
   };

   static private final ConceptFactory EMPTY_CONCEPT_FACTORY = new ConceptFactory() {
      public String getName() {
         return "Empty Concept Factory";
      }

      public Concept createConcept( final Long cuiCode ) {
         return new DefaultConcept( CuiCodeUtil.getInstance().getAsCui( cuiCode ) );
      }

      public Map<Long, Concept> createConcepts( final Collection<Long> cuiCodes ) {
         return Collections.emptyMap();
      }
   };

   final private Collection<String> _pairNames;
   final private Map<String, String> _pairDictionaryNames;
   final private Map<String, String> _pairConceptFactoryNames;
   final private Map<String, RareWordDictionary> _dictionaries;
   final private Map<String, ConceptFactory> _conceptFactories;
   final private TermConsumer _termConsumer;

   /**
    * @param termConsumer the consumer to add terms to the Cas
    */
   public DefaultDictionarySpec( final Map<String, String> pairDictionaryNames,
                                 final Map<String, String> pairConceptFactoryNames,
                                 final Map<String, RareWordDictionary> dictionaries,
                                 final Map<String, ConceptFactory> conceptFactories,
                                 final TermConsumer termConsumer ) {
      _pairNames = new HashSet<>( pairDictionaryNames.keySet() );
      _pairNames.addAll( pairConceptFactoryNames.keySet() );
      // TODO check for completion of pairings
      _pairDictionaryNames = Collections.unmodifiableMap( pairDictionaryNames );
      _pairConceptFactoryNames = Collections.unmodifiableMap( pairConceptFactoryNames );
      _dictionaries = Collections.unmodifiableMap( dictionaries );
      _conceptFactories = Collections.unmodifiableMap( conceptFactories );
      _termConsumer = termConsumer;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getPairNames() {
      return _pairNames;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public RareWordDictionary getDictionary( final String pairName ) {
      final String dictionaryName = _pairDictionaryNames.get( pairName );
      if ( dictionaryName != null ) {
         final RareWordDictionary dictionary = _dictionaries.get( dictionaryName );
         if ( dictionary != null ) {
            return dictionary;
         }
      }
      // TODO log
      return EMPTY_DICTIONARY;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ConceptFactory getConceptFactory( final String pairName ) {
      final String conceptFactoryName = _pairConceptFactoryNames.get( pairName );
      if ( conceptFactoryName != null ) {
         final ConceptFactory conceptFactory = _conceptFactories.get( conceptFactoryName );
         if ( conceptFactory != null ) {
            return conceptFactory;
         }
      }
      // TODO log
      return EMPTY_CONCEPT_FACTORY;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordDictionary> getPairedDictionaries( final String conceptFactoryName ) {
      final Collection<RareWordDictionary> dictionaries = new HashSet<>();
      for ( Map.Entry<String, String> pairConceptFactoryName : _pairConceptFactoryNames.entrySet() ) {
         if ( pairConceptFactoryName.getValue().equals( conceptFactoryName ) ) {
            dictionaries.add( getDictionary( pairConceptFactoryName.getKey() ) );
         }
      }
      return dictionaries;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<ConceptFactory> getPairedConceptFactories( final String dictionaryName ) {
      final Collection<ConceptFactory> conceptFactories = new HashSet<>();
      for ( Map.Entry<String, String> pairDictionaryName : _pairDictionaryNames.entrySet() ) {
         if ( pairDictionaryName.getValue().equals( dictionaryName ) ) {
            conceptFactories.add( getConceptFactory( pairDictionaryName.getKey() ) );
         }
      }
      return conceptFactories;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordDictionary> getDictionaries() {
      return new HashSet<>( _dictionaries.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<ConceptFactory> getConceptFactories() {
      return new HashSet<>( _conceptFactories.values() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public TermConsumer getConsumer() {
      return _termConsumer;
   }

}
