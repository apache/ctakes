package org.apache.ctakes.core.util;


import org.apache.ctakes.typesystem.type.syntax.WordToken;

import javax.annotation.concurrent.Immutable;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/16/2015
 * @deprecated use WordTokenUtil in (sub) package annotation.
 */
@Immutable
@Deprecated
final public class WordTokenUtil {

   private WordTokenUtil() {
   }


   /**
    * In some pipelines LVG is not run, hence a canonical form does not exist.
    * In order to prevent NPEs, this method checks for null values of canonical form and covered text
    *
    * @param wordToken of interest
    * @return The first non-null of the word token's canonical form, covered text .
    */
   @Deprecated
   static public String getCanonicalForm( final WordToken wordToken ) {
      return org.apache.ctakes.core.util.annotation.WordTokenUtil.getCanonicalForm( wordToken );
   }


}
