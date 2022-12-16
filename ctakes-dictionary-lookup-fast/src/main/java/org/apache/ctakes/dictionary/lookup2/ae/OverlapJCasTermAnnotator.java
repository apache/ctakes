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
package org.apache.ctakes.dictionary.lookup2.ae;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.textspan.DefaultTextSpan;
import org.apache.ctakes.dictionary.lookup2.textspan.MultiTextSpan;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Checks for terms that overlap a window.  All tokens of the term must exist in the window in order,
 * but not necessarily contiguously
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 12/6/13
 */
@PipeBitInfo(
      name = "Dictionary Lookup (Overlap)",
      description = "Annotates clinically-relevant terms.  Terms can overlap dictionary entries.",
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION
)
final public class OverlapJCasTermAnnotator extends AbstractJCasTermAnnotator {

   // LOG4J logger based on interface name
   final private Logger _logger = Logger.getLogger( "OverlapJCasTermAnnotator" );

   /**
    * specifies the number of consecutive non-comma tokens that can be skipped
    */
   static private final String CONS_SKIP_PRP_KEY = "consecutiveSkips";
   /**
    * specifies the number of total tokens that can be skipped
    */
   static private final String TOTAL_SKIP_PRP_KEY = "totalTokenSkips";


   @ConfigurationParameter( name = CONS_SKIP_PRP_KEY, mandatory = false,
         description = "Number of consecutive non-comma tokens that can be skipped" )
   private int _consecutiveSkipMax = 2;

   @ConfigurationParameter( name = TOTAL_SKIP_PRP_KEY, mandatory = false,
         description = "Number of total tokens that can be skipped" )
   private int _totalSkipMax = 4;


