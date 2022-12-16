package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context;


import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link org.cleartk.ml.feature.extractor.CleartkExtractor.Context} for extracting annotations appearing after the focus annotation.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class FollowingContext extends AbstractLeftToRightContext {

   /**
    * Constructs a context that will extract features over the following N annotations.
    *
    * @param end The number of annotations to extract.
    */
   public FollowingContext( int end ) {
      super( 0, end );
   }

   /**
    * Constructs a context that will extract features over a slice of the following N annotations.
    * <p>
    * The {@code begin} and {@code end} indexes count from 0, where index 0 identifies the
    * annotation immediately following the focus annotation. If either index is greater than the
    * index of the last possible annotation, special "out of bounds" features will be added for
    * each annotation that was requested but absent.
    *
    * @param begin The index of the first annotation to include.
    * @param end   The index of the last annotation to include. Must be greater than {@code begin}.
    */
   public FollowingContext( int begin, int end ) {
      super( begin, end );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected <T extends Annotation> List<T> select( final JCas jCas,
                                                    final Annotation focusAnnotation,
                                                    final Class<T> annotationClass,
                                                    final int count ) {

//         return JCasUtil.selectFollowing(jCas, annotationClass, focusAnnotation, count);

      final List<T> following = new ArrayList<>( count );

      for ( int i = 0; i < _windowCovered.size(); i++ ) {
         if ( annotationClass.isInstance( _windowCovered.get( i ) )
              && _windowCovered.get( i ).getBegin() >= focusAnnotation.getEnd() ) {
            following.add( (T)_windowCovered.get( i ) );
            if ( following.size() == count ) {
               break;
            }
         }
      }
      return following;
   }

}
