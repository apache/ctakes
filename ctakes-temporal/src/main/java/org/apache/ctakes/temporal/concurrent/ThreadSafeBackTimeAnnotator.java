package org.apache.ctakes.temporal.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
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
      name = "Thread safe Backward Timex Annotator",
      description = "Annotates absolute time / date Temporal expressions.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = { PipeBitInfo.TypeProduct.TIMEX }
)
final public class ThreadSafeBackTimeAnnotator extends BackwardsTimeAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeBackTimeAnnotator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      BtSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      BtSingleton.getInstance().process( jCas );
   }

   public static AnalysisEngineDescription createDataWriterDescription(
         Class<? extends DataWriter<String>> dataWriterClass, File outputDirectory )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeBackTimeAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING,
            true,
            DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
            dataWriterClass,
            DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
            outputDirectory );
   }

   public static AnalysisEngineDescription createAnnotatorDescription( final String modelPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeBackTimeAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING, false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelPath );
   }

   public static AnalysisEngineDescription createEnsembleDescription( final String modelPath,
                                                                      final String viewName )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeBackTimeAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING, false,
            BackwardsTimeAnnotator.PARAM_TIMEX_VIEW, viewName,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelPath );
   }


   private enum BtSingleton implements ThreadSafeWrapper<BackwardsTimeAnnotator> {
      INSTANCE;

      static public BtSingleton getInstance() {
         return INSTANCE;
      }

      private final BackwardsTimeAnnotator _delegate;
      private boolean _initialized;

      BtSingleton() {
         _delegate = new BackwardsTimeAnnotator();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public BackwardsTimeAnnotator getDelegate() {
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
