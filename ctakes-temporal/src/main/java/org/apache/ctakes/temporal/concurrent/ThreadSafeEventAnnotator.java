package org.apache.ctakes.temporal.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;

/**
 * Normally I would use composition and a singleton, but here extension is done for @ConfigurationParameter discovery.
 * Made a singleton mostly for model memory.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/20/2017
 */
@PipeBitInfo(
      name = "Thread safe Event Annotator",
      description = "Annotates Temporal Events.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.CHUNK, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION },
      products = { PipeBitInfo.TypeProduct.EVENT }
)
final public class ThreadSafeEventAnnotator extends EventAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeEventAnnotator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      EvSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      EvSingleton.getInstance().process( jCas );
   }

   public static AnalysisEngineDescription createDataWriterDescription(
         Class<?> dataWriter,
         File outputDirectory,
         float downratio,
         float featureSelect, float smoteNeighborNumber ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeEventAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING, true,
            DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, dataWriter,
            DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, outputDirectory,
            EventAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE, downratio,
            EventAnnotator.PARAM_FEATURE_SELECTION_THRESHOLD, featureSelect,
            EventAnnotator.PARAM_SMOTE_NUM_NEIGHBORS, smoteNeighborNumber );
   }

   public static AnalysisEngineDescription createAnnotatorDescription( final String modelPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeEventAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING, false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelPath );
   }

   public static AnalysisEngineDescription createAnnotatorDescription()
         throws ResourceInitializationException {
      return createAnnotatorDescription(
            String.format( "/%s/model.jar",
                  EventAnnotator.class.getName().toLowerCase().replace( '.', '/' ) ) );
   }

   private enum EvSingleton implements ThreadSafeWrapper<EventAnnotator> {
      INSTANCE;

      static public EvSingleton getInstance() {
         return INSTANCE;
      }

      private final EventAnnotator _delegate;
      private boolean _initialized;

      EvSingleton() {
         _delegate = new EventAnnotator();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public EventAnnotator getDelegate() {
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
