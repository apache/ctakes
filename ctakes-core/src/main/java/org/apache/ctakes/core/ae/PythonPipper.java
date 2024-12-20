package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @since {1/8/2024}
 */
@PipeBitInfo(
      name = "PythonPipper",
      description = "Will pip a specified python package.",
      role = PipeBitInfo.Role.SPECIAL
)
public class PythonPipper extends PythonRunner {
   static private final Logger LOGGER = LoggerFactory.getLogger( "PythonPipper" );

   static public final String PIP_PACKAGE_PARAM = "PipPackage";
   static public final String PIP_PACKAGE_DESC = "Path of the python package to pip.";
   @ConfigurationParameter(
         name = PIP_PACKAGE_PARAM,
         description = PIP_PACKAGE_DESC
   )
   private String _pipPackage;

   static public final String PIP_PARAM = "RunPip";
   static public final String PIP_DESC = "Run pip on the given package.  Set to no if pip is unwanted.";
   @ConfigurationParameter (
         name = PIP_PARAM,
         description = PIP_DESC,
         mandatory = false,
         defaultValue = "Yes"
   )
   private String _runPip;


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
   }

   private boolean runPip() {
      return _runPip.isEmpty()
            || _runPip.equalsIgnoreCase( "yes" )
            || _runPip.equalsIgnoreCase( "true" );
   }

   /**
    * Command is always pip.
    * @return false
    */
   @Override
   protected boolean isCommandMandatory() {
      return false;
   }

   /**
    *
    * {@inheritDoc}
    */
   @Override
   protected String getCommand() {
      return "-m pip install " + _pipPackage;
   }

   /**
    * Always wait on a pip
    * @return true
    */
   @Override
   protected boolean shouldWait() {
      return true;
   }

   /**
    * Only run if _pipPbj is yes.
    * @throws IOException -
    */
   protected void runCommand() throws IOException {
      if ( runPip() ) {
         super.runCommand();
      } else {
         LOGGER.info( "Skipping pip of {}", _pipPackage );
      }
   }

}
