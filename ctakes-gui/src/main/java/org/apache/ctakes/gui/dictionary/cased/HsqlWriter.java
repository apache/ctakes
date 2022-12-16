package org.apache.ctakes.gui.dictionary.cased;


import org.apache.ctakes.gui.dictionary.cased.term.CuiTerm;
import org.apache.ctakes.gui.dictionary.umls.VocabularyStore;
import org.apache.ctakes.gui.dictionary.util.HsqlUtil;
import org.apache.ctakes.gui.dictionary.util.JdbcUtil;
import org.apache.ctakes.gui.dictionary.util.RareWordUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/26/2020
 */
final public class HsqlWriter {

   static private final Logger LOGGER = Logger.getLogger( "HsqlWriter" );

   static public boolean writeHsql( final String hsqlPath,
                                    final String dictionaryName,
                                    final Collection<String> writtenSchema,
                                    final Collection<CuiTerm> cuiTerms ) {
      final String url = HsqlUtil.URL_PREFIX + hsqlPath.replace( '\\', '/' ) + "/" + dictionaryName + "/" +
                         dictionaryName;
      final Connection connection = JdbcUtil.createDatabaseConnection( url, "SA", "" );
      if ( !createDatabase( connection, writtenSchema ) ) {
         return false;
      }
      // Get Count of appearance in dictionary per term token
      final Map<String, Long> upperTokenCounts = getUpperTokenCounts( cuiTerms );
      final Map<String, Long> mixedTokenCounts = getMixedTokenCounts( cuiTerms );
      final Map<String, Long> lowerTokenCounts = getLowerTokenCounts( cuiTerms );
      // Create insert sql statements
      final String upperSql = JdbcUtil.createRowInsertSql( "UPPER", Synonym.values() );
      final String mixedSql = JdbcUtil.createRowInsertSql( "MIXED", Synonym.values() );
      final String lowerSql = JdbcUtil.createRowInsertSql( "LOWER", Synonym.values() );
      final String tuiSql = JdbcUtil.createCodeInsertSql( "TUI" );
      final String prefTextSql = JdbcUtil.createCodeInsertSql( "PREFERRED_TEXT" );
      final Map<String, String> insertCodeSqls = createCodeInsertSqls( writtenSchema );

      try {

         final PreparedStatement upperStatement = connection.prepareStatement( upperSql );
         final PreparedStatement mixedStatement = connection.prepareStatement( mixedSql );
         final PreparedStatement lowerStatement = connection.prepareStatement( lowerSql );
         final PreparedStatement tuiStatement = connection.prepareStatement( tuiSql );
         final PreparedStatement prefTextStatement = connection.prepareStatement( prefTextSql );
         final Map<String, PreparedStatement> codeStatements = createCodeStatements( connection, insertCodeSqls );

         for ( CuiTerm cuiTerm : cuiTerms ) {
            final long cui = cuiTerm.getCuiCode();
            // write main term table
            for ( String text : cuiTerm.getUpperOnly() ) {
               final RareWordUtil.IndexedRareWord indexedRareWord
                     = RareWordUtil.getIndexedRareWord( text, upperTokenCounts );
               if ( RareWordUtil.NULL_RARE_WORD.equals( indexedRareWord ) ) {
                  continue;
               }
               upperStatement.setLong( Synonym.CUI.getColumn(), cui );
               upperStatement.setString( Synonym.PREFIX.getColumn(), getPrefix( text, indexedRareWord.__word ) );
               upperStatement.setString( Synonym.INDEX_WORD.getColumn(), indexedRareWord.__word );
               upperStatement.setString( Synonym.SUFFIX.getColumn(), getSuffix( text, indexedRareWord.__word ) );
               upperStatement.setInt( Synonym.RANK.getColumn(), cuiTerm.getRank( text ) );
               upperStatement.setInt( Synonym.INSTANCES.getColumn(), cuiTerm.getInstances( text ) );
               upperStatement.executeUpdate();
            }
            for ( String text : cuiTerm.getMixedOnly() ) {
               final RareWordUtil.IndexedRareWord indexedRareWord
                     = RareWordUtil.getIndexedRareWord( text, mixedTokenCounts );
               if ( RareWordUtil.NULL_RARE_WORD.equals( indexedRareWord ) ) {
                  continue;
               }
               mixedStatement.setLong( Synonym.CUI.getColumn(), cui );
               mixedStatement.setString( Synonym.PREFIX.getColumn(), getPrefix( text, indexedRareWord.__word ) );
               mixedStatement.setString( Synonym.INDEX_WORD.getColumn(), indexedRareWord.__word );
               mixedStatement.setString( Synonym.SUFFIX.getColumn(), getSuffix( text, indexedRareWord.__word ) );
               mixedStatement.setInt( Synonym.RANK.getColumn(), cuiTerm.getRank( text ) );
               mixedStatement.setInt( Synonym.INSTANCES.getColumn(), cuiTerm.getInstances( text ) );
               mixedStatement.executeUpdate();
            }
            for ( String text : cuiTerm.getLowerOnly() ) {
               final RareWordUtil.IndexedRareWord indexedRareWord
                     = RareWordUtil.getIndexedRareWord( text, lowerTokenCounts );
               if ( RareWordUtil.NULL_RARE_WORD.equals( indexedRareWord ) ) {
                  continue;
               }
               lowerStatement.setLong( Synonym.CUI.getColumn(), cui );
               lowerStatement.setString( Synonym.PREFIX.getColumn(), getPrefix( text, indexedRareWord.__word ) );
               lowerStatement.setString( Synonym.INDEX_WORD.getColumn(), indexedRareWord.__word );
               lowerStatement.setString( Synonym.SUFFIX.getColumn(), getSuffix( text, indexedRareWord.__word ) );
               lowerStatement.setInt( Synonym.RANK.getColumn(), cuiTerm.getRank( text ) );
               lowerStatement.setInt( Synonym.INSTANCES.getColumn(), cuiTerm.getInstances( text ) );
               lowerStatement.executeUpdate();
            }
            // write tui table
            for ( int tui : cuiTerm.getTuis() ) {
               tuiStatement.setLong( 1, cui );
               tuiStatement.setInt( 2, tui );
               tuiStatement.executeUpdate();
            }
            // write preferred term table
            String preferredText = cuiTerm.getPreferredText();
            if ( !preferredText.isEmpty() ) {
               prefTextStatement.setLong( 1, cui );
               if ( preferredText.length() > 255 ) {
                  preferredText = preferredText.substring( 0, 255 );
               }
               prefTextStatement.setString( 2, preferredText );
               prefTextStatement.executeUpdate();
            }
            // write extra vocabulary code tables
            final Map<String, Collection<String>> schemaCodeMap = cuiTerm.getSchemaCodes();
            for ( Map.Entry<String, Collection<String>> schemaCodes : schemaCodeMap.entrySet() ) {
               final String schema = fixVocabName( schemaCodes.getKey() );
               final PreparedStatement statement = codeStatements.get( schema );
               statement.setLong( 1, cui );
               for ( String code : schemaCodes.getValue() ) {
                  setCodeAppropriately( statement, code, VocabularyStore.getInstance()
                                                                        .getVocabularyClass( schema ) );
                  statement.executeUpdate();
               }
            }
         }
         connection.commit();
         upperStatement.close();
         mixedStatement.close();
         lowerStatement.close();
         tuiStatement.close();
         prefTextStatement.close();
         for ( PreparedStatement codeStatement : codeStatements.values() ) {
            codeStatement.close();
         }

         connection.commit();
         final Statement shutdownStatement = connection.createStatement();
         shutdownStatement.execute( "SHUTDOWN" );
         shutdownStatement.close();
         connection.commit();
         connection.close();
      } catch ( SQLException sqlE ) {
         LOGGER.error( sqlE.getMessage() );
         return false;
      }
      return true;
   }


