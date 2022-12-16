package org.apache.ctakes.examples.ae;

import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationBuilder;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;


/**
 * Finds clinical procedures in text using regular expressions.
 * Accepts parameters for the procedure's regular expression and the procedure's CUI.
 */
public class ApacheConDemoEngine extends JCasAnnotator_ImplBase {

   @ConfigurationParameter(
         name = "REGEX",
         description = "Regular expression to use for matching clinical procedures.",
         defaultValue = "biopsy"
   )
   private String _regex;

   @ConfigurationParameter(
         name = "REGEX_CUI",
         description = "CUI for matched clinical procedure expressions.",
         defaultValue = "AC123"
   )
   private String _regexCui;

   /**
    * Finds Procedures using a regular expression and creates Identified Annotations.
    */
   @Override
   public void process( JCas jCas ) throws AnalysisEngineProcessException {
      IdentifiedAnnotationBuilder builder = new IdentifiedAnnotationBuilder().concept( jCas, _regexCui,
                                                                                       SemanticTui.T060 );
      try ( RegexSpanFinder finder = new RegexSpanFinder( _regex ) ) {
         finder.findSpans( jCas.getDocumentText() )
               .forEach( span ->
                               builder
                                     .span( span )
                                     .build( jCas ) );
      } catch ( IllegalArgumentException iaE ) {
         throw new AnalysisEngineProcessException( iaE );
      }
   }

}
