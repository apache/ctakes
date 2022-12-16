package org.apache.ctakes.fhir.resource;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
public interface FhirResourceParser<T extends TOP, R extends Resource> {

   T parseResource( final JCas jCas, final R resource );

}
