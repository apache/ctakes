package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class ApacheConCorefWriter extends AbstractJCasFileWriter {

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {
      File file = new File( outputDir, fileName + "_Corefs.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         final Map<IdentifiedAnnotation, Collection<Integer>> markableCorefs
               = EssentialAnnotationUtil.createMarkableCorefs( jCas );
         final Collection<IdentifiedAnnotation> requiredAnnotations
               = EssentialAnnotationUtil.getRequiredAnnotations( jCas, markableCorefs );

         final Map<Integer, Collection<IdentifiedAnnotation>> chains = new HashMap<>();
         for ( IdentifiedAnnotation annotation : requiredAnnotations ) {
            final Collection<Integer> corefs = markableCorefs.get( annotation );
            if ( corefs != null ) {
               for ( Integer coref : corefs ) {
                  chains.computeIfAbsent( coref, c -> new HashSet<>() )
                        .add( annotation );
               }
            }
         }
         for ( Map.Entry<Integer, Collection<IdentifiedAnnotation>> chain : chains.entrySet() ) {
            writer.write( "Coreference Chain : " + chain.getKey() + "\n" );
            String links = chain.getValue()
                                .stream()
                                .map( l -> "   " + l.getCoveredText() )
                                .collect( Collectors.joining( "\n" ) );
            writer.write( links + "\n" );
         }
      }
   }

}
