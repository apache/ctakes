package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Composition;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/19/2018
 */
final public class CompositionCreator implements FhirResourceCreator<TOP, Composition> {

   static private final Logger LOGGER = Logger.getLogger( "DpheCompositionCreator" );

   static public final String ID_NAME_COMPOSITION = "Composition";

   /**
    * {@inheritDoc}
    */
   @Override
   public Composition createResource( final JCas jCas, final TOP nullified, final FhirPractitioner practitioner,
                                      final FhirNoteSpecs noteSpecs ) {

      final Composition composition = new Composition();

      // 0 .. 1 , not necessary and mostly redundant with id
//      composition.setIdentifier( FhirElementFactory.createIdentifier( jCas, DPHE_COMPOSITION_ID, noteSpecs.getPeriod() ) );

      composition.addAuthor( practitioner.getPractitionerReference() );
      composition.setId( FhirElementFactory.createId( jCas, ID_NAME_COMPOSITION, noteSpecs.getNoteTime() ) );
      composition.setStatus( Composition.CompositionStatus.FINAL );
      composition.setType( getCompositionType() );
      composition.setLanguage( "English" );
      composition.setSubject( noteSpecs.getSubjectReference( FhirNoteSpecs.SUBJECT_PATIENT ) );

      // The following claim to have a 0 .. 1 cardinality, but the doc also states that they SHALL be listed.
      composition.setEncounter( noteSpecs.getEncounterReference() );
      composition.setCustodian( practitioner.getOrganizationReference() );
//      composition.addEvent( noteSpecs.getEvent() );
      composition.addAttester( practitioner.createAttester( noteSpecs ) );

      composition.setText( FhirElementFactory.createNarrative( jCas.getDocumentText() ) );

      return composition;
   }

   // TODO  CodeableConcept applicable to the note type
   public CodeableConcept getCompositionType() {
//            shortDefinition = "Kind of composition (LOINC if possible)",
//            formalDefinition = "Specifies the particular kind of composition (e.g. History and Physical, Discharge Summary, Progress Note). This usually equates to the purpose of making the composition."
      return new CodeableConcept();
   }


}
