package org.apache.ctakes.gui.dictionary.umls;


import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.gui.dictionary.util.FileUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.gui.dictionary.umls.MrstyIndex.CUI;
import static org.apache.ctakes.gui.dictionary.umls.MrstyIndex.TUI;


/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/17/14
 */
final public class MrstyParser {

   static private final Logger LOGGER = Logger.getLogger( "MrStyParser" );

   static private final String MRSTY_SUB_PATH = "/META/MRSTY.RRF";

   private MrstyParser() {
   }

   static public Map<Long, Concept> createConceptsForTuis( final String umlsPath,
                                                           final Collection<SemanticTui> wantedTuis ) {
      final String mrstyPath = umlsPath + MRSTY_SUB_PATH;
      LOGGER.info( "Compiling list of Cuis with wanted Tuis using " + mrstyPath );
      long lineCount = 0;
      final Map<Long, Concept> wantedConcepts = new HashMap<>();
      final Collection<SemanticTui> usedTuis = EnumSet.noneOf( SemanticTui.class );
      final Map<SemanticTui, Long> tuiCodeCount = new EnumMap<>( SemanticTui.class );
      for ( SemanticTui tui : wantedTuis ) {
         tuiCodeCount.put( tui, 0L );
      }
      try ( final BufferedReader reader = FileUtil.createReader( mrstyPath ) ) {
         List<String> tokens = FileUtil.readBsvTokens( reader, mrstyPath );
         while ( tokens != null ) {
            lineCount++;
            if ( tokens.size() > TUI._index ) {
               final SemanticTui tuiEnum = SemanticTui.valueOf( tokens.get( TUI._index ) );
               if ( !wantedTuis.contains( tuiEnum ) ) {
                  tokens = FileUtil.readBsvTokens( reader, mrstyPath );
                  continue;
               }
               final Long cuiCode = CuiCodeUtil.getInstance().getCuiCode( tokens.get( CUI._index ) );
               Concept concept = wantedConcepts.get( cuiCode );
               if ( concept == null ) {
                  concept = new Concept();
                  wantedConcepts.put( cuiCode, concept );
               }
               concept.addTui( tuiEnum );
               usedTuis.add( tuiEnum );
               final long count = tuiCodeCount.get( tuiEnum );
               tuiCodeCount.put( tuiEnum, (count + 1) );
            }
            if ( lineCount % 100000 == 0 ) {
//               LOGGER.info( "File Line " + lineCount + "\t Valid Cuis " + wantedConcepts.size() );
               final String counts = tuiCodeCount.entrySet().stream().map( e -> e.getKey().name() + " " + e.getValue() )
                     .collect( Collectors.joining( ", " ) );
               LOGGER.info( "File Line " + lineCount + "\t Cuis: " + counts );
            }
            tokens = FileUtil.readBsvTokens( reader, mrstyPath );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
//      LOGGER.info( "File Lines " + lineCount + "\t Valid Cuis " + wantedConcepts.size() + "\t for wanted Tuis" );
      final String counts = tuiCodeCount.entrySet().stream().map( e -> e.getKey().name() + " " + e.getValue() )
            .collect( Collectors.joining( ", " ) );
      LOGGER.info( "File Lines " + lineCount + "\t Cuis: " + counts );
      if ( usedTuis.size() != wantedTuis.size() ) {
         wantedTuis.removeAll( usedTuis );
         for ( SemanticTui missingTui : wantedTuis ) {
            LOGGER.warn( "Could not find Cuis for Tui " + missingTui + " " + missingTui.getSemanticType() );
         }
      }
      return wantedConcepts;
   }

}
