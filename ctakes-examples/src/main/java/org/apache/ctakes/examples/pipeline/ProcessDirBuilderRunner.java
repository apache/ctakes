package org.apache.ctakes.examples.pipeline;

import org.apache.ctakes.assertion.medfacts.cleartk.*;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.pipeline.EntityCollector;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/12/2016
 */
final public class ProcessDirBuilderRunner {

   static private final Logger LOGGER = Logger.getLogger( "ProcessDirBuilderRunner" );

   static private final String INPUT_DIR = "org/apache/ctakes/examples/notes/annotated";

   private ProcessDirBuilderRunner() {
   }

   /**
    * @param args an output directory for xmi files or none if xmi files are not wanted
    */
   public static void main( final String... args ) {
      try {
         PipelineBuilder builder = new PipelineBuilder();
         builder
               // Read files from a directory
               .readFiles( INPUT_DIR )
               // Add a simple pre-defined existing pipeline for Tokenization.
               // Equivalent of ClinicalPipelineFactory.getTokenProcessingPipeline()
               .add( SimpleSegmentAnnotator.class )
               .add( SentenceDetector.class )
               .add( TokenizerAnnotatorPTB.class )
               .add( ContextDependentTokenizerAnnotator.class )
               // The POSTagger has a -complex- startup, but it can create its own description to handle it
               .addDescription( POSTagger.createAnnotatorDescription() )
               // Add the Dependency parser for use by cleartk
               .addDescription( ClearNLPDependencyParserAE.createAnnotatorDescription() )
               // Add the cleartk attribute engines
               .addDescription( PolarityCleartkAnalysisEngine.createAnnotatorDescription() )
               .addDescription( UncertaintyCleartkAnalysisEngine.createAnnotatorDescription() )
               .addDescription( HistoryCleartkAnalysisEngine.createAnnotatorDescription() )
               .addDescription( ConditionalCleartkAnalysisEngine.createAnnotatorDescription() )
               .addDescription( GenericCleartkAnalysisEngine.createAnnotatorDescription() )
               .addDescription( SubjectCleartkAnalysisEngine.createAnnotatorDescription() )
               // Collect discovered entities for post-run information
               .collectEntities();
         if ( args.length > 0 ) {
            // Example to save the Aggregate descriptor to an xml file for external use such as the UIMA CVD
            builder.writeXMIs( args[ 0 ] );
         }
         // Run the pipeline with specified text
         builder.run();
         //Print out the IdentifiedAnnotation objects
         LOGGER.info( "\n" + EntityCollector.getInstance().toString() );
      } catch ( IOException | UIMAException multE ) {
         LOGGER.error( multE.getMessage() );
      }
   }

}
