package org.apache.ctakes.lvg.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;


/**
 * Utilizes a singleton to access a single instance of the LvgAnnotator.  This should help prevent thread safety issues.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/14/2017
 */
@PipeBitInfo(
      name = "Thread-Safe LVG",
      description = "Annotates Lexical Variants for terms with attempted thread safety.",
      dependencies = PipeBitInfo.TypeProduct.BASE_TOKEN
)
final public class ThreadSafeLvg extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeLvg" );


   /**
    * Calls initialize on the LvgSingleton.
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      LvgSingleton.getInstance().initialize( context );
   }

   /**
    * Calls initialize on the LvgSingleton.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LvgSingleton.getInstance().process( jCas );
   }


   /**
    * Necessary if the lvg.properties need to be copied into a temporary file.  Otherwise all defaults are fine.
    *
    * @return description
    * @throws ResourceInitializationException if the lvg annotator could not be initialized
    * @throws IOException                     if there was a problem creating the default LvgCmdApi resource
    */
   @SuppressWarnings( "resource" )
   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException,
                                                                               IOException {
      return AnalysisEngineFactory.createEngineDescription( ThreadSafeLvg.class,
            LvgAnnotator.PARAM_LVGCMDAPI_RESRC_KEY,
            LvgSingleton.getInstance().getDefaultLvgCmdApi() );
   }

}
