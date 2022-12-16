package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor;

import org.apache.ctakes.assertion.medfacts.cleartk.windowed.classifier.WindowedHistoryAttributeClassifier;
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
public class WindowedHistoryFeatureExtractor extends AbstractWindowedFeatureExtractor1<IdentifiedAnnotation> {


   @Override
   public List<Feature> extract( JCas jCas, IdentifiedAnnotation arg ) {

      List<Feature> features = new ArrayList<>();

      // Pull in general dependency-based features -- externalize to another extractor?
      ConllDependencyNode node = DependencyUtility.getNominalHeadNode( jCas, arg );
      if ( node != null ) {
//	      features.add(new Feature("DEPENDENCY_HEAD", node));
         features.add( new Feature( "DEPENDENCY_HEAD_word", node.getCoveredText() ) );
//	    	features.add(new Feature("DEPENDENCY_HEAD_pos", node.getPostag()));
         features.add( new Feature( "DEPENDENCY_HEAD_deprel", node.getDeprel() ) );
//	    	features.add(new Feature("DEPENDENCY_HEAD_lemma", node.getLemma()));
      }

      HashMap<String, Boolean> featsMap
            = WindowedHistoryAttributeClassifier.extract( jCas, _sentences, _sentence, _sentenceIndex, arg );

      // Pull in all the features that were used for the rule-based module
      features.addAll( hashToFeatureList( featsMap ) );

      // Pull in the result of the rule-based module as well
      features.add( new Feature( "HISTORY_CLASSIFIER_LOGIC", WindowedHistoryAttributeClassifier.classifyWithLogic( featsMap ) ) );

      // Add whether it is token preceded by "h/o"
      //features.add(new Feature("PRECEDED_BY_H_SLASH_O", HistoryAttributeClassifier.precededByH_O(jCas, arg)));

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
