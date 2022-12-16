package org.apache.ctakes.fhir.element;


import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.util.Collection;
import java.util.Date;

import static org.apache.ctakes.fhir.resource.SectionCreator.SECTION_EXT;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/22/2017
 */
final public class FhirElementFactory {

   static private final Logger LOGGER = Logger.getLogger( "FhirElementFactory" );

   static public final String CTAKES_FHIR_URL = "http://org.apache.ctakes/fhir/";
   static public final String SPAN_BEGIN_EXT = "span-begin";
   static public final String SPAN_END_EXT = "span-end";
   static public final String DOCTIMEREL_EXT = "doc-time-rel";
   static public final String RELATION_EXT_PREFIX = "relation-";
   static public final String COREF_INDEX_EXT = "coreference-index";

   static public final String CODING_TYPE_SYSTEM = "type-system";
   static public final String CODING_CUI = "cui";
   static public final String CODING_TUI = "tui";
   static public final String CODING_SEMANTIC = "semantic-group";
   static public final String CODING_PART_OF_SPEECH = "part-of-speech";

   static public final char DIVIDER_CHAR = '-';


   private FhirElementFactory() {
   }

   /**
    * @param url       full url.
    * @param extension url extension.
    * @return true if the url is "http://org.apache.ctakes/fhir/" + extension.
    */
   static public boolean isCtakesFhirExt( final String url, final String extension ) {
      return url.equals( CTAKES_FHIR_URL + extension );
   }

   /**
    * @param extension url extension.
    * @return "http://org.apache.ctakes/fhir/" + extension.
    */
   static public String createCtakesFhirUrl( final String extension ) {
      return CTAKES_FHIR_URL + extension;
   }

   /**
    * @param annotation -
    * @return annotation type, all ontology codes and covered text in a fhir codeable concept.
    */
   static public CodeableConcept createPrimaryCode( final IdentifiedAnnotation annotation ) {
      final CodeableConcept codeableConcept = createSimpleCode( annotation );
      codeableConcept.addCoding( new Coding( CODING_SEMANTIC, SemanticGroup.getBestGroup( annotation )
            .getName(), "" ) );
      final Collection<String> cuis = OntologyConceptUtil.getCuis( annotation );
      cuis.forEach( c -> codeableConcept.addCoding( new Coding( CODING_CUI, c, "" ) ) );
      final Collection<String> tuis = OntologyConceptUtil.getTuis( annotation );
      tuis.forEach( t -> codeableConcept.addCoding( new Coding( CODING_TUI, t, "" ) ) );
      for ( OntologyConcept concept : OntologyConceptUtil.getOntologyConcepts( annotation ) ) {
         final String scheme = concept.getCodingScheme();
         final String code = concept.getCode();
         String preferredText = null;
         if ( concept instanceof UmlsConcept ) {
            preferredText = ((UmlsConcept) concept).getPreferredText();
         }
         codeableConcept.addCoding( new Coding( scheme, code, preferredText ) );
      }
      return codeableConcept;
   }

   /**
    * @param baseToken -
    * @return annotation type, part of speech and covered text in a fhir codeable concept.
    */
   static public CodeableConcept createPosCode( final BaseToken baseToken ) {
      final CodeableConcept codeableConcept = createSimpleCode( baseToken );
      if ( baseToken instanceof WordToken ) {
         // We are only interested in tokens that are -words-
         final String pos = baseToken.getPartOfSpeech();
         codeableConcept.addCoding( new Coding( CODING_PART_OF_SPEECH, pos, "" ) );
      }
      return codeableConcept;
   }

   /**
    * @param annotation -
    * @return annotation type and covered text in a fhir codeable concept.
    */
   static public CodeableConcept createSimpleCode( final org.apache.uima.jcas.tcas.Annotation annotation ) {
      final String type = annotation.getType()
//            .getShortName();
                                    .getName();
      return createSimpleCode( CODING_TYPE_SYSTEM, type, null, annotation.getCoveredText() );
   }

   /**
    * @param system  -
    * @param code    -
    * @param display -
    * @param text    -
    * @return type, ontology code and covered text in a fhir codeable concept.
    */
   static public CodeableConcept createSimpleCode( final String system, final String code, final String display, final String text ) {
      final CodeableConcept codeableConcept = new CodeableConcept();
      codeableConcept.addCoding( new Coding( system, code, display ) );
      codeableConcept.setText( text );
      return codeableConcept;
   }

   /**
    * @param codeableConcept -
    * @param system          -
    * @param code            -
    * @param display         -
    * @return the given fhir codeable concept with the given code added as a fhir Coding.
    */
   static public CodeableConcept addCoding( final CodeableConcept codeableConcept, final String system, final String code, final String display ) {
      codeableConcept.addCoding( new Coding( system, code, display ) );
      return codeableConcept;
   }

   /**
    * @param jCas ye olde ...
    * @param name fhir object type / name.
    * @param code some numerical encoding to use in a fhir id.
    * @return fhir id containing name and code.
    */
   static public String createId( final JCas jCas, final String name, final int code ) {
      return createId( jCas, name, "" + Math.abs( code ) );
   }

