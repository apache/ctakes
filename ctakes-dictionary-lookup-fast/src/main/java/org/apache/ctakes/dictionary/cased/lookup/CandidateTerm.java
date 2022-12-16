package org.apache.ctakes.dictionary.cased.lookup;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.dictionary.cased.util.tokenize.TokenizedTerm;

import java.util.Arrays;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/14/2020
 */
@Immutable
final public class CandidateTerm {


   private final long _cuiCode;

   private final String[] _prefixes;
   private final String _rareWord;
   private final String[] _suffixes;
   final private boolean _allUpperCase;
   final private boolean _allLowerCase;
   final private boolean _matchesLookupCase;
   private final int _rank;
   private final int _instances;

   final private int _hashCode;


   public CandidateTerm( final TokenizedTerm tokenizedTerm, final int rareWordIndex ) {
      _cuiCode = tokenizedTerm.getCui();
      final String[] tokens = tokenizedTerm.getTokens();
      _prefixes = rareWordIndex == 0
                  ? new String[ 0 ]
                  : Arrays.copyOf( tokens, rareWordIndex );
      _rareWord = tokens[ rareWordIndex ];
      final int suffixLength = tokens.length - rareWordIndex - 1;
      _suffixes = new String[ suffixLength ];
      System.arraycopy( tokens, rareWordIndex + 1, _suffixes, 0, suffixLength );
      _allUpperCase = tokenizedTerm.isAllUpperCase();
      _allLowerCase = tokenizedTerm.isAllLowerCase();
      _matchesLookupCase = true;
      _hashCode = (_cuiCode + "_" + String.join( " ", tokens )).hashCode();
      _rank = 1;
      _instances = 1;
   }


   public CandidateTerm( final long cuiCode,
                         final String[] tokens,
                         final int rareWordIndex,
                         final boolean lookupAllUpper,
                         final boolean lookupAllLower,
                         final int rank,
                         final int instances ) {
      _cuiCode = cuiCode;
      _prefixes = rareWordIndex == 0
                  ? new String[ 0 ]
                  : Arrays.copyOf( tokens, rareWordIndex );
      _rareWord = tokens[ rareWordIndex ];
      final int suffixLength = tokens.length - rareWordIndex - 1;
      _suffixes = new String[ suffixLength ];
      System.arraycopy( tokens, rareWordIndex + 1, _suffixes, 0, suffixLength );
      boolean anyCaps = false;
      boolean anyLower = false;
      for ( char c : String.join( "", tokens ).toCharArray() ) {
         if ( Character.isUpperCase( c ) ) {
            anyCaps = true;
         } else if ( Character.isLowerCase( c ) ) {
            anyLower = true;
         }
         if ( anyCaps && anyLower ) {
            break;
         }
      }
      _allUpperCase = anyCaps && !anyLower;
      _allLowerCase = anyLower && !anyCaps;

      _hashCode = (cuiCode + "_" + String.join( " ", tokens )).hashCode();
      _matchesLookupCase = _allUpperCase == lookupAllUpper && _allLowerCase == lookupAllLower;
      _rank = rank;
      _instances = instances;
   }


   public CandidateTerm( final long cuiCode,
                         final String prefix,
                         final String rareWord,
                         final String suffix,
                         final boolean lookupAllUpper,
                         final boolean lookupAllLower,
                         final int rank,
                         final int instances ) {
      _cuiCode = cuiCode;
      _prefixes = prefix.isEmpty()
                  ? new String[ 0 ]
                  : StringUtil.fastSplit( prefix, ' ' );
      _rareWord = rareWord;
      _suffixes = suffix.isEmpty()
                  ? new String[ 0 ]
                  : StringUtil.fastSplit( suffix, ' ' );
      boolean anyCaps = false;
      boolean anyLower = false;
      for ( char c : (prefix + rareWord + suffix).toCharArray() ) {
         if ( Character.isUpperCase( c ) ) {
            anyCaps = true;
         } else if ( Character.isLowerCase( c ) ) {
            anyLower = true;
         }
         if ( anyCaps && anyLower ) {
            break;
         }
      }
      _allUpperCase = anyCaps && !anyLower;
      _allLowerCase = anyLower && !anyCaps;
      _hashCode = (cuiCode + "_"
                   + (prefix.isEmpty() ? "" : prefix + " ")
                   + rareWord
                   + (suffix.isEmpty() ? "" : " " + suffix))
            .hashCode();
      _matchesLookupCase = _allUpperCase == lookupAllUpper && _allLowerCase == lookupAllLower;
      _rank = rank;
      _instances = instances;
   }


   /**
    * @return umls cui for the term
    */
   public Long getCuiCode() {
      return _cuiCode;
   }


   /**
    * @return each token in the term as a separate String
    */
   public String[] getTokens() {
      final String[] tokens = new String[ _prefixes.length + 1 + _suffixes.length ];
      System.arraycopy( _prefixes, 0, tokens, 0, _prefixes.length );
      tokens[ _prefixes.length ] = _rareWord;
      System.arraycopy( _suffixes, 0, tokens, _prefixes.length + 1, _suffixes.length );
      return tokens;
   }


   public String[] getPrefixes() {
      return _prefixes;
   }

   public String[] getLowerPrefixes() {
      if ( isAllLowerCase() ) {
         return _prefixes;
      }
      return Arrays.stream( _prefixes ).map( String::toLowerCase ).toArray( String[]::new );
   }


   public String[] getSuffixes() {
      return _suffixes;
   }

   public String[] getLowerSuffixes() {
      if ( isAllLowerCase() ) {
         return _suffixes;
      }
      return Arrays.stream( _suffixes ).map( String::toLowerCase ).toArray( String[]::new );
   }

   /**
    * @return the index of the rare word used for indexing in the token array
    */
   public int getRareWordIndex() {
      return _prefixes.length;
   }


   public int getTokenCount() {
      return _prefixes.length + 1 + _suffixes.length;
   }

   public boolean isAllUpperCase() {
      return _allUpperCase;
   }

   public boolean isAllLowerCase() {
      return _allLowerCase;
   }

   public boolean matchesLookupCase() {
      return _matchesLookupCase;
   }

   public int getRank() {
      return _rank;
   }

   public int getInstances() {
      return _instances;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      return value instanceof CandidateTerm && value.hashCode() == hashCode();
//      if ( !(value instanceof LookupTerm) ) {
//         return false;
//      }
//      final LookupTerm other = (LookupTerm)value;
//      return other.getCuiCode().equals( _cuiCode ) && Arrays.equals( other.getTokens(), getTokens() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _hashCode;
   }

}
