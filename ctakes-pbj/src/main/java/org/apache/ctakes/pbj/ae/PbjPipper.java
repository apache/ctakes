package org.apache.ctakes.pbj.ae;

import org.apache.ctakes.core.ae.PythonRunner;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;

/**
 * @author DJ , chip-nlp
 * @since {2/2/2023}
 */
@PipeBitInfo(
      name = "PbjPipper",
      description = "Will pip PBJ based upon user request.",
      role = PipeBitInfo.Role.SPECIAL
)
public class PbjPipper extends PythonRunner {

   static private final Logger LOGGER = Logger.getLogger( "PbjPipper" );
   // to add a configuration parameter, type "param" and hit tab.

   static public final String PIP_PBJ_PARAM = "PipPbj";
   static public final String PIP_PBJ_DESC = "pip or do not pip PBJ python code.  Default is yes.";
   @ConfigurationParameter(
         name = PIP_PBJ_PARAM,
         description = PIP_PBJ_DESC,
         mandatory = false,
         defaultValue = "yes"
   )
   private String _pipPbj;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
   }

   /**
    * Does nothing.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
   }

   private boolean doPip() {
      return _pipPbj.isEmpty()
             || _pipPbj.equalsIgnoreCase( "yes" )
             || _pipPbj.equalsIgnoreCase( "true" );
   }

   protected boolean isCommandMandatory() {
      return false;
   }

   /**
    *
    * @return false as we only want to do this on initialization.
    */
   protected boolean processPerDoc() {
      return false;
   }

   /**
    *
    * @return the pip command to install PBJ.
    */
   protected String getCommand() {
      return "-m pip install resources/org/apache/ctakes/pbj/ctakes_pbj_py/";
   }

   /**
    *
    * @return true
    */
   protected boolean shouldWait() {
      return true;
   }

   /**
    * Only run if _pipPbj is yes.
    * @throws IOException -
    */
   protected void runCommand() throws IOException {
      if ( doPip() ) {
         LOGGER.info( "Since ctakes-pbj is pip installed from source, pip will always perform an install." );
         LOGGER.info( "To turn off the pip use \"set " + PIP_PBJ_PARAM + "=no\" in your piper file" );
         LOGGER.info( " or add \"--pipPbj no\" to your command line." );
         super.runCommand();
      }
   }


}
