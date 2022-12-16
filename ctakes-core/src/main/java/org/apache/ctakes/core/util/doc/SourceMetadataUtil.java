package org.apache.ctakes.core.util.doc;

import org.apache.ctakes.typesystem.type.structured.Metadata;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceProcessException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility class with convenience methods for a few commonly-used JCas Metadata types that are begged of the source
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2015
 */
final public class SourceMetadataUtil {

   static private final Logger LOGGER = Logger.getLogger( "SourceMetadataUtil" );

   static public final String UNKNOWN_PATIENT = "UnknownPatient";
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

}
