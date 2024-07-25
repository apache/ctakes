package org.apache.ctakes.core.cc;


import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/22/2018
 */
public class CuiLookupLister extends AbstractJCasFileWriter {

   static private final Logger LOGGER = LogManager.getLogger( "CuiLookupLister" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      try ( Writer writer = new BufferedWriter( new FileWriter( outputDir + "/" + documentId + "_cui.txt" ) ) ) {
         final Map<Sentence, List<IdentifiedAnnotation>> sentenceCodes
               = JCasUtil.indexCovered( jCas, Sentence.class, IdentifiedAnnotation.class );
         for ( Map.Entry<Sentence, List<IdentifiedAnnotation>> entry : sentenceCodes.entrySet() ) {
            final int sentenceBegin = entry.getKey()
                                           .getBegin();
            final int sentenceEnd = entry.getKey()
                                         .getEnd();
            for ( IdentifiedAnnotation annotation : entry.getValue() ) {
               if ( annotation.getBegin() >= sentenceBegin && annotation.getEnd() <= sentenceEnd ) {
                  for ( UmlsConcept umls : OntologyConceptUtil.getUmlsConcepts( annotation ) ) {
                     writer.write( umls.getCui() + '|' + umls.getTui() + '|' + annotation.getCoveredText() + '\n' );
                  }
               }
            }
         }
      }
   }

}
