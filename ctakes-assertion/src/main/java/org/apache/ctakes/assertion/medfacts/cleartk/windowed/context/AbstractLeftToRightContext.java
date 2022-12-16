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
import java.util.Iterator;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
abstract public class AbstractLeftToRightContext extends AbstractWindowedContext {

   public AbstractLeftToRightContext( int begin, int end ) {
      super( begin, end );
   }

   public <SEARCH_T extends Annotation> List<Feature> extract( JCas jCas, Annotation focusAnnotation,
                                                               CleartkExtractor.Bounds bounds,
                                                               Class<SEARCH_T> annotationClass,
                                                               FeatureExtractor1<SEARCH_T> extractor ) throws
                                                                                                       CleartkExtractorException {
      String featureName = extractor instanceof NamedFeatureExtractor1
                           ? ((NamedFeatureExtractor1<SEARCH_T>)extractor).getFeatureName()
                           : null;
      List<SEARCH_T> anns = this.select( jCas, focusAnnotation, annotationClass, this.end );
      int oobStart;
      if ( this.begin <= anns.size() ) {
         oobStart = 1;
         anns = anns.subList( this.begin, anns.size() );
      } else {
         oobStart = this.begin - anns.size() + 1;
         anns = new ArrayList<>();
      }
      List<Feature> features = new ArrayList<>();
      Iterator<SEARCH_T> iter = anns.iterator();
      for ( int pos = this.begin, oobPos = oobStart; pos < this.end; pos += 1 ) {
         SEARCH_T ann = iter.hasNext() ? iter.next() : null;
         if ( ann != null && bounds.contains( ann ) ) {
            for ( Feature feature : extractor.extract( jCas, ann ) ) {
               features.add( new WindowedContextFeature( this.getName(), pos, feature ) );
            }
         } else {
            features.add( new WindowedContextFeature( this.getName(), pos, oobPos, featureName ) );
            oobPos += 1;
         }
      }
      return features;
   }


}
