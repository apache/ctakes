package org.apache.ctakes.gui.dictionary.util;

import org.apache.log4j.Logger;

import java.sql.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/21/14
 */
final public class JdbcUtil {

   static private final Logger LOGGER = Logger.getLogger( "JdbcUtil" );

   private JdbcUtil() {
   }

   static private final String JDBC_DRIVER = "org.hsqldb.jdbcDriver";


   static public void registerDriver() {
      try {
         Driver driver = (Driver)Class.forName( JDBC_DRIVER ).newInstance();
         DriverManager.registerDriver( driver );
      } catch ( Exception e ) {
         // TODO At least four different exceptions are thrown here, and should be caught and handled individually
         LOGGER.error( "Could not register Driver " + JDBC_DRIVER );
         LOGGER.error( e.getMessage() );
         System.exit( 1 );
      }
   }

   static public Connection createDatabaseConnection( final String url, final String user, final String pass ) {
      registerDriver();
      LOGGER.info( "Connecting to " + url + " as " + user );
      Connection connection = null;
      try {
         connection = DriverManager.getConnection( url, user, pass );
      } catch ( SQLException sqlE ) {
         // thrown by Connection.prepareStatement(..) and getTotalRowCount(..)
         LOGGER.error( "Could not establish connection to " + url + " as " + user );
         LOGGER.error( sqlE.getMessage() );
         System.exit( 1 );
      }
      registerShutdownHook( connection );
      return connection;
   }

   //   static public String createRowInsertSql( final String tableName, final int valueCount ) {
   static public String createRowInsertSql( final String tableName, final Enum... fields ) {
      final String[] fieldNames = new String[ fields.length ];
      int i = 0;
      for ( Enum field : fields ) {
         fieldNames[ i ] = field.name();
         i++;
      }
      return createRowInsertSql( tableName, fieldNames );
   }

   static public String createCodeInsertSql( final String vocabulary ) {
      return createRowInsertSql( vocabulary, "CUI", vocabulary );
   }

   static public String createRowInsertSql( final String tableName, final String... fieldNames ) {
      final StringBuilder sb = new StringBuilder( "insert into" );
      sb.append( " " ).append( tableName );
      sb.append( " (" );
      for ( String fieldName : fieldNames ) {
         sb.append( fieldName.toUpperCase() ).append( ',' );
      }
      // remove last comma
      sb.setLength( sb.length() - 1 );
      sb.append( ") " );
      sb.append( " values (" );
      for ( int i = 0; i < fieldNames.length - 1; i++ ) {
         sb.append( "?," );
      }
      sb.append( "?)" );
      return sb.toString();
   }

   /**
    * register a shutdown hook that will shut down the database, removing temporary and lock files.
    *
    * @param connection -
    */
   static private void registerShutdownHook( final Connection connection ) {
      // Registers a shutdown hook for the Hsql instance so that it
      // shuts down nicely and any temporary or lock files are cleaned up.
      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
         try {
            final Statement shutdown = connection.createStatement();
            shutdown.execute( "SHUTDOWN" );
            shutdown.close();
            // The db is read-only, so there should be no need to roll back any transactions.
            connection.clearWarnings();
            connection.close();
         } catch ( SQLException sqlE ) {
            // ignore
         }
      } ) );
   }

}
