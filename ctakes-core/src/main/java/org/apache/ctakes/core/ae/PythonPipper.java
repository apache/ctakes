package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.MutableUimaContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

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
   static private final Logger LOGGER = LogManager.getLogger( "PythonPipper" );

   static public final String PIP_PACKAGE_PARAM = "PipPackage";
   static public final String PIP_PACKAGE_DESC = "Path of the python package to pip.";
   @ConfigurationParameter(
         name = PIP_PACKAGE_PARAM,
         description = PIP_PACKAGE_DESC
   )
   private String _pipPackage;


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


}
