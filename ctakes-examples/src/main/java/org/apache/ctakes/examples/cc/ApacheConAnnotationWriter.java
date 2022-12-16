package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.*;

/**
 *
 */
public class ApacheConAnnotationWriter extends AbstractJCasFileWriter {

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {
      File file = new File( outputDir, fileName + "_Annotations.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotations
               = JCasUtil.indexCovered( jCas, Sentence.class, IdentifiedAnnotation.class );
         List<Sentence> sentences = new ArrayList<>( sentenceAnnotations.keySet() );
         sentences.sort( Comparator.comparingInt( Sentence::getBegin ) );
         for ( Sentence sentence : sentences ) {
            for ( IdentifiedAnnotation annotation : sentenceAnnotations.get( sentence ) ) {
               if ( annotation instanceof EventMention || annotation instanceof AnatomicalSiteMention ) {
                  writer.write( " \"" + IdentifiedAnnotationUtil.getText( annotation ) + "\"   " );
               }
            }
            writer.write( "\n" );
         }
      }
   }

}
