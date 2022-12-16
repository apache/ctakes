package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Creates fhir domain resources of specific types determined by ctakes semantic grouping.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/21/2020
 */
final public class CtakesTypeCreator implements FhirResourceCreator<IdentifiedAnnotation, DomainResource> {

   static public final String GENERIC_EXT = "generic";
   static public final String UNCERTAIN_EXT = "uncertain";
   static public final String NEGATED_EXT = "negated";
   static public final String HISTORIC_EXT = "historic";


   /**
    * Creates fhir domain resources of specific types determined by ctakes semantic grouping.
    *
    * @param jCas ye olde ...
    * @return begin offset sorted list of FHIR DomainResources for identified annotations in jcas.
    */
   static public List<DomainResource> createTypeSystemResources( final JCas jCas ) {
      final CtakesTypeCreator creator = new CtakesTypeCreator();

      final FhirNoteSpecs noteSpecs = new FhirNoteSpecs( jCas );
      final FhirPractitioner practitioner = PractitionerCtakes.getInstance();

      final Map<IdentifiedAnnotation, Collection<Integer>> markableCorefs
            = EssentialAnnotationUtil.createMarkableCorefs( jCas );

      return EssentialAnnotationUtil.getRequiredAnnotations( jCas, markableCorefs )
                                    .stream()
                                    .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                                    .map( a -> creator.createResource( jCas, a, practitioner, noteSpecs ) )
                                    .collect( Collectors.toList() );
   }

   /**
    * Creates fhir domain resource of a specific type determined by ctakes semantic grouping.
    *
    * @param jCas         ye olde ...
    * @param annotation   -
    * @param practitioner -
    * @param noteSpecs    -
    * @return FHIR DomainResource for identified annotation.
    */
   public DomainResource createResource( final JCas jCas,
                                         final IdentifiedAnnotation annotation,
                                         final FhirPractitioner practitioner,
                                         final FhirNoteSpecs noteSpecs ) {
      final Reference practitionerRef = practitioner.getPractitionerReference();
      final Reference subjectRef = noteSpecs.getSubjectReference( annotation.getSubject() );
      final DomainResource resource = createDomainResource( annotation, practitionerRef, subjectRef );
      final String id
            = FhirElementFactory.createId( jCas, resource.getClass().getSimpleName(), annotation.hashCode() );
      resource.setId( id );
      // Add text span as an extension.
      resource.addExtension( FhirElementFactory.createSpanBegin( annotation ) );
      resource.addExtension( FhirElementFactory.createSpanEnd( annotation ) );
      // Add DocTimeRel as an extension.
      if ( annotation instanceof EventMention ) {
         final Extension dtr = FhirElementFactory.createDocTimeRel( (EventMention)annotation );
         if ( dtr != null ) {
            resource.addExtension( dtr );
         }
      }
      // Add generic, uncertainty, negation as modifier extensions.
      if ( IdentifiedAnnotationUtil.isGeneric( annotation ) ) {
         resource.addModifierExtension( FhirElementFactory.createTrueExtension( GENERIC_EXT ) );
      }
      if ( IdentifiedAnnotationUtil.isUncertain( annotation ) ) {
         resource.addModifierExtension( FhirElementFactory.createTrueExtension( UNCERTAIN_EXT ) );
      }
      if ( IdentifiedAnnotationUtil.isNegated( annotation ) ) {
         resource.addModifierExtension( FhirElementFactory.createTrueExtension( NEGATED_EXT ) );
      }
      // Add history of as a modifier extension.
      if ( IdentifiedAnnotationUtil.isHistoric( annotation ) ) {
         resource.addModifierExtension( FhirElementFactory.createTrueExtension( HISTORIC_EXT ) );
      }
      return resource;
   }


   static private DomainResource createDomainResource( final IdentifiedAnnotation annotation,
                                                       final Reference practitioner,
                                                       final Reference subject ) {
      final SemanticGroup group = SemanticGroup.getBestGroup( annotation );
      switch ( group ) {
         // Drug can also be Medication if it is general.  Immunization if known or NutritionOrder.
         case DRUG:
            return createDrug( annotation, practitioner, subject );
         // Can also be AdverseEvent, AllergyIntolerance.
         case DISORDER:
            return createCondition( annotation, practitioner, subject );
         // Can also be DetectedIssue.
         case PROCEDURE:
            return createProcedure( annotation, practitioner, subject );
         // May become BodyStructure instead of BodySite.
         case ANATOMY:
            return createBodySite( annotation, practitioner, subject );
         // May become ClinicalAttribute.
         case FINDING:
         case CLINICAL_ATTRIBUTE:
         case LAB:
            return createObservation( annotation, practitioner, subject );
      }
      return new Basic()
            .setCode( FhirElementFactory.createPrimaryCode( annotation ) )
            .setAuthor( practitioner )
            .setSubject( subject );
   }


   static private MedicationStatement createDrug( final IdentifiedAnnotation annotation,
                                                  final Reference practitioner,
                                                  final Reference subject ) {
      MedicationStatement.MedicationStatementStatus status = MedicationStatement.MedicationStatementStatus.NULL;
      if ( IdentifiedAnnotationUtil.isHistoric( annotation ) ) {
         status = MedicationStatement.MedicationStatementStatus.STOPPED;
      } else if ( IdentifiedAnnotationUtil.isRealAffirmed( annotation ) ) {
         status = MedicationStatement.MedicationStatementStatus.ACTIVE;
      }
      return new MedicationStatement()
            .setMedication( FhirElementFactory.createPrimaryCode( annotation ) )
            .setStatus( status )
            .setInformationSource( practitioner )
            .setSubject( subject );
   }

   static private Condition createCondition( final IdentifiedAnnotation annotation,
                                             final Reference practitioner,
                                             final Reference subject ) {
      return new Condition()
            .setCode( FhirElementFactory.createPrimaryCode( annotation ) )
            .setAsserter( practitioner )
            .setSubject( subject );
   }

   static private Observation createObservation( final IdentifiedAnnotation annotation,
                                                 final Reference practitioner,
                                                 final Reference subject ) {
      return new Observation()
            .setCode( FhirElementFactory.createPrimaryCode( annotation ) )
            .setStatus( Observation.ObservationStatus.UNKNOWN )
            .addPerformer( practitioner )
            .setSubject( subject );
   }

   static private Procedure createProcedure( final IdentifiedAnnotation annotation,
                                             final Reference practitioner,
                                             final Reference subject ) {
      Procedure.ProcedureStatus status = Procedure.ProcedureStatus.UNKNOWN;
      if ( IdentifiedAnnotationUtil.isHistoric( annotation ) ) {
         status = Procedure.ProcedureStatus.COMPLETED;
      } else if ( IdentifiedAnnotationUtil.isRealAffirmed( annotation ) ) {
         status = Procedure.ProcedureStatus.INPROGRESS;
      }
      // There is no author / reporter / recorder / asserter in hapi.
      return new Procedure()
            .setCode( FhirElementFactory.createPrimaryCode( annotation ) )
            .setStatus( status )
            .setSubject( subject );
   }

   static private BodySite createBodySite( final IdentifiedAnnotation annotation,
                                           final Reference practitioner,
                                           final Reference subject ) {
      // There is no author / reporter / recorder / asserter / information source in hapi.
      return new BodySite()
            .setCode( FhirElementFactory.createPrimaryCode( annotation ) )
            .setPatient( subject );
   }


}
