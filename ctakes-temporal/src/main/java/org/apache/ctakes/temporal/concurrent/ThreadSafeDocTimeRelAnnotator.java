package org.apache.ctakes.temporal.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.DocTimeRelAnnotator;
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
      name = "Thread safe DocTimeRel Annotator",
      description = "Annotates event relativity to document creation time.",
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.EVENT }
)
final public class ThreadSafeDocTimeRelAnnotator extends DocTimeRelAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeDocTimeRelAnnotator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      DtrSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      DtrSingleton.getInstance().process( jCas );
   }

   public static AnalysisEngineDescription createDataWriterDescription(
         Class<? extends DataWriter<String>> dataWriterClass,
         File outputDirectory ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeDocTimeRelAnnotator.class,
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
            ThreadSafeDocTimeRelAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING, false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelPath );
   }

   private enum DtrSingleton implements ThreadSafeWrapper<DocTimeRelAnnotator> {
      INSTANCE;

      static public DtrSingleton getInstance() {
         return INSTANCE;
      }

      private final DocTimeRelAnnotator _delegate;
      private boolean _initialized;

      DtrSingleton() {
         _delegate = new DocTimeRelAnnotator();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public DocTimeRelAnnotator getDelegate() {
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
