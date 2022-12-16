package org.apache.ctakes.postagger.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
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
      name = "Thread safe Part of Speech Tagger",
      description = "Annotate Parts of Speech.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.BASE_TOKEN, }
)
final public class ThreadSafePosTagger extends POSTagger {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafePosTagger" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      PosSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      PosSingleton.getInstance().process( jCas );
   }

   /**
    * @return a part of speech tagger using a default model
    * @throws ResourceInitializationException -
    */
   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafePosTagger.class,
            TypeSystemDescriptionFactory.createTypeSystemDescription(),
            TypePrioritiesFactory.createTypePriorities( Segment.class, Sentence.class, BaseToken.class ) );
   }

   /**
    * @param model a part of speech model
    * @return a part of speech tagger using the given model
    * @throws ResourceInitializationException -
    */
   public static AnalysisEngineDescription createAnnotatorDescription( final String model )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafePosTagger.class,
            TypeSystemDescriptionFactory.createTypeSystemDescription(),
            TypePrioritiesFactory.createTypePriorities( Segment.class, Sentence.class, BaseToken.class ),
            POSTagger.PARAM_POS_MODEL_FILE, model );
   }


   private enum PosSingleton implements ThreadSafeWrapper<POSTagger> {
      INSTANCE;

      static public PosSingleton getInstance() {
         return INSTANCE;
      }

      private final POSTagger _delegate;
      private boolean _initialized;

      PosSingleton() {
         _delegate = new POSTagger();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public POSTagger getDelegate() {
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
