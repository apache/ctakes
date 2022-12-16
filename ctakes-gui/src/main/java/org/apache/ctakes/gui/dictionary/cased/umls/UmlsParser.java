package org.apache.ctakes.gui.dictionary.cased.umls;


import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.gui.dictionary.cased.Ranks;
import org.apache.ctakes.gui.dictionary.cased.term.CuiTerm;
import org.apache.ctakes.gui.dictionary.cased.umls.file.MrConso;
import org.apache.ctakes.gui.dictionary.cased.umls.file.Tty;
import org.apache.ctakes.gui.dictionary.umls.CuiCodeUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.gui.dictionary.cased.umls.file.MrConso.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/26/2020
 */
public class UmlsParser {

   static private final Logger LOGGER = Logger.getLogger( "UmlsParser" );


   static public Collection<CuiTerm> createCuiTerms( final String consoPath,
                                                     final String styPath,
                                                     final String rankPath,
                                                     final Collection<SemanticTui> wantedTuis,
                                                     final Collection<String> wantedVocabularies,
                                                     final Collection<Tty> wantedTermTypes,
                                                     final Collection<String> wantedLanguages,
                                                     final Collection<String> writtenSchema ) {
//      final Collection<Long> wantedCuis = getWantedCuis( styPath, wantedTuis );
      final Map<Long, Collection<SemanticTui>> wantedCuis = getWantedCuis( styPath, wantedTuis );
      if ( wantedCuis.isEmpty() ) {
         return Collections.emptyList();
      }
      final Map<Long, CuiTerm> cuiTerms
            = buildTerms( consoPath, wantedCuis, wantedVocabularies, wantedTermTypes, wantedLanguages, writtenSchema );
      if ( cuiTerms.isEmpty() ) {
         return Collections.emptyList();
      }
      addTuis( cuiTerms, styPath, wantedTuis );

      populateRanks( rankPath, wantedVocabularies, wantedTermTypes );

      return cuiTerms.values();
   }

//   static private Collection<Long> getWantedCuis( final String styPath,
//                                                 final Collection<SemanticTui> wantedTuis ) {
//      LOGGER.info( "Building valid CUI collection based upon TUIs in " + styPath );
//      final Collection<Long> wantedCuis = new HashSet<>();
//      try ( final BufferedReader reader = new BufferedReader( new FileReader( styPath ) );
//            DotLogger logger = new DotLogger() ) {
//         String line = reader.readLine();
//         while ( line != null ) {
//            final String[] columns = StringUtil.fastSplit( line, '|' );
//            final SemanticTui semanticTui = SemanticTui.getTuiFromCode( columns[ 1 ] );
//            if ( wantedTuis.contains( semanticTui ) ) {
//               wantedCuis.add( CuiCodeUtil.getInstance().getCuiCode( columns[ 0 ] ) );
//            }
//            line = reader.readLine();
//         }
//      } catch ( IOException ioE ) {
//         LOGGER.error( ioE.getMessage() );
//      }
//      return wantedCuis;
//   }

