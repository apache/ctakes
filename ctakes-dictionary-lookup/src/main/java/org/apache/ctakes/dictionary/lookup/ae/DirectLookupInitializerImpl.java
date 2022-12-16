/**
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
package org.apache.ctakes.dictionary.lookup.ae;

import org.apache.ctakes.dictionary.lookup.DictionaryEngine;
import org.apache.ctakes.dictionary.lookup.algorithms.DirectPassThroughImpl;
import org.apache.ctakes.dictionary.lookup.algorithms.LookupAlgorithm;
import org.apache.ctakes.dictionary.lookup.phrasebuilder.PhraseBuilder;
import org.apache.ctakes.dictionary.lookup.phrasebuilder.VariantPhraseBuilderImpl;
import org.apache.ctakes.dictionary.lookup.vo.LookupAnnotation;
import org.apache.ctakes.dictionary.lookup.vo.LookupToken;
import org.apache.ctakes.typesystem.type.syntax.*;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.JCasUtil;

import java.util.*;

/**
 * @author Mayo Clinic
 */
public class DirectLookupInitializerImpl implements LookupInitializer {
   private final String CANONICAL_VARIANT_ATTR = "canonicalATTR";

   // LOG4J logger based on class name
   final private Logger iv_logger = Logger.getLogger( getClass().getName() );

   public DirectLookupInitializerImpl( final UimaContext uimaContext, final Properties props ) {
      // TODO property validation could be done here
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public LookupAlgorithm getLookupAlgorithm( DictionaryEngine dictEngine ) throws AnnotatorInitializationException {
      // variant support
      final String[] variantArr = {CANONICAL_VARIANT_ATTR};
      final PhraseBuilder pb = new VariantPhraseBuilderImpl( variantArr, true );
      return new DirectPassThroughImpl( dictEngine, pb );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<LookupToken> getLookupTokenIterator( final JCas jcas ) throws AnnotatorInitializationException {
      final List<LookupToken> ltList = new ArrayList<LookupToken>();

      final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
      final AnnotationIndex<Annotation> annotationIndex = indexes.getAnnotationIndex( BaseToken.type );
      for ( Annotation annotation : annotationIndex ) {
         if ( !(annotation instanceof BaseToken) ) {
            iv_logger.warn( getClass().getName() + " getLookupTokenIterator(..) Annotation is not a BaseToken" );
            continue;
         }
         final boolean isNonLookup = annotation instanceof NewlineToken
               || annotation instanceof PunctuationToken
               || annotation instanceof ContractionToken
               || annotation instanceof SymbolToken;
         if ( isNonLookup ) {
            continue;
         }
         final BaseToken bta = (BaseToken) annotation;
         final LookupToken lt = new LookupAnnotationToJCasAdapter( bta );
         if ( bta instanceof WordToken ) {
            final WordToken wta = (WordToken) bta;
            final String canonicalForm = wta.getCanonicalForm();
            if ( canonicalForm != null ) {
               lt.addStringAttribute( CANONICAL_VARIANT_ATTR, canonicalForm );
            }
         }
         ltList.add( lt );
      }
      return ltList.iterator();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<Annotation> getLookupWindowIterator( final JCas jcas ) throws AnnotatorInitializationException {
      final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
      return indexes.getAnnotationIndex( Sentence.type ).iterator();
   }

   /**
    * returns an empty map
    * {@inheritDoc}
    */
   @Override
   public Map<String, List<LookupAnnotation>> getContextMap( final JCas jcas,
                                                             final int windowBegin,
                                                             final int windowEnd ) {
      // not used for direct pass through algorithm, return empty map
      return Collections.emptyMap();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<LookupToken> getSortedLookupTokens( final JCas jcas,
                                                   final Annotation annotation ) throws AnnotatorInitializationException {
      final List<LookupToken> ltList = new ArrayList<LookupToken>();
      final List<BaseToken> inList = JCasUtil.selectCovered( jcas, BaseToken.class, annotation );
      for ( BaseToken bta : inList ) {
         final boolean isNonLookup = bta instanceof NewlineToken
               || bta instanceof PunctuationToken
               || bta instanceof ContractionToken
               || bta instanceof SymbolToken;
         if ( isNonLookup ) {
            continue;
         }
         final LookupToken lt = new LookupAnnotationToJCasAdapter( bta );
         if ( bta instanceof WordToken ) {
            WordToken wta = (WordToken) bta;
            String canonicalForm = wta.getCanonicalForm();
            if ( canonicalForm != null ) {
               lt.addStringAttribute( CANONICAL_VARIANT_ATTR, canonicalForm );
            }
         }
         ltList.add( lt );
      }
      return ltList;
   }

}
