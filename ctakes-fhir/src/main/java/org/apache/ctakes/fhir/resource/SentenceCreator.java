package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/12/2018
 */
final public class SentenceCreator implements FhirBasicCreator<Sentence> {

   static public final String SENTENCE_EXT = "document-sentence";
   static public final String ID_NAME_SENTENCE = "DocumentSentence";

   /**
    * {@inheritDoc}
    */
   @Override
   public String getIdName() {
      return ID_NAME_SENTENCE;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Basic createResource( final JCas jCas, final Sentence sentence,
                                final FhirPractitioner practitioner, final FhirNoteSpecs noteSpecs ) {
      return createAnnotationBasic( jCas, sentence, practitioner );
   }

}
