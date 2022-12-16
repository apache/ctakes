package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;

/**
 * Writes to file with a list of document metadata
 * and a list of information for clinical events (not anatomical sites).
 */
public class ApacheConDemoWriter extends AbstractJCasFileWriter {

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {
      File file = new File( outputDir, documentId + "_ApacheCon.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         NoteSpecs noteSpecs = new NoteSpecs( jCas );
         writer.write(
               "Document ID:   " + documentId + "\n" +
               "Document Type: " + noteSpecs.getDocumentType() + "\n" +
               "Patient ID:  " + noteSpecs.getPatientName() + "\n" +
               "---------------------------------------------\n" );
         for ( EventMention event : JCasUtil.select( jCas, EventMention.class ) ) {
            writer.write(
                  IdentifiedAnnotationUtil.getBestSemanticGroup( event )
                                          .getName() + " " +
                  String.join( ", ", IdentifiedAnnotationUtil.getCuis( event ) ) + " " +
                  "\"" + IdentifiedAnnotationUtil.getText( event ) + "\"\n" );
         }
      }
   }

}
