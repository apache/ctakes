package org.apache.ctakes.coreference.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.coreference.ae.MarkableSalienceAnnotator;
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
import java.util.logging.Logger;

/**
 * Normally I would use composition and a singleton, but here extension is done for @ConfigurationParameter discovery.
 * Made a singleton mostly for model memory.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/20/2017
 */
@PipeBitInfo(
      name = "Thread safe Markable Salience Annotator",
      description = "Annotates Markable Salience.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.PARAGRAPH, PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.MARKABLE, PipeBitInfo.TypeProduct.DEPENDENCY_NODE },
      usables = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class ThreadSafeMarkableSalienceAnnotator extends MarkableSalienceAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeMarkableSalienceAnnotator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      MsSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      MsSingleton.getInstance().process( jCas );
   }

   public static AnalysisEngineDescription createDataWriterDescription(
         Class<? extends DataWriter<Boolean>> dataWriterClass,
         File outputDirectory ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeMarkableSalienceAnnotator.class,
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
            ThreadSafeMarkableSalienceAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING, false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelPath );
   }


   private enum MsSingleton implements ThreadSafeWrapper<MarkableSalienceAnnotator> {
      INSTANCE;

      static public MsSingleton getInstance() {
         return INSTANCE;
      }

      private final MarkableSalienceAnnotator _delegate;
      private boolean _initialized;

      MsSingleton() {
         _delegate = new MarkableSalienceAnnotator();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public MarkableSalienceAnnotator getDelegate() {
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
