package org.apache.ctakes.core.util.patient;

import org.apache.ctakes.core.util.CalendarUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.structured.*;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Calendar;


/**
 * Populate a Patient with identifying, demographic, and document set information.
 * @author SPF , chip-nlp
 * @since {2/25/2026}
 */
final public class PatientBuilder {

   static private final String UNKNOWN_DATE = "UnknownDate";
   static private final String UNKNOWN_GENDER = "UnknownGender";
   static private final int UNKNOWN_COUNT = -1;
   static private final String UNKNOWN = "Unknown";

   private String _patientId = SourceMetadataUtil.UNKNOWN_PATIENT;
   private long _patientNum = SourceMetadataUtil.UNKNOWN_PATIENT_NUM;

   private String _gender = UNKNOWN_GENDER;
   private String _firstName = UNKNOWN;
   private String _middleName = UNKNOWN;
   private String _lastName = UNKNOWN;
   private String _birthDate = UNKNOWN_DATE;
   private String _deathDate = UNKNOWN_DATE;
   private Date _birthDateValue = null;
   private Date _deathDateValue = null;

   private int _docCount = UNKNOWN_COUNT;


   /**
    * Set values of this builder based upon the given Patient.  That Patient will be removed from all Cas indices.
    * @param patient -
    * @return this builder.
    */
   public PatientBuilder seedPatient( final Patient patient ) {
      setPatientId( patient.getPatientIdentifier() );
      setPatientNum( patient.getPatientNum() );
      setGender( patient.getDemographics().getGender() );
      setFirstName( patient.getDemographics().getFirstName() );
      setMiddleName( patient.getDemographics().getMiddleName() );
      setLastName( patient.getDemographics().getLastName() );
      setBirthDate( patient.getDemographics().getBirthDate() );
      setBirthDateValue( patient.getDemographics().getBirthDateValue() );
      setDeathDate( patient.getDemographics().getDeathDate() );
      setDeathDateValue( patient.getDemographics().getDeathDateValue() );
      patient.removeFromIndexes();
      return this;
   }


   /**
    * @param patientId The unique ID of a patient at an institution, in a study, in an insurance database, etc.
    * @return this builder
    */
   public PatientBuilder setPatientId( final String patientId ) {
      _patientId = patientId;
      return this;
   }

   /**
    * @param patientNum Depends upon user. e.g. Index of a patient in the run, index/number of the patient in a database, etc.
    * @return this builder
    */
   public PatientBuilder setPatientNum( final long patientNum ) {
      _patientNum = patientNum;
      return this;
   }

   /**
    * @param firstName Patient's first name.
    * @return this builder
    */
   public PatientBuilder setFirstName( final String firstName ) {
      _firstName = firstName;
      return this;
   }

   /**
    * @param middleName Patient's middle name.
    * @return this builder
    */
   public PatientBuilder setMiddleName( final String middleName ) {
      _middleName = middleName;
      return this;
   }

   /**
    * @param lastName Patient's last name.
    * @return this builder
    */
   public PatientBuilder setLastName( final String lastName ) {
      _lastName = lastName;
      return this;
   }

   /**
    * @param birthDate Patient's date of birth. Format of this date is not controlled.
    * @return this builder
    */
   public PatientBuilder setBirthDate( final String birthDate ) {
      _birthDate = birthDate;
      return this;
   }

   /**
    * @param birthDateValue Patient's date of birth. Format of this date is controlled.
    * @return this builder
    */
   public PatientBuilder setBirthDateValue( final Date birthDateValue ) {
      _birthDateValue = birthDateValue;
      return this;
   }

   /**
    * @param deathDate Patient's date of death. Format of this date is not controlled.
    * @return this builder
    */
   public PatientBuilder setDeathDate( final String deathDate ) {
      _deathDate = deathDate;
      return this;
   }

   /**
    * @param deathDateValue Patient's date of death. Format of this date is not controlled.
    * @return this builder
    */
   public PatientBuilder setDeathDateValue( final Date deathDateValue ) {
      _deathDateValue = deathDateValue;
      return this;
   }

   /**
    * @param gender Patient's date of gender. Format of this value is not controlled.
    * @return this builder
    */
   public PatientBuilder setGender( final String gender ) {
      _gender = gender;
      return this;
   }

   /**
    *
    * @param docCount number of documents belonging to this patient.
    * @return this builder
    */
   public PatientBuilder setDocCount( final int docCount ) {
      _docCount = docCount;
      return this;
   }

   /**
    * @param value set by user or null
    * @param defaultValue -
    * @return true if the value is not null, and is not the default value.
    */
   private boolean ifWrite( final String value, final String defaultValue ) {
      return value != null && !defaultValue.equals( value );
   }

   /**
    * @param value set by user or null
    * @param defaultValue -
    * @return true if the value is not the default value.
    */
   private boolean ifWrite( final int value, final int defaultValue ) {
      return value != defaultValue;
   }

   /**
    * @param value set by user or null
    * @param defaultValue -
    * @return true if the value is not the default value.
    */
   private boolean ifWrite( final long value, final long defaultValue ) {
      return value != defaultValue;
   }

   /**
    * @param jCas ye olde ...
    * @return the Patient populated with the data added in this builder.
    */
   public Patient build( final JCas jCas ) {
      JCasUtil.select( jCas, Patient.class ).forEach( FeatureStructureImplC::removeFromIndexes );
      final Patient patient = new Patient( jCas );
      final Metadata metadata = SourceMetadataUtil.getOrCreateMetadata( jCas );
      // Always set the Patient ID, even if it is unknown.
      patient.setPatientIdentifier( _patientId );
      SourceMetadataUtil.setPatientIdentifier( jCas, _patientId );
      if ( ifWrite( _patientNum, SourceMetadataUtil.UNKNOWN_PATIENT_NUM ) ) {
         patient.setPatientNum( _patientNum );
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
      if ( ifWrite( _birthDate, UNKNOWN_DATE ) ) {
         demographics.setBirthDate( _birthDate );
         if ( _birthDateValue == null ) {
            final Calendar normalized = CalendarUtil.getCalendar( _birthDate );
            if ( !CalendarUtil.NULL_CALENDAR.equals( normalized ) ) {
               _birthDateValue = CalendarUtil.createTypeDate( jCas, normalized );
            }
         }
      }
      if ( _birthDateValue != null ) {
         demographics.setBirthDateValue( _birthDateValue );
      }
      if ( ifWrite( _deathDate, UNKNOWN_DATE ) ) {
         demographics.setDeathDate( _deathDate );
         if ( _deathDateValue == null ) {
            final Calendar normalized = CalendarUtil.getCalendar( _deathDate );
            if ( !CalendarUtil.NULL_CALENDAR.equals( normalized ) ) {
               _deathDateValue = CalendarUtil.createTypeDate( jCas, normalized );
            }
         }
      }
      if ( _deathDateValue != null ) {
         demographics.setDeathDateValue( _deathDateValue );
      }
      if ( ifWrite( _gender, UNKNOWN_GENDER ) ) {
         demographics.setGender( _gender );
      }
      patient.setDemographics( demographics );

      if ( ifWrite( _docCount, UNKNOWN_COUNT ) ) {
         patient.setDocumentCount( _docCount );
      }
      patient.addToIndexes( jCas );
      return patient;
   }


}
