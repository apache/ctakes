package org.apache.ctakes.gui.dictionary.util;

import org.apache.ctakes.core.util.StringUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/15/14
 */
final public class TokenUtil {

   private TokenUtil() {
   }

   static public List<String> getBsvItems( final String line ) {
      return getSeparatedValueItems( line, '|' );
   }

   static public List<String> getTildeItems( final String line ) {
      return getSeparatedValueItems( line, '~' );
   }

   static public List<String> getCsvItems( final String line ) {
      return getSeparatedValueItems( line, ',' );
   }

   static private List<String> getSeparatedValueItems( final String line, final char separator ) {
      return Arrays.asList( StringUtil.fastSplit( line, separator ) );
//      if ( line == null || line.trim().isEmpty() ) {
//         return Collections.emptyList();
//      }
//      final List<String> tokens = new ArrayList<>();
//      int startIndex = 0;
//      int stopIndex = line.indexOf( separator );
//      while ( stopIndex > 0 && stopIndex < line.length() ) {
//         tokens.add( line.substring( startIndex, stopIndex ) );
//         startIndex = stopIndex + 1;
//         stopIndex = line.indexOf( separator, startIndex );
//      }
//      if ( startIndex < line.length() - 1 ) {
//         tokens.add( line.substring( startIndex ) );
//      } else {
//         tokens.add( "" );
//      }
//      return tokens;
   }


   static public String createBsvLine( final Collection<String> values ) {
      if ( values == null ) {
         return "";
      }
      return createBsvLine( values.toArray( new String[ values.size() ] ) );
   }

   static public String createBsvLine( final String... values ) {
      if ( values.length == 0 ) {
         return "";
      }
      final StringBuilder sb = new StringBuilder();
      for ( String value : values ) {
         sb.append( value ).append( "|" );
      }
      sb.setLength( sb.length() - 1 );
      return sb.toString();
   }

   static public String createCsvLine( final Collection<String> values ) {
      if ( values == null ) {
         return "";
      }
      return createCsvLine( values.toArray( new String[ values.size() ] ) );
   }

   static public String createCsvLine( final String... values ) {
      if ( values.length == 0 ) {
         return "";
      }
      final StringBuilder sb = new StringBuilder();
      for ( String value : values ) {
         sb.append( value ).append( "," );
      }
      sb.setLength( sb.length() - 1 );
      return sb.toString();
   }

}
