package org.apache.ctakes.rest.service.response;

import org.apache.ctakes.fhir.cc.FhirJsonWriter;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

/**
 * Formats desired information in jCas to a string.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/1/2020
 */
public interface ResponseFormatter {

   /**
    * @param jCas ye olde ...
    * @return a string containing information from jcas.
    * @throws AnalysisEngineProcessException hopefully not.
    */
   default String getResultText( final JCas jCas ) throws AnalysisEngineProcessException {
      return FhirJsonWriter.createJson( jCas );
   }

}
