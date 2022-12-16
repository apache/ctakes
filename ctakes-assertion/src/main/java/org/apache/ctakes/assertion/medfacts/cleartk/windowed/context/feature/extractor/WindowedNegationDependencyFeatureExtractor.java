package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor;

import org.apache.ctakes.assertion.util.NegationManualDepContextAnalyzer;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;

import java.util.ArrayList;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class WindowedNegationDependencyFeatureExtractor extends AbstractWindowedFeatureExtractor1<IdentifiedAnnotation> {

   NegationManualDepContextAnalyzer conAnal = null;

   public WindowedNegationDependencyFeatureExtractor(){
      conAnal = new NegationManualDepContextAnalyzer();
   }

   @Override
   public List<Feature> extract( JCas jcas, IdentifiedAnnotation focusAnnotation)
         throws CleartkExtractorException {
      List<Feature> feats = new ArrayList<>();
      Sentence sent = _sentence;

      List<ConllDependencyNode> nodes = DependencyUtility.getDependencyNodes(jcas, sent);
      ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, focusAnnotation);
      try {
         boolean[] regexFeats = conAnal.findNegationContext(nodes, headNode);
         for(int j = 0; j < regexFeats.length; j++){
            if(regexFeats[j]){
               feats.add(new Feature("DepPath_" + conAnal.getRegexName(j))); //"NEG_DEP_REGEX_"+j));
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         throw new CleartkExtractorException(e);
      }
      return feats;
   }

}
