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
      name = "PythonRunner",
      description = "Starts a Python process with the given parameters.",
      role = PipeBitInfo.Role.SPECIAL
)
public class PythonRunner extends AbstractCommandRunner {

   static private final Logger LOGGER = LoggerFactory.getLogger( "PythonRunner" );

   static public final String VENV_PARAM = "VirtualEnv";
   static public final String VENV_DESC = "Path to Python virtual environment.";
   @ConfigurationParameter(
         name = VENV_PARAM,
         description = VENV_DESC,
         mandatory = false
   )
   private String _venv;



   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _venv = SystemUtil.subVariableParameters( _venv, context );
      if ( _venv != null && !_venv.isEmpty() && !new File( _venv ).exists() ) {
         LOGGER.warn( "Cannot find Virtual Environment Directory " + _venv );
      }
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


   private String getModule() {
      final String command = getCommand().trim();
      final int dashMindex = command.indexOf( "-m " );
      if ( dashMindex >= 0 ) {
         final int spaceIndex = command.indexOf( ' ', dashMindex + 3 );
         if ( spaceIndex > 0 ) {
            return command.substring( dashMindex + 3, spaceIndex );
         }
         return command.substring( dashMindex + 3 );
      }
      final int spaceIndex = command.indexOf( ' ' );
      if ( spaceIndex < 0 ) {
         return command;
      }
      return command.substring( 0, spaceIndex );
   }

   private String getModuleName() {
      String module = getModule();
      if ( module.endsWith( ".py" ) ) {
         module = module.substring( 0, module.length()-3 );
      }
      int slashIndex = module.lastIndexOf( '/' );
      if ( slashIndex < 0 ) {
         slashIndex = module.lastIndexOf( '\\' );
      }
      if ( slashIndex < 0 ) {
         slashIndex = module.lastIndexOf( '.' );
      }
      if ( slashIndex >= 0 ) {
         return module.substring( slashIndex + 1 );
      }
      return module;
   }

   protected String getFullCommand() {
      final String commandDir = getCommandDir();
      final String command = getCommand();
      return ( commandDir == null || commandDir.isEmpty() )
             ? "python -u " + command
             : commandDir + File.separator + "python -u " + command;
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
      final String workingDir = getWorkingDir();
      if ( workingDir != null && !workingDir.isEmpty() ) {
         runner.setDirectory( workingDir );
      }
      if ( _venv != null && !_venv.trim().isEmpty() ) {
         runner.setVenv( _venv );
      }
      LOGGER.info( "Starting " + command + " ..." );
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
      return LoggerFactory.getLogger( getModuleName() );
   }


   static public AnalysisEngineDescription createEngineDescription( final String venv,
                                                                    final String command )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( PythonRunner.class,
                                                            PythonRunner.VENV_PARAM, venv,
                                                            AbstractCommandRunner.CMD_PARAM, command );
   }


}
