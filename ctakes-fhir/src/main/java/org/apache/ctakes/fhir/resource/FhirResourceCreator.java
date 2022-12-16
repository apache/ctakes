package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
public interface FhirResourceCreator<T extends TOP, R extends Resource> {

   R createResource( final JCas jCas, final T top, final FhirPractitioner practitioner, final FhirNoteSpecs noteSpecs );

}
