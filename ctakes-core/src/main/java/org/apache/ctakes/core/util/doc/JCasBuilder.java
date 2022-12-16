package org.apache.ctakes.core.util.doc;


import org.apache.ctakes.typesystem.type.structured.*;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Facade to "easily" populate a JCas with creator, patient and note information.
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


   public JCasBuilder overwrite() {
      _overwrite = true;
      return this;
   }

   public JCasBuilder setInstitutionId( final String institutionId ) {
      _institutionId = institutionId;
      return this;
   }

   public JCasBuilder setAuthorSpecialty( final String authorSpecialty ) {
      _authorSpecialty = authorSpecialty;
      return this;
   }

   public JCasBuilder setPatientId( final String patientId ) {
      _patientId = patientId;
      return this;
   }

   public JCasBuilder setPatientNum( final long patientNum ) {
      _patientNum = patientNum;
      return this;
   }

   public JCasBuilder setFirstName( final String firstName ) {
      _firstName = firstName;
      return this;
   }

   public JCasBuilder setMiddleName( final String middleName ) {
      _middleName = middleName;
      return this;
   }

   public JCasBuilder setLastName( final String lastName ) {
      _lastName = lastName;
      return this;
   }

   public JCasBuilder setBirthDay( final String birthday ) {
      _birthday = birthday;
      return this;
   }

   public JCasBuilder setDeathday( final String deathday ) {
      _deathday = deathday;
      return this;
   }

   public JCasBuilder setGender( final String gender ) {
      _gender = gender;
      return this;
   }

   public JCasBuilder setInstanceId( final String instanceId ) {
      _instanceId = instanceId;
      return this;
   }

   public JCasBuilder setEncounterId( final String encounterId ) {
      _encounterId = encounterId;
      return this;
   }

//   public JCasBuilder setEncounterNum( final int encounterNum ) {
//      _encounterNum = encounterNum;
//      return this;
//   }

   public JCasBuilder setDocId( final String docId ) {
      _docId = docId;
      return this;
   }

   public JCasBuilder setDocIdPrefix( final String docIdPrefix ) {
      _docIdPrefix = docIdPrefix;
      return this;
   }

   public JCasBuilder setDocType( final String docType ) {
      _docType = docType;
      return this;
   }

   public JCasBuilder setDocSubType( final String docSubType ) {
      _docSubType = docSubType;
      return this;
   }

   public JCasBuilder setDocStandard( final String docStandard ) {
      _docStandard = docStandard;
      return this;
   }

   public JCasBuilder setDocRevisionNum( final int docRevisionNum ) {
      _docRevisionNum = docRevisionNum;
      return this;
   }

   public JCasBuilder setDocTime( final String docTime ) {
      _docTime = docTime;
      return this;
   }

   public JCasBuilder setDocPath( final String docPath ) {
      _docPath = docPath;
      return this;
   }

   public JCasBuilder nullDocText() {
      _nullDocText = true;
      return this;
   }

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

   private boolean ifWrite( final String value, final String defaultValue ) {
      return value != null && ( _overwrite || !defaultValue.equals( value ) );
   }

   private boolean ifWrite( final int value, final int defaultValue ) {
      return _overwrite || value != defaultValue;
   }

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
