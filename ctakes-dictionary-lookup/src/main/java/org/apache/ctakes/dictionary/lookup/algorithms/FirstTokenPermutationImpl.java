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
package org.apache.ctakes.dictionary.lookup.algorithms;

import org.apache.ctakes.dictionary.lookup.DictionaryEngine;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.phrasebuilder.PhraseBuilder;
import org.apache.ctakes.dictionary.lookup.vo.LookupAnnotation;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupToken;
import org.apache.log4j.Logger;

import java.util.*;


/**
 * <b>OVERVIEW: </b> Each LookupToken is fed into a "first token" Dictionary. A
 * hit indicates an anchor and the window around this anchor is based on
 * context. This hit also contains all the presentations from the Dictionary
 * where the "first token" is contained.
 * <p/>
 * The window is determined by finding the largest overlapping context window
 * annotation. Permutations of LookupTokens found within this window are used to
 * match against the presentations found earlier. If context window annotations
 * are not provided, a fixed window is used based on the specified max
 * permutation level.
 * <p/>
 * <b>OPTIONAL CONTEXT: </b> context window annotations
 *
 * @author Mayo Clinic
 */
public class FirstTokenPermutationImpl implements LookupAlgorithm {
   // LOG4J logger based on class name
   final private Logger iv_logger = Logger.getLogger( getClass().getName() );

   /**
    * Key value for context map. Value is expected to be a List of
    * LookupAnnotation objects in sorted order.
    */
   public static final String CTX_KEY_WINDOW_ANNOTATIONS = "WINDOW_ANNOTATIONS";

   /**
    * Key value for LookupToken attribute. Value is expected to be either TRUE
    * or FALSE. This indicates whether to use this token for a "first token"
    * lookup or not. This is optional.
    */
   public static final String LT_KEY_USE_FOR_LOOKUP = "USE_FOR_LOOKUP";

   final private DictionaryEngine iv_firstTokenDictEngine;
   final private PhraseBuilder iv_phrBuilder;

   final private int iv_maxPermutationLevel;
   // key = level Integer, value = Permutation list
   final private Map<Integer, List<List<Integer>>> iv_permCacheMap;

   private String[] iv_textMetaFieldNames;

