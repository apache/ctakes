package org.apache.ctakes.fhir.resource;

import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/9/2018
 */
public class CompositionParser {

   static private final Logger LOGGER = Logger.getLogger( "CompositionParser" );

   public void parseResource( final JCas jCas, final Basic resource ) {

//      composition.addAuthor( practitioner.getPractitionerReference() );
//      composition.setId( FhirElementFactory.createId( jCas, ID_NAME_COMPOSITION, noteSpecs.getNoteTime() ) );
//      composition.setStatus( Composition.CompositionStatus.FINAL );
//      composition.setType( getCompositionType() );
//      composition.setLanguage( "English" );
//      composition.setSubject( noteSpecs.getSubjectReference( FhirNoteSpecs.SUBJECT_PATIENT ) );

      // The following claim to have a 0 .. 1 cardinality, but the doc also states that they SHALL be listed.
//      composition.setEncounter( noteSpecs.getEncounterReference() );
//      composition.setCustodian( practitioner.getOrganizationReference() );
//      composition.addEvent( noteSpecs.getEvent() );
//      composition.addAttester( practitioner.createAttester( noteSpecs ) );

//      composition.setText( FhirElementFactory.createNarrative( jCas.getDocumentText() ) );
      final Narrative narrative = resource.getText();
      final XhtmlNode htmlNode = narrative.getDiv();
      final String docText = htmlNode.allText();
      jCas.setDocumentText( docText );
   }

}
