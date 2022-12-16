package org.apache.ctakes.dictionary.cased.util.bsv;


import org.apache.ctakes.core.resource.FileLocator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class BsvFileParser {

   private BsvFileParser() {
   }


   static public Collection<String[]> parseBsvFile( final String bsvFilePath ) throws IOException {
      return parseBsvFile( bsvFilePath, Integer.MAX_VALUE );
   }


   static public Collection<String[]> parseBsvFile( final String bsvFilePath,
                                                    final int columnCount ) throws IOException {
      return parseBsvFile( bsvFilePath, new StringArrayCreator( columnCount ) );
   }


   static public <T> Collection<T> parseBsvFile( final String bsvFilePath,
                                                 final BsvObjectCreator<T> objectCreator ) throws IOException {
      final Collection<T> bsvObjects = new ArrayList<>();
      final BufferedReader reader
            = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( bsvFilePath ) ) );
      String line = reader.readLine();
      while ( line != null ) {
         final T bsvObject = objectCreator.createBsvObject( line );
         if ( bsvObject != null ) {
            bsvObjects.add( bsvObject );
         }
         line = reader.readLine();
      }
      return bsvObjects;
   }


}
