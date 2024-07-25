package org.apache.ctakes.pbj.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.pbj.cr.PbjReceiver;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;

import static org.apache.ctakes.pbj.cr.PbjReceiver.*;
import static org.apache.ctakes.pbj.util.PbjConstants.*;

/**
 * @author SPF , chip-nlp
 * @since {5/16/2023}
 */
@PipeBitInfo(
      name = "PbjReceiverAE",
      description = "Annotation Engine wrapper for the PbjReceiver.",
      role = PipeBitInfo.Role.ANNOTATOR
)
public class PbjReceiverAE extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LogManager.getLogger( "PbjReceiverAE" );

   // Duplicates of all the PbjReceiver configuration parameters.

   @ConfigurationParameter(
         name = PARAM_RECEIVER_NAME,
         description = DESC_RECEIVER_NAME,
         mandatory = false,
         defaultValue = DEFAULT_USER
   )
   private String _userName;

   @ConfigurationParameter(
         name = PARAM_RECEIVER_PASS,
         description = DESC_RECEIVER_PASS,
         mandatory = false,
         defaultValue = DEFAULT_PASS
   )
   private String _password;


   @ConfigurationParameter(
         name = PARAM_HOST,
         description = DESC_HOST,
         mandatory = false,
         defaultValue = DEFAULT_HOST
   )
   private String _host;

   @ConfigurationParameter(
         name = PARAM_PORT,
         description = DESC_PORT,
         mandatory = false
   )
   private int _port = DEFAULT_PORT;

   @ConfigurationParameter(
         name = PARAM_QUEUE,
         description = DESC_QUEUE
   )
   private String _queue;

   @ConfigurationParameter(
         name = PARAM_ACCEPT_STOP,
         description = DESC_ACCEPT_STOP,
         mandatory = false,
         defaultValue = DEFAULT_ACCEPT_STOP
   )
   private String _acceptStop;

   private PbjReceiver _delegate;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _delegate = new PbjReceiver();
      // ConfigParam initialization for CollectionReaders is done in initialize(), without a UimaContext.
      // That is unfortunate for us, but we can set all the parameter values explicitly.
      _delegate.setUserName( _userName );
      _delegate.setPassword( _password );
      _delegate.setHost( _host );
      _delegate.setPort( _port );
      _delegate.setQueue( _queue );
      _delegate.setAcceptStop( _acceptStop );
      _delegate.initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      // From https://issues.apache.org/jira/browse/UIMA-1718
      ((CASImpl)jcas.getCas()).restoreClassLoaderUnlockCas();
      jcas.reset();
      try {
         if ( _delegate.hasNext() ) {
            _delegate.getNext( jcas );
         }
      } catch ( CollectionException | IOException cE ) {
         throw new AnalysisEngineProcessException( cE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      _delegate.disconnect();
   }

}
