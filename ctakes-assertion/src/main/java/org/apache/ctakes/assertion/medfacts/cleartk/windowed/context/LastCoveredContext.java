package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class LastCoveredContext extends AbstractRightToLeftContext {
   public LastCoveredContext( int end ) {
      super( 0, end );
   }

   public LastCoveredContext( int begin, int end ) {
      super( begin, end );
   }


   protected <T extends Annotation> List<T> select( final JCas jCas,
                                                    final Annotation focusAnnotation,
                                                    final Class<T> annotationClass,
                                                    final int count ) {
      final List<T> annotations = selectCovered( focusAnnotation, annotationClass );
      return annotations.subList( Math.max( annotations.size() - count, 0 ), annotations.size() );
   }


}
