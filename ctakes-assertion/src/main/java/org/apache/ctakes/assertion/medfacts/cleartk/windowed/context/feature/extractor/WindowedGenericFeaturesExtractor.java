package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor;

import org.apache.ctakes.assertion.medfacts.cleartk.windowed.classifier.WindowedGenericAttributeClassifier;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class WindowedGenericFeaturesExtractor extends AbstractWindowedFeatureExtractor1<IdentifiedAnnotation> {


   @Override
   public List<Feature> extract( JCas jCas, IdentifiedAnnotation arg ) {

      List<Feature> features = new ArrayList<>();

      // Pull in general dependency-based features -- externalize to another extractor?
      ConllDependencyNode node = DependencyUtility.getNominalHeadNode( jCas, arg );
      if ( node != null ) {
         features.add( new Feature( "DEPENDENCY_HEAD", node.getCoveredText() ) );
         features.add( new Feature( "DEPENDENCY_HEAD_deprel", node.getDeprel() ) );
      }

      HashMap<String, Boolean> featsMap = WindowedGenericAttributeClassifier.extract( jCas, _sentence, arg );

      // Pull in all the features that were used for the rule-based module
      features.addAll( hashToFeatureList( featsMap ) );
      // Pull in the result of the rule-based module as well
      features.add( new Feature( "GENERIC_CLASSIFIER_LOGIC", WindowedGenericAttributeClassifier.classifyWithLogic( featsMap ) ) );


      return features;
   }

   private static Collection<? extends Feature> hashToFeatureList(
         HashMap<String, Boolean> featsIn ) {

      Collection<Feature> featsOut = new HashSet<>();
      for ( String featName : featsIn.keySet() ) {
         featsOut.add( new Feature( featName, featsIn.get( featName ) ) );
      }

      return featsOut;
   }

}