   static private Map<Long, Collection<SemanticTui>> getWantedCuis( final String styPath,
                                                                    final Collection<SemanticTui> wantedTuis ) {
      LOGGER.info( "Building valid CUI collection based upon TUIs in " + styPath );
      final Map<Long, Collection<SemanticTui>> wantedCuis = new HashMap<>();
      try ( final BufferedReader reader = new BufferedReader( new FileReader( styPath ) );
            DotLogger logger = new DotLogger() ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] columns = StringUtil.fastSplit( line, '|' );
            final SemanticTui semanticTui = SemanticTui.getTuiFromCode( columns[ 1 ] );
            if ( wantedTuis.contains( semanticTui ) ) {
               wantedCuis.computeIfAbsent( CuiCodeUtil.getInstance().getCuiCode( columns[ 0 ] ),
                     c -> EnumSet.noneOf( SemanticTui.class ) )
                         .add( semanticTui );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      return wantedCuis;
   }


//   static private Map<Long,CuiTerm> buildTerms( final String consoPath,
//                                               final Collection<Long> wantedCuis,
//                                               final Collection<String> wantedVocabularies,
//                                               final Collection<Tty> wantedTermTypes,
//                                               final Collection<String> wantedLanguages,
//                                                final Collection<String> writtenSchema ) {
//      LOGGER.info( "Creating Terms from " + consoPath );
//      final Collection<String> wantedTextTypes = wantedTermTypes.stream().map( Tty::name ).collect( Collectors.toSet() );
//
//      final Map<Long, CuiTerm> cuiTerms = new HashMap<>();
//
//      try ( final BufferedReader reader = new BufferedReader( new FileReader( consoPath ) );
//            DotLogger logger = new DotLogger() ) {
//         String line = reader.readLine();
//         while ( line != null ) {
//            final String[] columns = StringUtil.fastSplit( line, '|' );
//            if ( !wantedTextTypes.contains( getMrConso( columns, TTY ) )
//                 || !wantedVocabularies.contains( getMrConso( columns, SAB ) )
//                 || !wantedLanguages.contains( getMrConso( columns, LAT ) ) ) {
//               line = reader.readLine();
//               continue;
//            }
//            final String text = getMrConso( columns, STR );
//            if ( hasSpecialChars( text ) ) {
//               line = reader.readLine();
//               continue;
//            }
//            final String cui = getMrConso( columns, CUI );
//            final long cuiCode = CuiCodeUtil.getInstance().getCuiCode( cui );
//            if ( wantedCuis.contains( cuiCode ) ) {
//               final CuiTerm term
//                     = cuiTerms.computeIfAbsent( cuiCode, CuiTerm::new );
//               final String vocabulary = getMrConso( columns, SAB );
//               term.addSynonym( text,
//                     vocabulary,
//                     getMrConso( columns, TS ),
//                     getMrConso( columns, STT ),
//                     getMrConso( columns, TTY ) );
//               if ( writtenSchema.contains( vocabulary ) ) {
//                  term.addSchemaCode( vocabulary, getMrConso( columns, CODE ) );
//               }
//            }
//            line = reader.readLine();
//         }
//      } catch ( IOException ioE ) {
//         LOGGER.info( ioE.getMessage() );
//      }
//      LOGGER.info( "Terms: " + cuiTerms.size() );
//      return cuiTerms;
//   }

   static private Map<Long, CuiTerm> buildTerms( final String consoPath,
                                                 final Map<Long, Collection<SemanticTui>> wantedCuis,
                                                 final Collection<String> wantedVocabularies,
                                                 final Collection<Tty> wantedTermTypes,
                                                 final Collection<String> wantedLanguages,
                                                 final Collection<String> writtenSchema ) {
      LOGGER.info( "Creating Terms from " + consoPath );
      final Collection<String> wantedTextTypes = wantedTermTypes.stream()
                                                                .map( Tty::name )
                                                                .collect( Collectors.toSet() );

      final Map<Long, CuiTerm> cuiTerms = new HashMap<>();

      try ( final BufferedReader reader = new BufferedReader( new FileReader( consoPath ) );
            DotLogger logger = new DotLogger() ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] columns = StringUtil.fastSplit( line, '|' );
            if ( !wantedTextTypes.contains( getMrConso( columns, TTY ) )
                 || !wantedVocabularies.contains( getMrConso( columns, SAB ) )
                 || !wantedLanguages.contains( getMrConso( columns, LAT ) ) ) {
               line = reader.readLine();
               continue;
            }
            final String text = getMrConso( columns, STR );
            if ( hasSpecialChars( text ) ) {
               line = reader.readLine();
               continue;
            }
            final String cui = getMrConso( columns, CUI );
            final long cuiCode = CuiCodeUtil.getInstance().getCuiCode( cui );
            final Collection<SemanticTui> tuis = wantedCuis.get( cuiCode );
            if ( tuis != null ) {
               final CuiTerm term
                     = cuiTerms.computeIfAbsent( cuiCode, CuiTerm::new );
               final String vocabulary = getMrConso( columns, SAB );
               term.addSynonym( text,
                     vocabulary,
                     tuis,
                     getMrConso( columns, TS ),
                     getMrConso( columns, STT ),
                     getMrConso( columns, TTY ) );
               if ( writtenSchema.contains( vocabulary ) ) {
                  term.addSchemaCode( vocabulary, getMrConso( columns, CODE ) );
               }
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.info( ioE.getMessage() );
      }
      LOGGER.info( "Terms: " + cuiTerms.size() );
      return cuiTerms;
   }


   static private String getMrConso( final String[] columns,
                                     final MrConso conso ) {
      return columns[ conso.ordinal() ];
   }

   static private boolean hasSpecialChars( final String text ) {
      // strips off all non-ASCII characters
      String txt = text.replaceAll( "[^\\x00-\\x7F]", "" );
      // erases all the ASCII control characters
      txt = txt.replaceAll( "[\\p{Cntrl}&&[^\r\n\t]]", "" );
      // removes non-printable characters from Unicode
      txt = txt.replaceAll( "\\p{C}", "" );
      return !text.equals( txt );
   }


   static private void addTuis( final Map<Long, CuiTerm> cuiTerms,
                                final String styPath,
                                final Collection<SemanticTui> wantedTuis ) {
      LOGGER.info( "Adding TUIs to Terms " + styPath );
      try ( final BufferedReader reader = new BufferedReader( new FileReader( styPath ) );
            DotLogger logger = new DotLogger() ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] columns = StringUtil.fastSplit( line, '|' );
            final SemanticTui semanticTui = SemanticTui.getTuiFromCode( columns[ 1 ] );
            if ( wantedTuis.contains( semanticTui ) ) {
               final CuiTerm cuiTerm = cuiTerms.get( CuiCodeUtil.getInstance().getCuiCode( columns[ 0 ] ) );
               if ( cuiTerm != null ) {
                  cuiTerm.addTui( semanticTui );
               }
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private void populateRanks( final String rankPath,
                                      final Collection<String> wantedVocabularies,
                                      final Collection<Tty> wantedTextTypes ) {
      LOGGER.info( "Ranking Vocabulary Text Types from " + rankPath );
      try ( final BufferedReader reader = new BufferedReader( new FileReader( rankPath ) );
            DotLogger logger = new DotLogger() ) {
         String line = reader.readLine();
         while ( line != null ) {
            final String[] columns = StringUtil.fastSplit( line, '|' );
            final Tty tty = Tty.getType( columns[ 2 ] );
            if ( wantedVocabularies.contains( columns[ 1 ] ) && wantedTextTypes.contains( tty ) ) {
               try {
                  final int rank = Integer.parseInt( columns[ 0 ] );
                  // 1 = Vocab name, 2 = Term Type (PT, AB, etc)
                  Ranks.getInstance().setUmlsRank( columns[ 1 ], columns[ 2 ], rank );
               } catch ( NumberFormatException nfE ) {
                  Ranks.getInstance().setUmlsRank( columns[ 1 ], columns[ 2 ], -1 );
               }
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }


   // TODO - allow User to blacklist
//
//   // Pref Text | Cui | unwanted Synonym
//   static public void removeUnwantedSynonyms( final String customUnwantedPath,
//                                              final Map<String, CuiTerm> cuiTerms ) {
//      if ( !new File( customUnwantedPath ).isFile() ) {
//         LOGGER.warn( "No custom unwanted synonym file." );
//         return;
//      }
//      long count = 0;
//      try ( final BufferedReader reader = new BufferedReader( new FileReader( customUnwantedPath ) ) ) {
//         String line = reader.readLine();
//         while ( line != null ) {
//            if ( !line.isEmpty() && !line.startsWith( "//" ) ) {
//               final String[] columns = StringUtil.fastSplit( line, '|' );
//               if ( columns.length == 3 ) {
//                  final String cui = CuiUtil.getCui( columns[ 1 ] );
//                  final CuiTerm term = cuiTerms.get( cui );
//                  if ( term != null ) {
//                     term.addUnwantedSynonym( columns[ 2 ] );
//                     count++;
//                  }
//               } else {
//                  LOGGER.warn( "Poorly formed line: " + line );
//               }
//            }
//            line = reader.readLine();
//         }
//      } catch ( IOException ioE ) {
//         LOGGER.error( ioE.getMessage() );
//      }
//      LOGGER.info( "Removed " + count + " unwanted term synonyms" );
//   }


}
