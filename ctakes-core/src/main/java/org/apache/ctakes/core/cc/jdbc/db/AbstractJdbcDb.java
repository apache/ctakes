package org.apache.ctakes.core.cc.jdbc.db;


import org.apache.ctakes.core.cc.jdbc.table.JdbcTable;
import org.apache.uima.resource.ResourceInitializationException;
import sqlWrapper.WrappedConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
abstract public class AbstractJdbcDb implements JdbcDb {

   private final Connection _connection;

   private final Collection<JdbcTable<?>> _jdbcTables = new ArrayList<>();

   public AbstractJdbcDb( final String driver,
                          final String url,
                          final String user,
                          final String pass,
                          final String keepAlive ) throws ResourceInitializationException {
      _connection = createConnection( driver, url, user, pass, keepAlive );
   }

   /**
    * @param driver    -
    * @param url       -
    * @param user      -
    * @param pass      -
    * @param keepAlive -
    * @return a connection to the database
    * @throws ResourceInitializationException -
    */
   static private Connection createConnection( final String driver,
                                               final String url,
                                               final String user,
                                               final String pass,
                                               final String keepAlive ) throws ResourceInitializationException {
      final Object[] emptyObjectArray = new Object[ 0 ];
      try {
         if ( keepAlive != null && !keepAlive.isEmpty() && Boolean.valueOf( keepAlive ) ) {
            return new WrappedConnection( user, pass, driver, url );
         }
         final Class driverClass = Class.forName( driver );
         return DriverManager.getConnection( url, user, pass );
      } catch ( ClassNotFoundException | SQLException multE ) {
         throw new ResourceInitializationException( "Could not construct " + driver,
               emptyObjectArray, multE );
      }
   }

   public Connection getConnection() {
      return _connection;
   }

   final public void addTable( final JdbcTable<?> table ) {
      _jdbcTables.add( table );
   }

   final public Collection<JdbcTable<?>> getTables() {
      return _jdbcTables;
   }

}
