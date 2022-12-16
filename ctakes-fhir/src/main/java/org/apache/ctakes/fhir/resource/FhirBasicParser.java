package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.fhir.element.FhirElementParser;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.Basic;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
public interface FhirBasicParser<T extends Annotation> extends FhirResourceParser<T, Basic> {

   default void addTextSpan( final T type, final Basic resource, final Logger logger ) {
      final Pair<Integer> textSpan = FhirElementParser.getTextSpan( resource.getExtension() );
      if ( textSpan == null ) {
         logger.error( "Could not parse text span for basic " + resource.getId() );
         return;
      }
      type.setBegin( textSpan.getValue1() );
      type.setEnd( textSpan.getValue2() );
   }

}
