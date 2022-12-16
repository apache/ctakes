package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context;


import org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.WindowedContextFeature;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import java.util.ArrayList;
import java.util.List;

/**
 * * A {@link CleartkExtractor.Context} that aggregates the features of other contexts into a "bag" where position
 * * information of each individual feature is no longer maintained. Position information is not
 * * entirely lost - the span of the bag is encoded as part of the feature name that is shared by
 * * all of the features within the bag.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class WindowedBag implements CleartkExtractor.Context {
   private CleartkExtractor.Context[] contexts;

   private String name;

   /**
    * Constructs a {@link CleartkExtractor.Context} which converts the features extracted by the argument contexts
    * into a bag of features where all features have the same name.
    *
    * @param contexts The contexts which should be combined into a bag.
    */
   public WindowedBag( CleartkExtractor.Context... contexts ) {
      this.contexts = contexts;
      String[] names = new String[ contexts.length + 1 ];
      names[ 0 ] = "Bag";
      for ( int i = 1; i < names.length; ++i ) {
         names[ i ] = contexts[ i - 1 ].getName();
      }
      this.name = Feature.createName( names );
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public <SEARCH_T extends Annotation> List<Feature> extract(
         JCas jCas,
         Annotation focusAnnotation,
         CleartkExtractor.Bounds bounds,
         Class<SEARCH_T> annotationClass,
         FeatureExtractor1<SEARCH_T> extractor ) throws CleartkExtractorException {
      List<Feature> features = new ArrayList<>();
      for ( CleartkExtractor.Context context : this.contexts ) {
         for ( Feature feature : context.extract(
               jCas,
               focusAnnotation,
               bounds,
               annotationClass,
               extractor ) ) {
            WindowedContextFeature contextFeature = (WindowedContextFeature)feature;
            Feature f2 = new Feature( contextFeature.feature.getName(), feature.getValue() );
            features.add( new WindowedContextFeature( this.getName(), f2 ) );
         }
      }
      return features;
   }

}