package org.apache.ctakes.examples.pipeline;


import org.apache.ctakes.core.pipeline.EntityCollector;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.IOException;

/**
 * Build and run a pipeline using a {@link PiperFileReader} and a {@link PipelineBuilder}.
 * <p>
 * Example of a running a pipeline programatically w/o uima xml descriptor xml files
 * Adds the default Tokenization pipeline and adding the Example HelloWorld Annotator
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2016
 */
final public class HelloWorldPropsPiperRunner {

   static private final Logger LOGGER = Logger.getLogger( "HelloWorldPropsPiperRunner" );

   static private final String PIPER_FILE_PATH
         = "org/apache/ctakes/examples/pipeline/HelloWorldProps.piper";

   static private final String DOC_TEXT
         = "Hello World!  I feel no pain.  My father takes aspirin.  My sister might have a headache.";

   private HelloWorldPropsPiperRunner() {
   }

   /**
    * @param args an output directory for xmi files or none if xmi files are not wanted
    */
   public static void main( final String... args ) {
      try {
         // Add a simple pre-defined existing pipeline for Tokenization from file
         final PiperFileReader reader = new PiperFileReader( PIPER_FILE_PATH );
         // Add the property extraction pipeline
         PipelineBuilder builder = reader.getBuilder();
         if ( args.length > 0 ) {
            // Example to save the Aggregate descriptor to an xml file for external use such as the UIMA CVD
            builder.writeXMIs( args[ 0 ] );
         }
         // Run the pipeline with specified text
         builder.run( DOC_TEXT );
         // Log the IdentifiedAnnotation objects
         LOGGER.info( "\n" + EntityCollector.getInstance().toString() );
      } catch ( IOException | UIMAException multE ) {
         LOGGER.error( multE.getMessage() );
      }
   }


}
