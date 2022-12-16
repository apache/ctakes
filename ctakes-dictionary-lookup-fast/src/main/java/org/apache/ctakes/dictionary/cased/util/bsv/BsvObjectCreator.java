package org.apache.ctakes.dictionary.cased.util.bsv;

import org.apache.ctakes.core.util.StringUtil;

import java.util.Arrays;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
public interface BsvObjectCreator<T> {

   T createBsvObject( final String[] columns );

   default T createBsvObject( final String line ) {
      if ( isCommentLine( line ) ) {
         return null;
      }
      final String[] columns = StringUtil.fastSplit( line, '|' );
      if ( isAnyColumnEmpty( columns ) ) {
         return null;
      }
      return createBsvObject( columns );
   }

   default boolean isCommentLine( final String line ) {
      return line.isEmpty() || line.startsWith( "//" ) || line.startsWith( "#" );
   }

   default boolean isAnyColumnEmpty( final String[] columns ) {
      return Arrays.stream( columns ).anyMatch( c -> c.trim().isEmpty() );
   }

}
