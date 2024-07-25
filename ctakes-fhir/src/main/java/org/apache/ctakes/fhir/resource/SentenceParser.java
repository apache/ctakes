package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
final public class SentenceParser implements FhirBasicParser<Sentence> {

   static private final Logger LOGGER = LogManager.getLogger( "SentenceBasicParser" );

   public Sentence parseResource( final JCas jCas, final Basic resource ) {
      final Sentence sentence = new Sentence( jCas );
      addTextSpan( sentence, resource, LOGGER );
      return sentence;
   }

}
