package org.apache.ctakes.core.util.textspan;

import org.apache.uima.jcas.tcas.Annotation;

import java.util.Comparator;

/**
 * Compares first by begin, then by end if the begins are equal.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/14/2017
 */
public enum DefaultAspanComparator implements Comparator<Annotation> {
   INSTANCE;

   public static DefaultAspanComparator getInstance() {
      return INSTANCE;
   }

   /**
    * Compares first by begin, then by end if the begins are equal.
    * {@inheritDoc}
    */
   @Override
   public int compare( final Annotation annotation1, final Annotation annotation2 ) {
      return DefaultSpanComparator.compareIndices( annotation1.getBegin(), annotation2.getBegin(),
            annotation1.getEnd(), annotation2.getEnd() );
   }

}
