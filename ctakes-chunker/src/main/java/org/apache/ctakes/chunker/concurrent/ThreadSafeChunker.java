package org.apache.ctakes.chunker.concurrent;

import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
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
      name = "Thread safe Chunker",
      description = "Annotator that generates chunks of any kind as specified by the chunker model and the chunk creator.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = { PipeBitInfo.TypeProduct.CHUNK }
)
final public class ThreadSafeChunker extends Chunker {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeChunker" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      ChunkerSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      ChunkerSingleton.getInstance().process( jCas );
   }

   /**
    * @return a chunker using a default model
    * @throws ResourceInitializationException -
    */
   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ThreadSafeChunker.class );
   }

   /**
    * @param model a chunker model
    * @return a chunker using the given model
    * @throws ResourceInitializationException -
    */
   public static AnalysisEngineDescription createAnnotatorDescription( final String model ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ThreadSafeChunker.class,
            Chunker.PARAM_CHUNKER_MODEL_FILE, model );
   }


   private enum ChunkerSingleton implements ThreadSafeWrapper<Chunker> {
      INSTANCE;

      static public ChunkerSingleton getInstance() {
         return INSTANCE;
      }

      private final Chunker _delegate;
      private boolean _initialized;

      ChunkerSingleton() {
         _delegate = new Chunker();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public Chunker getDelegate() {
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
