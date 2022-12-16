package org.apache.ctakes.core.util;

import org.apache.ctakes.typesystem.type.structured.Metadata;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceProcessException;

import java.sql.Timestamp;

/**
 * Utility class with convenience methods for a few commonly-used JCas Metadata types that are begged of the source
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2015
 * @deprecated use SourceMetadataUtil in (sub) package doc
 */
@Deprecated
final public class SourceMetadataUtil {

   @Deprecated
   static public final String UNKNOWN_PATIENT = org.apache.ctakes.core.util.doc.SourceMetadataUtil.UNKNOWN_PATIENT;
   @Deprecated
   static public final long UNKNOWN_PATIENT_NUM
         = org.apache.ctakes.core.util.doc.SourceMetadataUtil.UNKNOWN_PATIENT_NUM;

   private SourceMetadataUtil() {
   }

   /**
    * @param jcas ye olde jay-cas
    * @return the patient identifier for the source or {@link #UNKNOWN_PATIENT}
    */
   @Deprecated
   static public String getPatientIdentifier( final JCas jcas ) {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getPatientIdentifier( jcas );
   }

   /**
    * @param jCas      ye olde
    * @param patientId the patient identifier for the source
    */
   @Deprecated
   static public void setPatientIdentifier( final JCas jCas, final String patientId ) {
      org.apache.ctakes.core.util.doc.SourceMetadataUtil.setPatientIdentifier( jCas, patientId );
   }

   /**
    * @param jcas ye olde jay-cas
    * @return the patient id for the source or {@link #UNKNOWN_PATIENT_NUM} if one is not found
    */
   @Deprecated
   static public long getPatientNum( final JCas jcas ) {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getPatientNum( jcas );
   }

   /**
    * @param jcas ye olde jay-cas
    * @return the Metadata for the given jcas or null if one is not found
    */
   @Deprecated
   static public Metadata getMetadata( final JCas jcas ) {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getMetadata( jcas );
   }

   /**
    * @param jCas ye olde jay-cas
    * @return the Metadata for the given jcas
    */
   @Deprecated
   static public Metadata getOrCreateMetadata( final JCas jCas ) {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getOrCreateMetadata( jCas );
   }

   /**
    * The first step in utilizing SourceData is getting it!
    *
    * @param jcas ye olde jay-cas
    * @return the metadata for the source associated with the jcas or null if one is not found
    */
   @Deprecated
   static public SourceData getSourceData( final JCas jcas ) {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getSourceData( jcas );
   }

   /**
    * @param jCas ye olde jay-cas
    * @return the metadata for the source associated with the jcas
    */
   @Deprecated
   static public SourceData getOrCreateSourceData( final JCas jCas ) {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getOrCreateSourceData( jCas );
   }

   /**
    * @param sourcedata -
    * @return the instance id or -1 if there isn't one
    * @throws ResourceProcessException if the internal value is not parseable as long
    */
   @Deprecated
   static public long getInstanceNum( final SourceData sourcedata ) throws ResourceProcessException {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getInstanceNum( sourcedata );
   }

   /**
    * @param sourcedata -
    * @return the encounter id
    * @throws ResourceProcessException if the encounter id does not exist or is not parseable as an int
    */
   @Deprecated
   static public int getEncounterNum( final SourceData sourcedata ) throws ResourceProcessException {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getEncounterNum( sourcedata );
   }

   /**
    * @param sourcedata -
    * @return the author specialty
    */
   @Deprecated
   static public String getProviderId( final SourceData sourcedata ) {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getProviderId( sourcedata );
   }

   /**
    * @param sourcedata -
    * @return the original date for the source
    */
   @Deprecated
   static public Timestamp getStartDate( final SourceData sourcedata ) {
      return org.apache.ctakes.core.util.doc.SourceMetadataUtil.getStartDate( sourcedata );
   }

}
