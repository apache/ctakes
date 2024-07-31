package org.apache.ctakes.gui.dictionary.cased;


import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.gui.dictionary.cased.umls.file.Tty;
import org.apache.ctakes.gui.dictionary.util.HsqlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/28/2020
 */
final public class CasedReadmeWriter {

   static private final Logger LOGGER = LoggerFactory.getLogger( "CasedReadmeWriter" );


   private CasedReadmeWriter() {
   }


   static public boolean writeReadme( final String hsqlPath,
                                      final String dictionaryName,
                                      final String consoPath,
                                      final String styPath,
                                      final String rankPath,
                                      final Collection<SemanticTui> wantedTuis,
                                      final Collection<String> wantedVocabularies,
                                      final Collection<Tty> wantedTermTypes,
                                      final Collection<String> wantedLanguages,
                                      final Collection<String> writtenSchema ) {
      final String url = HsqlUtil.URL_PREFIX + hsqlPath.replace( '\\', '/' ) + "/" + dictionaryName + "/" +
                         dictionaryName;
      final File piperFile = new File( hsqlPath, dictionaryName + ".readme" );
      try ( final Writer writer = new BufferedWriter( new FileWriter( piperFile ) ) ) {
         writer.write( "This readme file contains user-selected settings used for "
                       + "Case-sensitive Dictionary Lookup dictionary " + dictionaryName + ".\n\n" );
         writer.write( "UMLS Source Files ...\n" );
         writer.write( "MRCONSO : " + consoPath + "\n" );
         writer.write( "MRSTY   : " + styPath + "\n" );
         writer.write( "MRRANK  : " + rankPath + "\n" );
         writer.write( "\nSemantic Types ...\n" );
         final Map<SemanticGroup, List<SemanticTui>> tuisMap
               = wantedTuis.stream()
                           .collect( Collectors.groupingBy( SemanticTui::getGroup ) );
         for ( SemanticGroup group : SemanticGroup.values() ) {
            final String tuis = tuisMap.getOrDefault( group, Collections.emptyList() )
                                       .stream()
                                       .map( SemanticTui::getSemanticType )
                                       .sorted()
                                       .collect( Collectors.joining( "\n" ) );
            if ( !tuis.isEmpty() ) {
               writer.write( group.getLongName() + "\n" );
               writer.write( tuis + "\n\n" );
            }
         }
         writer.write( "\nVocabularies ...\n" );
         final String vocabs = wantedVocabularies.stream()
                                                 .sorted()
                                                 .collect( Collectors.joining( "\n" ) );
         writer.write( vocabs + "\n" );
         writer.write( "\nTerm Types ...\n" );
         final String tty = wantedTermTypes.stream()
                                           .sorted()
                                           .map( t -> t.name() + " " + t.getDescription() )
                                           .collect( Collectors.joining( "\n" ) );
         writer.write( tty + "\n" );

         writer.write( "\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }


}
