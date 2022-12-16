package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context;


import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class PrecedingContext extends AbstractRightToLeftContext {

   public PrecedingContext( int end ) {
      super( 0, end );
   }

   public PrecedingContext( int begin, int end ) {
      super( begin, end );
   }

   protected <T extends Annotation> List<T> select( final JCas jCas,
                                                    final Annotation focusAnnotation,
                                                    final Class<T> annotationClass,
                                                    final int count ) {
      final List<T> preceding = new ArrayList<>( count );

      for ( int i = _windowCovered.size() - 1; i >= 0; i-- ) {
         if ( annotationClass.isInstance( _windowCovered.get( i ) )
              && _windowCovered.get( i ).getEnd() <= focusAnnotation.getBegin() ) {
            preceding.add( (T)_windowCovered.get( i ) );
            if ( preceding.size() == count ) {
               break;
            }
         }
      }
      Collections.reverse( preceding );
      return preceding;
   }

}
