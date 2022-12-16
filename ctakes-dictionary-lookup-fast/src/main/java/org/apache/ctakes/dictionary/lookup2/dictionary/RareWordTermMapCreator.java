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
package org.apache.ctakes.dictionary.lookup2.dictionary;

import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.collection.ArrayListMap;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Given a collection of {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm} Objects,
 * this factory can create a Map of {@link org.apache.ctakes.dictionary.lookup2.term.RareWordTerm} collections
 * indexed by rare word.
 * This map can be used to create a {@link MemRareWordDictionary}
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/9/14
 */
final public class RareWordTermMapCreator {

   static private final Logger LOGGER = Logger.getLogger( "RareWordTermMapCreator" );

   private RareWordTermMapCreator() {
   }

   static private final Collection<String> PREFIXES = new HashSet<>( Arrays.asList(
         "e-",
         "a-",
         "u-",
         "x-",
         "agro-",
         "ante-",
         "anti-",
         "arch-",
         "be-",
         "bi-",
         "bio-",
         "co-",
         "counter-",
         "cross-",
         "cyber-",
         "de-",
         "eco-",
         "ex-",
         "extra-",
         "inter-",
         "intra-",
         "macro-",
         "mega-",
         "micro-",
         "mid-",
         "mini-",
         "multi-",
         "neo-",
         "non-",
         "over-",
         "pan-",
         "para-",
         "peri-",
         "post-",
         "pre-",
         "pro-",
         "pseudo-",
         "quasi-",
         "re-",
         "semi-",
         "sub-",
         "super-",
         "tri-",
         "ultra-",
         "un-",
         "uni-",
         "vice-",
         // From email from Colin Warner <colinw@ldc.upenn.edu> on 7/25/2010
         "electro-",
         "gasto-",
         "homo-",
         "hetero-",
         "ortho-",
         "phospho-" ) );
//   static private final String[] SUFFIXES = { "-esque", "-ette", "-fest", "-fold", "-gate", "-itis", "-less", "-most",
//                                              "-o-torium", "-rama", "-wise" };

