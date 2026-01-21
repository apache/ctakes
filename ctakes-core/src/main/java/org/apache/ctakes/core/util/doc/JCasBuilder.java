package org.apache.ctakes.core.util.doc;


import org.apache.ctakes.typesystem.type.structured.*;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Populate a JCas with creator, patient and document information.
 * This follows the Builder pattern, but can be used to modify an existing cas as well as create one.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/22/2019
 */
final public class JCasBuilder {

   //   For compatibility with sql db : Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]
   static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );

   static private final String UNKNOWN_DATE = "UnknownDate";
   static private final String UNKNOWN_GENDER = "UnknownGender";
   static private final String UNKNOWN = "Unknown";


   private String _institutionId = UNKNOWN;
   private String _authorSpecialty = UNKNOWN;

   private String _patientId = SourceMetadataUtil.UNKNOWN_PATIENT;
   private long _patientNum = SourceMetadataUtil.UNKNOWN_PATIENT_NUM;

   private String _firstName = UNKNOWN;
   private String _middleName = UNKNOWN;
   private String _lastName = UNKNOWN;

   private String _birthday = UNKNOWN_DATE;
   private String _deathday = UNKNOWN_DATE;
   private String _gender = UNKNOWN_GENDER;


   private String _instanceId = "";
   //   private long _instanceNum = -1;
   private String _encounterId = "";
//   private int _encounterNum = -1;

   private String _docId = DocIdUtil.NO_DOCUMENT_ID;
   private String _docIdPrefix = DocIdUtil.NO_DOCUMENT_ID_PREFIX;
   private String _docType = NoteSpecs.ID_NAME_CLINICAL_NOTE;
   private String _docSubType = "";
   private String _docStandard = "";
   private int _docRevisionNum = 1;
   private String _docTime = ""; //DATE_FORMAT.format( System.currentTimeMillis() );
   private String _docPath = "";

   private boolean _nullDocText = false;
   private String _docText = "";

   private boolean _overwrite = false;


   /**
    * Overwrite any information already in a JCas.
    * @return this builder.
    */
   public JCasBuilder overwrite() {
      _overwrite = true;
      return this;
   }

   /**
    * @param institutionId Identity of the institution.  e.g. hospital, health system, insurance company, study, etc.
    * @return this builder
    */
   public JCasBuilder setInstitutionId( final String institutionId ) {
      _institutionId = institutionId;
      return this;
   }

   /**
    * @param authorSpecialty The medical (or professional) specialty of a document's author.
    * @return this builder
    */
   public JCasBuilder setAuthorSpecialty( final String authorSpecialty ) {
      _authorSpecialty = authorSpecialty;
      return this;
   }

   /**
    * @param patientId The unique ID of a patient at an institution, in a study, in an insurance database, etc.
    * @return this builder
    */
   public JCasBuilder setPatientId( final String patientId ) {
      _patientId = patientId;
      return this;
   }

   /**
    * @param patientNum Depends upon user. e.g. Index of a patient in the run, index/number of the patient in a database, etc.
    * @return this builder
    */
   public JCasBuilder setPatientNum( final long patientNum ) {
      _patientNum = patientNum;
      return this;
   }

   /**
    * @param firstName Patient's first name.
    * @return this builder
    */
   public JCasBuilder setFirstName( final String firstName ) {
      _firstName = firstName;
      return this;
   }

   /**
    * @param middleName Patient's middle name.
    * @return this builder
    */
   public JCasBuilder setMiddleName( final String middleName ) {
      _middleName = middleName;
      return this;
   }

   /**
    * @param lastName Patient's last name.
    * @return this builder
    */
   public JCasBuilder setLastName( final String lastName ) {
      _lastName = lastName;
      return this;
   }

   /**
    * @param birthday Patient's date of birth. Format of this date is not controlled.
    * @return this builder
    */
   public JCasBuilder setBirthDay( final String birthday ) {
      _birthday = birthday;
      return this;
   }

   /**
    * @param deathday Patient's date of death. Format of this date is not controlled.
    * @return this builder
    */
   public JCasBuilder setDeathday( final String deathday ) {
      _deathday = deathday;
      return this;
   }

   /**
    * @param gender Patient's date of gender. Format of this value is not controlled.
    * @return this builder
    */
   public JCasBuilder setGender( final String gender ) {
      _gender = gender;
      return this;
   }

   /**
    * @param instanceId This is pretty nebulous, and can be used for anything.
    * @return this builder
    */
   public JCasBuilder setInstanceId( final String instanceId ) {
      _instanceId = instanceId;
      return this;
   }


   /**
    * @param encounterId Can be the same or different from a document ID, or empty.
    * @return this builder
    */
   public JCasBuilder setEncounterId( final String encounterId ) {
      _encounterId = encounterId;
      return this;
   }