   static private String fixVocabName( final String vocabulary ) {
      return vocabulary.toUpperCase().replace( '.', '_' ).replace( '-', '_' );
   }

   static private Map<String, String> createCodeInsertSqls( final Collection<String> writtenSchema ) {
      return writtenSchema.stream().map( HsqlWriter::fixVocabName )
                          .collect( Collectors.toMap( Function.identity(), HsqlWriter::createCodeInsertSql ) );
   }

   static public String createCodeInsertSql( final String vocabulary ) {
      return JdbcUtil.createRowInsertSql( vocabulary, "CUI", vocabulary );
   }

   static private Map<String, PreparedStatement> createCodeStatements( final Connection connection,
                                                                       final Map<String, String> insertCodeSqls )
         throws SQLException {
      final Map<String, PreparedStatement> codeStatements = new HashMap<>( insertCodeSqls.size() );
      for ( Map.Entry<String, String> codeSql : insertCodeSqls.entrySet() ) {
         codeStatements.put( codeSql.getKey(), connection.prepareStatement( codeSql.getValue() ) );
      }
      return codeStatements;
   }

   static private void setCodeAppropriately( final PreparedStatement statement, final String code,
                                             final Class<?> type ) throws SQLException {
      if ( String.class.equals( type ) ) {
         statement.setString( 2, code );
      } else if ( Double.class.equals( type ) ) {
         statement.setDouble( 2, Double.valueOf( code ) );
      } else if ( Long.class.equals( type ) ) {
         statement.setLong( 2, Long.valueOf( code ) );
      } else if ( Integer.class.equals( type ) ) {
         statement.setInt( 2, Integer.valueOf( code ) );
      } else {
         LOGGER.error( "Could not set code for " + type.getName() );
         statement.setString( 2, code );
      }
   }


