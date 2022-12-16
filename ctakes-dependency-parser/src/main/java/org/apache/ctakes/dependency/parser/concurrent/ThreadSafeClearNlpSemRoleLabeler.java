package org.apache.ctakes.dependency.parser.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Normally I would use composition and a singleton, but here extension is done for @ConfigurationParameter discovery.
 * Made a singleton mostly for model memory.
 * ClearNLPSemanticRoleLabelerAE is almost thread safe ... the shared models are not immutable.
 * <p>
 * {@inheritDoc}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/20/2017
 */
@TypeCapability(
      inputs = {
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:partOfSpeech",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:tokenNumber",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:end",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:begin",
            "org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode"
      } )
@PipeBitInfo(
      name = "Thread safe ClearNLP Semantic Role Labeler",
      description = "Adds Semantic Roles Relations.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN,
            PipeBitInfo.TypeProduct.DEPENDENCY_NODE },
      products = { PipeBitInfo.TypeProduct.SEMANTIC_RELATION }
)
final public class ThreadSafeClearNlpSemRoleLabeler extends ClearNLPSemanticRoleLabelerAE {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeClearNlpSemRoleLabeler" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      SemSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      SemSingleton.getInstance().process( jCas );
   }

   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeClearNlpSemRoleLabeler.class,
            SRL_PARSER_MODEL_KEY,
            defaultParserResource,
            SRL_PRED_MODEL_KEY,
            defaultPredictionResource,
            SRL_ROLE_MODEL_KEY,
            defaultRoleResource );
   }

   private enum SemSingleton implements ThreadSafeWrapper<ClearNLPSemanticRoleLabelerAE> {
      INSTANCE;

      static public SemSingleton getInstance() {
         return INSTANCE;
      }

      private final ClearNLPSemanticRoleLabelerAE _delegate;
      private boolean _initialized;

      SemSingleton() {
         _delegate = new ClearNLPSemanticRoleLabelerAE();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public ClearNLPSemanticRoleLabelerAE getDelegate() {
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
