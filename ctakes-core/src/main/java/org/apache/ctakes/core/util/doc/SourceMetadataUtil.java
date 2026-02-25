package org.apache.ctakes.core.util.doc;

import org.apache.ctakes.core.util.CalendarUtil;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.refsem.Time;
import org.apache.ctakes.typesystem.type.structured.Metadata;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceProcessException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * Utility class with convenience methods for a few commonly-used JCas Metadata types that are begged of the source
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2015
 */
final public class SourceMetadataUtil {

   static private final Logger LOGGER = LoggerFactory.getLogger( "SourceMetadataUtil" );

   static public final String UNKNOWN_PATIENT = "Unknown Patient";
   static public final long UNKNOWN_PATIENT_NUM = -1;

   private SourceMetadataUtil() {
   }

   /**
    * @param jcas ye olde jay-cas
    * @return the patient identifier for the source or {@link #UNKNOWN_PATIENT}
    */
   static public String getPatientIdentifier( final JCas jcas ) {
      final Metadata metadata = getMetadata( jcas );
      if ( metadata != null ) {
         final String patientId = metadata.getPatientIdentifier();
         if ( patientId != null && !patientId.isEmpty() ) {
            return patientId;
         }
      }
      final String docPrefix = DocIdUtil.getDocumentIdPrefix( jcas );
      if ( docPrefix != null && !docPrefix.isEmpty() ) {
         return docPrefix;
      }
      return UNKNOWN_PATIENT;
   }

   /**
    * @param jCas      ye olde
    * @param patientId the patient identifier for the source
    */
   static public void setPatientIdentifier( final JCas jCas, final String patientId ) {
      final Metadata metadata = getOrCreateMetadata( jCas );
      metadata.setPatientIdentifier( patientId );
   }

   /**
    * @param jcas ye olde jay-cas
    * @return the patient id for the source or {@link #UNKNOWN_PATIENT_NUM} if one is not found
    */
   static public long getPatientNum( final JCas jcas ) {
      final Metadata metadata = getMetadata( jcas );
      if ( metadata == null ) {
         return UNKNOWN_PATIENT_NUM;
      }
      return metadata.getPatientID();
   }

   /**
    * @param jcas ye olde jay-cas
    * @return the Metadata for the given jcas or null if one is not found
    */
   static public Metadata getMetadata( final JCas jcas ) {
      final Collection<Metadata> metadatas = JCasUtil.select( jcas, Metadata.class );
      if ( metadatas == null || metadatas.isEmpty() ) {
         return null;
      }
      return new ArrayList<>( metadatas ).get( 0 );
   }

   /**
    * @param jCas ye olde jay-cas
    * @return the Metadata for the given jcas
    */
   static public Metadata getOrCreateMetadata( final JCas jCas ) {
      final Metadata metadata = getMetadata( jCas );
      if ( metadata != null ) {
         return metadata;
      }
      final Metadata newMetadata = new Metadata( jCas );
      newMetadata.addToIndexes();
      return newMetadata;
   }

   /**
    * The first step in utilizing SourceData is getting it!
    *
    * @param jcas ye olde jay-cas
    * @return the metadata for the source associated with the jcas or null if one is not found
    */
   static public SourceData getSourceData( final JCas jcas ) {
      final Metadata metadata = getMetadata( jcas );
      if ( metadata == null ) {
         return null;
      }
      return metadata.getSourceData();
   }

   /**
    * @param jCas ye olde jay-cas
    * @return the metadata for the source associated with the jcas
    */
   static public SourceData getOrCreateSourceData( final JCas jCas ) {
      final SourceData sourceData = getSourceData( jCas );
      if ( sourceData != null ) {
         return sourceData;
      }
      final Metadata metadata = getOrCreateMetadata( jCas );
      final SourceData newSourceData = new SourceData( jCas );
      metadata.setSourceData( newSourceData );
      return newSourceData;
   }

   /**
    * @param sourcedata -
    * @return the instance id or -1 if there isn't one
    * @throws ResourceProcessException if the internal value is not parseable as long
    */
   static public long getInstanceNum( final SourceData sourcedata ) throws ResourceProcessException {
      final String instance = sourcedata.getSourceInstanceId();
      if ( instance == null || instance.isEmpty() ) {
         return -1;
      }
      long instanceNum;
      try {
         instanceNum = Long.parseLong( instance );
      } catch ( NumberFormatException nfE ) {
         // thrown by Integer.parseInt
         throw new ResourceProcessException( nfE );
      }
      return instanceNum;
   }

