package org.apache.ctakes.dictionary.cased.lookup;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.dictionary.cased.dictionary.CasedDictionary;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class ContiguousLookupEngine {

   static private final Logger LOGGER = Logger.getLogger( "ContiguousLookupEngine" );


   /**
    * Given a dictionary, tokens, and lookup token indices, populate a terms collection with discovered terms
    *
    * @param dictionary   -
    * @param lookupTokens -
    * @return map of text spans to terms discovered at those text spans.
    */
   public final Map<Pair<Integer>, Collection<DiscoveredTerm>> findTerms( final CasedDictionary dictionary,
                                                                          final List<LookupToken> lookupTokens,
                                                                          final int consecutiveSkipMax,
                                                                          final int totalSkipMax ) {
      final Map<Pair<Integer>, Collection<DiscoveredTerm>> discoveredTermMap = new HashMap<>();
      int lookupTokenIndex = -1;
      Collection<CandidateTerm> candidateTerms;
      for ( LookupToken lookupToken : lookupTokens ) {
         lookupTokenIndex++;
         if ( !lookupToken.isValidIndexToken() ) {
            continue;
         }
         candidateTerms = dictionary.getCandidateTerms( lookupToken );
         if ( candidateTerms == null || candidateTerms.isEmpty() ) {
            continue;
         }
         for ( CandidateTerm candidateTerm : candidateTerms ) {
            if ( candidateTerm.getTokenCount() == 1 ) {
               // Single word term, add and move on
               discoveredTermMap.computeIfAbsent( lookupToken.getTextSpan(), s -> new HashSet<>() )
                                .add( new DiscoveredTerm( candidateTerm ) );
               continue;
            }
            if ( candidateTerm.getPrefixes().length >= lookupTokenIndex
                 || lookupTokenIndex + candidateTerm.getSuffixes().length >= lookupTokens.size() ) {
               // term will extend beyond window
               continue;
            }
            if ( isMismatch( getPrefixMatch( candidateTerm, lookupTokens, lookupTokenIndex ) ) ) {
               continue;
            }
            if ( isMismatch( getSuffixMatch( candidateTerm, lookupTokens, lookupTokenIndex ) ) ) {
               continue;
            }
            final int spanBegin = lookupTokens.get( lookupTokenIndex - candidateTerm.getPrefixes().length ).getBegin();
            final int spanEnd = lookupTokens.get( lookupTokenIndex + candidateTerm.getSuffixes().length ).getEnd();
            discoveredTermMap.computeIfAbsent( new Pair<>( spanBegin, spanEnd ), s -> new HashSet<>() )
                             .add( new DiscoveredTerm( candidateTerm ) );
         }
      }
      return discoveredTermMap;
   }


   static private final Pair<Integer> HIT = new Pair<>( 0, 0 );
   static private final Pair<Integer> MISS = new Pair<>( -1, -1 );

   static private boolean isMismatch( final Pair<Integer> skips ) {
      return MISS.equals( skips );
   }


   /**
    * Hopefully the jit will inline this method
    *
    * @param candidateTerm    rare word term to check for match
    * @param allTokens        all tokens in a window
    * @param lookupTokenIndex index of first token in allTokens to check
    * @return the consecutiveSkips and totalSkips required to make the prefix fit the tokens.  -1,-1 if no fit.
    */
   public static Pair<Integer> getPrefixMatch( final CandidateTerm candidateTerm,
                                               final List<LookupToken> allTokens,
                                               final int lookupTokenIndex ) {
      final String[] prefixes = candidateTerm.getPrefixes();
      final String[] lowerPrefixes = candidateTerm.getLowerPrefixes();
      if ( prefixes.length == 0 ) {
         return HIT;
      }
      int tokenIndex = lookupTokenIndex - 1;
      LookupToken lookupToken = allTokens.get( tokenIndex );
      for ( int i = prefixes.length - 1; i >= 0; i-- ) {
         if ( candidateTerm.isAllUpperCase() ) {
            if ( !lookupToken.isAllUpperCase() ) {
               return MISS;
            }
            if ( !prefixes[ i ].equals( lookupToken.getText() ) ) {
               return MISS;
            }
         }
         if ( !candidateTerm.isAllUpperCase() && !candidateTerm.isAllLowerCase() ) {
            if ( !prefixes[ i ].equals( lookupToken.getText() ) ) {
               return MISS;
            }
         }
         if ( lowerPrefixes[ i ].equals( lookupToken.getLowerText() ) ) {
            tokenIndex--;
            lookupToken = allTokens.get( tokenIndex );
            continue;
         }
         // the token normal didn't match
         return MISS;
      }
      // the token normal matched
      return HIT;
   }

   /**
    * Hopefully the jit will inline this method
    *
    * @param candidateTerm    rare word term to check for match
    * @param allTokens        all tokens in a window
    * @param lookupTokenIndex index of first token in allTokens to check
    * @return the consecutiveSkips and totalSkips required to make the prefix fit the tokens.  -1,-1 if no fit.
    */
   public static Pair<Integer> getSuffixMatch( final CandidateTerm candidateTerm,
                                               final List<LookupToken> allTokens,
                                               final int lookupTokenIndex ) {
      final String[] suffixes = candidateTerm.getSuffixes();
      //  TODO - Do we really want lower-case candidates?
      //   They should be stored in the dictionary as the desired case.
      final String[] lowerSuffixes = candidateTerm.getLowerSuffixes();
      if ( suffixes.length == 0 ) {
         return HIT;
      }
      int tokenIndex = lookupTokenIndex + 1;
      LookupToken lookupToken = allTokens.get( tokenIndex );
      for ( int i = 0; i < suffixes.length; i++ ) {
         if ( candidateTerm.isAllUpperCase() ) {
            if ( !lookupToken.isAllUpperCase() ) {
               return MISS;
            }
            if ( !suffixes[ i ].equals( lookupToken.getText() ) ) {
               return MISS;
            }
         }
         if ( !candidateTerm.isAllUpperCase() && !candidateTerm.isAllLowerCase() ) {
            if ( !suffixes[ i ].equals( lookupToken.getText() ) ) {
               return MISS;
            }
         }
         if ( lowerSuffixes[ i ].equals( lookupToken.getLowerText() ) ) {
            tokenIndex--;
            lookupToken = allTokens.get( tokenIndex );
            continue;
         }
         // the token normal didn't match
         return MISS;
      }
      // the token normal matched
      return HIT;
   }


   /**
    * Hopefully the jit will inline this method
    *
    * @param candidateTerm      rare word term to check for match
    * @param allTokens          all tokens in a window
    * @param lookupTokenIndex   index of first token in allTokens to check
    * @param consecutiveSkipMax -
    * @param totalSkipMax       -
    * @return the consecutiveSkips and totalSkips required to make the prefix fit the tokens.  -1,-1 if no fit.
    */
   public static Pair<Integer> getPrefixMatch( final CandidateTerm candidateTerm,
                                               final List<LookupToken> allTokens,
                                               final int lookupTokenIndex,
                                               final int consecutiveSkipMax,
                                               final int totalSkipMax ) {
      final String[] prefixes = candidateTerm.getPrefixes();
      if ( prefixes.length == 0 ) {
         return HIT;
      }
      int tokenIndex = lookupTokenIndex - 1;
      for ( int i = prefixes.length - 1; i >= 0; i-- ) {
         if ( prefixes[ i ].equals( allTokens.get( tokenIndex ).getText() ) ) {
            tokenIndex--;
            continue;
         }
         // the token normal didn't match
         // TODO Add overlap logic ...
         return MISS;
      }
      // the token normal matched
      return HIT;
   }


   /**
    * Hopefully the jit will inline this method
    *
    * @param candidateTerm      rare word term to check for match
    * @param allTokens          all tokens in a window
    * @param lookupTokenIndex   index of first token in allTokens to check
    * @param consecutiveSkipMax -
    * @param totalSkipMax       -
    * @return the consecutiveSkips and totalSkips required to make the prefix fit the tokens.  -1,-1 if no fit.
    */
   public static Pair<Integer> getSuffixMatch( final CandidateTerm candidateTerm,
                                               final List<LookupToken> allTokens,
                                               final int lookupTokenIndex,
                                               final int consecutiveSkipMax,
                                               final int totalSkipMax ) {
      final String[] suffixes = candidateTerm.getSuffixes();
      if ( suffixes.length == 0 ) {
         return HIT;
      }
      int tokenIndex = lookupTokenIndex + 1;
      for ( String suffix : suffixes ) {
         if ( suffix.equals( allTokens.get( tokenIndex ).getText() ) ) {
            tokenIndex++;
            continue;
         }
         // the token normal didn't match
         // TODO Add overlap logic ...
         return MISS;
      }
      // the token normal matched
      return HIT;
   }

}
