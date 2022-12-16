package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.Collection;

/**
 *
 */
public class ApacheConSentenceWriter extends AbstractJCasFileWriter {

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {
      File file = new File( outputDir, fileName + "_Sentences.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         Collection<Sentence> sentences = JCasUtil.select( jCas, Sentence.class );
         for ( Sentence sentence : sentences ) {
            writer.write( sentence.getCoveredText() + "\n" );
            writer.write( "-------------------------------------------------------------------\n" );
         }
      }
   }

}
