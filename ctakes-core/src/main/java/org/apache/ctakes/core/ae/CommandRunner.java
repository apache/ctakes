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

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {5/4/2022}
 */
@PipeBitInfo(
      name = "CommandRunner",
      description = "Runs an external process.",
      role = PipeBitInfo.Role.SPECIAL
)
public class CommandRunner extends AbstractCommandRunner {

   static private final Logger LOGGER = LoggerFactory.getLogger( "CommandRunner" );


   static public final String SET_JAVAHOME_PARAM = "SetJavaHome";
   static public final String SET_JAVAHOME_DESC = "Set JAVA_HOME to the Java running cTAKES.  Default is yes.";
   @ConfigurationParameter(
         name = SET_JAVAHOME_PARAM,
         description = SET_JAVAHOME_DESC,
         defaultValue = "yes",
         mandatory = false
   )
   private String _setJavaHome;



   protected boolean setJavaHome() {
      return _setJavaHome.equalsIgnoreCase( "yes" ) || _setJavaHome.equalsIgnoreCase( "true" );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      if ( processPerDoc() ) {
         return;
      }
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
      if ( !processPerDoc() ) {
         return;
      }
      try {
         runCommand();
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }


    protected void runCommand() throws IOException {
      final String command = getFullCommand();
      final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( command );
      final String logFile = getLogFile();
      if ( logFile != null && !logFile.isEmpty() ) {
         runner.setLogFiles( logFile );
      } else {
         final Logger logger = getRunLogger();
         runner.setLogger( logger );
      }
      runner.wait( shouldWait() );
      if ( !setJavaHome() ) {
         runner.setSetJavaHome( false );
      }
      final String workingDir = getWorkingDir();
      if ( workingDir != null && !workingDir.isEmpty() ) {
         runner.setDirectory( workingDir );
      }
      LOGGER.info( "Running " + command + " ..." );
      if ( logFile != null && !logFile.isEmpty() ) {
         LOGGER.info( "Log File is " + logFile );
      }
      SystemUtil.run( runner );
      pause();
   }

   private Logger getRunLogger() {
      final String logName = getLogName();
      if ( logName != null && !logName.isEmpty() ) {
         return LoggerFactory.getLogger( logName );
      }
      final String command = getCommand().trim();
      final int spaceIndex = command.indexOf( ' ' );
      if ( spaceIndex < 0 ) {
         return LoggerFactory.getLogger( command );
      }
      return LoggerFactory.getLogger( command.substring( 0, spaceIndex ) );
   }

   static public AnalysisEngineDescription createEngineDescription( final String command )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( CommandRunner.class,
                                                            AbstractCommandRunner.CMD_PARAM, command );
   }

}
