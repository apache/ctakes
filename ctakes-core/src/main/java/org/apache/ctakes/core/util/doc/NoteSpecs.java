package org.apache.ctakes.core.util.doc;


import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.util.doc.DocIdUtil.NO_DOCUMENT_ID_PREFIX;
import static org.apache.ctakes.core.util.doc.SourceMetadataUtil.UNKNOWN_PATIENT;

/**
 * Contains information on a note.  This is information can exist beyond the life of a cas.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class NoteSpecs {

   static private final Logger LOGGER = Logger.getLogger( "NoteSpecs" );

   // ClinicalNote is not yet a fhir resource, but it may be in the future and is the best fit.
   // http://wiki.hl7.org/index.php?title=ClinicalNote_FHIR_Resource_Proposal
   static public final String ID_NAME_CLINICAL_NOTE = "ClinicalNote";

   static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyyMMddhhmm" );
   //   For compatibility with sql db : Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]
   static private final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );

   static public final String SUBJECT_PATIENT = "patient";
   static public final String DEFAULT_PATIENT_NAME = "Generic";

   private final String _documentId;
   private final String _documentType;
   private final String _documentText;
   private final String _patientName;
   private final Collection<String> _subjects;
   private final Date _noteDate;

   /**
    * @param jCas ye olde ...
    */
   public NoteSpecs( final JCas jCas ) {
      final SourceData sourceData = SourceMetadataUtil.getSourceData( jCas );
      _noteDate = createNoteDate( sourceData );
      _documentId = DocIdUtil.getDocumentID( jCas );
      _documentType = createDocumentType( sourceData );
      _documentText = jCas.getDocumentText();
      _patientName = createPatientName( jCas, _documentId );
      _subjects = createSubjects( jCas );
   }

   /**
    * @return the standard document id, usually set by a collection reader.
    */
   public String getDocumentId() {
      return _documentId;
   }

   /**
    * @return name of patient, or the default "Generic".
    */
   public String getPatientName() {
      return _patientName;
   }

   /**
    * @return all subject names in the note.
    */
   public Collection<String> getSubjects() {
      return _subjects;
   }

   /**
    * @param jCas ye olde ...
    * @return collection of all subjects for all identified annotations.
    */
   private Collection<String> createSubjects( final JCas jCas ) {
      return JCasUtil.select( jCas, IdentifiedAnnotation.class )
            .stream()
            .map( IdentifiedAnnotation::getSubject )
            .filter( Objects::nonNull )
            .map( String::toLowerCase )
            .filter( s -> !SUBJECT_PATIENT.equals( s ) )
            .distinct()
            .collect( Collectors.toList() );
   }

   /**
    * @return the creation or possibly sign off date for the note as a Date object, or -now- as a default.
    */
   public Date getNoteDate() {
      return _noteDate;
   }

   /**
    * @return the creation or possibly sign off date for the note, or -now- as a default, as a formatted String, yyyyMMddhhmm.
    */
   public String getNoteTime() {
      return DATE_FORMAT.format( _noteDate );
   }

   /**
    * @return type of note or the default "ClinicalNote".
    */
   public String getDocumentType() {
      return _documentType;
   }

   /**
    * @return complete text from the note.
    */
   public String getDocumentText() {
      return _documentText;
   }

   /**
    * The patient name according to: Source Metadata, Note file parent directory, note name prefixing '_'.
    * @param jCas ye olde ...
    * @return the patient name, often specified by the collection reader, or the default "Generic".
    */
   static private String createPatientName( final JCas jCas, final String documentId ) {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      if ( patientId != null && !patientId.isEmpty() && !patientId.equals( UNKNOWN_PATIENT ) ) {
         return patientId;
      }
      final String idPrefix = DocIdUtil.getDocumentIdPrefix( jCas );
      if ( idPrefix != null && !idPrefix.isEmpty() && !idPrefix.equals( NO_DOCUMENT_ID_PREFIX ) ) {
         return idPrefix;
      }
      if ( !documentId.isEmpty() ) {
         final int firstScore = documentId.indexOf( '_' );
         if ( firstScore > 2 ) {
            // assume that the note id begins with the patient name.
            // This could cause serious problems if note id begins with note type or some other item.
            return documentId.substring( 0, firstScore );
         }
      }
      return DEFAULT_PATIENT_NAME;
   }

   /**
    * @param sourceData -
    * @return type of note or the default "ClinicalNote".
    */
   static private String createDocumentType( final SourceData sourceData ) {
      if ( sourceData == null ) {
         return ID_NAME_CLINICAL_NOTE;
      }
      final String sourceType = sourceData.getNoteTypeCode();
      return sourceType == null ? ID_NAME_CLINICAL_NOTE : sourceType;
   }

   /**
    * @param sourceData -
    * @return the creation or possibly sign off date for the note as a Date object, or -now- as a default.
    */
   static private Date createNoteDate( final SourceData sourceData ) {
      if ( sourceData == null ) {
         return new Date();
      }
      final String sourceDateText = sourceData.getSourceOriginalDate();
      if ( sourceDateText != null ) {
         try {
            return TIMESTAMP_FORMAT.parse( sourceDateText );
         } catch ( ParseException pE ) {
            // do nothing
         }
      }
      return new Date();
   }


}
