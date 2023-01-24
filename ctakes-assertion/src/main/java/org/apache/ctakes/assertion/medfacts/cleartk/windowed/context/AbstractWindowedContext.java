package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;

import java.util.ArrayList;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
abstract public class AbstractWindowedContext implements CleartkExtractor.Context {

   protected int begin;
   protected int end;
   private final String name;

   protected List<Annotation> _windowCovered = new ArrayList<>();

   public <T extends Annotation> void setWindow( final List<T> windowCovered ) {
      _windowCovered.clear();
      _windowCovered.addAll( windowCovered );
   }


   public AbstractWindowedContext( int begin, int end ) {
      if ( begin > end ) {
         String message = "expected begin < end, found begin=%d end=%d";
         throw new IllegalArgumentException( String.format( message, begin, end ) );
      } else {
         this.begin = begin;
         this.end = end;
         this.name = Feature.createName( this.getClass().getSimpleName(),
               String.valueOf( this.begin ), String.valueOf( this.end ) );
      }
   }

   public String getName() {
      return this.name;
   }

   @SuppressWarnings("unchecked")
   protected <T extends Annotation> List<T> selectCovered( final Annotation covering, final Class<T> coveredClass ) {
//      System.out.print( "LastCoveredContext focusAnnotation "
//                          + covering.getClass().getName() + " " + covering.getCoveredText()
//                          + "   want covering " + coveredClass.getName() );
      final List<T> covered = new ArrayList<>();
      for ( Annotation annotation : _windowCovered ) {
         if ( coveredClass.isInstance( annotation )
              && annotation.getBegin() >= covering.getBegin()
              && annotation.getEnd() <= covering.getEnd() ) {
//            System.out.print( "  Yes " + annotation.getClass().getName() + " " + annotation.getCoveredText() );
            covered.add( (T)annotation );
         }
//         System.out.print( "  No " + annotation.getClass().getName() + " " + annotation.getCoveredText() );
      }
//      System.out.println();
      return covered;
   }

   protected abstract <T extends Annotation> List<T> select( JCas var1, Annotation var2, Class<T> var3, int var4 );

}
