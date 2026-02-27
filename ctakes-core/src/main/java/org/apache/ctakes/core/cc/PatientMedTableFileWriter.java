package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.structured.Patient;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * Todo - refactor and 'merge' parts of AbstractPatientFileWriter (maybe interface that one)?
 * @author SPF , chip-nlp
 * @since {2/26/2026}
 */
@PipeBitInfo (
      name = "PatientMedTableFileWriter",
      description = "Writes Patient medications in a table.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, IDENTIFIED_ANNOTATION },
      usables = { DOCUMENT_ID_PREFIX }
)
public class PatientMedTableFileWriter extends AbstractTableFileWriter {
   // If you do not need to utilize the entire cas, or need more than the doc cas, consider AbstractFileWriter<T>. 
   static private final Logger LOGGER = LoggerFactory.getLogger( "PatientMedTableFileWriter" );

   private int _patientDocCount = 0;
   private final Map<String,PatientMed> _patientMeds = new HashMap<>();
   private final Map<String,Integer> _patientMedCounts = new HashMap<>();

   /**
    * Add medication data from the cas to table rows.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      final List<List<String>> dataRows = createDataRows( jcas );
      if ( dataRows.isEmpty() ) {
         return;
      }
      dataRows.forEach( this::addDataRow );
   }

   /**
    * Write the table to file.
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      setHeaderRow( createHeaderRow( null ) );
      final List<List<String>> data = getData();
      try {
         writeFile( data, getOutputDirectory( null, getRootDirectory(), null ), "PatientMedication", "" );
         writeComplete( data );
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }

   /**
    * @param jCas       ignored
    * @param documentId ignored
    * @return the subdirectory set with the PARAM_SUBDIR parameter
    */
   @Override
   protected String getSubdirectory( final JCas jCas, final String documentId ) {
      return getSimpleSubDirectory();
   }

   /**
    * Patient ID, Medication Names, CUI, RNorm, NCI Cui, Count
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Arrays.asList(
            " Patient ID ",
            " Medication Names ",
            " CUI ",
            " RxNorm ",
            " NCI Cui ",
            " Count " );
   }

   /**
    * Does nothing.
    * @param jCas the jcas passed to the process( jcas ) method.
    */
   @Override
   protected void createData( final JCas jCas ) {
      return;
   }

   /**
    * Add all medication information from the document.
    * If it is the last document, add a table row with all medication information.
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      // Get the patient from the cas for patient information. This is new for ctakes 7.
      final Patient patient = JCasUtil.select( jCas, Patient.class ).stream().findFirst().orElse( null );
      if ( patient == null ) {
         LOGGER.error( "No Patient for doc {}", DocIdUtil.getDocumentID( jCas ) );
         System.exit( -1 );
      }

      // Get the medication mentions,
      // turn each medication mention into a PatientMedRecord,
      // add each PatientMedRecord to the map (uniquely), increasing a count of identical meds.
      for ( MedicationMention med : JCasUtil.select( jCas, MedicationMention.class ) ) {
         final String name = IdentifiedAnnotationUtil.getPreferredText( med );
         _patientMeds.put( name, new PatientMed( med ) );
         _patientMedCounts.merge( name, 1, Integer::sum );
      }
      // Increase the internal count of documents processed.
      _patientDocCount++;
      if ( _patientDocCount < patient.getDocumentCount() ) {
         // We aren't on the last document, just move on without writing anything to a table.
         // Null will cause the pipeline to continue without writing to a table.
         return Collections.emptyList();
      }
      // The patient has been completed, lets make a data row.
      _patientDocCount = 0;
      return Collections.singletonList( createTableRow( patient.getPatientIdentifier() ) );
   }


   /**
    * Create single table row with information from all PatientMeds.
    * @param patientId -
    * @return -
    */
   private List<String> createTableRow( final String patientId ) {
      final List<String> names
            = _patientMeds.keySet().stream()
                          .sorted( String.CASE_INSENSITIVE_ORDER )
                          .toList();
      final StringBuilder prefTexts = new StringBuilder();
      final StringBuilder cuis = new StringBuilder();
      final StringBuilder rxnorms = new StringBuilder();
      final StringBuilder nciCuis = new StringBuilder();
      final StringBuilder counts = new StringBuilder();
      for ( String name : names ) {
         PatientMed med = _patientMeds.get( name );
         prefTexts.append( name ).append( ";" );
         cuis.append( med.cuis ).append( ";" );
         rxnorms.append( med.rxnorms ).append( ";" );
         nciCuis.append( med.nciCuis ).append( ";" );
         counts.append( _patientMedCounts.get( name ) ).append( ";" );
      }
      return Arrays.asList(
            patientId,
            prefTexts.toString(),
            cuis.toString(),
            rxnorms.toString(),
            nciCuis.toString(),
            counts.toString() );
   }


   /**
    * @param c -
    * @return sorted list.
    */
   static private List<String> toSortedList( final Collection<String> c ) {
      return c.stream().sorted().toList();
   }

   /**
    *
    * @param cuis -
    * @param rxnorms -
    * @param nciCuis -
    */
   private record PatientMed( List<String> cuis,
                              List<String> rxnorms,
                              List<String> nciCuis ) {
      private PatientMed( final MedicationMention med ) {
         this( toSortedList( IdentifiedAnnotationUtil.getCuis( med ) ),
               toSortedList( IdentifiedAnnotationUtil.getCodes( med, "RXNORM" ) ),
               toSortedList( IdentifiedAnnotationUtil.getCodes( med, "NCIT" ) ) );
      }
   }

}