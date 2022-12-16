package org.apache.ctakes.temporal.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.temporal.ae.EventEventRelationAnnotator;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
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
      name = "Thread safe E-E TLinker",
      description = "Creates Event - Event TLinks.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.EVENT },
      products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
final public class ThreadSafeEventEventRelAnnotator extends EventEventRelationAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeEventEventRelAnnotator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      EvErSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      EvErSingleton.getInstance().process( jCas );
   }

   public static AnalysisEngineDescription createDataWriterDescription(
         Class<? extends DataWriter<String>> dataWriterClass,
         File outputDirectory,
         double probabilityOfKeepingANegativeExample ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeEventEventRelAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING,
            true,
            DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
            dataWriterClass,
            DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
            outputDirectory,
            RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
            // not sure why this has to be cast; something funny going on in uimaFIT maybe?
            (float) probabilityOfKeepingANegativeExample );
   }

   public static AnalysisEngineDescription createAnnotatorDescription( final String modelPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeEventEventRelAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING, false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelPath );
   }


   private enum EvErSingleton implements ThreadSafeWrapper<EventEventRelationAnnotator> {
      INSTANCE;

      static public EvErSingleton getInstance() {
         return INSTANCE;
      }

      private final EventEventRelationAnnotator _delegate;
      private boolean _initialized;

      EvErSingleton() {
         _delegate = new EventEventRelationAnnotator();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public EventEventRelationAnnotator getDelegate() {
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
