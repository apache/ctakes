package org.apache.ctakes.core.util;


import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/16/2017
 */
final public class StringUtil {

   static private final Logger LOGGER = Logger.getLogger( "StringUtil" );

   private StringUtil() {
   }

   /**
    * Splits a string using a character.  Faster than String.split( regex )
    *
    * @param line full text to split
    * @param c    character at which to split
    * @return array of substrings or the original line if there are no characters c
    */
   static public String[] fastSplit( final String line, final char c ) {
      int nextSplit = line.indexOf( c );
      if ( nextSplit < 0 ) {
         return new String[]{ line };
      }
      final String[] tokens = new String[ line.length() + 1 ];
      int index = 0;
      int lastSplit = -1;
      while ( nextSplit >= 0 ) {
         tokens[ index ] = line.substring( lastSplit + 1, nextSplit );
         lastSplit = nextSplit;
         nextSplit = line.indexOf( c, lastSplit + 1 );
         index++;
      }
      if ( lastSplit + 1 < line.length() ) {
         tokens[ index ] = line.substring( lastSplit + 1 );
         index++;
      }
      return Arrays.copyOf( tokens, index );
   }


}
