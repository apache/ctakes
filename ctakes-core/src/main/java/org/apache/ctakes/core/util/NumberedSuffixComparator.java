package org.apache.ctakes.core.util;

import org.apache.log4j.Logger;

import java.util.Comparator;

/**
 * Compares to strings that may contain numbers, such that:
 * "abc_10" > "abc_2"
 * "abc_010" > "abc_10";
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/25/2017
 */
public class NumberedSuffixComparator implements Comparator<String> {
   static private final Logger LOGGER = Logger.getLogger( "NumberedSuffixComparator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public int compare( final String text1, final String text2 ) {
      final int len1 = text1.length();
      final int len2 = text2.length();
      int i1 = 0;
      int i2 = 0;
      while ( i1 < len1 && i2 < len2 ) {
         char c1 = text1.charAt( i1 );
         char c2 = text2.charAt( i2 );
         if ( Character.isDigit( c1 ) && Character.isDigit( c2 ) ) {
            final String numText1 = getIntText( text1, i1 );
            final String numText2 = getIntText( text2, i2 );
            int value = compareNumText( numText1, numText2 );
            if ( value != 0 ) {
               return value;
            }
            value = numText1.length() - numText2.length();
            if ( value != 0 ) {
               return value;
            }
            i1 += numText1.length();
            i2 += numText2.length();
            continue;
         }
         final int value = Character.compare( c1, c2 );
         if ( value != 0 ) {
            return value;
         }
         i1++;
         i2++;
      }
      if ( len1 < len2 ) {
         return -1;
      } else if ( len2 < len1 ) {
         return 1;
      }
      return 0;
   }

   static private String getIntText( final String text, final int index ) {
      int i = index;
      final StringBuilder sb = new StringBuilder();
      final int length = text.length();
      char c;
      while ( i < length ) {
         c = text.charAt( i );
         if ( !Character.isDigit( c ) ) {
            return sb.toString();
         }
         sb.append( c );
         i++;
      }
      return sb.toString();
   }

   /**
    * @param numText1 filled with digits
    * @param numText2 filled with digits
    * @return -1, 0, 1 if first number less than, equal to, greater than second number
    */
   static private int compareNumText( final String numText1, final String numText2 ) {
      try {
         final long num1 = Long.parseUnsignedLong( numText1 );
         final long num2 = Long.parseUnsignedLong( numText2 );
         return Long.compare( num1, num2 );
      } catch ( NumberFormatException nfE ) {
         LOGGER.debug( nfE.getMessage(), nfE );
      }
      return 0;
   }


}
