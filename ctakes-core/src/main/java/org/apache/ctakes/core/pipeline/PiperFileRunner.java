package org.apache.ctakes.core.pipeline;


import com.lexicalscope.jewel.cli.CliFactory;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/13/2016
 */
final public class PiperFileRunner {

   static private final Logger LOGGER = Logger.getLogger( "PiperFileRunner" );

   private PiperFileRunner() {
   }

   /**
    * @param args general run options
    */
   public static void main( final String... args ) {
      if ( !run( args ) ) {
         System.exit( 1 );
      }
   }

   /**
    * @param args general run options
    */
   public static boolean run( final String... args ) {
      final CliOptionals options = CliFactory.parseArguments( CliOptionals.class, args );
      try {
         final PiperFileReader reader = new PiperFileReader();
         final PipelineBuilder builder = reader.getBuilder();
         // set the input directory parameter if needed
         final String inputDir = options.getInputDirectory();
         if ( !inputDir.isEmpty() ) {
            builder.set( ConfigParameterConstants.PARAM_INPUTDIR, inputDir );
         }
         // set the output directory parameter if needed
         final String outputDir = options.getOutputDirectory();
         // if xmi output directory is set but standard output directory is not, use xmi out as standard out
         final String xmiOutDir = options.getXmiOutDirectory();
         // if html output directory is set but standard output directory is not, use html out as standard out
         final String htmlOutDir = options.getHtmlOutDirectory();
         if ( !outputDir.isEmpty() ) {
            builder.set( ConfigParameterConstants.PARAM_OUTPUTDIR, outputDir );
         } else if ( !xmiOutDir.isEmpty() ) {
            builder.set( ConfigParameterConstants.PARAM_OUTPUTDIR, xmiOutDir );
         } else if ( !htmlOutDir.isEmpty() ) {
            builder.set( ConfigParameterConstants.PARAM_OUTPUTDIR, htmlOutDir );
         }
         // load the piper file
         reader.setCliOptionals( options );
         reader.loadPipelineFile( options.getPiperPath() );
         // if an input directory was specified but the piper didn't add a collection reader, add the default reader
         if ( !inputDir.isEmpty() && builder.getReader() == null ) {
            builder.readFiles( inputDir );
         }
         // if an xmi output directory was specified but the piper didn't add the xmi writer, add the
         if ( !xmiOutDir.isEmpty() ) {
            if ( builder.getAeNames().stream().map( String::toLowerCase )
                  .noneMatch( n -> n.contains( "xmiwriter" ) ) ) {
               builder.writeXMIs( xmiOutDir );
            }
         }
         if ( !htmlOutDir.isEmpty() ) {
            if ( builder.getAeNames().stream().map( String::toLowerCase )
                        .noneMatch( n -> n.contains( "htmlwriter" ) ) ) {
               builder.writeHtml( htmlOutDir );
            }
         }
         // run the pipeline
         builder.run();
      } catch ( UIMAException | IOException multE ) {
         LOGGER.error( multE.getMessage() );
         final String logPath = Paths.get( "cTAKES.error.log" ).toFile().getAbsolutePath();
         try {
            final PrintStream stream = new PrintStream( logPath );
            multE.printStackTrace( stream );
            LOGGER.info( "\nFor more information please see log file " + logPath );
            LOGGER.info( "This is a log file on your machine listing information that may be useful in debugging your failed run." );
            LOGGER.info(
                  "Seriously, don't ignore this message.  If you want to get to the root of a problem, check the error log file " +
                  logPath );
         } catch ( FileNotFoundException fnfE ) {
            LOGGER.warn( "Could not write to log file " + logPath );
            multE.printStackTrace();
         }
         return false;
      }
      return true;
   }


}
