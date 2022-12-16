package org.apache.ctakes.dictionary.lookup2.concurrent;

import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Normally I would use composition and a singleton, but here extension is done for @ConfigurationParameter discovery.
 * Made a singleton mostly for dictionary memory.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/20/2017
 */
@PipeBitInfo(
      name = "Thread safe Dictionary Lookup (Default)",
      description = "Annotates clinically-relevant terms.  Terms must match dictionary entries exactly.",
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION
)
final public class ThreadSafeFastLookup extends DefaultJCasTermAnnotator {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeFastLookup" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      DlSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      DlSingleton.getInstance().process( jCas );
   }

   /**
    * @return dictionary lookup with defaults
    * @throws ResourceInitializationException -
    */
   static public AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ThreadSafeFastLookup.class );
   }

   /**
    * @param descriptorPath path to lookup configuration xml file
    * @return dictionary lookup using the given configuration
    * @throws ResourceInitializationException -
    */
   static public AnalysisEngineDescription createAnnotatorDescription( final String descriptorPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ThreadSafeFastLookup.class,
            ConfigParameterConstants.PARAM_LOOKUP_XML, descriptorPath );
   }

   private enum DlSingleton implements ThreadSafeWrapper<DefaultJCasTermAnnotator> {
      INSTANCE;

      static public DlSingleton getInstance() {
         return INSTANCE;
      }

      private final DefaultJCasTermAnnotator _delegate;
      private boolean _initialized;

      DlSingleton() {
         _delegate = new DefaultJCasTermAnnotator();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public DefaultJCasTermAnnotator getDelegate() {
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
