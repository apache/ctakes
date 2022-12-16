package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.Collection;

/**
 *
 */
public class ApacheConSectionWriter extends AbstractJCasFileWriter {

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {
      File file = new File( outputDir, fileName + "_Sections.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         Collection<Segment> sections = JCasUtil.select( jCas, Segment.class );
         for ( Segment section : sections ) {
            writer.write( "Section ID : " + section.getId() + "\t" + "Document Text : " + section.getTagText() + "\n" );
            writer.write( section.getCoveredText() + "\n" );
         }
      }
   }

}