   /**
    * @param jCas ye olde ...
    * @param name fhir object type / name.
    * @param code some text encoding to use in a fhir id.
    * @return fhir id containing name and code.
    */
   static public String createId( final JCas jCas, final String name, final String code ) {
      return DocIdUtil.getDocumentID( jCas ) + DIVIDER_CHAR + name + DIVIDER_CHAR + code;
   }

//   Identifiers are 0 .. 1 , so unnecessary
//   static public Identifier createIdentifier( final JCas jCas,
//                                              final org.apache.uima.jcas.tcas.Annotation annotation,
//                                              final Period period ) {
//      return createIdentifier( jCas, annotation.getType().getShortName(), annotation.hashCode(), period );
//   }
//
//   static public Identifier createIdentifier( final JCas jCas,
//                                              final String name,
//                                              final Period period ) {
//      return createIdentifier( jCas, name, name.hashCode(), period );
//   }
//
//   static public Identifier createIdentifier( final JCas jCas,
//                                              final String name,
//                                              final int code,
//                                              final Period period ) {
//      return createIdentifier( jCas, name, ""+Math.abs( code ), period );
//   }
//
//   static public Identifier createIdentifier( final JCas jCas,
//                                              final String name,
//                                              final String code,
//                                              final Period period ) {
//      final String id = DocumentIDAnnotationUtil.getDocumentID( jCas )
//            + "_" + name
//            + "_" + code;
//      final Identifier identifier = new Identifier();
//      identifier.setSystem( CTAKES_FHIR_URL + IDENTIFIER_EXT );
//      identifier.setUse( Identifier.IdentifierUse.SECONDARY );
//      identifier.setValue( id );
//      identifier.setPeriod( period );
//      return identifier;
//   }

   /**
    * @param number some integer
    * @return given integer wrapped in a fhir SimpleQuantity object
    */
   static public SimpleQuantity createQuantity( final int number ) {
      final SimpleQuantity quantity = new SimpleQuantity();
      quantity.setValue( number );
      return quantity;
   }

   /**
    * @param startMillis -
    * @param endMillis   -
    * @return fhir period spanning the given milliseconds.
    */
   static public Period createPeriod( final long startMillis, final long endMillis ) {
      final Period period = new Period();
      period.setStart( new Date( startMillis ), TemporalPrecisionEnum.MILLI );
      period.setEnd( new Date( endMillis ), TemporalPrecisionEnum.MILLI );
      return period;
   }

   /**
    * @param text -
    * @return fhir narrative object containing the given text wrapped in an html "div".
    */
   static public Narrative createNarrative( final String text ) {
      final Narrative narrative = new Narrative();
      narrative.setStatus( Narrative.NarrativeStatus.GENERATED );
      final XhtmlNode htmlNode = new XhtmlNode( NodeType.Element, "div" );
      htmlNode.addText( text );
      narrative.setDiv( htmlNode );
      return narrative;
   }

   /**
    * @param annotation ctakes annotation.
    * @return a fhir extension representing the annotation's beginning text span offset.
    */
   static public Extension createSpanBegin( final org.apache.uima.jcas.tcas.Annotation annotation ) {
      final UnsignedIntType begin = new UnsignedIntType( annotation.getBegin() );
      return new Extension( createCtakesFhirUrl( SPAN_BEGIN_EXT ), begin );
   }

   /**
    * @param annotation ctakes annotation.
    * @return a fhir extension representing the annotation's ending text span offset.
    */
   static public Extension createSpanEnd( final org.apache.uima.jcas.tcas.Annotation annotation ) {
      final UnsignedIntType end = new UnsignedIntType( annotation.getEnd() );
      return new Extension( createCtakesFhirUrl( SPAN_END_EXT ), end );
   }

   /**
    * @param eventMention ctakes event.
    * @return a fhir extension representing the time relative to document creation.
    */
   static public Extension createDocTimeRel( final EventMention eventMention ) {
      final Event event = eventMention.getEvent();
      if ( event == null ) {
         return null;
      }
      final EventProperties eventProperties = event.getProperties();
      if ( eventProperties == null ) {
         return null;
      }
      final String dtr = eventProperties.getDocTimeRel();
      if ( dtr == null || dtr.isEmpty() ) {
         return null;
      }
      return new Extension( createCtakesFhirUrl( DOCTIMEREL_EXT ), new StringType( dtr ) );
   }

   /**
    * @param name   relation name.
    * @param target fhir object for the relation target.
    * @return a fhir extension with the relation name and fhir reference to the target.
    */
   static public Extension createRelation( final String name, final Basic target ) {
      return new Extension( createCtakesFhirUrl( RELATION_EXT_PREFIX + name ), new Reference( target ) );
   }

   /**
    * @param index coreference chain index.
    * @return a fhir extension indicating the coreference chain to which an object belongs.
    */
   static public Extension createCorefIndex( final int index ) {
      return new Extension( createCtakesFhirUrl( COREF_INDEX_EXT ), new UnsignedIntType( index ) );
   }

   /**
    * @param sectionRef reference to fhir object with section.
    * @return a fhir extension indicating to which section an object belongs.
    */
   static public Extension createSectionExtension( final Reference sectionRef ) {
      return new Extension( createCtakesFhirUrl( SECTION_EXT ), sectionRef );
   }

   /**
    * @param name some property name.
    * @return an extension indicating that an object property is true.
    */
   static public Extension createTrueExtension( final String name ) {
      return new Extension( createCtakesFhirUrl( name ), new BooleanType( true ) );
   }

}
