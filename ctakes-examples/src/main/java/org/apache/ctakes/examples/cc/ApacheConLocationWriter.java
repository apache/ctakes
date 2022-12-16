package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.Collection;

/**
 *
 */
public class ApacheConLocationWriter extends AbstractJCasFileWriter {

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( JCas jCas,
                          String outputDir,
                          String documentId,
                          String fileName ) throws IOException {
      File file = new File( outputDir, fileName + "_Locations.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         Collection<LocationOfTextRelation> locations = JCasUtil.select( jCas, LocationOfTextRelation.class );
         for ( LocationOfTextRelation location : locations ) {
            writer.write( location.getArg1()
                                  .getArgument()
                                  .getCoveredText()
                          + "  = located at =  " + location.getArg2()
                                                           .getArgument()
                                                           .getCoveredText() + "\n" );
         }
      }
   }

}
