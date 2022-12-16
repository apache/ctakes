package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor;


import org.apache.ctakes.assertion.util.SemanticClasses;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.FragmentUtils;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.util.CleartkInitializationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
abstract public class AbstractTreeFragmentFeatureExtractor1
      extends AbstractWindowedFeatureExtractor1<IdentifiedAnnotation> {

   public static final String PARAM_OUTPUTDIR = "outputDir";
   public static final String PARAM_SEMDIR = "semDir";
   protected HashSet<SimpleTree> frags = null;
   protected SemanticClasses sems = null;
   protected String prefix = null;

   public AbstractTreeFragmentFeatureExtractor1( String prefix, String resourceFilename )
         throws CleartkInitializationException {
      initializeFrags( resourceFilename );
      this.prefix = prefix;
      try {
         sems = new SemanticClasses( FileLocator.getAsStream( "org/apache/ctakes/assertion/all_cues.txt" ) );
      } catch ( Exception e ) {
         throw new CleartkInitializationException( e, "org/apache/ctakes/assertion/all_cues.txt", "Could not find semantic classes resource.", new Object[] {} );
      }
   }

   private void initializeFrags( String resourceFilename ) {
      frags = new HashSet<SimpleTree>();
      InputStream fragsFilestream = null;
      try {
         fragsFilestream = FileLocator.getAsStream( resourceFilename );
         Scanner scanner = new Scanner( fragsFilestream );
         while ( scanner.hasNextLine() ) {
            frags.add( FragmentUtils.frag2tree( scanner.nextLine().trim() ) );
         }
         fragsFilestream.close();
      } catch ( IOException e ) {
         System.err.println( "Trouble with tree fragment file: " + e );
      }
   }

   @Override
   public abstract List<Feature> extract( JCas jcas, IdentifiedAnnotation annotation );

}