   static private boolean createDatabase( final Connection connection, final Collection<String> writtenSchema ) {
      try {
         // main tables
         createSynonymTable( connection, "UPPER" );
         createSynonymTable( connection, "MIXED" );
         createSynonymTable( connection, "LOWER" );
         // tui table
         createTable( connection, "TUI", "CUI BIGINT", "TUI INTEGER" );
         createIndex( connection, "TUI", "CUI" );
         // preferred text table
         createTable( connection, "PREFERRED_TEXT", "CUI BIGINT", "PREFERRED_TEXT VARCHAR(255)" );
         createIndex( connection, "PREFERRED_TEXT", "CUI" );

         // schema codes tables
         for ( String vocabulary : writtenSchema ) {
            final String jdbcClass = VocabularyStore.getInstance().getJdbcClass( vocabulary );
            final String tableName = fixVocabName( vocabulary );
            createTable( connection, tableName, "CUI BIGINT", tableName + " " + jdbcClass );
            createIndex( connection, tableName, "CUI" );
         }

         executeStatement( connection, "SET WRITE_DELAY 10" );
      } catch ( SQLException sqlE ) {
         LOGGER.error( sqlE.getMessage() );
         return false;
      }
      return true;
   }

   static private void createSynonymTable( final Connection connection, final String tableName ) throws SQLException {
      createTable( connection, tableName,
            "CUI BIGINT",
            "PREFIX VARCHAR(78)",
            "INDEX_WORD VARCHAR(48)",
            "SUFFIX VARCHAR(78)",
            "RANK INTEGER",
            "INSTANCES INTEGER" );
      createIndex( connection, tableName, "INDEX_WORD" );
   }

   static private void createTable( final Connection connection, final String tableName, final String... fieldNames )
         throws SQLException {
      final String fields = String.join( ",", fieldNames );
      final String creator = "CREATE MEMORY TABLE " + tableName + "(" + fields + ")";
      executeStatement( connection, creator );
   }

   static private void createIndex( final Connection connection, final String tableName,
                                    final String indexField ) throws SQLException {
      final String indexer = "CREATE INDEX IDX_" + tableName + " ON " + tableName + "(" + indexField + ")";
      executeStatement( connection, indexer );
   }

   static private void executeStatement( final Connection connection, final String command ) throws SQLException {
      final Statement statement = connection.createStatement();
      statement.execute( command );
      statement.close();
   }


   static private final Pattern SPACE_PATTERN = Pattern.compile( "\\s+" );

   static private Map<String, Long> getUpperTokenCounts( final Collection<CuiTerm> cuiTerms ) {
      return cuiTerms.stream()
                     .map( CuiTerm::getUpperOnly )
                     .flatMap( Collection::stream )
                     .map( SPACE_PATTERN::split )
                     .flatMap( Arrays::stream )
                     .filter( RareWordUtil::isRarableToken )
                     .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }

   static private Map<String, Long> getMixedTokenCounts( final Collection<CuiTerm> cuiTerms ) {
      return cuiTerms.stream()
                     .map( CuiTerm::getMixedOnly )
                     .flatMap( Collection::stream )
                     .map( SPACE_PATTERN::split )
                     .flatMap( Arrays::stream )
                     .filter( RareWordUtil::isRarableToken )
                     .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }

   static private Map<String, Long> getLowerTokenCounts( final Collection<CuiTerm> cuiTerms ) {
      return cuiTerms.stream()
                     .map( CuiTerm::getLowerOnly )
                     .flatMap( Collection::stream )
                     .map( SPACE_PATTERN::split )
                     .flatMap( Arrays::stream )
                     .filter( RareWordUtil::isRarableToken )
                     .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }


   static private String getPrefix( final String text, final String indexedRareWord ) {
      if ( text.equals( indexedRareWord ) ) {
         return "";
      }
      if ( text.startsWith( indexedRareWord ) ) {
         return "";
      }
      return text.substring( 0, text.indexOf( indexedRareWord ) ).trim();
   }

   static private String getSuffix( final String text, final String indexedRareWord ) {
      if ( text.equals( indexedRareWord ) ) {
         return "";
      }
      if ( text.endsWith( indexedRareWord ) ) {
         return "";
      }
      return text.substring( text.indexOf( indexedRareWord ) + indexedRareWord.length() ).trim();
   }


}
