package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor;

import org.apache.ctakes.assertion.pipelines.GenerateDependencyRepresentation;
import org.apache.ctakes.assertion.util.AssertionDepUtils;
import org.apache.ctakes.assertion.util.AssertionTreeUtils;
import org.apache.ctakes.constituency.parser.util.TreeUtils;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.util.CleartkInitializationException;

import java.util.ArrayList;
import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class WindowedDependencyWordsFragmentExtractor extends AbstractTreeFragmentFeatureExtractor1 {

   public WindowedDependencyWordsFragmentExtractor( String prefix, String fragsPath )
         throws CleartkInitializationException {
      super( prefix, fragsPath );
   }

   @Override
   public List<Feature> extract( JCas jCas, IdentifiedAnnotation mention ) {
      List<Feature> features = new ArrayList<Feature>();

//      List<Sentence> sents = JCasUtil.selectCovering(jCas, Sentence.class, mention.getBegin(), mention.getEnd());
//      if(sents != null && sents.size() > 0){

      Sentence sent = _sentence;
      List<ConllDependencyNode> nodes = JCasUtil.selectCovered( ConllDependencyNode.class, sent );

      SimpleTree tree
            = AssertionDepUtils.getTokenTreeString( jCas, nodes, mention, GenerateDependencyRepresentation.UP_NODES );
      if ( tree == null ) {
         System.err.println( "Tree is null!" );
      } else {
         AssertionTreeUtils.replaceDependencyWordsWithSemanticClasses( tree, sems );
         for ( SimpleTree frag : frags ) {
            if ( TreeUtils.containsDepFragIgnoreCase( tree, frag ) ) {
               features.add( new Feature( "TreeFrag_" + prefix, frag.toString() ) );
            }
         }
      }

//      }
      return features;
   }

}
