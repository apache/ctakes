package org.apache.ctakes.constituency.parser.concurrent;

import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.core.concurrent.ThreadSafeWrapper;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

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
      name = "Thread safe Constituency Parser",
      description = "Adds Terminal Treebank Nodes, necessary for Coreference Markables.",
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.SENTENCE },
      products = { PipeBitInfo.TypeProduct.TREE_NODE }
)
public class ThreadSafeConstituencyParser extends ConstituencyParser {

   static private final Logger LOGGER = Logger.getLogger( "ThreadSafeConstituencyParser" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      CpSingleton.getInstance().initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      CpSingleton.getInstance().process( jCas );
   }

   public static AnalysisEngineDescription createAnnotatorDescription( final String modelPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ThreadSafeConstituencyParser.class,
            ConstituencyParser.PARAM_MODEL_FILENAME, modelPath );
   }

   public static AnalysisEngineDescription createAnnotatorDescription()
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( ThreadSafeConstituencyParser.class );
   }


   private enum CpSingleton implements ThreadSafeWrapper<ConstituencyParser> {
      INSTANCE;

      static public CpSingleton getInstance() {
         return INSTANCE;
      }

      private final ConstituencyParser _delegate;
      private boolean _initialized;

      CpSingleton() {
         _delegate = new ConstituencyParser();
      }

      final private Object LOCK = new Object();

      @Override
      public Object getLock() {
         return LOCK;
      }

      @Override
      public ConstituencyParser getDelegate() {
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
