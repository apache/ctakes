package org.apache.ctakes.dictionary.lookup2.util;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 9/5/2014
 */
final public class TuiCodeUtil {

   private TuiCodeUtil() {
   }

   static public String getAsTui( final Integer code ) {
      final StringBuilder sb = new StringBuilder( 4 );
      sb.append( code );
      return getAsTui( sb );
   }

   static public String getAsTui( final String code ) {
      if ( code.length() == 4 && code.startsWith( "T" ) ) {
         return code;
      }
      final StringBuilder sb = new StringBuilder( 4 );
      sb.append( code.replace( "T", "" ) );
      return getAsTui( sb );
   }

   static private String getAsTui( final StringBuilder sb ) {
      while ( sb.length() < 3 ) {
         sb.insert( 0, '0' );
      }
      sb.insert( 0, 'T' );
      return sb.toString();
   }


   static public Collection<String> getIntAsTuis( final Collection<Integer> tuiCodes ) {
      return tuiCodes.stream().map( TuiCodeUtil::getAsTui ).collect( Collectors.toSet() );
   }

   static public Collection<String> getStringAsTuis( final Collection<String> tuiNums ) {
      return tuiNums.stream().map( TuiCodeUtil::getAsTui ).collect( Collectors.toSet() );
   }

   static public Integer getTuiCode( final String tui ) {
      final String tuiText = getAsTui( tui );
      final String tuiNum = tuiText.substring( 1 );
      try {
         return Integer.parseInt( tuiNum );
      } catch ( NumberFormatException nfE ) {
         System.err.println( "Could not create Tui Code for " + tui );
      }
      return -1;
   }


}
