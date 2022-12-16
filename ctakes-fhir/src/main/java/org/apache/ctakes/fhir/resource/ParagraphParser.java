package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
final public class ParagraphParser implements FhirBasicParser<Paragraph> {

   static private final Logger LOGGER = Logger.getLogger( "ParagraphBasicParser" );

   public Paragraph parseResource( final JCas jCas, final Basic resource ) {
      final Paragraph paragraph = new Paragraph( jCas );
      addTextSpan( paragraph, resource, LOGGER );
      return paragraph;
   }

}
