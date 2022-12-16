package org.apache.ctakes.coreference.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.coreference.ae.MentionClusterCoreferenceAnnotator;
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

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * Normally I would use composition and a singleton, but here extension is done for @ConfigurationParameter discovery.
 * Made a singleton mostly for model memory.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/20/2017
 */
@PipeBitInfo(
      name = "Thread safe Coreference (Clusters)",
      description = "Coreference annotator using mention-synchronous paradigm.",
      dependencies = { BASE_TOKEN, SENTENCE, SECTION, IDENTIFIED_ANNOTATION, MARKABLE },
      products = { COREFERENCE_RELATION }
)
public class ThreadSafeMentionClusterCoreferencer extends MentionClusterCoreferenceAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeMentionClusterCoreferencer" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      MccSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      MccSingleton.getInstance().process( jCas );
   }

   public static AnalysisEngineDescription createDataWriterDescription(
         Class<? extends DataWriter<String>> dataWriterClass,
         File outputDirectory,
         float downsamplingRate ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeMentionClusterCoreferencer.class,
            CleartkAnnotator.PARAM_IS_TRAINING,
            true,
            MentionClusterCoreferenceAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
            downsamplingRate,
            DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
            dataWriterClass,
            DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
            outputDirectory,
            MentionClusterCoreferenceAnnotator.PARAM_SINGLE_DOCUMENT,
            false );
   }

   public static AnalysisEngineDescription createAnnotatorDescription( final String modelPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeMentionClusterCoreferencer.class,
            CleartkAnnotator.PARAM_IS_TRAINING, false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelPath );
   }

   public static AnalysisEngineDescription createMultidocAnnotatorDescription( final String modelPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeMentionClusterCoreferencer.class,
            CleartkAnnotator.PARAM_IS_TRAINING, false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelPath,
            MentionClusterCoreferenceAnnotator.PARAM_SINGLE_DOCUMENT, false );
   }


   private enum MccSingleton implements ThreadSafeWrapper<MentionClusterCoreferenceAnnotator> {
      INSTANCE;

      static public MccSingleton getInstance() {
         return INSTANCE;
      }

      private final MentionClusterCoreferenceAnnotator _delegate;
      private boolean _initialized;

      MccSingleton() {
         _delegate = new MentionClusterCoreferenceAnnotator();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public MentionClusterCoreferenceAnnotator getDelegate() {
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