//   public JCasBuilder setEncounterNum( final int encounterNum ) {
//      _encounterNum = encounterNum;
//      return this;
//   }

   /**
    * @param docId Document ID.  Depending upon requirements of the run, this may need to be unique.
    * @return this builder
    */
   public JCasBuilder setDocId( final String docId ) {
      _docId = docId;
      return this;
   }

   /**
    * @param docIdPrefix Can be anything identifying a collection of documents.  Patient ID, Document Type, etc.
    * @return this builder
    */
   public JCasBuilder setDocIdPrefix( final String docIdPrefix ) {
      _docIdPrefix = docIdPrefix;
      return this;
   }

   /**
    * @param docType the type of document, for instance imaging note or pathology report.
    * @return this builder
    */
   public JCasBuilder setDocType( final String docType ) {
      _docType = docType;
      return this;
   }

   /**
    * @param docSubType a subtype.  e.g. type = pathology report, subtype = biopsy analysis.
    * @return this builder
    */
   public JCasBuilder setDocSubType( final String docSubType ) {
      _docSubType = docSubType;
      return this;
   }

   /**
    * @param docStandard -
    * @return this builder
    */
   public JCasBuilder setDocStandard( final String docStandard ) {
      _docStandard = docStandard;
      return this;
   }

   /**
    * @param docRevisionNum for a document that may have had several iterations of editing.
    * @return this builder
    */
   public JCasBuilder setDocRevisionNum( final int docRevisionNum ) {
      _docRevisionNum = docRevisionNum;
      return this;
   }

   /**
    * @param docTime time of document. Nebulous. First write? Sign off? Also, time format is not specified.
    * @return this builder
    */
   public JCasBuilder setDocTime( final String docTime ) {
      _docTime = docTime;
      return this;
   }

   /**
    * @param docPath path to the file containing the doc, a database cell containing the doc, net location, etc.
    * @return this builder
    */
   public JCasBuilder setDocPath( final String docPath ) {
      _docPath = docPath;
      return this;
   }

   /**
    * Use if the document text should be null.
    * @return this builder
    */
   public JCasBuilder nullDocText() {
      _nullDocText = true;
      return this;
   }

   /**
    * @param docText text contents of the document.
    * @return this builder
    */
   public JCasBuilder setDocText( final String docText ) {
      _docText = docText;
      return this;
   }

   /**
    * @return a jcas created from scratch and populated with data added in this builder.
    * @throws UIMAException is the fresh jcas cannot be created.
    */
   public JCas build() throws UIMAException {
      return populate( JCasFactory.createJCas() );
   }

   /**
    * @param jCas ye olde ...
    * @return a jcas  that has been reset (emptied of previous information) and populated with data added in this builder.
    */
   public JCas rebuild( final JCas jCas ) {
      jCas.reset();
      return populate( jCas );
   }

   /**
    * @param value set by user or null
    * @param defaultValue -
    * @return true if the value is not null, and is not the default value or overwrite is true.
    */
   private boolean ifWrite( final String value, final String defaultValue ) {
      return value != null && ( _overwrite || !defaultValue.equals( value ) );
   }

   /**
    * @param value set by user or null
    * @param defaultValue -
    * @return true if the value is not the default value or overwrite is true.
    */
   private boolean ifWrite( final int value, final int defaultValue ) {
      return _overwrite || value != defaultValue;
   }

   /**
    * @param value set by user or null
    * @param defaultValue -
    * @return true if the value is not the default value or overwrite is true.
    */
   private boolean ifWrite( final long value, final long defaultValue ) {
      return _overwrite || value != defaultValue;
   }


   /**
    * @param jCas ye olde ...
    * @return the given jcas populated with the data added in this builder.
    */
   public JCas populate( final JCas jCas ) {
      final Metadata metadata = SourceMetadataUtil.getOrCreateMetadata( jCas );

      if ( ifWrite( _patientId, SourceMetadataUtil.UNKNOWN_PATIENT ) ) {
         SourceMetadataUtil.setPatientIdentifier( jCas, _patientId );
      }
      if ( ifWrite( _patientNum, SourceMetadataUtil.UNKNOWN_PATIENT_NUM ) ) {
         metadata.setPatientID( _patientNum );
      }

      final Demographics demographics = new Demographics( jCas );
      metadata.setDemographics( demographics );
      if ( ifWrite( _firstName, UNKNOWN ) ) {
         demographics.setFirstName( _firstName );
      }
      if ( ifWrite( _middleName, UNKNOWN ) ) {
         demographics.setMiddleName( _middleName );
      }
      if ( ifWrite( _lastName, UNKNOWN ) ) {
         demographics.setLastName( _lastName );
      }
      if ( ifWrite( _birthday, UNKNOWN_DATE ) ) {
         demographics.setBirthDate( _birthday );
      }
      if ( ifWrite( _deathday, UNKNOWN_DATE ) ) {
         demographics.setDeathDate( _deathday );
      }
      if ( ifWrite( _gender, UNKNOWN_GENDER ) ) {
         demographics.setGender( _gender );
      }

      final SourceData sourceData = SourceMetadataUtil.getOrCreateSourceData( jCas );
      if ( ifWrite( _institutionId, UNKNOWN ) ) {
         sourceData.setSourceInstitution( _institutionId );
      }
      if ( ifWrite( _authorSpecialty, UNKNOWN ) ) {
         sourceData.setAuthorSpecialty( _authorSpecialty );
      }
      if ( ifWrite( _encounterId, "" ) ) {
         sourceData.setSourceEncounterId( _encounterId );
      }
      if ( ifWrite( _instanceId, UNKNOWN ) ) {
         sourceData.setSourceInstanceId( _instanceId );
      }

      if ( ifWrite( _docId, DocIdUtil.NO_DOCUMENT_ID ) ) {
         final DocumentID documentId = new DocumentID( jCas );
         documentId.setDocumentID( _docId );
         documentId.addToIndexes();
      }

      if ( ifWrite( _docIdPrefix, DocIdUtil.NO_DOCUMENT_ID_PREFIX ) ) {
         final DocumentIdPrefix documentIdPrefix = new DocumentIdPrefix( jCas );
         documentIdPrefix.setDocumentIdPrefix( _docIdPrefix );
         documentIdPrefix.addToIndexes();
      }

      if ( ifWrite( _docType, NoteSpecs.ID_NAME_CLINICAL_NOTE ) ) {
         sourceData.setNoteTypeCode( _docType );
      }
      if ( ifWrite( _docSubType, "" ) ) {
         sourceData.setNoteSubTypeCode( _docSubType );
      }
      if ( ifWrite( _docStandard, "" ) ) {
         sourceData.setDocumentStandard( _docStandard );
      }

      if ( ifWrite( _docTime, "" ) ) {
         sourceData.setSourceRevisionDate( _docTime );
      }
      if ( ifWrite( _docRevisionNum, 1 ) ) {
         sourceData.setSourceRevisionNbr( _docRevisionNum );
      }

      if ( ifWrite( _docPath, "" ) ) {
         final DocumentPath documentPath = new DocumentPath( jCas );
         documentPath.setDocumentPath( _docPath );
         documentPath.addToIndexes();
      }

      if ( !_nullDocText ) {
         jCas.setDocumentText( _docText );
      }

      return jCas;
   }

}
