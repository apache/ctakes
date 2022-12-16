package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

import java.util.List;

import static org.apache.ctakes.fhir.resource.SectionCreator.CODING_SECTION_ID;
import static org.apache.ctakes.fhir.resource.SectionCreator.CODING_SECTION_NAME;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
final public class SectionParser implements FhirBasicParser<Segment> {

   static private final Logger LOGGER = Logger.getLogger( "SectionBasicParser" );

   public Segment parseResource( final JCas jCas, final Basic resource ) {
      final CodeableConcept codeableConcept = resource.getCode();
      final List<Coding> codings = codeableConcept.getCoding();
      String preferredText = "";
      String id = "";
      for ( Coding coding : codings ) {
         final String system = coding.getSystem();
         if ( system.equals( CODING_SECTION_NAME ) ) {
            preferredText = coding.getCode();
         } else if ( system.equals( CODING_SECTION_ID ) ) {
            id = coding.getCode();
         }
      }
      final String tagText = codeableConcept.getText();
      final Segment segment = new Segment( jCas );
      addTextSpan( segment, resource, LOGGER );
      segment.setPreferredText( preferredText );
      segment.setTagText( tagText );
      if ( id.isEmpty() ) {
         segment.setId( "SIMPLE_SEGMENT" );
      } else {
         segment.setId( id );
      }
      return segment;
   }

}
