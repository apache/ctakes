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
package org.apache.ctakes.dictionary.cased.util.tokenize;

import org.apache.ctakes.dictionary.cased.lookup.CandidateTerm;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Given a collection of {@link CandidateTerm} Objects,
 * this factory can create a Map of {@link RareWordTerm} collections
 * indexed by rare word.
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/9/14
 */
final public class TokenizedTermMapper {

   static private final Logger LOGGER = Logger.getLogger( "TokenizedTermMapper" );

   private TokenizedTermMapper() {
   }


   // LookupDesc for the standard excluded pos tags are
   //   VB,VBD,VBG,VBN,VBP,VBZ,CC,CD,DT,EX,LS,MD,PDT,POS,PP,PP$,PRP,PRP$,RP,TO,WDT,WP,WPS,WRB
   // Listing every verb in the language seems a pain, but listing the others is possible.
   // Verbs should be rare in the dictionaries, excepting perhaps the activity and concept dictionaries
   // CD, CC, DT, EX, MD, PDT, PP, PP$, PRP, PRP$, RP, TO, WDT, WP, WPS, WRB
   // why not WP$ (possessive wh- pronoun "whose")
   // PP$ is a Brown POS tag, not Penn Treebank (as are the rest)
   static private final Collection<String> BAD_POS_TERMS = new HashSet<>( Arrays.asList(
         // CD  cardinal number
         "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
         // CC  coordinating conjunction
         "and", "or", "but", "for", "nor", "so", "yet", "both",
         // DT  determiner
         "this", "that", "these", "those", "the", "all", "an", "another", "any", "each",
         "either", "many", "much", "neither", "no", "some", "such", "that", "the", "them", "these", "this", "those",
         // EX  existential there
         "there",
         // IN
         "among", "upon", "in", "into", "below", "atop", "until", "over", "under", "towards", "to",
         "whether", "despite", "if",
         // MD  modal
         "can", "should", "will", "may", "might", "must", "could", "would", "need", "ought", "shall",
         "cannot", "shouldn",
         // PDT  predeterminer
         "some", "any", "all", "both", "half", "none", "twice",
         // PP  prepositional phrase (preposition)
         "at", "before", "after", "behind", "beneath", "beside", "between", "into", "through", "across", "of",
         "concerning", "like", "except", "with", "without", "toward", "to", "past", "against", "during", "until",
         "throughout", "below", "besides", "beyond", "from", "inside", "near", "outside", "since", "upon",
         // PP$  possessive personal pronoun - Brown POS tag, not Penn TreeBank
         "my", "our",
         // PRP  personal pronoun
         "i", "you", "he", "she", "it", "him", "himself", "we",
         // PRP$  possesive pronoun
         "mine", "yours", "his", "hers", "its", "our", "ours", "theirs",
         // RP  particle  - this contains some prepositions
         "about", "off", "up", "along", "away", "back", "by", "down", "forward", "in", "on", "out",
         "over", "around", "under",
         // TO  to  - also a preposition
         "to",
         // WDT  wh- determiner
         "what", "whatever", "which", "whichever", "that",
         // WP, WPS, WP$  wh- pronoun, nominative wh- pronoun
         "who", "whom", "which", "that", "whoever", "whomever", "whose",
         // WRB
         "how", "where", "when", "however", "wherever", "whenever", "wherein", "why" ) );

   static private final Collection<String> BAD_UPPER_POS_TERMS
         = BAD_POS_TERMS.stream()
                        .map( String::toUpperCase )
                        .collect( Collectors.toSet() );

//   static public Map<String, Collection<CandidateTerm>> createTermMap( final Collection<TokenizedTerm> tokenizedTerms ) {
//      final Map<String, Collection<CandidateTerm>> termMap = new HashMap<>();
//      final Map<String, Long> tokenCountMap = createTokenCountMap( tokenizedTerms );
//      for ( TokenizedTerm tokenizedTerm : tokenizedTerms ) {
//         final String[] tokens = tokenizedTerm.getTokens();
//         final int rareWordIndex = getRareWordIndex( tokens, tokenCountMap );
//         if ( rareWordIndex < 0 ) {
//            LOGGER.warn( "Bad Rare Word Index for " + String.join( " ", tokens ) );
//            continue;
//         }
//         termMap.computeIfAbsent( tokens[ rareWordIndex ], l -> new ArrayList<>() )
//                .add( new CandidateTerm( tokenizedTerm, rareWordIndex ) );
//      }
//      return termMap;
//   }


   static public void createTermMap( final Collection<TokenizedTerm> tokenizedTerms,
                                     final Map<String, Collection<CandidateTerm>> upperTerms,
                                     final Map<String, Collection<CandidateTerm>> mixedTerms,
                                     final Map<String, Collection<CandidateTerm>> lowerTerms ) {
      final Map<String, Long> tokenCountMap = createTokenCountMap( tokenizedTerms );
      for ( TokenizedTerm tokenizedTerm : tokenizedTerms ) {
         final String[] tokens = tokenizedTerm.getTokens();
         final int rareWordIndex = getRareWordIndex( tokens, tokenCountMap );
         if ( rareWordIndex < 0 ) {
            LOGGER.warn( "Bad Rare Word Index for " + String.join( " ", tokens ) );
            continue;
         }
         if ( tokenizedTerm.isAllUpperCase() ) {
            upperTerms.computeIfAbsent( tokens[ rareWordIndex ], l -> new ArrayList<>() )
                      .add( new CandidateTerm( tokenizedTerm, rareWordIndex ) );
         } else if ( tokenizedTerm.isAllLowerCase() ) {
            lowerTerms.computeIfAbsent( tokens[ rareWordIndex ], l -> new ArrayList<>() )
                      .add( new CandidateTerm( tokenizedTerm, rareWordIndex ) );
         } else {
            mixedTerms.computeIfAbsent( tokens[ rareWordIndex ], l -> new ArrayList<>() )
                      .add( new CandidateTerm( tokenizedTerm, rareWordIndex ) );
         }
      }
   }


   static private Map<String, Long> createTokenCountMap( final Collection<TokenizedTerm> tokenizedTerms ) {
      return tokenizedTerms.stream()
                           .map( TokenizedTerm::getTokens )
                           .flatMap( Arrays::stream )
                           .filter( TokenizedTermMapper::isRarableToken )
                           .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }


   static private int getRareWordIndex( final String[] tokens, final Map<String, Long> tokenCountMap ) {
      if ( tokens.length == 1 ) {
         return 0;
      }
      int bestIndex = 0;
      long bestCount = Integer.MAX_VALUE;
      for ( int i = 0; i < tokens.length; i++ ) {
         if ( isRarableToken( tokens[ i ] ) ) {
            final Long count = tokenCountMap.get( tokens[ i ] );
            if ( count != null && count < bestCount ) {
               bestIndex = i;
               bestCount = count;
            }
         }
      }
      return bestIndex;
   }


   static private boolean isRarableToken( final String token ) {
      if ( token.length() <= 1 ) {
         return false;
      }
      boolean hasLetter = false;
      for ( int i = 0; i < token.length(); i++ ) {
         if ( Character.isLetter( token.charAt( i ) ) ) {
            hasLetter = true;
            break;
         }
      }
      if ( !hasLetter ) {
         return false;
      }
      return !BAD_POS_TERMS.contains( token ) && !BAD_UPPER_POS_TERMS.contains( token );
   }


}
