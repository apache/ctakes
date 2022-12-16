package org.apache.ctakes.gui.dictionary.umls;

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 7/14/14
 */
final public class DoseUtil {

   private DoseUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "DoseUtil" );


   // some of these are not strictly units, e.g. "ud" : "ut dictum" or "as directed"
   // but can be properly trimmed as they appear in the same place as would a unit

   static private final String[] UNIT_ARRAY = { "gr", "gm", "gram", "grams", "g",
                                                "mg", "milligram", "milligrams", "kg",
                                                "microgram", "micrograms", "mcg", "ug",
                                                "millicurie", "mic", "oz",
                                                "lf", "ml", "liter", "milliliter", "l",
                                                "milliequivalent", "meq",
//                                           "hour", "hours", "hr", "day", "days", "daily", //"24hr", "8hr", "12hr",
//                                                "week", "weeks", "weekly", "biweekly",
                                                "usp", "titradose",
                                                "unit", "units", "unt", "iu", "u", "mmu",
                                                "mm", "cm", "cc",
                                                "gauge", "intl", "au", "bau", "mci", "ud",
                                                "ww", "vv", "wv",
                                                "%", "percent", "%ww", "%vv", "%wv",
                                                "actuation", "actuat", "vial", "vil", "packet", "pkt" };
   static private final Collection<String> UNITS = Arrays.asList( UNIT_ARRAY );

   static private final Pattern SPACE_PATTERN = Pattern.compile( "\\s+" );


   static public boolean hasUnit( final String text ) {
      final String[] splits = SPACE_PATTERN.split( text );
      if ( splits.length <= 1 ) {
         return UNITS.contains( "text" );
      }
      for ( int i = 0; i < splits.length - 1; i++ ) {
         final String split = splits[ i ];
         if ( !Character.isDigit( split.charAt( 0 ) ) ) {
            continue;
         }
         for ( int j = 1; j < split.length(); j++ ) {
            // check to see if word is concatenated number and unit
            if ( !Character.isDigit( split.charAt( j ) ) ) {
               if ( UNITS.contains( split.substring( j ) ) ) {
                  return true;
               }
            }
         }
         // check next word for unit
         i++;
         if ( UNITS.contains( splits[ i ] ) ) {
            return true;
         }
      }
      return false;
   }


}
