package org.apache.ctakes.examples.pipeline;


import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.examples.ae.ExampleHelloWorldAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.IOException;

/**
 * Build and run a pipeline using a {@link PipelineBuilder}.
 * <p>
 * Example of a running a pipeline programatically w/o uima xml descriptor xml files
 * Adds the default Tokenization pipeline and adding the Example HelloWorld Annotator
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2016
 */
final public class HelloWorldBuilderRunner {

   static private final Logger LOGGER = Logger.getLogger( "HelloWorldBuilderRunner" );

   static private final String DOC_TEXT = "Hello World!";

   private HelloWorldBuilderRunner() {
   }

   /**
    * @param args an output directory for xmi files or none if xmi files are not wanted
    */
   public static void main( final String... args ) {
      try {
         PipelineBuilder builder = new PipelineBuilder();
         builder
               // Add a simple pre-defined existing pipeline for Tokenization.
               // Equivalent of ClinicalPipelineFactory.getTokenProcessingPipeline()
               .add( SimpleSegmentAnnotator.class )
               .add( SentenceDetector.class )
               .add( TokenizerAnnotatorPTB.class )
               .add( ContextDependentTokenizerAnnotator.class )
               // The POSTagger has a -complex- startup, but it can create its own description to handle it
               .addDescription( POSTagger.createAnnotatorDescription() )
               // add the simple Hello World Annotator
               .add( ExampleHelloWorldAnnotator.class );
         if ( args.length > 0 ) {
            // Example to save the Aggregate descriptor to an xml file for external use such as the UIMA CVD
            builder.writeXMIs( args[ 0 ] );
         }
         // Run the pipeline with specified text
         builder.run( DOC_TEXT );
      } catch ( IOException | UIMAException multE ) {
         LOGGER.error( multE.getMessage() );
      }
   }


}
