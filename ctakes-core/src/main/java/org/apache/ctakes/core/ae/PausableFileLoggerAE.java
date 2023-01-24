package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.ae.inert.PausableAE;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {12/19/2022}
 */
abstract public class PausableFileLoggerAE extends PausableAE {

   static public final String LOG_FILE_PARAM = "LogFile";
   static public final String LOG_FILE_DESC = "File to which cTAKES output should be sent.";
   @ConfigurationParameter(
         name = LOG_FILE_PARAM,
         description = LOG_FILE_DESC,
         mandatory = false
   )
   private String _logFile;

   /**
    *
    * @return true if the process should be run for each document.
    */
   abstract protected boolean processPerDoc();

   /**
    * Some command to run.
    * @throws IOException -
    */
   abstract protected void runCommand() throws IOException;

   /**
    *
    * @return the path of a file to which logs should be written.
    */
   protected String getLogFile() {
      return _logFile;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
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

}