   /**
    * Set the number of consecutive and total tokens that can be skipped (optional).  Defaults are 2 and 4.
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      _logger.info( "Maximum consecutive tokens that can be skipped: " + _consecutiveSkipMax );
      _logger.info( "Maximum tokens that can be skipped: " + _totalSkipMax );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void findTerms( final RareWordDictionary dictionary,
                          final List<FastLookupToken> allTokens,
                          final List<Integer> lookupTokenIndices,
                          final CollectionMap<TextSpan, Long, ? extends Collection<Long>> termsFromDictionary ) {
      Collection<RareWordTerm> rareWordHits;
      for ( Integer lookupTokenIndex : lookupTokenIndices ) {
         final FastLookupToken lookupToken = allTokens.get( lookupTokenIndex );
         rareWordHits = dictionary.getRareWordHits( lookupToken );
         if ( rareWordHits == null || rareWordHits.isEmpty() ) {
            continue;
         }
         for ( RareWordTerm rareWordHit : rareWordHits ) {
            if ( rareWordHit.getText().length() < _minimumLookupSpan ) {
               continue;
            }
            if ( rareWordHit.getTokenCount() == 1 ) {
               // Single word term, add and move on
               termsFromDictionary.placeValue( lookupToken.getTextSpan(), rareWordHit.getCuiCode() );
               continue;
            }
            final int termStartIndex = lookupTokenIndex - rareWordHit.getRareWordIndex();
            if ( termStartIndex < 0 || termStartIndex + rareWordHit.getTokenCount() > allTokens.size() ) {
               // term will extend beyond window
               continue;
            }
            final TextSpan overlapSpan = getOverlapTerm( allTokens, lookupTokenIndex, rareWordHit,
                  _consecutiveSkipMax, _totalSkipMax );
            if ( overlapSpan != null ) {
               termsFromDictionary.placeValue( overlapSpan, rareWordHit.getCuiCode() );
            }
         }
      }
   }


   /**
    * Check to see if a given term overlaps a set of tokens
    *
    * @param allTokens        all tokens in a window
    * @param lookupTokenIndex index of rare word in the window of all tokens
    * @param rareWordHit      some possible term
    * @return a spanned term that is in the window in some overlapping manner, or null
    */
   static private TextSpan getOverlapTerm( final List<FastLookupToken> allTokens, final int lookupTokenIndex,
                                           final RareWordTerm rareWordHit,
                                           final int consecutiveSkipMax, final int totalSkipMax ) {
      final String[] hitTokens = rareWordHit.getTokens();
      final List<TextSpan> missingSpanKeys = new ArrayList<>();
      int consecutiveSkips = 0;
      int totalSkips = 0;
      int firstWordIndex = -1;
      if ( rareWordHit.getRareWordIndex() == 0 ) {
         firstWordIndex = lookupTokenIndex;
      } else {
         int nextRareWordIndex = rareWordHit.getRareWordIndex() - 1;
         for ( int allTokensIndex = lookupTokenIndex - 1; allTokensIndex >= 0; allTokensIndex-- ) {
            if ( hitTokens[ nextRareWordIndex ].equals( allTokens.get( allTokensIndex ).getText() )
                 || hitTokens[ nextRareWordIndex ].equals( allTokens.get( allTokensIndex ).getVariant() ) ) {
               nextRareWordIndex--;
               if ( nextRareWordIndex < 0 ) {
                  firstWordIndex = allTokensIndex;
                  break;
               }
               consecutiveSkips = 0;
               continue;
            }
            missingSpanKeys.add( allTokens.get( allTokensIndex ).getTextSpan() );
            if ( !allTokens.get( allTokensIndex ).getText().equals( "," ) ) {
               // things like "blood, urine, sputum cultures" should pick up "blood culture" and "urine culture"
               consecutiveSkips++;
               if ( consecutiveSkips > consecutiveSkipMax ) {
                  break;
               }
            }
            totalSkips++;
            if ( totalSkips > totalSkipMax ) {
               break;
            }
         }
         if ( firstWordIndex == -1 ) {
            return null;
         }
      }
      int lastWordIndex = -1;
      if ( rareWordHit.getRareWordIndex() == rareWordHit.getTokenCount() - 1 ) {
         lastWordIndex = lookupTokenIndex;
      } else {
         consecutiveSkips = 0;
         int nextRareWordIndex = rareWordHit.getRareWordIndex() + 1;
         for ( int allTokensIndex = lookupTokenIndex + 1; allTokensIndex < allTokens.size(); allTokensIndex++ ) {
            if ( hitTokens[ nextRareWordIndex ].equals( allTokens.get( allTokensIndex ).getText() )
                 || hitTokens[ nextRareWordIndex ].equals( allTokens.get( allTokensIndex ).getVariant() ) ) {
               nextRareWordIndex++;
               if ( nextRareWordIndex >= rareWordHit.getTokenCount() ) {
                  lastWordIndex = allTokensIndex;
                  break;
               }
               consecutiveSkips = 0;
               continue;
            }
            missingSpanKeys.add( allTokens.get( allTokensIndex ).getTextSpan() );
            consecutiveSkips++;
            if ( consecutiveSkips > consecutiveSkipMax ) {
               break;
            }
            totalSkips++;
            if ( totalSkips > totalSkipMax ) {
               break;
            }
         }
         if ( lastWordIndex == -1 ) {
            return null;
         }
      }
      if ( missingSpanKeys.isEmpty() ) {
         return new DefaultTextSpan( allTokens.get( firstWordIndex ).getStart(),
               allTokens.get( lastWordIndex ).getEnd() );
      }
      return new MultiTextSpan( allTokens.get( firstWordIndex ).getStart(),
            allTokens.get( lastWordIndex ).getEnd(), missingSpanKeys );
   }


   static public AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( OverlapJCasTermAnnotator.class );
   }

   // TODO: Ambiguous constructor for AnalysisEngineDescription named as AnnotatorDescription. Consider refactor
   //
   // TODO: Create UTest for deprecated JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY
   // Make sure deprecated JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY was correctly replaced by
   //   ConfigParameterConstants.PARAM_LOOKUP_XML
   static public AnalysisEngineDescription createAnnotatorDescription( final String descriptorPath )
         throws ResourceInitializationException {

      return AnalysisEngineFactory.createEngineDescription( OverlapJCasTermAnnotator.class,
              ConfigParameterConstants.PARAM_LOOKUP_XML, descriptorPath );
   }

   // TODO: Ambiguous constructor for AnalysisEngineDescription named as AnnotatorDescription. Consider refactor
   //
   // TODO: Create UTest for deprecated JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY
   // Make sure deprecated JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY was correctly replaced by
   //   ConfigParameterConstants.PARAM_LOOKUP_XML
   static public AnalysisEngineDescription createAnnotatorDescription(final String descriptorPath,
                                                                   final int consecutiveSkipMax,
                                                                   final int totalSkipMax )
      throws ResourceInitializationException {

      return AnalysisEngineFactory.createEngineDescription( OverlapJCasTermAnnotator.class,
           ConfigParameterConstants.PARAM_LOOKUP_XML, descriptorPath,
           CONS_SKIP_PRP_KEY, consecutiveSkipMax,
           TOTAL_SKIP_PRP_KEY, totalSkipMax );
   }

}
