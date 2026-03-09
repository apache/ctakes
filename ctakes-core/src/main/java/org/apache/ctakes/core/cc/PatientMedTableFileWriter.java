package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.structured.Patient;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

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

   static public final String DATE_WRITE_FORMAT_PARAM = "DateWriteFormat";
   static public final String DATE_WRITE_FORMAT_DESC = "A format to write dates.  e.g. dd-MM-yyyy_HH:mm:ss";
   @ConfigurationParameter (
         name = DATE_WRITE_FORMAT_PARAM,
         description = DATE_WRITE_FORMAT_DESC,
         defaultValue = "ddMMyyyykkmmss",
         mandatory = false
   )
   private String _dateWriteFormat;

   static public final String CAS_DATE_FORMAT_PARAM = "CasDateFormat";
   static public final String CAS_DATE_FORMAT_DESC = "Set a value for parameter CasDateFormat.";
   @ConfigurationParameter (
         name = CAS_DATE_FORMAT_PARAM,
         description = CAS_DATE_FORMAT_DESC,
         defaultValue = "MMddyyyykkmmss",
         mandatory = false
   )
   private String _casDateFormat;

   private DateTimeFormatter _dateWriteFormatter;
   private DateTimeFormatter _casDateFormatter;

   private int _patientDocCount = 0;
