package org.apache.ctakes.relationextractor.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.DegreeOfRelationExtractorAnnotator;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Normally I would use composition and a singleton, but here extension is done for @ConfigurationParameter discovery.
 * Made a singleton mostly for model memory.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/20/2017
 */
@PipeBitInfo(
      name = "Thread safe Degree of Annotator",
      description = "Annotates Degree Of relations.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
      products = { PipeBitInfo.TypeProduct.DEGREE_RELATION }
)
final public class ThreadSafeDegreeExtractor extends DegreeOfRelationExtractorAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeDegreeExtractor" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      DoSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      DoSingleton.getInstance().process( jCas );
   }

   /**
    * @return a degree of relation extractor
    * @throws ResourceInitializationException -
    */
   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ThreadSafeDegreeExtractor.class );
   }

   private enum DoSingleton implements ThreadSafeWrapper<DegreeOfRelationExtractorAnnotator> {
      INSTANCE;

      static public DoSingleton getInstance() {
         return INSTANCE;
      }

      private final DegreeOfRelationExtractorAnnotator _delegate;
      private boolean _initialized;

      DoSingleton() {
         _delegate = new DegreeOfRelationExtractorAnnotator();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public DegreeOfRelationExtractorAnnotator getDelegate() {
         return _delegate;
      }

      @Override
      public boolean isInitialized() {
         return _initialized;
      }

      @Override
      public void setInitialized( final boolean initialized ) {
         _initialized = initialized;
      }

      /**
       * Calls initialize on the single instance if and only if it has not already been initialized
       */
      @Override
      public void initialize( final UimaContext context ) throws ResourceInitializationException {
         synchronized (LOCK) {
            if ( !isInitialized() ) {
               LOGGER.info( "Initializing ..." );
               getDelegate().initialize( context );
               setInitialized( true );
            }
         }
      }

      /**
       * Calls process on the single instance if it is not already processing
       */
      @Override
      public void process( final JCas jCas ) throws AnalysisEngineProcessException {
         synchronized (LOCK) {
            LOGGER.info( "Extracting Degree relations ..." );
            getDelegate().process( jCas );
            LOGGER.info( "Finished." );
         }
      }

   }

}
