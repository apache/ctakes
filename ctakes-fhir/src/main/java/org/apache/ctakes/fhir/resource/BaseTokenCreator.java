package org.apache.ctakes.fhir.resource;


import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class BaseTokenCreator implements FhirBasicCreator<BaseToken> {

   static public final String ID_NAME_BASE_TOKEN = "BaseToken";

   /**
    * {@inheritDoc}
    */
   @Override
   public String getIdName() {
      return ID_NAME_BASE_TOKEN;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Basic createResource( final JCas jCas, final BaseToken baseToken,
                                final FhirPractitioner practitioner, final FhirNoteSpecs noteSpecs ) {
      final Basic basic = createAnnotationBasic( jCas, baseToken, practitioner );

      // The 'code' is the part of speech.
      basic.setCode( FhirElementFactory.createPosCode( baseToken ) );

      return basic;
   }

}