   /**
    * @param sourcedata -
    * @return the encounter id
    * @throws ResourceProcessException if the encounter id does not exist or is not parseable as an int
    */
   static public int getEncounterNum( final SourceData sourcedata ) throws ResourceProcessException {
      final String encounter = sourcedata.getSourceEncounterId();
      int encounterNum;
      try {
         encounterNum = Integer.parseInt( encounter );
      } catch ( NumberFormatException nfE ) {
         // thrown by Integer.parseInt
         throw new ResourceProcessException( nfE );
      }
      return encounterNum;
   }

   /**
    * @param sourcedata -
    * @return the author specialty
    */
   static public String getProviderId( final SourceData sourcedata ) {
      return sourcedata.getAuthorSpecialty();
   }

   /**
    * @param sourcedata -
    * @return the original date for the source
    */
   static public Timestamp getStartDate( final SourceData sourcedata ) {
      final String sourceDate = sourcedata.getSourceOriginalDate();
      return Timestamp.valueOf( sourceDate );
   }

   /**
    * Get the ctakes type Date representation of the document creation date as stored by "SourceCreationDate".
    * If that does not exist then get one as stored by "SourceOriginalDate", and if that doesn't exist "SourceRevisionDate".
    * If those two do not exist or cannot be normalized, use the current date.
    * @param jCas -
    * @return A normalized Date representing the creation date of the document. The current date if one did not exist.
    */
   static public Date getDocDate( final JCas jCas ) {
      final SourceData sourceData = getOrCreateSourceData( jCas );
      final Date docDate = sourceData.getSourceCreationDate();
      if ( docDate != null ) {
         return docDate;
      }
      final String startDate = sourceData.getSourceOriginalDate();
      if ( startDate != null && !startDate.isBlank() ) {
         final Calendar calendar = CalendarUtil.getCalendar( startDate );
         if ( !CalendarUtil.NULL_CALENDAR.equals( calendar ) ) {
            return setDocCreationDate( jCas, calendar );
         }
      }
      final String revisionDate = sourceData.getSourceRevisionDate();
      if ( revisionDate != null && !revisionDate.isBlank() ) {
         final Calendar calendar = CalendarUtil.getCalendar( revisionDate );
         if ( !CalendarUtil.NULL_CALENDAR.equals( calendar ) ) {
            return setDocCreationDate( jCas, calendar );
         }
      }
      return setDocCreationDate( jCas, Calendar.getInstance() );
   }

   /**
    * Get the ctakes type Time representation of the document creation time as stored by "SourceCreationTime".
    * If that does not exist then get one as stored by "SourceOriginalDate", and if that doesn't exist "SourceRevisionDate".
    * If those two do not exist or cannot be normalized, use the current time.
    * @param jCas -
    * @return A normalized Date representing the creation date of the document. The current date if one did not exist.
    */
   static public Time getDocTime( final JCas jCas ) {
      final SourceData sourceData = getOrCreateSourceData( jCas );
      final Time docTime = sourceData.getSourceCreationTime();
      if ( docTime != null ) {
         return docTime;
      }
      final String startDate = sourceData.getSourceOriginalDate();
      if ( startDate != null && !startDate.isBlank() ) {
         final Calendar calendar = CalendarUtil.getCalendar( startDate );
         if ( !CalendarUtil.NULL_CALENDAR.equals( calendar ) ) {
            setDocCreationDate( jCas, calendar );
            return sourceData.getSourceCreationTime();
         }
      }
      final String revisionDate = sourceData.getSourceRevisionDate();
      if ( revisionDate != null && !revisionDate.isBlank() ) {
         final Calendar calendar = CalendarUtil.getCalendar( revisionDate );
         if ( !CalendarUtil.NULL_CALENDAR.equals( calendar ) ) {
            setDocCreationDate( jCas, calendar );
            return sourceData.getSourceCreationTime();
         }
      }
      setDocCreationDate( jCas, Calendar.getInstance() );
      return sourceData.getSourceCreationTime();
   }


   /**
    *
    * @param jCas -
    * @param calendar old, simple java object representing a date and time.
    * @return ctakes type system date made from the given calendar. The current Date if calendar is null.
    */
   static public Date setDocCreationDate( final JCas jCas, final Calendar calendar ) {
      if ( calendar == null || CalendarUtil.NULL_CALENDAR.equals( calendar ) ) {
         return setDocCreationDate( jCas, Calendar.getInstance() );
      }
      final Date date = CalendarUtil.createTypeDate( jCas, calendar );
      final Time time = CalendarUtil.createTypeTime( jCas, calendar );
      final SourceData sourceData = getOrCreateSourceData( jCas );
      sourceData.setSourceCreationDate( date );
      sourceData.setSourceCreationTime( time );
      return date;
   }


}
