package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.Basic;

import java.util.Date;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/12/2018
 */
public interface FhirBasicCreator<T extends Annotation> extends FhirResourceCreator<T, Basic> {

   String getIdName();

   default Basic createAnnotationBasic( final JCas jCas,
                                        final T annotation,
                                        final FhirPractitioner practitioner ) {
      final Basic basic = new Basic();
      // The 'id' is name of the Resource type (class).  e.g. DiseaseDisorderMention
      basic.setId( FhirElementFactory.createId( jCas, getIdName(), annotation.hashCode() ) );
      // The 'code' is the full ontology concept array: cuis, snomeds, urls, preferred text, PLUS covered text.
      basic.setCode( FhirElementFactory.createSimpleCode( annotation ) );
      // Add Creation Date as now.
      basic.setCreated( new Date() );
      // Add Author (ctakes).
      basic.setAuthor( practitioner.getPractitionerReference() );
      // Add text span as an extension.
      basic.addExtension( FhirElementFactory.createSpanBegin( annotation ) );
      basic.addExtension( FhirElementFactory.createSpanEnd( annotation ) );
      return basic;
   }

}
