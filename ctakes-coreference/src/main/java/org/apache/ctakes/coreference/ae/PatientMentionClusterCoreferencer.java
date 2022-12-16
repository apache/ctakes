package org.apache.ctakes.coreference.ae;

import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.fit.internal.ExtendedLogger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2017
 */
public class PatientMentionClusterCoreferencer extends AbstractPatientConsumer {

   static private final Logger LOGGER = Logger.getLogger( "PatientMentionClusterCoreferencer" );

   private final MentionClusterCoreferenceAnnotator _delegate;

   public PatientMentionClusterCoreferencer() {
      super( "PatientMentionClusterCoreferencer", "Creating Coreferences" );
      _delegate = new MentionClusterCoreferenceAnnotator();
   }

   /**
    * Call initialize() on super and the delegate
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      _delegate.initialize( context );
      super.initialize( context );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getEngineName() {
      return _delegate.getEngineName();
   }

   /**
    * {@inheritDoc}
    *
    * @return The logger for the delegate
    */
   @Override
   public ExtendedLogger getLogger() {
      return _delegate.getLogger();
   }

   /**
    * Call destroy on super and the delegate
    * {@inheritDoc}
    */
   @Override
   public void destroy() {
      super.destroy();
      _delegate.destroy();
   }

   /**
    * Set the resultSpecification in this and the delegate to the same object
    * {@inheritDoc}
    */
   @Override
   public void setResultSpecification( final ResultSpecification resultSpecification ) {
      super.setResultSpecification( resultSpecification );
      _delegate.setResultSpecification( resultSpecification );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void processPatientCas( final JCas patientJcas ) throws AnalysisEngineProcessException {
      _delegate.process( patientJcas );
   }

   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      _delegate.collectionProcessComplete();
   }
}
