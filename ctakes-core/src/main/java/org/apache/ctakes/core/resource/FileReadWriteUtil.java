package org.apache.ctakes.core.resource;


import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/23/2017
 */
final public class FileReadWriteUtil {

   private FileReadWriteUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "FileReadWriteUtil" );

   /**
    * Reading text from a file or resource is done everywhere, but a common implementation is missing from ctakes.
    *
    * @param path for file or resource.
    * @return a single string of text from the file.
    * @throws IOException if the resource cannot be read.
    */
   static public String readText( final String path ) throws IOException {
      final InputStream stream = FileLocator.getAsStream( path );
      final StringBuilder sb = new StringBuilder();
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) ) ) {
         String line;
         while ( (line = reader.readLine()) != null ) {
            sb.append( line ).append( "\n" );
         }
      }
      return sb.toString();
   }

   /**
    * Reading text from a file or resource is done everywhere, but a common implementation is missing from ctakes.
    *
    * @param path for file or resource.
    * @return a list containing each line of text in the file.
    * @throws IOException if the resource cannot be read.
    */
   static public List<String> readLines( final String path ) throws IOException {
      final InputStream stream = FileLocator.getAsStream( path );
      final List<String> lines = new ArrayList<>();
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) ) ) {
         String line;
         while ( (line = reader.readLine()) != null ) {
            lines.add( line );
         }
      }
      return lines;
   }

   /**
    * Writing text to a file is done everywhere, but a common implementation is missing from ctakes.
    *
    * @param text     to be written.
    * @param filepath for output file.
    * @throws IOException if the file cannot be written.
    */
   static public void writeText( final String text, final String filepath ) throws IOException {
      final Path path = Paths.get( filepath );
      try ( Writer writer = Files.newBufferedWriter( path ) ) {
         writer.write( text );
      }
   }


}
