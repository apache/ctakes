package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.external.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
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
public class CtakesRunner extends PausableFileLoggerAE {

   static private final Logger LOGGER = LoggerFactory.getLogger( "CtakesRunner" );

   static public final String PIPE_PARAM = "Pipeline";
   static public final String PIPE_DESC = "Piper parameters. Make sure to quote.";
   @ConfigurationParameter(
         name = PIPE_PARAM,
         description = PIPE_DESC
   )
   private String _pipeline;


   static private final String JAVA_CMD = "-Xms512M -Xmx3g org.apache.ctakes.core.pipeline.PiperFileRunner";


   protected boolean processPerDoc() {
      return false;
   }

   protected Logger getPauseLogger() {
      return LOGGER;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _pipeline = SystemUtil.subVariableParameters( _pipeline, context );
      try {
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

   /**
    *
    * {@inheritDoc}
    */
   @Override
   protected String getLogFile() {
      final String logFile = super.getLogFile();
      if ( logFile != null && !logFile.isEmpty() ) {
         return logFile;
      }
      String piper = "UnknownPiper";
      try {
         piper = getPiper();
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      return getLogFile( "ctakes_" + piper + ".log" );
   }


   private String getPiper() throws IOException {
      final int pIndex = _pipeline.indexOf( "-p " );
      if ( pIndex < 0 ) {
         throw new IOException( "Improper Piper Runner Specification " + _pipeline );
      }
      final int spaceIndex = _pipeline.indexOf( ' ', pIndex + 4 );
      if ( spaceIndex < 4 ) {
         throw new IOException( "Improper Piper Runner Specification " + _pipeline );
      }
      String piper = _pipeline.substring( pIndex + 3, spaceIndex );
      int slashIndex = piper.lastIndexOf( '/' );
      if ( slashIndex < 0 ) {
         slashIndex = piper.lastIndexOf( '\\' );
      }
      if ( slashIndex >= 0 ) {
         piper = piper.substring( slashIndex + 1 );
      }
      if ( piper.endsWith( ".piper" ) ) {
         piper = piper.substring( 0, piper.length()-6 );
      }
      return piper;
   }

   protected void runCommand() throws IOException {
      final String java_home = System.getProperty( "java.home" );
      final String fullCommand = "\"" + java_home + File.separator + "bin" + File.separator
            + "java\" " + JAVA_CMD + " " + _pipeline;
      final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( fullCommand );
      final String logFile = getLogFile();
      runner.setLogFiles( logFile );
//      LOGGER.info( "Starting cTAKES with " + _cli + " ..." );
      LOGGER.info( "Starting external cTAKES pipeline with {} ...", _pipeline );
//      LOGGER.info( "Full command:\n{}", fullCommand );
      SystemUtil.run( runner );
      pause();
   }


   static public AnalysisEngineDescription createEngineDescription( final String pipe )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( CtakesRunner.class, CtakesRunner.PIPE_PARAM, pipe );
   }


}
