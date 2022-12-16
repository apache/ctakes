package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.fhir.element.FhirElementParser;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;

import java.util.Collection;
import java.util.List;

import static org.apache.ctakes.fhir.element.FhirElementFactory.*;
import static org.apache.ctakes.fhir.resource.IdentifiedAnnotationCreator.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
final public class IdentifiedAnnotationParser implements FhirBasicParser<IdentifiedAnnotation> {

   static private final Logger LOGGER = Logger.getLogger( "IdentifiedAnnotationBasicParser" );

   public IdentifiedAnnotation parseResource( final JCas jCas, final Basic resource ) {

      final CodeableConcept codeableConcept = resource.getCode();
      final List<Coding> codings = codeableConcept.getCoding();
      // Get annotation semantic group id
      final String groupName = FhirElementParser.getCode( codeableConcept, CODING_SEMANTIC );
      final SemanticGroup group = SemanticGroup.getGroup( groupName );
      // create annotation
      final IdentifiedAnnotation annotation = createSemanticAnnotation( jCas, group );
      annotation.setTypeID( group.getCode() );
      // create OntologyConceptArray
      final Collection<UmlsConcept> umlsConcepts = FhirElementParser.getUmlsConceptArray( jCas, codeableConcept );
      final FSArray conceptArr = new FSArray( jCas, umlsConcepts.size() );
      int arrIdx = 0;
      for ( UmlsConcept umlsConcept : umlsConcepts ) {
         conceptArr.set( arrIdx, umlsConcept );
         arrIdx++;
      }
      annotation.setOntologyConceptArr( conceptArr );
      // text span
      addTextSpan( annotation, resource, LOGGER );

      // Set properties
      final List<Extension> extensions = resource.getExtension();
      extensions.addAll( resource.getModifierExtension() );
      if ( FhirElementParser.hasExtension( GENERIC_EXT, extensions ) ) {
         annotation.setGeneric( true );
      }
      if ( FhirElementParser.hasExtension( UNCERTAIN_EXT, extensions ) ) {
         annotation.setUncertainty( CONST.NE_UNCERTAINTY_PRESENT );
      }
      if ( FhirElementParser.hasExtension( NEGATED_EXT, extensions ) ) {
         annotation.setPolarity( CONST.NE_POLARITY_NEGATION_PRESENT );
      }
      if ( FhirElementParser.hasExtension( HISTORIC_EXT, extensions ) ) {
         annotation.setHistoryOf( CONST.NE_HISTORY_OF_PRESENT );
      }
      // TODO subject
      annotation.setSubject( FhirElementParser.getSubjectId( resource ) );
      if ( annotation instanceof EventMention ) {
         final Event event = createEvent( jCas, extensions );
         if ( event != null ) {
            ((EventMention) annotation).setEvent( event );
         }
      }

      annotation.addToIndexes();
      return annotation;
   }

   static private IdentifiedAnnotation createSemanticAnnotation( final JCas jcas, final SemanticGroup group ) {
      return group.getCreator().apply( jcas );
   }

   static private Event createEvent( final JCas jCas, final Collection<Extension> extensions ) {
      final String dtrUrl = createCtakesFhirUrl( DOCTIMEREL_EXT );
      for ( Extension extension : extensions ) {
         if ( !extension.getUrl()
               .equals( dtrUrl ) ) {
            continue;
         }
         final String dtr = FhirElementParser.parseString( extension );
         final EventProperties properties = new EventProperties( jCas );
         properties.setDocTimeRel( dtr );
         final Event event = new Event( jCas );
         event.setProperties( properties );
         return event;
      }
      return null;
   }

}
