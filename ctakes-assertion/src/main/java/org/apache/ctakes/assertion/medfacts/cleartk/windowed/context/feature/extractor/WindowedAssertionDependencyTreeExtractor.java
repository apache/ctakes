package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor;

import com.google.common.collect.Lists;
import org.apache.ctakes.assertion.pipelines.GenerateDependencyRepresentation;
import org.apache.ctakes.assertion.util.AssertionDepUtils;
import org.apache.ctakes.assertion.util.AssertionTreeUtils;
import org.apache.ctakes.assertion.util.SemanticClasses;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.TreeFeature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.util.CleartkInitializationException;

import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class WindowedAssertionDependencyTreeExtractor extends AbstractWindowedFeatureExtractor1<IdentifiedAnnotation> {

   protected SemanticClasses sems = null;

   public WindowedAssertionDependencyTreeExtractor() throws CleartkInitializationException {
      try {
         sems = new SemanticClasses( FileLocator.getAsStream( "org/apache/ctakes/assertion/all_cues.txt" ) );
      } catch ( Exception e ) {
         throw new CleartkInitializationException( e, "org/apache/ctakes/assertion/semantic_classes", "Could not find semantic classes resource.", new Object[] {} );
      }
   }

   @Override
   public List<Feature> extract( JCas jCas, IdentifiedAnnotation arg1 )
         throws CleartkExtractorException {
      List<Feature> feats = Lists.newArrayList();
      TreeFeature f1 = null;
      String treeString = null;

//      List<Sentence> sents = JCasUtil.selectCovering(jCas, Sentence.class, arg1.getBegin(), arg1.getEnd());
//      if(sents == null || sents.size() == 0){
//         treeString = "(S (no parse))";
//      }else{
      Sentence sent = _sentence;
      List<ConllDependencyNode> nodes = JCasUtil.selectCovered( ConllDependencyNode.class, sent );

      //treeString = AnnotationDepUtils.getTokenRelTreeString(jCas, nodes, new Annotation[]{arg1}, new String[]{"CONCEPT"}, true);
//      treeString = AssertionDepUtils.getTokenRelTreeString(jCas, nodes, arg1, "CONCEPT");
      SimpleTree tree
            = AssertionDepUtils.getTokenTreeString( jCas, nodes, arg1, GenerateDependencyRepresentation.UP_NODES );

      if ( tree == null ) {
         treeString = "(S (no parse))";
      } else {
         AssertionTreeUtils.replaceDependencyWordsWithSemanticClasses( tree, sems );
         treeString = tree.toString();
//        treeString = treeString.replaceAll("\\(([^ ]+) \\)", "$1");
      }
//      }

      f1 = new TreeFeature( "TK_DW", treeString );
      feats.add( f1 );
      return feats;
   }

}
