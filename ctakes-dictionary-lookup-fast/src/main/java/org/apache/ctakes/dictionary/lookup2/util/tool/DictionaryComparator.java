package org.apache.ctakes.dictionary.lookup2.util.tool;

import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {5/10/2023}
 */
final public class DictionaryComparator {

   static private final String INSERT_SQL = "INSERT INTO CUI_TERMS VALUES(";

   public static void main( String... args ) {
      final File script1 = new File( args[0] );
      final File script2 = new File( args[1] );
      compareScriptCuis( script1, script2 );
   }

   static private void compareScriptCuis( final File script1, final File script2 ) {
      final int sql_length = INSERT_SQL.length();
      final Collection<String> cuis1 = new HashSet<>();
      final Collection<String> synonyms1 = new HashSet<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( script1 ) ) ) {
         String line = "";
         while ( line != null ) {
            if ( line.startsWith( INSERT_SQL ) ) {
               line = line.substring( sql_length );
               final String[] splits = StringUtil.fastSplit( line, ',' );
               cuis1.add( splits[ 0 ] );
               final String[] splitz = StringUtil.fastSplit( line, '\'' );
               synonyms1.add( splitz[ 1 ] );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
      final Collection<String> cuis2 = new HashSet<>();
      final Collection<String> synonyms2 = new HashSet<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( script2 ) ) ) {
         String line = "";
         while ( line != null ) {
            if ( line.startsWith( INSERT_SQL ) ) {
               line = line.substring( sql_length );
               final String[] splits = StringUtil.fastSplit( line, ',' );
               cuis2.add( splits[ 0 ] );
               final String[] splitz = StringUtil.fastSplit( line, '\'' );
               synonyms2.add( splitz[ 1 ] );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
      }
      System.out.println( "CUIs in " + script1.getName() + " : " + cuis1.size() );
      System.out.println( "CUIs in " + script2.getName() + " : " + cuis2.size() );

      final Collection<String> missingFrom1 = new HashSet<>( cuis1 );
      missingFrom1.removeAll( cuis2 );
      System.out.println( "CUIs in " + script1.getName() + " not in " + script2.getName() + " : " + missingFrom1.size() );

      final Collection<String> missingFrom2 = new HashSet<>( cuis2 );
      missingFrom2.removeAll( cuis1 );
      System.out.println( "CUIs in " + script2.getName() + " not in " + script1.getName() + " : " + missingFrom2.size() );



      System.out.println( "Synonyms in " + script1.getName() + " : " + synonyms1.size() );
      System.out.println( "Synonyms in " + script2.getName() + " : " + synonyms2.size() );

      final Collection<String> mizzingFrom1 = new HashSet<>( synonyms1 );
      mizzingFrom1.removeAll( synonyms2 );
      System.out.println( "Synonyms in " + script1.getName() + " not in " + script2.getName() + " : " + mizzingFrom1.size() );

      final Collection<String> mizzingFrom2 = new HashSet<>( synonyms2 );
      mizzingFrom2.removeAll( synonyms1 );
      System.out.println( "Synonyms in " + script2.getName() + " not in " + script1.getName() + " : " + mizzingFrom2.size() );
   }

}
