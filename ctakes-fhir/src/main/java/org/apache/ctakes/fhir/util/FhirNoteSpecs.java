package org.apache.ctakes.fhir.util;


import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fhir information about a clinical note, including the patient and other subjects.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class FhirNoteSpecs {

   static private final Logger LOGGER = Logger.getLogger( "FhirNoteSpecs" );

   // ClinicalNote is not yet a fhir resource, but it may be in the future and is the best fit.
   // http://wiki.hl7.org/index.php?title=ClinicalNote_FHIR_Resource_Proposal
   static public final String ID_NAME_CLINICAL_NOTE = "ClinicalNote";

   static public final String SUBJECT_PATIENT = "patient";
   static private final String CTAKES_ENCOUNTER_ID = "ctakes_encounter";
   static private final String ID_NAME_SUBJECT = "Subject";

   private final NoteSpecs _noteSpecs;
   private final String _clinicalNoteId;

   private final Reference _mainPatientRef;
   private final Map<String, Reference> _subjectRefs;
   private final Period _period;

   private final Reference _encounterReference;

   /**
    * @param jCas ye olde ...
    */
   public FhirNoteSpecs( final JCas jCas ) {
      _noteSpecs = new NoteSpecs( jCas );

      final String noteDateText = _noteSpecs.getNoteTime();
      _clinicalNoteId = FhirElementFactory.createId( jCas, ID_NAME_CLINICAL_NOTE, noteDateText );

      final Date noteDate = _noteSpecs.getNoteDate();
      final long noteDateMillis = noteDate.getTime();
      _period = FhirElementFactory.createPeriod( noteDateMillis, noteDateMillis + 1000 );
      final Patient mainPatient = new Patient();
      final String patientName = _noteSpecs.getPatientName();
      mainPatient.addName( createHumanName( patientName ) );
      mainPatient.setId( FhirElementFactory.createId( jCas, patientName, patientName.hashCode() ) );
      _mainPatientRef = new Reference( mainPatient );
      _subjectRefs = createSubjects( jCas );
      final SourceData sourceData = SourceMetadataUtil.getSourceData( jCas );
      final Encounter encounter = createEncounter( jCas, sourceData, noteDateText );
      _encounterReference = new Reference( encounter );
   }

   /**
    * @return the standard document id, usually set by a collection reader.
    */
   public String getDocumentId() {
      return _noteSpecs.getDocumentId();
   }

   /**
    * @return name of patient, or the default "Generic".
    */
   public String getPatientName() {
      return _noteSpecs.getPatientName();
   }

   /**
    * @return fhir patient.
    */
   public Patient getPatient() {
      return (Patient) _mainPatientRef.getResource();
   }

   /**
    * @return subjects in note as fhir resources.
    */
   public Collection<Resource> getSubjects() {
      final Collection<Resource> subjects = new ArrayList<>();
      subjects.add( (Resource) _mainPatientRef.getResource() );
      _subjectRefs.values()
            .forEach( s -> subjects.add( (Resource) s.getResource() ) );
      return subjects;
   }

   /**
    * @param name subject name.
    * @return subject as a fhir reference.
    */
   public Reference getSubjectReference( final String name ) {
      if ( name == null || name.isEmpty() || SUBJECT_PATIENT.equalsIgnoreCase( name ) ) {
         return _mainPatientRef;
      }
      return _subjectRefs.getOrDefault( name.toLowerCase(), _mainPatientRef );
   }

   /**
    * @return fhir period of validity for the note information.
    */
   public Period getPeriod() {
      return _period;
   }

   /**
    * @param jCas    ye olde ...
    * @param subject subject name.
    * @return fhir reference for subject.
    */
   private Reference createSubject( final JCas jCas, final String subject ) {
      final RelatedPerson relation = new RelatedPerson( _mainPatientRef );
      relation.setId( FhirElementFactory.createId( jCas, ID_NAME_SUBJECT, subject.hashCode() ) );
      return new Reference( relation );
   }

   /**
    * @param jCas ye olde ...
    * @return map of subject names to fhir references.
    */
   private Map<String, Reference> createSubjects( final JCas jCas ) {
      return _noteSpecs.getSubjects().stream()
            .collect( Collectors.toMap( Function.identity(), s -> createSubject( jCas, s ) ) );
   }

   /**
    * @return the creation or possibly sign off date for the note as a Date object, or -now- as a default.
    */
   public Date getNoteDate() {
      return _noteSpecs.getNoteDate();
   }

   /**
    * @return the creation or possibly sign off date for the note, or -now- as a default, as a formatted String, yyyyMMddhhmm.
    */
   public String getNoteTime() {
      return _noteSpecs.getNoteTime();
   }

   /**
    * @return unique fhir id for the note.
    */
   public String getClinicalNoteId() {
      return _clinicalNoteId;
   }

   /**
    * @return encounter id as a fhir reference.
    */
   public Reference getEncounterReference() {
      return _encounterReference;
   }

   /**
    * @return type of note or the default "ClinicalNote".
    */
   public String getDocumentType() {
      return _noteSpecs.getDocumentType();
   }

   /**
    * @return complete text from the note.
    */
   public String getDocumentText() {
      return _noteSpecs.getDocumentText();
   }

   /**
    * @param patientName -
    * @return fhir human name object for the patient.
    */
   static private HumanName createHumanName( final String patientName ) {
      final Collection<String> prefixes = Arrays.asList( "Dr. ", "Mr. ", "Mrs. ", "Miss ", "Ms. ", "Sir " );
      final HumanName humanName = new HumanName();
      humanName.setUse( HumanName.NameUse.USUAL );
      String fullName = patientName;
      for ( String prefix : prefixes ) {
         if ( patientName.startsWith( prefix ) ) {
            humanName.addPrefix( prefix );
            fullName = patientName.substring( prefix.length() + 1 );
            break;
         }
      }
      final int spaceIndex = fullName.indexOf( ' ' );
      if ( spaceIndex > 0 ) {
         humanName.setFamily( fullName.substring( spaceIndex + 1 ) );
         humanName.addGiven( fullName.substring( 0, spaceIndex ) );
      } else {
         humanName.addGiven( fullName );
      }
      return humanName;
   }

   /**
    * @param jCas       ye olde ...
    * @param sourceData -
    * @param noteTime   yyyyMMddhhmm.
    * @return fhir encounter.
    */
   static private Encounter createEncounter( final JCas jCas, final SourceData sourceData, final String noteTime ) {
      String encounterId = CTAKES_ENCOUNTER_ID;
      if ( sourceData != null ) {
         final String sourceEncounterId = sourceData.getSourceEncounterId();
         if ( sourceEncounterId != null ) {
            encounterId = sourceEncounterId;
         }
      }
      final Encounter encounter = new Encounter();
      encounter.setId( FhirElementFactory.createId( jCas, encounterId, noteTime ) );
      encounter.setStatus( Encounter.EncounterStatus.FINISHED );
      return encounter;
   }


}
