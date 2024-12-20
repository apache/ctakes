package org.apache.ctakes.core.pipeline;


import com.lexicalscope.jewel.cli.CliFactory;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UIMAException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *   Runs the pipeline in the piper file specified by -p (piperfile)
 *   with any other provided arguments.  Standard arguments are:
 *     -i , --inputDir {inputDirectory}
 *     -o , --outputDir {outputDirectory}
 *     -s , --subDir {subDirectory}  (for i/o)
 *     --xmiOut {xmiOutputDirectory} (if different from -o)
 *     -l , --lookupXml {dictionaryConfigFile} (fast only)
 *     --key {umlsKey}
 *     -? , --help
 *   Other parameters may be declared in the piper file using the cli command:
 *     cli {parameterName}={singleCharacter}
 *   For instance, for declaration of ParagraphAnnotator path to regex file optional parameter PARAGRAPH_TYPES_PATH,
 *   in the custom piper file add the line:
 *     cli PARAGRAPH_TYPES_PATH=t
 *   and when executing this class use:
 *      PiperFileRunner( -p, path/to/my/custom.piper, -t, path/to/my/custom.bsv  ... );
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/13/2016
 */
final public class PiperFileRunner {

   static private final Logger LOGGER = LoggerFactory.getLogger( "PiperFileRunner" );

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
         // Workaround https://github.com/apache/uima-uimaj/issues/234
         // https://github.com/ClearTK/cleartk/issues/470
         try {
            LOGGER.debug( "Creating empty CAS to make certain that the typesystem is initialized ..." );
            CasCreationUtils.createCas();
         } catch ( ResourceInitializationException riE ) {
            LOGGER.error( "Could not create base CAS for initialization.\n{}", riE.getMessage() );
            LOGGER.error( Arrays.stream( riE.getStackTrace() )
                                .map( StackTraceElement::toString )
                                .collect( Collectors.joining("\n" ) ) );
         }
         // run the pipeline
         builder.run();
      } catch ( UIMAException | IOException multE ) {
         LOGGER.error( multE.getMessage() );
         final String logPath = Paths.get( "cTAKES.error.log" ).toFile().getAbsolutePath();
         try {
            final PrintStream stream = new PrintStream( logPath );
            multE.printStackTrace( stream );
            LOGGER.info( "\nFor more information please see log file {}", logPath );
            LOGGER.info( "This is a log file on your machine listing information that may be useful in debugging your failed run." );
            LOGGER.info(
                  "Seriously, don't ignore this message.  If you want to get to the root of a problem, check the error log file {}",
                  logPath );
         } catch ( FileNotFoundException fnfE ) {
            LOGGER.warn( "Could not write to log file {}", logPath );
            Arrays.stream( multE.getStackTrace() ).map( Object::toString ).forEach( LOGGER::warn );
         }
         return false;
      }
      return true;
   }


}
