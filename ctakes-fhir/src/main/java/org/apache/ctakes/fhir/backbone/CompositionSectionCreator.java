package org.apache.ctakes.fhir.backbone;


import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Composition;

import static org.hl7.fhir.dstu3.model.Composition.SectionComponent;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/19/2018
 */
final public class CompositionSectionCreator {

   static private final Logger LOGGER = Logger.getLogger( "CompositionSectionCreator" );

   private CompositionSectionCreator() {
   }

   /**
    * @param jCas    ye olde ...
    * @param segment uima type system representation of a clinical note section.
    * @return fhir SectionComponent, part of the Composition.
    */
   static public SectionComponent createSectionComponent( final JCas jCas, final Segment segment ) {
      final SectionComponent sectionComponent = new SectionComponent();

      sectionComponent.setId( FhirElementFactory.createId( jCas, segment.getId(), segment.hashCode() ) );
      sectionComponent.setMode( Composition.SectionMode.SNAPSHOT );
      sectionComponent.setTitle( segment.getPreferredText() );

      // The 'code' is the normalized section name PLUS section tag text.
      final CodeableConcept codeableConcept = FhirElementFactory.createSimpleCode( segment );
      codeableConcept.addCoding( new Coding( "section-name", segment.getPreferredText(), null ) );
      if ( !segment.getPreferredText()
            .equals( segment.getId() ) ) {
         codeableConcept.addCoding( new Coding( "id", segment.getId(), null ) );
      }
      codeableConcept.setText( segment.getTagText() );
      sectionComponent.setCode( codeableConcept );

      // Add text span as an extension.
      sectionComponent.addExtension( FhirElementFactory.createSpanBegin( segment ) );
      sectionComponent.addExtension( FhirElementFactory.createSpanEnd( segment ) );

      return sectionComponent;
   }

}
