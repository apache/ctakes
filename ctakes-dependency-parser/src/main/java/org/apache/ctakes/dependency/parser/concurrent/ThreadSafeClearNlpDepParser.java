package org.apache.ctakes.dependency.parser.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Normally I would use composition and a singleton, but here extension is done for @ConfigurationParameter discovery.
 * Made a singleton mostly for model memory.
 * ClearNLPDependencyParserAE is almost thread safe ... the shared models are not immutable.
 * {@inheritDoc}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/20/2017
 */
@TypeCapability(
      inputs = {
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:partOfSpeech",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:normalizedForm",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:tokenNumber",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:end",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:begin"
      } )
@PipeBitInfo(
      name = "ClearNLP Dependency Parser",
      description = "Analyses Sentence Structure, storing information in nodes.",
      role = PipeBitInfo.Role.SPECIAL,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = { PipeBitInfo.TypeProduct.DEPENDENCY_NODE }
)
final public class ThreadSafeClearNlpDepParser extends ClearNLPDependencyParserAE {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeClearNlpSemRoleLabeler" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      DepSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      DepSingleton.getInstance().process( jCas );
   }

   // If someone calls this, they want the default model, lazy initialization of the external resources:
   public static synchronized AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return createAnnotatorDescription( defaultParserResource, defaultLemmatizerResource );
   }

   public static AnalysisEngineDescription createAnnotatorDescription( final ExternalResourceDescription parserDesc,
                                                                       final ExternalResourceDescription lemmaDesc )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeClearNlpDepParser.class, DEP_MODEL_KEY, parserDesc, LEM_MODEL_KEY, lemmaDesc );
   }

   private enum DepSingleton implements ThreadSafeWrapper<ClearNLPDependencyParserAE> {
      INSTANCE;

      static public DepSingleton getInstance() {
         return INSTANCE;
      }

      private final ClearNLPDependencyParserAE _delegate;
      private boolean _initialized;

      DepSingleton() {
         _delegate = new ClearNLPDependencyParserAE();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public ClearNLPDependencyParserAE getDelegate() {
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
