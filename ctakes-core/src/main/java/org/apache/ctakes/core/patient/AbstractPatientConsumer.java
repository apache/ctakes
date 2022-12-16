package org.apache.ctakes.core.patient;

import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Collection;


/**
 * Extend this annotator to consume a patient cas once the current patient name has changed.
 * For instance, when processing patientA doc1, patientA doc2, patientA doc3 this annotator does nothing.
 * After the processing of patientB doc1 this annotator will process a cas containing all documents for patientA.
 * At the end of the pipeline, the last unprocessed patient will be processed.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2017
 */
@PipeBitInfo(
      name = "AbstractPatientConsumer",
      description = "Abstract Engine to take action on a patient level instead of document level.",
      role = PipeBitInfo.Role.ANNOTATOR
)
abstract public class AbstractPatientConsumer extends JCasAnnotator_ImplBase implements NamedEngine {

   static public final String REMOVE_PATIENT = "RemovePatient";
   static public final String ENGINE_NAME = "EngineName";

   @ConfigurationParameter(
         name = REMOVE_PATIENT,
         description = "The Patient Consumer should remove the patient from the cache when finished.",
         defaultValue = "true"
   )
   private boolean _removePatient;

   @ConfigurationParameter(
         name = ENGINE_NAME,
         description = "The Name to use for this Patient Consumer.  Must be unique in the pipeline",
         mandatory = false
   )
   private String _engineName;

   private final String _action;
   private final Logger _logger;

   protected AbstractPatientConsumer( final String aeName, final String action ) {
      _action = action;
      _logger = Logger.getLogger( aeName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getEngineName() {
      if ( _engineName != null && !_engineName.isEmpty() ) {
         return _engineName;
      }
      return getClass().getSimpleName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      PatientNoteStore.getInstance().registerEngine( getEngineName() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      processPatients();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      processPatients();
   }

   protected void processPatients() throws AnalysisEngineProcessException {
      final Collection<JCas> completedCases = PatientNoteStore.getInstance().popPatientCases( getEngineName() );
      for ( JCas patientCas : completedCases ) {
         final String patientName = SourceMetadataUtil.getPatientIdentifier( patientCas );
         _logger.info( _action + " for patient " + patientName + " ..." );
         processPatientCas( patientCas );
      }
   }

   /**
    * @param patientJcas JCas containing multiple views for a single patient.
    * @throws AnalysisEngineProcessException if there is some problem.
    */
   abstract protected void processPatientCas( final JCas patientJcas ) throws AnalysisEngineProcessException;

}
