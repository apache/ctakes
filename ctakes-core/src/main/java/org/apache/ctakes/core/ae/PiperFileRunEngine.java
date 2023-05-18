package org.apache.ctakes.core.ae;

import com.lexicalscope.jewel.cli.CliFactory;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.CliOptionals;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;


/**
 * @author SPF , chip-nlp
 * @since {12/29/2022}
 */
@PipeBitInfo(
      name = "PiperFileRunEngine",
      description = "Analysis Engine that executes the PiperFileRunner.  Kludge for desc files (CPE).",
      role = PipeBitInfo.Role.SPECIAL
)
public class PiperFileRunEngine extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PiperFileRunEngine" );

   @ConfigurationParameter(
      name = "PiperParams",
      description = "Command Line Parameters normally used to run a piper file."
   )
   private String _piperParams;

   private AnalysisEngine _analysisEngine;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      if ( _piperParams == null ) {
         usage();
      }
      String piperParams = _piperParams.trim();
      if ( piperParams.startsWith( "\"" ) && piperParams.endsWith( "\"" ) ) {
         piperParams = piperParams.substring( 1, piperParams.length()-1 ).trim();
      }
      if ( piperParams.isEmpty() ) {
         usage();
      }
      LOGGER.info( "Initializing Piper File with parameters " + piperParams + " ...");
      initialize( piperParams.split( "\\s+" ) );
   }

   private void initialize( final String ... args ) {
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
         // if an xmi output directory was specified but the piper didn't add the xmi writer, add the
         if ( !xmiOutDir.isEmpty() ) {
            if ( builder.getAeNames()
                        .stream()
                        .map( String::toLowerCase )
                        .noneMatch( n -> n.contains( "xmiwriter" ) ) ) {
               builder.writeXMIs( xmiOutDir );
            }
         }
         if ( !htmlOutDir.isEmpty() ) {
            if ( builder.getAeNames()
                        .stream()
                        .map( String::toLowerCase )
                        .noneMatch( n -> n.contains( "htmlwriter" ) ) ) {
               builder.writeHtml( htmlOutDir );
            }
         }
         // Initializes.
         final AnalysisEngineDescription description = builder.getAnalysisEngineDesc();
         _analysisEngine = AnalysisEngineFactory.createEngine( description );
      } catch ( UIMAException | IOException multE ) {
         error( multE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Running Pipeline ..." );
      try {
         _analysisEngine.process( jcas );
      } catch ( AnalysisEngineProcessException aepE ) {
         error( aepE );
      }
   }

   static private void usage() {
      LOGGER.error( "No Piper Parameters were specified using PiperParams.\n"
                    + "Mandatory parameter is:\n"
                    + " -p piperFilePath\n"
                    + "Typical optional parameters are:\n"
                    + " -i inputDirectory\n"
                    + " -o outputDirectory\n"
                    + " --key umlsPassKey" );
      System.exit( 1 );
   }

   static private void error( final Exception multE ) {
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
      System.exit( 1 );
   }

}
