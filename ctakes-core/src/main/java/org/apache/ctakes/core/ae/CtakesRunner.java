package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {5/10/2022}
 */
@PipeBitInfo(
      name = "CtakesRunner",
      description = "Starts a new instance of cTAKES with the given piper parameters.",
      role = PipeBitInfo.Role.SPECIAL
)
public class CtakesRunner extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "CtakesRunner" );

   static public final String CLI_PARAM = "Pipe";
   static public final String CLI_DESC = "Piper parameters. Make sure to quote.";
   @ConfigurationParameter(
         name = CLI_PARAM,
         description = CLI_DESC
   )
   private String _cli;

   static public final String LOG_FILE_PARAM = "LogFile";
   static public final String LOG_FILE_DESC = "File to which cTAKES output should be sent.";
   @ConfigurationParameter(
         name = LOG_FILE_PARAM,
         description = LOG_FILE_DESC,
         mandatory = false
   )
   private String _logFile;

   static public final String PAUSE_PARAM = "Pause";
   static public final String PAUSE_DESC = "Pause for some seconds after launching.  Default is 0";
   @ConfigurationParameter(
         name = PAUSE_PARAM,
         description = PAUSE_DESC,
         mandatory = false
   )
   private int _pause = 0;

   static private final String JAVA_CMD = "-Xms512M -Xmx3g org.apache.ctakes.core.pipeline.PiperFileRunner";

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _cli = SystemUtil.subVariableParameters( _cli, context );
      try {
         final String piper = getPiper();
         if ( _logFile == null || _logFile.isEmpty() ) {
            _logFile = "ctakes_" + piper + ".log";
         }
         runCommand();
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      // Implementation of the process(..) method is mandatory, even if it does nothing.
   }


   private String getPiper() throws IOException {
      final int pIndex = _cli.indexOf( "-p " );
      if ( pIndex < 0 ) {
         throw new IOException( "Improper Piper Runner Specification " + _cli );
      }
      final int spaceIndex = _cli.indexOf( ' ', pIndex + 4 );
      if ( spaceIndex < 4 ) {
         throw new IOException( "Improper Piper Runner Specification " + _cli );
      }
      String piper = _cli.substring( pIndex + 3, spaceIndex );
      int slashIndex = piper.lastIndexOf( '/' );
      if ( slashIndex < 0 ) {
         slashIndex = piper.lastIndexOf( '\\' );
      }
      if ( slashIndex >= 0 ) {
         piper = piper.substring( slashIndex + 1 );
      }
      return piper;
   }

   private void runCommand() throws IOException {
      final String java_home = System.getProperty( "java.home" );
      final SystemUtil.CommandRunner runner =
            new SystemUtil.CommandRunner( "\"" + java_home + File.separator + "bin" + File.separator
                                          + "java\" " + JAVA_CMD + " " + _cli );
      runner.setLogFiles( _logFile );
//      LOGGER.info( "Starting cTAKES with " + _cli + " ..." );
      LOGGER.info( "Starting external cTAKES pipeline with " + _cli + " ..." );
      SystemUtil.run( runner );
      if ( _pause < 1 ) {
         return;
      }
      final long pause = _pause * 1000L;
      LOGGER.info( "Pausing " + _pause + " seconds ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         Thread.sleep( pause );
      } catch ( IOException | InterruptedException multE ) {
         // do nothing
      }
   }


   static public AnalysisEngineDescription createEngineDescription( final String pipe )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( CtakesRunner.class, CtakesRunner.CLI_PARAM, pipe );
   }


}
