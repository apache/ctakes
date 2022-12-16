package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.Collection;

/**
 *
 */
public class ApacheConParagraphWriter extends AbstractJCasFileWriter {

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {
      File file = new File( outputDir, fileName + "_Paragraphs.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         Collection<Paragraph> paragraphs = JCasUtil.select( jCas, Paragraph.class );
         for ( Paragraph paragraph : paragraphs ) {
            writer.write( paragraph.getCoveredText() + "\n" );
            writer.write( "-------------------------------------------------------------------" );
         }
      }
   }

}
