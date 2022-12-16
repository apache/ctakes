package org.apache.ctakes.rest.service.response;


import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/1/2020
 */
final public class CuiListFormatter implements ResponseFormatter {

   /**
    * Returns a list of Cuis.
    * {@inheritDoc}
    */
   @Override
   public String getResultText( final JCas jCas ) throws AnalysisEngineProcessException {
      return OntologyConceptUtil.getCuiCounts( jCas )
                                .entrySet()
                                .stream()
                                .map( e -> e.getKey() + " : " + e.getValue() )
                                .sorted()
                                .collect( Collectors.joining( "\n" ) );
   }

}