   static private final Collection<String> SUFFIXES = new HashSet<>( Arrays.asList(
         "-esque",
         "-ette",
         "-fest",
         "-fold",
         "-gate",
         "-itis",
         "-less",
         "-most",
         "-o-torium",
         "-rama",
         "-wise" ) );


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
         "and", "or", "but", "for", "nor", "so", "yet",
         // DT  determiner
         "this", "that", "these", "those", "the",
         // EX  existential there
         "there",
         // MD  modal
         "can", "should", "will", "may", "might", "must", "could", "would",
         // PDT  predeterminer
         "some", "any", "all", "both", "half", "none", "twice",
         // PP  prepositional phrase (preposition)
         "at", "before", "after", "behind", "beneath", "beside", "between", "into", "through", "across", "of",
         "concerning", "like", "except", "with", "without", "toward", "to", "past", "against", "during", "until",
         "throughout", "below", "besides", "beyond", "from", "inside", "near", "outside", "since", "upon",
         // PP$  possessive personal pronoun - Brown POS tag, not Penn TreeBank
         "my", "our",
         // PRP  personal pronoun
         "i", "you", "he", "she", "it",
         // PRP$  possesive pronoun
         "mine", "yours", "his", "hers", "its", "ours", "theirs",
         // RP  particle  - this contains some prepositions
         "about", "off", "up", "along", "away", "back", "by", "down", "forward", "in", "on", "out",
         "over", "around", "under",
         // TO  to  - also a preposition
         "to",
         // WDT  wh- determiner
         "what", "whatever", "which", "whichever",
         // WP, WPS  wh- pronoun, nominative wh- pronoun
         "who", "whom", "which", "that", "whoever", "whomever",
         // WRB
         "how", "where", "when", "however", "wherever", "whenever" ) );

   static public CollectionMap<String, RareWordTerm, List<RareWordTerm>> createRareWordTermMap(
         final Iterable<CuiTerm> cuiTerms ) {
      final CollectionMap<String, RareWordTerm, List<RareWordTerm>> rareWordTermMap = new ArrayListMap<>();
      final Map<String, Integer> tokenCountMap = createTokenCountMap( cuiTerms );
      for ( CuiTerm cuiTerm : cuiTerms ) {
         final String term = cuiTerm.getTerm();
         final String rareWord = getRareWord( term, tokenCountMap );
         final int wordIndex = getWordIndex( term, rareWord );
         final int tokenCount = getTokenCount( term );
         if ( wordIndex < 0 ) {
            LOGGER.warn( "Bad Rare Word Index for " + rareWord + " in " + term );
            continue;
         }
         final RareWordTerm rareWordTerm = new RareWordTerm( term, cuiTerm.__cui, rareWord, wordIndex, tokenCount );
         rareWordTermMap.placeValue( rareWord, rareWordTerm );
      }
      return rareWordTermMap;
   }

   static private Map<String, Integer> createTokenCountMap( final Iterable<CuiTerm> cuiTerms ) {
      final Map<String, Integer> tokenCountMap = new HashMap<>();
      for ( CuiTerm cuiTerm : cuiTerms ) {
//         final String[] tokens = LookupUtil.fastSplit( cuiTerm.getTerm(), ' ' );
         final String[] tokens = StringUtil.fastSplit( cuiTerm.getTerm(), ' ' );
         for ( String token : tokens ) {
            if ( isRarableToken( token ) ) {
               // Don't bother to store counts for single-character tokens
               Integer count = tokenCountMap.get( token );
               if ( count == null ) {
                  count = 0;
               }
               tokenCountMap.put( token, (count + 1) );
            }
         }
      }
      return tokenCountMap;
   }

   static private String getRareWord( final String tokenizedTerm, final Map<String, Integer> tokenCountMap ) {
//      final String[] tokens = LookupUtil.fastSplit( tokenizedTerm, ' ' );
      final String[] tokens = StringUtil.fastSplit( tokenizedTerm, ' ' );
      if ( tokens.length == 1 ) {
         return tokens[ 0 ];
      }
      String bestWord = tokens[ 0 ];
      int bestCount = Integer.MAX_VALUE;
      for ( String token : tokens ) {
         if ( isRarableToken( token ) ) {
            Integer count = tokenCountMap.get( token );
            if ( count != null && count < bestCount ) {
               bestWord = token;
               bestCount = count;
            }
         }
      }
      return bestWord;
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
      return !BAD_POS_TERMS.contains( token );
   }

   static private int getWordIndex( final String tokenizedTerm, final String word ) {
      int index = 0;
//      final String[] tokens = LookupUtil.fastSplit( tokenizedTerm, ' ' );
      final String[] tokens = StringUtil.fastSplit( tokenizedTerm, ' ' );
      for ( String token : tokens ) {
         if ( token.equals( word ) ) {
            return index;
         }
         index++;
      }
      return -1;
   }

   static private int getTokenCount( final String tokenizedTerm ) {
//      return LookupUtil.fastSplit( tokenizedTerm, ' ' ).length;
      return StringUtil.fastSplit( tokenizedTerm, ' ' ).length;
   }


   // Can also use:
   // tokenizer = new TokenizerPTB();  List<Token> tokenList = tokenizer.tokenize( term );
   // for( token ) {
   //   startIndex = token.getStartOffset();
   //   endIndex = token.getEndOffset();
   //   tokenText = term.substring( startIndex, endIndex+1 );
   //   sb.append( tokenText ).append( " " );
   // }
   // but what a roundabout!
   static private String getTokenizedTerm( final String term ) {
      if ( term.isEmpty() ) {
         return term;
      }
      final String[] splits = term.split( "\\s+" );
      if ( splits.length == 0 ) {
         return "";
      }
      final StringBuilder sb = new StringBuilder();
      for ( String split : splits ) {
         final List<String> tokens = getTokens( split );
         for ( String token : tokens ) {
            sb.append( token ).append( " " );
         }
      }
      // trim whitespace
      sb.setLength( Math.max( 0, sb.length() - 1 ) );
      return sb.toString();
   }

   // TODO should this be exactly the same as getTokens in TextTokenizer (dictionary gui code)  ? probably ...
   static private List<String> getTokens( final String word ) {
      final List<String> tokens = new ArrayList<>();
      final StringBuilder sb = new StringBuilder();
      final int count = word.length();
      for ( int i = 0; i < count; i++ ) {
         final char c = word.charAt( i );
         if ( Character.isLetterOrDigit( c ) ) {
            sb.append( c );
            continue;
         }
         if ( c == '-' && (isPrefix( sb.toString() ) || isSuffix( word, i + 1 )) ) {
            // what precedes is a prefix or what follows is a suffix so append the dash to the current word and move on
            sb.append( c );
            continue;
         }
         if ( (c == '\'' && isOwnerApostrophe( word, i + 1 ))
               || (c == '.' && isNumberDecimal( word, i + 1 )) ) {
            // what follows is an 's or .# so add the preceding and move on
            if ( sb.length() != 0 ) {
               tokens.add( sb.toString() );
               sb.setLength( 0 );
            }
            sb.append( c );
            continue;
         }
         // Wasn't a special symbol for consideration, so add the previous and symbol separately
         if ( sb.length() != 0 ) {
            tokens.add( sb.toString() );
            sb.setLength( 0 );
         }
         tokens.add( "" + c );
      }
      if ( sb.length() != 0 ) {
         tokens.add( sb.toString() );
      }
      return tokens;
   }

   static private boolean isPrefix( final String word ) {
      return PREFIXES.contains( word + "-" );
   }

   static private boolean isSuffix( final String word, final int startIndex ) {
      if ( word.length() <= startIndex ) {
         return false;
      }
      final String nextCharTerm = getNextCharTerm( word.substring( startIndex ) );
      if ( nextCharTerm.isEmpty() ) {
         return false;
      }
      return SUFFIXES.contains( "-" + nextCharTerm );
   }

   static private boolean isOwnerApostrophe( final CharSequence word, final int startIndex ) {
      return word.length() == startIndex + 1 && word.charAt( startIndex ) == 's';
   }

   static private boolean isNumberDecimal( final CharSequence word, final int startIndex ) {
      // Bizarre scenario in which ctakes tokenizes ".2" as a fraction, but not ".22"
      return word.length() == startIndex + 1 && Character.isDigit( word.charAt( startIndex ) );
   }

   static private String getNextCharTerm( final String word ) {
//      final StringBuilder sb = new StringBuilder();
      final int count = word.length();
//      for ( int i = 0; i < count; i++ ) {
//         final char c = word.charAt( i );
//         if ( !Character.isLetterOrDigit( c ) ) {
//            return sb.toString();
//         }
//         sb.append( c );
//      }
//      return sb.toString();
      for ( int i = 0; i < count; i++ ) {
         final char c = word.charAt( i );
         if ( !Character.isLetterOrDigit( c ) ) {
            return word.substring( 0, i );
         }
      }
      return word;
   }


   static public class CuiTerm {

      final private String __term;
      final private Long __cui;
      final private int __hashcode;

      public CuiTerm( final String cui, final String term ) {
         __term = getTokenizedTerm( term );
         __cui = CuiCodeUtil.getInstance().getCuiCode( cui );
         __hashcode = (__cui + "_" + __term).hashCode();
      }

      public Long getCui() {
         return __cui;
      }

      public String getTerm() {
         return __term;
      }

      public boolean equals( final Object value ) {
         return value instanceof CuiTerm
                && __term.equals( ((CuiTerm)value).__term )
                && __cui.equals( ((CuiTerm)value).__cui );
      }

      public int hashCode() {
         return __hashcode;
      }
   }

}
