package org.apache.ctakes.examples.cc;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {8/17/2021}
 */
public final class TokenPrinter extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "TokenPrinter" );

   /**
    * Entry point for processing.
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final Collection<BaseToken> tokens = JCasUtil.select( jCas, BaseToken.class );
      for ( BaseToken token : tokens ) {
         LOGGER.info( token.getClass()
                           .getSimpleName() + " " + token.getCoveredText() );
      }
   }

}
