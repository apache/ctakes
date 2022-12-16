package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 *
 */
public class ApacheConTlinkWriter extends AbstractJCasFileWriter {

   /*
    * Given a binary text relation, returns a string with information about that relation.
    */
   static private final Function<BinaryTextRelation, String> toRelInfo = r ->
         r.getArg1()
          .getArgument()
          .getCoveredText()
         + "  = " + r.getCategory()
         + " =  " + r.getArg2()
                     .getArgument()
                     .getCoveredText() +
         "\n";

   /*
    * Given a binary text relation, returns the first character offset of the relation's first argument.
    */
   static private final ToIntFunction<BinaryTextRelation> getRelBegin = r ->
         r.getArg1()
          .getArgument()
          .getBegin();

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {

      final StringBuilder sb = new StringBuilder();
      JCasUtil.select( jCas, TemporalTextRelation.class )
              .stream()
              .sorted( Comparator.comparingInt( getRelBegin ) )
              .map( toRelInfo )
              .forEach( sb::append );

      File file = new File( outputDir, fileName + "_Tlinks.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( sb.toString() );
      }

   }


}
