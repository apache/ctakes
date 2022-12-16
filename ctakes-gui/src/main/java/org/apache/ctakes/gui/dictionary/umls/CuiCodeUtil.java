package org.apache.ctakes.gui.dictionary.umls;


import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 9/5/2014
 */
public enum CuiCodeUtil {
   INSTANCE;

   static public CuiCodeUtil getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "CuiCodeUtil" );
   static private final long PREFIX_MULTIPLIER = 100000000;

   final private List<PrefixerPair> _prefixerPairList = new ArrayList<>();

   CuiCodeUtil() {
      // Add the standard C as the default encoding prefix
      _prefixerPairList.add( new PrefixerPair( "C0000000" ) );
   }

   public String getAsCui( final Long code ) {
      final long multiplier = code / PREFIX_MULTIPLIER;
      if ( code < 0 || multiplier < 0 || multiplier >= _prefixerPairList.size() ) {
         LOGGER.error( "Could not create Cui String for " + code );
         return "" + code;
      }
      return _prefixerPairList.get( (int)multiplier ).getAsCui( code % PREFIX_MULTIPLIER );
   }


   public Long getCuiCode( final String cui ) {
      final PrefixerPair prefixerPair = new PrefixerPair( cui );
      int prefixerIndex = _prefixerPairList.indexOf( prefixerPair );
      if ( prefixerIndex < 0 ) {
         prefixerIndex = _prefixerPairList.size();
         _prefixerPairList.add( prefixerPair );
      }
      return prefixerPair.getCuiCode( cui, prefixerIndex );
   }


   static private final class PrefixerPair {
      final private int __digitCount;
      final private char[] __prefix;
      final private int __hashCode;

      private PrefixerPair( final String cui ) {
         final char[] chars = cui.toCharArray();
         int digitCount = 0;
         while ( digitCount < chars.length
                 && digitCount < 7
                 && Character.isDigit( chars[ chars.length - 1 - digitCount ] ) ) {
            digitCount++;
         }
         __digitCount = digitCount;
         __prefix = Arrays.copyOfRange( chars, 0, chars.length - digitCount );
         __hashCode = digitCount + Arrays.hashCode( __prefix );
      }

      private Long getCuiCode( final String cui, final int multiplier ) {
         final String cuiNum = cui.substring( cui.length() - __digitCount, cui.length() );
         try {
            return PREFIX_MULTIPLIER * multiplier + Long.parseLong( cuiNum );
         } catch ( NumberFormatException nfE ) {
            LOGGER.error( "Could not create Cui Code for " + cui );
         }
         return -1l;
      }

      private String getAsCui( final Long code ) {
         final char[] codeChars = String.valueOf( code ).toCharArray();
         if ( codeChars.length > __digitCount ) {
            LOGGER.error( "Invalid code " + code + " for prefix " + __prefix
                          + " has more than " + __digitCount + " digits" );
            return String.valueOf( __prefix ) + String.valueOf( codeChars );
         }
         final int cuiLength = __prefix.length + __digitCount;
         final char[] cuiChars = new char[ cuiLength ];
         System.arraycopy( __prefix, 0, cuiChars, 0, __prefix.length );
         System.arraycopy( codeChars, 0, cuiChars, cuiLength - codeChars.length, codeChars.length );
         for ( int i = __prefix.length; i < cuiLength - codeChars.length; i++ ) {
            cuiChars[ i ] = '0';
         }
         return String.valueOf( cuiChars );
      }

      public int hashCode() {
         return __hashCode;
      }

      public boolean equals( final Object other ) {
         return other instanceof PrefixerPair
                && __hashCode == ((PrefixerPair)other).__hashCode
                && __digitCount == ((PrefixerPair)other).__digitCount
                && Arrays.equals( __prefix, ((PrefixerPair)other).__prefix );
      }
   }

   // todo
   // todo switch to int: 32 bit signed, max = 2,147,483,647
   // todo if we keep final 7 digits for the numerical then we have 213 possible prefixes
   // todo
   // todo can probably change the code and the db will be fine, change the db too
   // todo


}
