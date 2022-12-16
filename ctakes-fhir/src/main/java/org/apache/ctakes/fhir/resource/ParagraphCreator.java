package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/12/2018
 */
final public class ParagraphCreator implements FhirBasicCreator<Paragraph> {

   static public final String ID_NAME_PARAGRAPH = "DocumentParagraph";

   /**
    * {@inheritDoc}
    */
   @Override
   public String getIdName() {
      return ID_NAME_PARAGRAPH;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Basic createResource( final JCas jCas, final Paragraph paragraph,
                                final FhirPractitioner practitioner, final FhirNoteSpecs noteSpecs ) {
      return createAnnotationBasic( jCas, paragraph, practitioner );
   }

}