   /**
    * Constructor
    *
    * @param firstTokenDictEngine Dictionary that is indexed against first tokens.
    * @param phraseBuilder        Builds phrases to match against Dictionary.
    * @param textMetaFieldNames   MetaFieldNames used to extract presentations.
    * @param maxPermutationLevel  Max permutation Level allowed.
    */
   public FirstTokenPermutationImpl( final DictionaryEngine firstTokenDictEngine,
                                     final PhraseBuilder phraseBuilder,
                                     final String textMetaFieldNames[],
                                     final int maxPermutationLevel ) {
      iv_firstTokenDictEngine = firstTokenDictEngine;
      iv_phrBuilder = phraseBuilder;
      iv_textMetaFieldNames = textMetaFieldNames;

      iv_maxPermutationLevel = maxPermutationLevel;
      iv_permCacheMap = new HashMap<>( maxPermutationLevel );
      for ( int i = 0; i <= maxPermutationLevel; i++ ) {
         final List<List<Integer>> permList = PermutationUtil.getPermutationList( i );
         iv_permCacheMap.put( i, permList );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<LookupHit> lookup( final List<LookupToken> lookupTokenList,
                                        final Map<String, List<LookupAnnotation>> contextMap ) throws Exception {
      // setup optional window context data
      final List<LookupAnnotation> windowAnnotations = getWindowAnnotations( contextMap );
      final boolean useWindowAnnots = !windowAnnotations.isEmpty();
      // map of all the annotation start indices as keys and the annotations with those indices as values
      final Map<Integer, List<LookupAnnotation>> wStartOffsetMap = getMultipleStartOffsetMap( windowAnnotations );
      // map of all the annotation end indices as keys and the annotations with those indices as values
      final Map<Integer, List<LookupAnnotation>> wEndOffsetMap = getMultipleEndOffsetMap( windowAnnotations );
      // map of all lookupTokens and their index within the lookupTokenList.  Faster for fetching than List.indexOf(..)
      final Map<LookupToken, Integer> ltListIndexMap = getListIndexMap( lookupTokenList );
      // map of all the token start indices as keys and the tokens with those indices as values
      final Map<Integer, List<LookupToken>> ltStartOffsetMap = getMultipleStartOffsetMap( lookupTokenList );
      // map of all the token end indices as keys and the tokens with those indices as values
      final Map<Integer, List<LookupToken>> ltEndOffsetMap = getMultipleEndOffsetMap( lookupTokenList );

      final List<LookupHit> lookupHits = new ArrayList<>();
      for ( int currentIndex = 0; currentIndex < lookupTokenList.size(); currentIndex++ ) {
         final LookupToken lookupToken = lookupTokenList.get( currentIndex );
         final String useForLookupString = lookupToken.getStringAttribute( LT_KEY_USE_FOR_LOOKUP );
         final boolean useForLookup = Boolean.valueOf( useForLookupString );
         if ( !useForLookup ) {
            continue;
         }
         final Collection<MetaDataHit> firstTokenHits = getFirstTokenHits( lookupToken );
         if ( firstTokenHits == null || firstTokenHits.isEmpty() ) {
            continue;
         }
         int wEndOffset = -1;
         if ( useWindowAnnots ) {
            // get the largest overlapping window annotation
            final LookupAnnotation windowAnnotation = getLargestWindowAnnotation( currentIndex, lookupToken,
                                                                                 ltStartOffsetMap, ltEndOffsetMap,
                                                                                 ltListIndexMap,
                                                                                 wStartOffsetMap, wEndOffsetMap );
            if ( windowAnnotation != null ) {
               wEndOffset = windowAnnotation.getEndOffset();
            }
         }
         if ( wEndOffset == -1 ) {
            iv_logger.debug( "Window size set to max perm level." );
            wEndOffset = getFixedWindowEndOffset( currentIndex, lookupToken, lookupTokenList );
         }
         final List<LookupToken> endLookupTokenList = getLookupTokenList( wEndOffset, ltEndOffsetMap, false );
         if ( endLookupTokenList.isEmpty() ) {
            iv_logger.debug( "Invalid window:" + currentIndex + "," + wEndOffset );
            continue;
         }
         final LookupToken endLookupToken = endLookupTokenList.get( endLookupTokenList.size() - 1 );
         final int startTokenIndex = currentIndex;
         final int endTokenIndex = ltListIndexMap.get( endLookupToken );
         // list of LookupToken objects bound by the window
         final List<LookupToken> wLookupTokenList = lookupTokenList.subList( startTokenIndex, endTokenIndex + 1 );
         // use permutation algorithm to find any hits inside the window
         // Note: currentIndex - startTokenIndex is always = 0. What was the intention?  12-26-2012 SPF
         final Collection<LookupHit> lhCol = getLookupHits( firstTokenHits, wLookupTokenList,
                                                            currentIndex - startTokenIndex );
         lookupHits.addAll( lhCol );
      }
      return lookupHits;
   }

   private Map<String,Set<MetaDataHit>> getNamedMetaDataHits( final Collection<MetaDataHit> firstTokenHits ) {
      final Map<String,Set<MetaDataHit>> namedMetaDataHits = new HashMap<>();
      for ( MetaDataHit firstTokenHit : firstTokenHits ) {
         for ( String name : iv_textMetaFieldNames ) {
            String text = firstTokenHit.getMetaFieldValue( name );
            if ( text != null ) {
               text = text.toLowerCase();
               Set<MetaDataHit> mdhSet = namedMetaDataHits.get( text );
               if ( mdhSet == null ) {
                  mdhSet = new HashSet<>();
               }
               mdhSet.add( firstTokenHit );
               namedMetaDataHits.put( text, mdhSet );
            } else {
               if ( iv_logger.isDebugEnabled() ) {
                  iv_logger.debug( "MetaField " + name + " contains no data." );
               }
            }
         }
      }
      return namedMetaDataHits;
   }

   private Collection<LookupHit> getLookupHits( final Collection<MetaDataHit> firstTokenHits,
                                                final List<LookupToken> wLookupTokenList,
                                                final int firstTokenIndex ) throws Exception {
      if ( wLookupTokenList.size() - 1 > iv_maxPermutationLevel ) {
         iv_logger.debug( "Beyond permutation cache size." );
         return Collections.emptyList();
      }
      final Map<String,Set<MetaDataHit>> namedMetaDataHits = getNamedMetaDataHits( firstTokenHits );

      final List<LookupHit> lookupHits = new ArrayList<>();
      final LookupToken firstWordLookupToken = wLookupTokenList.get( firstTokenIndex );
      final int firstWordStartOffset = firstWordLookupToken.getStartOffset();
      final int firstWordEndOffset = firstWordLookupToken.getEndOffset();
      final List<LookupToken> singleTokenList = Arrays.asList( firstWordLookupToken );
      final String[] firstWordPhrases = iv_phrBuilder.getPhrases( singleTokenList );
      for ( int i=0; i<firstWordPhrases.length; i++ ) {
         // perform toLowerCase() here instead of in the iterations below 2-21-13 spf
         firstWordPhrases[i] = firstWordPhrases[i].toLowerCase();
      }
      int permutationIndex = wLookupTokenList.size();
      if ( firstTokenIndex < wLookupTokenList.size() && permutationIndex > 0 ) {
         permutationIndex--;
      }
      final List<List<Integer>> permutationList = iv_permCacheMap.get( permutationIndex );
      for ( List<Integer> permutations : permutationList ) {
         // Moved sort and offset calculation from inner (per MetaDataHit) iteration 2-21-2013 spf
         List<Integer> permutationsSorted = (List) ((ArrayList)permutations).clone();
         Collections.sort( permutationsSorted );
         int startOffset = firstWordStartOffset;
         int endOffset = firstWordEndOffset;
         if ( !permutationsSorted.isEmpty() ) {
            int firstIdx = permutationsSorted.get( 0 );
            if ( firstIdx <= firstTokenIndex ) {
               firstIdx--;
            }
            final LookupToken firstToken = wLookupTokenList.get( firstIdx );
            if ( firstToken.getStartOffset() < firstWordStartOffset ) {
               startOffset = firstToken.getStartOffset();
            }
            int lastIdx = permutationsSorted.get( permutationsSorted.size() - 1 );
            if ( lastIdx <= firstTokenIndex ) {
               lastIdx--;
            }
            final LookupToken lastToken = wLookupTokenList.get( lastIdx );
            if ( lastToken.getEndOffset() > firstWordEndOffset ) {
               endOffset = lastToken.getEndOffset();
            }
         }
         // convert permutation idx back into LookupTokens
         final List<LookupToken> tempLookupTokens = new ArrayList<>();
         for ( Integer idx : permutations ) {
            if ( idx <= firstTokenIndex ) {
               idx--;
            }
            final LookupToken lookupToken = wLookupTokenList.get( idx );
            tempLookupTokens.add( lookupToken );
         }
         final String[] lookupTokenPhrases = iv_phrBuilder.getPhrases( tempLookupTokens );
         for ( String lookupTokenPhrase : lookupTokenPhrases ) {
            // perform toLowerCase() here instead of repeating in each inner loop
            lookupTokenPhrase = lookupTokenPhrase.toLowerCase();
            for ( String firstWordPhrase : firstWordPhrases ) {
               // perform toLowerCase() here so it isn't done for the whole concatenated string
//               firstWordPhrase = firstWordPhrase.toLowerCase();
               final StringBuilder phraseSB = new StringBuilder();
               phraseSB.append( firstWordPhrase ).append( ' ' ).append( lookupTokenPhrase );
               final String fullPhrase = phraseSB.toString().trim();
               final Set<MetaDataHit> mdhSet = namedMetaDataHits.get( fullPhrase );
               if ( mdhSet == null ) {
                  continue;
               }
               for ( MetaDataHit mdh : mdhSet ) {
                  final LookupHit lh = new LookupHit( mdh, startOffset, endOffset );
                  lookupHits.add( lh );
               }
            }
         }
      }
      return lookupHits;
   }

   /**
    * Extracts the list of LookupAnnotation objects representing noun phrases
    * from the context map.
    *
    * @param contextMap Map where key=Impl specific String object and value=List of
    *                   LookupAnnotation objects
    * @return list of window annotations or empty list if null
    */
   private List<LookupAnnotation> getWindowAnnotations( final Map<String, List<LookupAnnotation>> contextMap ) {
      final List<LookupAnnotation> list = contextMap.get( CTX_KEY_WINDOW_ANNOTATIONS );
      if ( list == null || list.isEmpty() ) {
         iv_logger.debug( "No context window annotations." );
         return Collections.emptyList();
      }
      return list;
   }

   /**
    * Determines the number of ListTokens are contained within the specified
    * start and end offsets;
    *
    * @param ltStartOffsetMap -
    * @param ltEndOffsetMap   -
    * @param ltListIndexMap   -
    * @param startOffset      -
    * @param endOffset        -
    * @return                 -
    */
   private int getNumberOfListTokens( final Map<Integer, List<LookupToken>> ltStartOffsetMap,
                                      final Map<Integer, List<LookupToken>> ltEndOffsetMap,
                                      final Map<LookupToken, Integer> ltListIndexMap,
                                      final int startOffset, final int endOffset ) {
      final List<LookupToken> startLookupTokenList = getLookupTokenList( startOffset, ltStartOffsetMap, true );
      final List<LookupToken> endLookupTokenList = getLookupTokenList( endOffset, ltEndOffsetMap, false );

      if ( startLookupTokenList.isEmpty() || endLookupTokenList.isEmpty() ) {
         iv_logger.debug( "Invalid window:" + startOffset + "," + endOffset );
         return -1;
      }
      final LookupToken startLookupToken = startLookupTokenList.get( 0 );
      final Integer startIdx = ltListIndexMap.get( startLookupToken );

      final LookupToken endLookupToken = endLookupTokenList.get( endLookupTokenList.size() - 1 );
      final Integer endIdx = ltListIndexMap.get( endLookupToken );

      return endIdx - startIdx + 1;
   }

   /**
    * Attempts to get a list of LookupToken objects at the specified offset. If
    * there are none, this method attempts to try nearby offsets based on the
    * traversal direction.
    *
    * @param offset -
    * @param ltOffsetMap -
    * @param traverseRight -
    * @return list of lookup tokens in window, never null
    */
   private static List<LookupToken> getLookupTokenList( final int offset,
                                                 final Map<Integer, List<LookupToken>> ltOffsetMap,
                                                 final boolean traverseRight ) {
      // first attempt the original offset, which will be the case most of the time
      List<LookupToken> lookupTokenList = ltOffsetMap.get( offset );
      if ( lookupTokenList != null ) {
         return lookupTokenList;
      }
      // otherwise traverse some nearby offsets and attempt to find a token
      // TODO hardcoded max offset window is 10 char
      final int offsetWindow = 10;
      if ( traverseRight ) {
         final int max = offset + offsetWindow;
         for ( int i = offset; i <= max; i++ ) {
            lookupTokenList = ltOffsetMap.get( i );
            if ( lookupTokenList != null ) {
               return lookupTokenList;
            }
         }
      } else {
         final int min = offset - offsetWindow;
         for ( int i = offset; i >= min; i-- ) {
            lookupTokenList = ltOffsetMap.get( i );
            if ( lookupTokenList != null ) {
               return lookupTokenList;
            }
         }
      }
      // no tokens in window - return an empty list, not null
      return Collections.emptyList();
   }

   /**
    * Determines the largest overlapping window annotation for the specified
    * LookupToken.
    */
   private LookupAnnotation getLargestWindowAnnotation( final int tokenIdx, final LookupToken lt,
                                                        final Map<Integer, List<LookupToken>> ltStartOffsetMap,
                                                        final Map<Integer, List<LookupToken>> ltEndOffsetMap,
                                                        final Map<LookupToken, Integer> ltListIndexMap,
                                                        final Map<Integer, List<LookupAnnotation>> wStartOffsetMap,
                                                        final Map<Integer, List<LookupAnnotation>> wEndOffsetMap ) {
      final Set<LookupAnnotation> startCandidateSet = new HashSet<>();
      final Set<LookupAnnotation> endCandidateSet = new HashSet<>();

      for ( Map.Entry<Integer, List<LookupAnnotation>> entry : wStartOffsetMap.entrySet() ) {
         final Integer startOffset = entry.getKey();
         if ( startOffset <= lt.getStartOffset() ) {
            startCandidateSet.addAll( entry.getValue() );
         }
      }
      for ( Map.Entry<Integer, List<LookupAnnotation>> entry : wEndOffsetMap.entrySet() ) {
         final Integer endOffset = entry.getKey();
         if ( endOffset >= lt.getEndOffset() ) {
            endCandidateSet.addAll( entry.getValue() );
         }
      }
      // union to get window annotations that are overlapping with LookupToken
      startCandidateSet.retainAll( endCandidateSet );

      // find largest overlapping window annotation
      LookupAnnotation largestWindowAnnot = null;
      for ( LookupAnnotation tempLookupAnnot : startCandidateSet ) {
         if ( largestWindowAnnot == null || tempLookupAnnot.getLength() > largestWindowAnnot.getLength() ) {
            // now see if we can handle the size of this window (permutation wise)
            final int ltCount = getNumberOfListTokens( ltStartOffsetMap, ltEndOffsetMap, ltListIndexMap,
                                                       tempLookupAnnot.getStartOffset(),
                                                       tempLookupAnnot.getEndOffset() );

            if ( ltCount <= iv_maxPermutationLevel && ltCount > 0 ) {
               largestWindowAnnot = tempLookupAnnot;
            } else if ( iv_logger.isDebugEnabled() ) {
               iv_logger.debug( "Window size of " + ltCount
                                + " exceeds the max permutation level of " + iv_maxPermutationLevel + "." );
            }
         }
      }
      return largestWindowAnnot;
   }

   private int getFixedWindowEndOffset( final int tokenIdx, final LookupToken lt, final List<LookupToken> ltList ) {
      // This iterates to the last index, then returns the last valid offset.
      // If we were performing max() this might be understandable ...
      //      int fixedEndOffset = 0;
      //      for ( int i = tokenIdx; (i < tokenIdx + iv_maxPermutationLevel)
      //            && (i < ltList.size()); i++ ) {
      //         LookupToken tempLookupToken = (LookupToken) ltList.get( i );
      //         if ( tempLookupToken != null ) {
      //            fixedEndOffset = tempLookupToken.getEndOffset();
      //         }
      //      }
      //      return fixedEndOffset;

      // Go backward and return the first valid end offset ...
      final int count = Math.min( tokenIdx + iv_maxPermutationLevel, ltList.size() );
      if ( count <= 0 ) {
         return 0;
      }
      for ( int i = count - 1; i >= 0; i-- ) {
         final LookupToken tempLookupToken = ltList.get( i );
         if ( tempLookupToken != null ) {
            return tempLookupToken.getEndOffset();
         }
      }
      return 0;
   }

   /**
    * Creates a map that binds an object from a list to its index position.
    *
    * @param list -
    * @return -
    */
   static private Map<LookupToken, Integer> getListIndexMap( final List<LookupToken> list ) {
      final Map<LookupToken, Integer> m = new HashMap<>( list.size() );
      for ( int i = 0; i < list.size(); i++ ) {
         m.put( list.get( i ), i );
      }
      return m;
   }

   /**
    * Creates a map that uses the start offset to index the LookupAnnotation objects.
    *
    * @param lookupAnnotList -
    * @return map of integers and lookup annotations
    */
   static private <T extends LookupAnnotation> Map<Integer, T> getSingleStartOffsetMap( final List<T> lookupAnnotList ) {
      final Map<Integer, T> m = new HashMap<Integer, T>();
      for ( T lookupAnnotation : lookupAnnotList ) {
         final Integer key = lookupAnnotation.getStartOffset();
         m.put( key, lookupAnnotation );
      }
      return m;
   }

   /**
    * Creates a map that uses the start offset to index the LookupAnnotation objects.
    *
    * @param lookupAnnotList -
    * @return map of integers and lookup annotation lists
    */
   static private <T extends LookupAnnotation> Map<Integer, List<T>> getMultipleStartOffsetMap( final List<T> lookupAnnotList ) {
      final Map<Integer, List<T>> m = new HashMap<>();
      for ( T lookupAnnotation : lookupAnnotList ) {
         final Integer key = lookupAnnotation.getStartOffset();
         List<T> list = m.get( key );
         if ( list == null ) {
            list = new ArrayList<>();
         }
         list.add( lookupAnnotation );
         m.put( key, list );
      }
      return m;
   }

   /**
    * Creates a map that uses the end offset to index the LookupAnnotation objects.
    *
    * @param lookupAnnotList -
    * @return map of integers and lookup annotations
    */
   static private <T extends LookupAnnotation> Map<Integer, T> getSingleEndOffsetMap( final List<T> lookupAnnotList ) {
      final Map<Integer, T> m = new HashMap<>();
      for ( T lookupAnnotation : lookupAnnotList ) {
         final Integer key = lookupAnnotation.getEndOffset();
         m.put( key, lookupAnnotation );
      }
      return m;
   }

   /**
    * Creates a map that uses the end offset to index the LookupAnnotation objects.
    *
    * @param lookupAnnotList -
    * @return map of integers and lookup annotation lists
    */
   static private <T extends LookupAnnotation> Map<Integer, List<T>> getMultipleEndOffsetMap( final List<T> lookupAnnotList ) {
      final Map<Integer, List<T>> m = new HashMap<>();
      for ( T lookupAnnotation : lookupAnnotList ) {
         final Integer key = lookupAnnotation.getEndOffset();
         List<T> list = m.get( key );
         if ( list == null ) {
            list = new ArrayList<>();
         }
         list.add( lookupAnnotation );
         m.put( key, list );
      }
      return m;
   }


   /**
    * Gets the hits for the specified LookupToken. This uses the first token Dictionary.
    *
    * @param firstLookupToken -
    * @return -
    * @throws Exception
    */
   private Collection<MetaDataHit> getFirstTokenHits( final LookupToken firstLookupToken ) throws Exception {
      final List<LookupToken> singleTokenList = Arrays.asList( firstLookupToken );
      final String[] phrases = iv_phrBuilder.getPhrases( singleTokenList );
      final List<MetaDataHit> metaDataHits = new ArrayList<>();
      for ( String phrase : phrases ) {
         final Collection<MetaDataHit> phraseMetaDataHits = iv_firstTokenDictEngine.metaLookup( phrase );
         if ( !phraseMetaDataHits.isEmpty() ) {
            metaDataHits.addAll( phraseMetaDataHits );
         }
      }
      return metaDataHits;
   }
}
