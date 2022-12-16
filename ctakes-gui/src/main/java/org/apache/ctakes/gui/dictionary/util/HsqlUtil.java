package org.apache.ctakes.gui.dictionary.util;


import org.apache.ctakes.gui.dictionary.umls.VocabularyStore;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/12/2015
 */
final public class HsqlUtil {

   static private final Logger LOGGER = Logger.getLogger( "HsqlUtil" );

   static public final String URL_PREFIX = "jdbc:hsqldb:file:";

   private HsqlUtil() {
   }


   static public boolean createDatabase( final Connection connection ) {
      try {
         // main table
         createTable( connection, "CUI_TERMS",
               "CUI BIGINT", "RINDEX INTEGER", "TCOUNT INTEGER", "TEXT VARCHAR(255)", "RWORD VARCHAR(48)" );
         createIndex( connection, "CUI_TERMS", "RWORD" );
         // tui table
         createTable( connection, "TUI", "CUI BIGINT", "TUI INTEGER" );
         createIndex( connection, "TUI", "CUI" );
         // preferred term table
         createTable( connection, "PREFTERM", "CUI BIGINT", "PREFTERM VARCHAR(511)" );
         createIndex( connection, "PREFTERM", "CUI" );
         // vocabulary tables
         for ( String vocabulary : VocabularyStore.getInstance().getAllVocabularies() ) {
            final String jdbcClass = VocabularyStore.getInstance().getJdbcClass( vocabulary );
            final String tableName = vocabulary.replace( '.', '_' ).replace( '-', '_' );
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

   static private void createTable( final Connection connection, final String tableName, final String... fieldNames )
         throws SQLException {
      final String fields = Arrays.stream( fieldNames ).collect( Collectors.joining( "," ) );
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

}
