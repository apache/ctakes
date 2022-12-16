package org.apache.ctakes.dictionary.cased.util.jdbc;


import org.apache.ctakes.dictionary.lookup2.util.JdbcConnectionFactory;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.uima.UimaContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class JdbcUtil {

   private JdbcUtil() {
   }

   static public final String HSQL_DRIVER = "org.hsqldb.jdbcDriver";
   static public final String UPPER_TABLE = "UPPER";
   static public final String MIXED_TABLE = "MIXED";
   static public final String LOWER_TABLE = "LOWER";
   static public final String DEFAULT_USER = "sa";
   static public final String DEFAULT_PASS = "";


   static public String getParameterValue( final String rootName,
                                           final String parameterName,
                                           final UimaContext uimaContext,
                                           final String defaultValue ) {
      final String value = EnvironmentVariable.getEnv( rootName + '_' + parameterName, uimaContext );
      if ( value != null && !value.equals( EnvironmentVariable.NOT_PRESENT ) ) {
         return value;
      }
      return defaultValue;
   }


   static public PreparedStatement createPreparedStatement( final String name,
                                                            final String jdbcDriver,
                                                            final String jdbcUrl,
                                                            final String jdbcUser,
                                                            final String jdbcPass,
                                                            final String tableName,
                                                            final String indexName ) throws SQLException {
      if ( jdbcDriver == null || jdbcDriver.isEmpty() ) {
         throw new SQLException( "No JDBC Driver specified for " + name );
      }
      if ( jdbcUrl == null || jdbcUrl.isEmpty() ) {
         throw new SQLException( "No URL specified for " + name );
      }
      if ( tableName == null || tableName.isEmpty() ) {
         throw new SQLException( "No Table specified for " + name );
      }
      // DO NOT use try with resources here.  Try with resources uses a closable and closes it when exiting the try
      final Connection connection = JdbcConnectionFactory.getInstance()
                                                         .getConnection( jdbcDriver, jdbcUrl, jdbcUser, jdbcPass );
      if ( connection == null ) {
         throw new SQLException( "Could not connect to " + name );
      }
      return createSelectCall( connection, tableName, indexName );
   }


   /**
    * @return an sql call to use for term lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   static private PreparedStatement createSelectCall( final Connection connection,
                                                      final String table,
                                                      final String index ) throws SQLException {
      final String lookupSql = "SELECT * FROM " + table + " WHERE " + index + " = ?";
      return connection.prepareStatement( lookupSql );
   }

   /**
    * @param statement an sql call to use for lookup
    * @param text      of the text to use for lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   static public void fillSelectCall( final PreparedStatement statement, final String text ) throws SQLException {
      statement.clearParameters();
      statement.setString( 1, text );
   }

   /**
    * @param statement an sql call to use for lookup
    * @param value     of the long to use for lookup
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   static public void fillSelectCall( final PreparedStatement statement, final long value ) throws SQLException {
      statement.clearParameters();
      statement.setLong( 1, value );
   }


}
