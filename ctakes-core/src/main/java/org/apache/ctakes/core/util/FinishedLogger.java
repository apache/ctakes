package org.apache.ctakes.core.util;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2018
 * @deprecated use FinishedLogger in (sub) package log
 */
@PipeBitInfo(
      name = "Deprecated Finished Logger",
      description = "use FinishedLogger in (sub) package log.",
      role = PipeBitInfo.Role.SPECIAL
)
final public class FinishedLogger extends JCasAnnotator_ImplBase {

   final org.apache.ctakes.core.util.log.FinishedLogger _delegate
         = new org.apache.ctakes.core.util.log.FinishedLogger();

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      Logger.getLogger( "FinishedLogger" ).warn( "Deprecated use FinishedLogger in (sub) package log." );
      _delegate.initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      _delegate.process( jCas );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      _delegate.collectionProcessComplete();
   }

}