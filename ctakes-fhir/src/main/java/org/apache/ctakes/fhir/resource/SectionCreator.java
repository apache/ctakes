package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class SectionCreator implements FhirBasicCreator<Segment> {

   static private final Logger LOGGER = Logger.getLogger( "SectionBasicCreator" );

   static public final String CODING_SECTION_NAME = "section-name";
   static public final String CODING_SECTION_ID = "section-id";

   static public final String SECTION_EXT = "document-section";
   static public final String ID_NAME_SECTION = "DocumentSection";

   /**
    * {@inheritDoc}
    */
   @Override
   public String getIdName() {
      return ID_NAME_SECTION;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Basic createResource( final JCas jCas, final Segment section, final FhirPractitioner practitioner,
                                final FhirNoteSpecs noteSpecs ) {
      final Basic basic = createAnnotationBasic( jCas, section, practitioner );

      // The 'code' is the normalized section name PLUS section tag text.
      final CodeableConcept codeableConcept = FhirElementFactory.createSimpleCode( section );
      codeableConcept.addCoding( new Coding( CODING_SECTION_NAME, section.getPreferredText(), null ) );
      if ( !section.getPreferredText()
            .equals( section.getId() ) ) {
         codeableConcept.addCoding( new Coding( CODING_SECTION_ID, section.getId(), null ) );
      }
      codeableConcept.setText( section.getTagText() );
      basic.setCode( codeableConcept );

      return basic;
   }

}
