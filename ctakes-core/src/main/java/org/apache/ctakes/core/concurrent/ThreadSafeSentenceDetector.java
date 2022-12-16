package org.apache.ctakes.core.concurrent;

import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
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
      name = "Thread Safe Sentence Detector",
      description = "Annotates Sentences based upon an OpenNLP model.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.SENTENCE }
)
final public class ThreadSafeSentenceDetector extends SentenceDetector {

   static private final Logger LOGGER = Logger.getLogger( "ThreadedSentenceDetector" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      SdSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      SdSingleton.getInstance().process( jCas );
   }

   /**
    * @return a sentence detector
    * @throws ResourceInitializationException -
    */
   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ThreadSafeSentenceDetector.class );
   }

   private enum SdSingleton implements ThreadSafeWrapper<SentenceDetector> {
      INSTANCE;

      static public SdSingleton getInstance() {
         return INSTANCE;
      }

      private final SentenceDetector _delegate;
      private boolean _initialized;

      SdSingleton() {
         _delegate = new SentenceDetector();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public SentenceDetector getDelegate() {
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
   }

}