//   private final Map<String,PatientMed> _patientMeds = new HashMap<>();
//   private final Map<String,Integer> _patientMedCounts = new HashMap<>();
   private boolean _isFirstWrite = true;

   static private final LocalDateTime TODAY_DATE = LocalDateTime.now();
   private String _runStartTime;

   private MedCell _medCell;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _casDateFormatter = DateTimeFormatter.ofPattern( _casDateFormat );
      _dateWriteFormatter = DateTimeFormatter.ofPattern( _dateWriteFormat );
      _runStartTime = _casDateFormatter.format( OffsetDateTime.now() );
   }

   /**
    * Add medication data from the cas to table rows.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      createData( jcas );
      final List<List<String>> data = getData();
      if ( data == null || data.isEmpty() ) {
         // Not done with patient.
         return;
      }
      // Done with patient, append data to file.
      try {
         appendFile( data, "", "", "", _isFirstWrite, false );
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
      _isFirstWrite = false;
   }

   /**
    * Write the table to file.
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      try {
         appendFile( Collections.emptyList(), "", "", "", _isFirstWrite, true );
         writeComplete( Collections.emptyList() );
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

   // TODO - refactor up to AbstractFileWriter
   protected File getOutputFile( final String outputDir,
                                 final String documentId,
                                 final String fileName ) {
      final String simpleOutputDir = getOutputDirectory( null, getRootDirectory(), null );
      return super.getOutputFile( simpleOutputDir, "PatientMedication_"+_runStartTime, "" );
   }

   /**
    * {@inheritDoc}
    */
   public void appendFile( final List<List<String>> dataRows,
                          final String outputDir,
                          final String documentId,
                          final String fileName,
                           final boolean writeHeader,
                           final boolean writeFooter ) throws IOException {
      final TableType tableType = getTableType();
      final File file = getOutputFile( outputDir, documentId, fileName );
      LOGGER.info( "Appending {} Table to {} ...", tableType.name(), file.getPath() );
      try ( Writer writer = new BufferedWriter( new FileWriter( file, true ) ) ) {
         if ( writeHeader ) {
            final String header = createTableHeader( tableType, getHeaderRow() );
            writer.write( header );
         }
         for ( List<String> dataRow : dataRows ) {
            final String row = createTableRow( tableType, dataRow );
            writer.write( row );
         }
         if ( writeFooter ) {
            final String footer = createTableFooter( tableType, getFooterRow() );
            writer.write( footer );
         }
      }
   }

   /**
    * Patient ID, Medication Names, CUI, RNorm, NCI Cui, Count
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Arrays.asList(
            " Patient ID ",
            " Medications (Name, [CUIs], [RxNorms], [NCI Cuis], earliest evidence ddMMyyyy240000, count) ",
            " document count " );
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
      final LocalDateTime docDate = getDocDate( jCas );
      if ( _medCell == null ) {
         _medCell = new MedCell();
      }
      // Get the medication mentions,
      // turn each medication mention into a PatientMedRecord,
      // add each PatientMedRecord to the map (uniquely), increasing a count of identical meds.
      for ( MedicationMention med : JCasUtil.select( jCas, MedicationMention.class ) ) {
         _medCell.update( new PatientMed( med ), docDate );
//         final String name = IdentifiedAnnotationUtil.getPreferredText( med );
//         _patientMeds.put( name, new PatientMed( med ) );
//         _patientMedCounts.merge( name, 1, Integer::sum );
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

   private LocalDateTime getDocDate( final JCas jCas ) {
      final SourceData sourceData = SourceMetadataUtil.getOrCreateSourceData( jCas );
      String casDate = sourceData.getSourceRevisionDate();
      if ( casDate == null || casDate.isEmpty() ) {
         casDate = sourceData.getSourceOriginalDate();
      }
      if ( casDate == null || casDate.isEmpty() ) {
         return TODAY_DATE;
      }
//            LOGGER.info( "Cas Date " + casDate + "  Run Start " + _runStartTime );
      LocalDateTime dateTime;
      try {
         dateTime = LocalDateTime.parse( casDate, _casDateFormatter );
      } catch ( DateTimeParseException dtpE ) {
         final LocalDate date = LocalDate.parse( casDate, _casDateFormatter );
         dateTime = date.atStartOfDay();
      }
      return dateTime;
   }

   /**
    * Create single table row with information from all PatientMeds.
    * @param patientId -
    * @return -
    */
   private List<String> createTableRow( final String patientId ) {
      if ( _medCell == null ) {
         return Arrays.asList( patientId, "", ""+_patientDocCount );
      }
      final String cell = _medCell.getCell();
      _medCell = null;
      return Arrays.asList( patientId, cell, ""+_patientDocCount );
//      final List<String> names
//            = _patientMeds.keySet().stream()
//                          .sorted( String.CASE_INSENSITIVE_ORDER )
//                          .toList();
//      final StringBuilder prefTexts = new StringBuilder();
//      final StringBuilder cuis = new StringBuilder();
//      final StringBuilder rxnorms = new StringBuilder();
//      final StringBuilder nciCuis = new StringBuilder();
//      final StringBuilder counts = new StringBuilder();
//      for ( String name : names ) {
//         PatientMed med = _patientMeds.get( name );
//         prefTexts.append( name ).append( ";" );
//         cuis.append( med.cuis ).append( ";" );
//         rxnorms.append( med.rxnorms ).append( ";" );
//         nciCuis.append( med.nciCuis ).append( ";" );
//         counts.append( _patientMedCounts.get( name ) ).append( ";" );
//      }
//      return Arrays.asList(
//            patientId,
//            prefTexts.toString(),
//            cuis.toString(),
//            rxnorms.toString(),
//            nciCuis.toString(),
//            counts.toString() );
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
   private record PatientMed( String name,
                              List<String> cuis,
                              List<String> rxnorms,
                              List<String> nciCuis ) {
      private PatientMed( final MedicationMention med ) {
         this( IdentifiedAnnotationUtil.getPreferredText( med ),
               toSortedList( IdentifiedAnnotationUtil.getCuis( med ) ),
               toSortedList( IdentifiedAnnotationUtil.getCodes( med, "RXNORM" ) ),
               toSortedList( IdentifiedAnnotationUtil.getCodes( med, "NCI" ) ) );
      }
   }


   private class MedCell {
      final private Map<String, LocalDateTime> _nameDateMap = new HashMap<>();
      final private Map<String, PatientMed> _nameMedMap = new HashMap<>();
      private final Map<String,Integer> _nameCounts = new HashMap<>();

      private void update( final Collection<PatientMed> patientMeds, final LocalDateTime docDate ) {
         patientMeds.forEach( m -> update( m, docDate ) );
      }

      private void update( final PatientMed patientMed , final LocalDateTime docDate ) {
         final String name = patientMed.name();
         final LocalDateTime statusDate = _nameDateMap.get( name );
         if ( statusDate == null ) {
            _nameDateMap.put( name, docDate );
            _nameMedMap.put( name, patientMed );
         } else if ( docDate.isBefore( statusDate ) ) {
            _nameDateMap.put( name, docDate );
         }
         _nameCounts.merge( name, 1, Integer::sum );
      }

      private boolean hasInfo() {
         return !_nameDateMap.isEmpty();
      }

      private String getCell() {
         return _nameMedMap.keySet()
                        .stream()
                        .sorted()
                        .map( this::getValueCell )
                        .collect( Collectors.joining( ";" ) );
      }

      private String getValueCell( final String name ) {
         final LocalDateTime date = _nameDateMap.get( name );
         if ( date == null ) {
            return "";
         }
         PatientMed med = _nameMedMap.get( name );
         return name + ',' +
               med.cuis() + ',' +
               med.rxnorms() + ',' +
               med.nciCuis() + ',' +
               date.format( _dateWriteFormatter ) + ',' +
               _nameCounts.get( name );
      }
   }


}