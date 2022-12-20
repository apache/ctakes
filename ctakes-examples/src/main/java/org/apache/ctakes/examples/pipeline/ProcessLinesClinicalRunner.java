package org.apache.ctakes.examples.pipeline;


import org.apache.ctakes.core.cr.LinesFromFileCollectionReader;
import org.apache.ctakes.core.pipeline.EntityCollector;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.File;
import java.io.IOException;

/**
 * Build and run a pipeline using a {@link PiperFileReader} and a {@link PipelineBuilder}.
 * <p>
 * Example of a running a pipeline programmatically w/o uima xml descriptor xml files
 * Adds the default Clinical pipeline and reading from lines in a file
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2016
 */
final public class ProcessLinesClinicalRunner {

   static private final Logger LOGGER = Logger.getLogger( "ProcessLinesClinicalRunner" );

   static private final String PIPER_FILE_PATH = "org/apache/ctakes/clinical/pipeline/DefaultFastPipeline.piper";

   static private final String INPUT_FILE_PATH = "org/apache/ctakes/examples/notes/right_knee_arthroscopy";

   private ProcessLinesClinicalRunner() {
   }


   /**
    * @param args none
    */
   public static void main( final String... args ) {
      try {
         // Create a piper file reader, but don't load the piper yet - we want to create a reader with parameters
         final PiperFileReader reader = new PiperFileReader();
         final PipelineBuilder builder = reader.getBuilder();
         // Add the Lines from File reader
//         final File inputFile = FileLocator.locateFile( INPUT_FILE_PATH );
         final File inputFile = FileLocator.getFile( INPUT_FILE_PATH );
         builder.reader( LinesFromFileCollectionReader.class,
               LinesFromFileCollectionReader.PARAM_INPUT_FILE_NAME, inputFile.getAbsolutePath() );
         // Add the lines from the piper file
         reader.loadPipelineFile( PIPER_FILE_PATH );
         // Collect IdentifiedAnnotation object information for output - simple for examples
         builder.collectEntities();
         // Run the pipeline with specified text
         builder.run();
         // Log the IdentifiedAnnotation object information
         LOGGER.info( "\n" + EntityCollector.getInstance().toString() );
      } catch ( IOException | UIMAException multE ) {
         LOGGER.error( multE.getMessage() );
      }
   }


}
