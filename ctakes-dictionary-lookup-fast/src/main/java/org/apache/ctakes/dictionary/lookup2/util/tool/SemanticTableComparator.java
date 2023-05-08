package org.apache.ctakes.dictionary.lookup2.util.tool;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.*;

/**
 * Gets statistical CUI information to compare 2 directories run with different dictionaries.
 * Uses the output from SemanticTableFileWriter.
 *
 * @author SPF , chip-nlp
 * @since {4/28/2023}
 */
final public class SemanticTableComparator {

   static private final List<String> COLUMNS = Arrays.asList(
         " Semantic Group ",
         " Semantic Type ",
         " Section ",
         " Span ",
         " Negated ",
         " Uncertain ",
         " Generic ",
         " CUI ",
         " Preferred Text ",
         " Document Text " );
   static private final int TYPE_INDEX = 1;
   static private final int SPAN_INDEX = 3;
   static private final int CUI_INDEX = 7;
   static private final int PREF_INDEX = 8;
   static private final int TEXT_INDEX = 9;

   public static void main( String... args ) {
      final File dir1 = new File( args[0] );
      final File dir2 = new File( args[1] );
      final String outputFile = args[2];
      System.out.println( "Getting Filenames ...");
      final Collection<String> dir1files = getBsvFileNames( dir1 );
      final Collection<String> dir2files = getBsvFileNames( dir2 );
      System.out.println( "Comparing Filenames ..." );
      compareFileLists( dir1, outputFile, dir1files, dir2files );
      compareFileLists( dir2, outputFile, dir2files, dir1files );
      final Collection<String> files = new HashSet<>( dir1files );
      files.retainAll( dir2files );
      compareFileCuis( dir1, dir2, files, outputFile );
   }

   static private Collection<String> getBsvFileNames( final File dir ) {
      if ( !dir.isDirectory() ) {
         System.err.println( "Not a directory: " + dir );
         System.exit( 1 );
      }
      final Collection<String> files = new HashSet<>();
      for ( String file : Objects.requireNonNull( dir.list() ) ) {
         final int bsvIndex = file.toLowerCase().indexOf( ".bsv" );
         if ( bsvIndex > 0 ) {
            files.add( file.substring( 0, bsvIndex ) );
         }
      }
      return files;
   }

