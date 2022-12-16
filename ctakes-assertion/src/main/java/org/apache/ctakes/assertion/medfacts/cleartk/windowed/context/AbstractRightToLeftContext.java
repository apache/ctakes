package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context;

import org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.WindowedContextFeature;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;

import java.util.ArrayList;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
abstract public class AbstractRightToLeftContext extends AbstractWindowedContext {

   public AbstractRightToLeftContext( int begin, int end ) {
      super( begin, end );
   }

   public <SEARCH_T extends Annotation> List<Feature> extract( JCas jCas, Annotation focusAnnotation,
                                                               CleartkExtractor.Bounds bounds,
                                                               Class<SEARCH_T> annotationClass,
                                                               FeatureExtractor1<SEARCH_T> extractor )
         throws CleartkExtractorException {
      String featureName = extractor instanceof NamedFeatureExtractor1
                           ? ((NamedFeatureExtractor1<SEARCH_T>)extractor).getFeatureName()
                           : null;

      // slice the appropriate annotations from the CAS
      List<SEARCH_T> anns = this.select( jCas, focusAnnotation, annotationClass, this.end );
      int missing = this.end - anns.size();
      anns = anns.subList( 0, Math.max( 0, anns.size() - this.begin ) );

      // figure out how many items are out of bounds
      int oobPos = missing;
      for ( SEARCH_T ann : anns ) {
         if ( !bounds.contains( ann ) ) {
            oobPos += 1;
         }
      }

      // extract features at each position
      List<Feature> features = new ArrayList<Feature>();
      for ( int pos = this.end - 1; pos >= this.begin; pos -= 1 ) {

         // if the annotation at the current position is in bounds, extract features from it
         int adjustedPos = this.end - 1 - pos - missing;
         SEARCH_T ann = adjustedPos >= 0 ? anns.get( adjustedPos ) : null;
         if ( ann != null && bounds.contains( ann ) ) {
            for ( Feature feature : extractor.extract( jCas, ann ) ) {
               features.add( new WindowedContextFeature( this.getName(), pos, feature ) );
            }
         }

         // if the annotation at the current position is out of bounds, add an out-of-bounds feature
         else {
            features.add( new WindowedContextFeature( this.getName(), pos, oobPos, featureName ) );
            oobPos -= 1;
         }
      }
      return features;
   }

}
