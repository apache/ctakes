package org.apache.ctakes.fhir.resource;


import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.Basic;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class AnnotationCreator implements FhirBasicCreator<Annotation> {

   static public final String ID_NAME_ANNOTATION = "Annotation";

   /**
    * {@inheritDoc}
    */
   @Override
   public String getIdName() {
      return ID_NAME_ANNOTATION;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Basic createResource( final JCas jCas, final Annotation annotation,
                                final FhirPractitioner practitioner, final FhirNoteSpecs noteSpecs ) {
      return createAnnotationBasic( jCas, annotation, practitioner );
   }

}
