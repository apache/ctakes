package org.apache.ctakes.core.concurrent;

import org.apache.ctakes.core.ae.SentenceDetectorAnnotatorBIO;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

/**
 * Normally I would use composition and a singleton, but here extension is done for @ConfigurationParameter discovery.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/19/2017
 */
@PipeBitInfo(
      name = "Thread Safe Sentence Detector BIO",
      description = "Thread safe sentence detector that uses B I O for determination.  " +
            "Useful for documents in which newlines may not indicate sentence boundaries.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = PipeBitInfo.TypeProduct.SECTION,
      products = PipeBitInfo.TypeProduct.SENTENCE
)
final public class ThreadSafeSentenceDetectorBio extends SentenceDetectorAnnotatorBIO {

   static private final Logger LOGGER = LoggerFactory.getLogger( "ThreadSafeSentenceDetectorBio" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      SdBioSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      SdBioSingleton.getInstance().process( jCas );
   }

   /**
    * @param modelPath model using b i o tagging
    * @return a sentence detector using the given model
    * @throws ResourceInitializationException -
    */
   public static AnalysisEngineDescription createAnnotatorDescription( final String modelPath ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeSentenceDetectorBio.class,
            SentenceDetectorAnnotatorBIO.PARAM_IS_TRAINING,
            false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            modelPath,
            SentenceDetectorAnnotatorBIO.PARAM_FEAT_CONFIG,
            SentenceDetectorAnnotatorBIO.FEAT_CONFIG.CHAR );
   }

   /**
    * @return a sentence detector using a default model
    * @throws ResourceInitializationException -
    */
   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return createAnnotatorDescription( "/org/apache/ctakes/core/models/sentdetect/model.jar" );
   }

   private enum SdBioSingleton implements ThreadSafeWrapper<SentenceDetectorAnnotatorBIO> {
      INSTANCE;

      static public SdBioSingleton getInstance() {
         return INSTANCE;
      }

      private final SentenceDetectorAnnotatorBIO _delegate;
      private boolean _initialized;

      SdBioSingleton() {
         _delegate = new SentenceDetectorAnnotatorBIO();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public SentenceDetectorAnnotatorBIO getDelegate() {
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
