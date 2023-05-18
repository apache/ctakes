package org.apache.ctakes.dictionary.lookup2.util.tool;

import org.apache.ctakes.dictionary.lookup2.util.JdbcConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author SPF , chip-nlp
 * @since {4/28/2023}
 */
final public class DictionaryCuiCounter {

   // Edit this list as needed.
   static private final String[] SOURCES = {
//         "SNOMEDCT_US", "CHV", "MTH", "MSH", "NCI", "RXNORM" };
         "SNOMEDCT_US" };

   /**
    * jdbc driver
    * database url.  e.g.  "jdbc:hsqldb:file:testdb".
    * user name.  e.g. "sa".
    * user password.  e.g. "".
    * @param args from the command line
    */
   public static void main( String... args ) {
      final String jdbcDriver = args[0];
      final String jdbcUrl = args[1];
      final String jdbcUser = args[2];
      String jdbcPass = "";
      if ( args.length > 3 ) {
         jdbcPass = args[ 3 ];
      }
      try {
         // DO NOT use try with resources here.  Try with resources uses a closable and closes it when exiting the try
         final Connection connection = JdbcConnectionFactory.getInstance()
                                                            .getConnection( jdbcDriver, jdbcUrl, jdbcUser, jdbcPass );
         countItem( connection, "total CUIs",
                    "SELECT COUNT ( DISTINCT CUI ) as counts FROM CUI_TERMS" );
         countItem( connection, "total Synonyms",
                    "SELECT COUNT ( TEXT ) as counts FROM CUI_TERMS" );
         countItem( connection, "unique Synonyms",
                    "SELECT COUNT ( DISTINCT TEXT ) as counts FROM CUI_TERMS" );
         countItem( connection, "top 10 CUIs",
                    "SELECT COUNT ( CUI ) FROM CUI_TERMS GROUP BY CUI ORDER BY COUNT ( CUI ) DESC LIMIT 10" );
         System.out.println( "Top 10 CUIs:" );
         getItemText( connection, "SELECT CUI FROM CUI_TERMS GROUP BY CUI ORDER BY COUNT ( CUI ) LIMIT 10" );
         // Edit list SOURCES as needed.
         for ( String source : SOURCES ) {
            countItem( connection, "total " + source + " Codes",
                       "SELECT COUNT ( DISTINCT " + source + " ) as counts FROM " + source );
         }
      } catch ( SQLException sqlE ) {
         System.err.println( sqlE.getMessage() );
      }
   }

   static private void countItem( final Connection connection,
                                      final String name,
                                      final String sql ) throws SQLException {
      final PreparedStatement statement = connection.prepareStatement( sql );
      final ResultSet resultSet = statement.executeQuery();
      while ( resultSet.next() ) {
         final long count = resultSet.getLong( 1 );
         System.out.println( "Number of " + name + ": " + count );
      }
      // Though the ResultSet interface documentation states that there are automatic closures,
      // it is up to the driver to implement this behavior ...  historically some drivers have not done so
      resultSet.close();
   }

   static private void getItemText( final Connection connection,
                                  final String sql ) throws SQLException {
      final PreparedStatement statement = connection.prepareStatement( sql );
      final ResultSet resultSet = statement.executeQuery();
      while ( resultSet.next() ) {
         final String text = resultSet.getString( 1 );
         System.out.println( text );
      }
      // Though the ResultSet interface documentation states that there are automatic closures,
      // it is up to the driver to implement this behavior ...  historically some drivers have not done so
      resultSet.close();
   }


}
