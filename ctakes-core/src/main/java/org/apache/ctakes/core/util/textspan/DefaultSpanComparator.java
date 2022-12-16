package org.apache.ctakes.core.util.textspan;

import java.util.Comparator;

/**
 * Compares first by begin, then by end if the begins are equal.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/14/2017
 */
public enum DefaultSpanComparator implements Comparator<TextSpan> {
   INSTANCE;

   public static DefaultSpanComparator getInstance() {
      return INSTANCE;
   }

   /**
    * Compares first by begin, then by end if the begins are equal.
    * {@inheritDoc}
    */
   @Override
   public int compare( final TextSpan span1, final TextSpan span2 ) {
      return compareIndices( span1.getBegin(), span2.getBegin(), span1.getEnd(), span2.getEnd() );
   }

   /**
    * Compares first by begin, then by end if the begins are equal.
    */
   static public int compareIndices( final int begin1, final int begin2, final int end1, final int end2 ) {
      final int byBegin = begin1 - begin2;
      if ( byBegin != 0 ) {
         return byBegin;
      }
      return end1 - end2;
   }

}
