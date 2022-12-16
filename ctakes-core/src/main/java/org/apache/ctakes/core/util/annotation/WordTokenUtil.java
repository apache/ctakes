package org.apache.ctakes.core.util.annotation;


import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.log4j.Logger;

import javax.annotation.concurrent.Immutable;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/16/2015
 */
@Immutable
final public class WordTokenUtil {

   static private final Logger LOGGER = Logger.getLogger( "WordTokenUtil" );

   static private final String MISSING_WORDTOKEN_TEXT = "MISSING_WORDTOKEN_TEXT";

   private WordTokenUtil() {
   }


   /**
    * In some pipelines LVG is not run, hence a canonical form does not exist.
    * In order to prevent NPEs, this method checks for null values of canonical form and covered text
    *
    * @param wordToken of interest
    * @return The first non-null of the word token's canonical form, covered text or {@link #MISSING_WORDTOKEN_TEXT}.
    */
   static public String getCanonicalForm( final WordToken wordToken ) {
      final String canonicalForm = wordToken.getCanonicalForm();
      if ( canonicalForm != null && !canonicalForm.isEmpty() ) {
         return canonicalForm;
      }
      final String coveredText = wordToken.getCoveredText();
      if ( coveredText == null ) {
         return MISSING_WORDTOKEN_TEXT;
      }
      return coveredText;
   }


}