   static private void compareFileLists( final File dir1,
                                         final String outputFile,
                                         final Collection<String> names1,
                                         final Collection<String> names2 ) {
      final Collection<String> uncommon = new HashSet<>( names2 );
      uncommon.removeAll( names1 );
      if ( uncommon.isEmpty() ) {
         return;
      }
      final File out1 = new File( dir1, outputFile );
      try ( Writer writer = new BufferedWriter( new FileWriter( out1 ) ) ) {
         writer.write( uncommon.size() + " Missing Files for " + dir1 + "\n");
         final List<String> sorted = new ArrayList<>( uncommon );
         Collections.sort( sorted );
         for ( String file : sorted ) {
            writer.write( file + "\n" );
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
   }

   static private void compareFileCuis( final File dir1,
                                        final File dir2,
                                        final Collection<String> files,
                                        final String outputFile ) {
      final Map<String,String> cuiPrefMap = new HashMap<>();
      final Collection<String> allCuis1 = new HashSet<>();
      final Collection<String> allCuis2 = new HashSet<>();
      final Collection<String> allTypes1 = new HashSet<>();
      final Collection<String> allTypes2 = new HashSet<>();
      final Collection<String> undiscovered1 = new HashSet<>();
      final Collection<String> undiscovered2 = new HashSet<>();
      files.forEach( f -> compareFileCuis( dir1, dir2, f, outputFile,
                                           allCuis1, allCuis2,
                                           allTypes1, allTypes2,
                                           undiscovered1, undiscovered2,
                                           cuiPrefMap ) );
      final File outCommon = new File( dir1.getParent(), outputFile );
      final StringBuilder sb = new StringBuilder();
      final Collection<String> uncommon = new HashSet<>( allTypes1 );
      uncommon.removeAll( allTypes2 );
      sb.append( "\n\n" + uncommon.size() + " Missing Types for " + dir2 + "\n" );
      uncommon.stream().sorted().map( t -> t+"\n" ).forEach( sb::append );
      uncommon.clear();
      uncommon.addAll( allTypes2 );
      uncommon.removeAll( allTypes1 );
      sb.append( "\n\n" + uncommon.size() + " Missing Types for " + dir1 + "\n" );
      uncommon.stream().sorted().map( t -> t+"\n" ).forEach( sb::append );
      uncommon.clear();
      uncommon.addAll( allCuis1 );
      uncommon.removeAll( allCuis2 );
      sb.append( "\n\n" + uncommon.size() + " Missing CUIs for " + dir2 + "\n" );
      uncommon.stream().sorted()
              .map( c -> c + " " + cuiPrefMap.getOrDefault( c, "" ) + "\n" )
              .forEach( sb::append );
      uncommon.clear();
      uncommon.addAll( allCuis2 );
      uncommon.removeAll( allCuis1 );
      sb.append( "\n\n" + uncommon.size() + " Missing CUIs for " + dir1 + "\n" );
      uncommon.stream().sorted()
              .map( c -> c + " " + cuiPrefMap.getOrDefault( c, "" ) + "\n" )
              .forEach( sb::append );
      sb.append( "\n\n" + undiscovered2.size() + " Missing Texts for " + dir2 + "\n" );
      undiscovered2.stream().sorted().map( t -> t+"\n" ).forEach( sb::append );
      sb.append( "\n\n" + undiscovered1.size() + " Missing Texts for " + dir1 + "\n" );
      undiscovered1.stream().sorted().map( t -> t+"\n" ).forEach( sb::append );
      try ( Writer writer = new BufferedWriter( new FileWriter( outCommon ) ) ) {
         writer.write( files.size()  + " Total files.\n" );
         writer.write( allTypes1.size() + " Total Types for " + dir1 + "\n" );
         writer.write( allTypes2.size() + " Total Types for " + dir2 + "\n");
         writer.write( allCuis1.size() + " Total CUIs for " + dir1 + "\n" );
         writer.write( allCuis2.size() + " Total CUIs for " + dir2 + "\n");
         writer.write( sb.toString() );
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
   }


   static private void compareFileCuis( final File dir1,
                                        final File dir2,
                                        final String file,
                                        final String outputFile,
                                        final Collection<String> allCuis1,
                                        final Collection<String> allCuis2,
                                        final Collection<String> allTypes1,
                                        final Collection<String> allTypes2,
                                        final Collection<String> undiscovered1,
                                        final Collection<String> undiscovered2,
                                        final Map<String,String> cuiPrefMap ) {
      System.out.println( "Comparing File " + file + " ..." );
      final File out1 = new File( dir1, outputFile );
      final File out2 = new File( dir2, outputFile );
      final List<Pair<String>> typeSpans1 = new ArrayList<>();
      final List<String> texts1 = new ArrayList<>();
      final List<List<String>> cuis1 = new ArrayList<>();
      final List<Pair<String>> typeSpans2 = new ArrayList<>();
      final List<String> texts2 = new ArrayList<>();
      final List<List<String>> cuis2 = new ArrayList<>();
      getSpanCuis( dir1, file, typeSpans1, cuis1, texts1, cuiPrefMap );
      getSpanCuis( dir2, file, typeSpans2, cuis2, texts2, cuiPrefMap );
      writeMissingCuis( out2, file, typeSpans1, texts1, cuis1, typeSpans2, cuis2, undiscovered2 );
      writeMissingCuis( out1, file, typeSpans2, texts2, cuis2, typeSpans1, cuis1, undiscovered1 );
      cuis1.forEach( allCuis1::addAll );
      cuis2.forEach( allCuis2::addAll );
      typeSpans1.forEach( t -> allTypes1.add( t.getValue1() ) );
      typeSpans2.forEach( t -> allTypes2.add( t.getValue1() ) );
   }

   static private void writeMissingCuis( final File out2,
                                          final String file,
                                          final List<Pair<String>> typeSpans1,
                                          final List<String> texts1,
                                          final List<List<String>> cuis1,
                                          final List<Pair<String>> typeSpans2,
                                          final List<List<String>> cuis2,
                                         final Collection<String> undiscoveredText ) {
      try ( Writer writer2 = new BufferedWriter( new FileWriter( out2, true ) ) ) {
         writer2.write( "\nFile: " + file + "\n" );
         if ( typeSpans2.isEmpty() ) {
            writer2.write( "No discoveries, other file has " + typeSpans1.size() + " discoveries\n" );
            return;
         }
         for ( int i=0; i < typeSpans1.size(); i++ ) {
            final Pair<String> typeSpan = typeSpans1.get( i );
            final int index2 = typeSpans2.indexOf( typeSpan );
            if ( index2 < 0 ) {
               writer2.write( "All CUIs are missing for text \"" + texts1.get( i )
                              + "\" , type " +  typeSpan.getValue1() + " at " + typeSpan.getValue2() + "  "
                              + String.join( " , ", cuis1.get( i ) ) + "\n" );
               undiscoveredText.add( texts1.get( i ).toLowerCase() );
               continue;
            }
            final Collection<String> uncommon = new HashSet<>( cuis1.get( i ) );
            uncommon.removeAll( cuis2.get( index2 ) );
            if ( !uncommon.isEmpty() ) {
               writer2.write( uncommon.size() + " CUIs  are missing for text \"" + texts1.get( i )
                              + "\" , type " +  typeSpan.getValue1() + " at " + typeSpan.getValue2() + "  "
                              + String.join( " , ", uncommon ) + "\n" );
            }
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
   }



   static private void getSpanCuis( final File dir,
                                   final String file,
                                   final List<Pair<String>> typeSpans,
                                    final List<List<String>> cuis,
                                   final List<String> texts,
                                   final Map<String,String> cuiPrefMap ) {
      final File bsv = new File( dir, file + ".bsv" );
      if ( !bsv.exists() ) {
         return;
      }
      try ( BufferedReader reader = new BufferedReader( new FileReader( bsv ) ) ) {
         String line = "";
         while ( line != null ) {
            if ( line.isEmpty() || line.startsWith( COLUMNS.get( 0 ) ) ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            final String type = splits[ TYPE_INDEX ];
            final String span = splits[ SPAN_INDEX ];
            typeSpans.add( new Pair<>( type, span ) );
            texts.add( splits[ TEXT_INDEX ] );
            final String cuisCol = splits[ CUI_INDEX ];
            final List<String> cuiList = Arrays.asList( StringUtil.fastSplit( cuisCol, ';' ) );
            cuis.add( cuiList );
            cuiList.forEach( c -> cuiPrefMap.put( c, splits[ PREF_INDEX ] ) );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }


}
