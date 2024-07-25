package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * @author DJ , chip-nlp
 * @since {2/1/2023}
 */
abstract public class AbstractCommandRunner extends PausableFileLoggerAE {

   static private final Logger LOGGER = LogManager.getLogger( "AbstractCommandRunner" );


   // This parameter is not mandatory so that extending classes can have fixed commands.
   static public final String CMD_PARAM = "Command";
   static public final String CMD_DESC = "A full command line to be executed. Make sure to quote.";
   @ConfigurationParameter(
         name = CMD_PARAM,
         description = CMD_DESC,
         mandatory = false

   )
   private String _cmd;

   static public final String CMD_DIR_PARAM = "CommandDir";
   static public final String CMD_DIR_DESC = "The Command Executable's directory.";
   @ConfigurationParameter(
         name = CMD_DIR_PARAM,
         description = CMD_DIR_DESC,
         mandatory = false
   )
   private String _cmdDir;

   static public final String DIR_PARAM = "WorkingDir";
   static public final String DIR_DESC = "The Working Directory directory.";
   @ConfigurationParameter(
         name = DIR_PARAM,
         description = DIR_DESC,
         mandatory = false
   )
   private String _workDir;

   static public final String PER_DOC_PARAM = "PerDoc";
   static public final String PER_DOC_DESC = "yes to run the command once per document. Default is no.";
   @ConfigurationParameter(
         name = PER_DOC_PARAM,
         description = PER_DOC_DESC,
         defaultValue = "no",
         mandatory = false
   )
   private String _perDoc;

   static public final String LOG_NAME_PARAM = "Log";
   static public final String LOG_NAME_DESC = "A name for the streaming logger.  Default is the Command.";
   @ConfigurationParameter(
         name = LOG_NAME_PARAM,
         description = LOG_NAME_DESC,
         mandatory = false
   )
   private String _logName;

   /**
    *
    * @return true if the command must be explicitly set in the piper file.
    */
   protected boolean isCommandMandatory() {
      return true;
   }

   protected String getCommand() {
      return _cmd;
   }

   protected String getCommandDir() {
      return _cmdDir;
   }

   protected String getWorkingDir() {
      return _workDir;
   }

   protected boolean processPerDoc() {
      return _perDoc.equalsIgnoreCase( "yes" ) || _perDoc.equalsIgnoreCase( "true" );
   }

   protected String getLogName() {
      return _logName;
   }

   protected String getFullCommand() {
      final String commandDir = getCommandDir();
      final String command = getCommand();
      return ( commandDir == null || commandDir.isEmpty() ) ? command : commandDir + File.separator + command;
   }

   public void logInfo( final String info ) {
      LOGGER.info( info );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _cmd = SystemUtil.subVariableParameters( _cmd, context );
      _cmdDir = SystemUtil.subVariableParameters( _cmdDir, context );
      _workDir = SystemUtil.subVariableParameters( _workDir, context );
      if ( _cmdDir != null && !_cmdDir.isEmpty() && !new File( _cmdDir ).exists() ) {
         LOGGER.warn( "Cannot find Command Directory " + _cmdDir );
      }
      if ( _workDir != null && !_workDir.isEmpty() && !new File( _workDir ).exists() ) {
         LOGGER.warn( "Cannot find Working Directory " + _workDir );
      }
      if ( isCommandMandatory() && ( _cmd == null || _cmd.trim().isEmpty() ) ) {
         throw new ResourceInitializationException( new IOException( "Parameter " + CMD_PARAM + " must be set." ) );
      }
   }


}
