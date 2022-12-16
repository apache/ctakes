package org.apache.ctakes.examples.cr;


import org.apache.ctakes.core.cr.AbstractFileTreeReader;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads a tree of files, building documents from lines starting with "Text:"
 */
public class ApacheConDemoReader extends AbstractFileTreeReader {

   /**
    * Places Document Text (and other information) in JCas.
    *
    * @param jCas unpopulated jcas data container.
    * @param file file to be read.
    * @throws IOException should anything bad happen.
    */
   protected void readFile( JCas jCas, File file ) throws IOException {
      // Read the file, building a document only using lines preceded by "Text:"
      String documentText;
      try ( Stream<String> textStream = Files.lines( file.toPath() ) ) {
         documentText = textStream
               .filter( line -> line.startsWith( "Text:" ) )
               .map( line -> line.substring( 5 )
                                 .trim() )
               .collect( Collectors.joining( "\n" ) );
      }
      // Based upon the file, the provided JCasBuilder attempts to determine metadata
      // document ID, document type, patient ID, and creation time.
      // Here we set the document text on the builder and populate the the jcas.
      getJCasBuilder( file )
            .setDocText( documentText )
            .populate( jCas );
   }

}
