package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.dictionary.lookup2.util.JdbcConnectionFactory;
import org.apache.ctakes.dictionary.lookup2.util.TuiCodeUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.sql.*;
import java.util.*;

import static org.apache.ctakes.dictionary.lookup2.util.JdbcConnectionFactory.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
public class JdbcConceptFactory extends AbstractConceptFactory {

   // LOG4J logger based on class name
   static final private Logger LOGGER = Logger.getLogger( "JdbcConceptFactory" );

   static private final String TABLE_KEY_SUFFIX = "TABLE";
   static private final String INT_CLASS = "INT";
   static private final String LONG_CLASS = "LONG";
   static private final String TEXT_CLASS = "TEXT";
   static private final String TUI_CLASS = Concept.TUI;
   static private final String PREFTERM_CLASS = Concept.PREFTERM;


   private final Collection<ConceptTableInfo> _conceptTableInfos;


   static private class ConceptTableInfo {
      //      private final String __tableName;
      private final String __conceptName;
      private final String __classType;
      private final PreparedStatement __preparedStatement;

      private ConceptTableInfo( final String tableName, final String conceptName, final String classType,
                                final PreparedStatement preparedStatement ) {
//         __tableName = tableName;
         __conceptName = conceptName;
         __classType = classType;
         __preparedStatement = preparedStatement;
      }
   }


   // TODO  In a future release (2 from now = 3.2.5) these -correction- methods should be removed

   /**
    * Older tables were declared by type and name.  Now they are declared by table name and value class type.
    *
    * @param tableName older values, actually table type declaration (snomedtable, icd9table, icd10table)
    * @return snomedcttable for snomedtable, icd9cmtable and icd10pcstable, otherwise the provided table name
    * @deprecated In a future release (2 from now = 3.2.5) these -correction- methods should be removed
    */
   static private String adjustOldTableName( final String tableName ) {
      if ( tableName.equalsIgnoreCase( "SNOMEDTABLE" ) ) {
         return "SNOMEDCTTABLE";
      } else if ( tableName.equalsIgnoreCase( "ICD9TABLE" ) ) {
         return "ICD9CMTABLE";
      } else if ( tableName.equalsIgnoreCase( "ICD10TABLE" ) ) {
         return "ICD10PCSTABLE";
      }
      return tableName;
   }

   /**
    * Older tables were declared by type and name.  Now they are declared by table name and value class type.
    *
    * @param typeOrOldName older values, actually table names (snomedct, rxnorm, icd9cm, icd10pcs)
    * @return long for snomedct, text for rxnorm, icd9cm and icd10pcs, otherwise the provided value class type
    * @deprecated In a future release (2 from now = 3.2.5) these -correction- methods should be removed
    */
   static private String adjustOldTableClass( final String typeOrOldName ) {
      if ( typeOrOldName.equalsIgnoreCase( "SNOMEDCT" ) ) {
         return LONG_CLASS;
      } else if ( typeOrOldName.equalsIgnoreCase( "RXNORM" )
                  || typeOrOldName.equalsIgnoreCase( "ICD9CM" )
                  || typeOrOldName.equalsIgnoreCase( "ICD10PCS" ) ) {
         return TEXT_CLASS;
      }
      return typeOrOldName;
   }


   /**
    * @param properties properties that may or may not contain "*Table" key
    * @return map of table names and table value types
    */
   static private Map<String, String> getConceptTables( final Properties properties ) {
      final Collection<String> keys = properties.stringPropertyNames();
      final Map<String, String> conceptTables = new HashMap<>();
      for ( String key : keys ) {
         final String keyName = key.trim().toUpperCase();
         if ( !keyName.endsWith( TABLE_KEY_SUFFIX ) ) {
            continue;
         }
         conceptTables.put( adjustOldTableName( keyName ), adjustOldTableClass( properties.getProperty( key ) ) );
      }
      return conceptTables;
   }


   public JdbcConceptFactory( final String name, final UimaContext uimaContext, final Properties properties )
         throws SQLException {
      this( name,
            properties.getProperty( JDBC_DRIVER ), properties.getProperty( JDBC_URL ),
            properties.getProperty( JDBC_USER ), properties.getProperty( JDBC_PASS ),
            getConceptTables( properties ) );
   }

   public JdbcConceptFactory( final String name,
                              final String jdbcDriver, final String jdbcUrl,
                              final String jdbcUser, final String jdbcPass,
                              final Map<String, String> conceptTables )
         throws SQLException {
      super( name );
      boolean connected = false;
      try {
         // DO NOT use try with resources here.
         // Try with resources uses a closable and closes it when exiting the try block
         final Connection connection
               = JdbcConnectionFactory.getInstance().getConnection( jdbcDriver, jdbcUrl, jdbcUser, jdbcPass );
         connected = connection != null;
         _conceptTableInfos = createTableInfos( connection, conceptTables );
      } catch ( SQLException sqlE ) {
         if ( !connected ) {
            LOGGER.error( "Could not Connect to Concept Factory " + name );
         } else {
            LOGGER.error( "Could not create Concept Data Selection Call", sqlE );
         }
         throw sqlE;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Concept createConcept( final Long cuiCode ) {
      final CollectionMap<String, String, ? extends Collection<String>> codes = new HashSetMap<>();
      String prefTerm = null;
      for ( ConceptTableInfo conceptTableInfo : _conceptTableInfos ) {
         switch ( conceptTableInfo.__classType ) {
            case TUI_CLASS: {
               codes.addAllValues( conceptTableInfo.__conceptName,
                     getTuiCodes( conceptTableInfo.__preparedStatement, cuiCode ) );
               break;
            }
            case PREFTERM_CLASS: {
               prefTerm = getPreferredTerm( conceptTableInfo.__preparedStatement, cuiCode );
               break;
            }
            case INT_CLASS: {
               codes.addAllValues( conceptTableInfo.__conceptName,
                     getIntegerCodes( conceptTableInfo.__preparedStatement, cuiCode ) );
               break;
            }
            case LONG_CLASS: {
               codes.addAllValues( conceptTableInfo.__conceptName,
                     getLongCodes( conceptTableInfo.__preparedStatement, cuiCode ) );
               break;
            }
            case TEXT_CLASS: {
               codes.addAllValues( conceptTableInfo.__conceptName,
                     getStringCodes( conceptTableInfo.__preparedStatement, cuiCode ) );
               break;
            }
         }
      }
      return new DefaultConcept( CuiCodeUtil.getInstance().getAsCui( cuiCode ), prefTerm, codes );
   }

   /**
    * @param selectCall jdbc selection call
    * @param cuiCode    cui of interest
    * @return collection of tuis that are related to cui as obtained with the selectCall
    */
   static private Collection<String> getTuiCodes( PreparedStatement selectCall, final Long cuiCode ) {
      final Collection<String> codes = new HashSet<>();
      try {
         fillSelectCall( selectCall, cuiCode );
         final ResultSet resultSet = selectCall.executeQuery();
         while ( resultSet.next() ) {
            codes.add( TuiCodeUtil.getAsTui( resultSet.getInt( 2 ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return codes;
   }

   /**
    * @param selectCall jdbc selection call
    * @param cuiCode    cui of interest
    * @return preferred term for the cui as obtained with the selectCall
    */
   static private String getPreferredTerm( PreparedStatement selectCall, final Long cuiCode ) {
      String preferredName = "";
      try {
         fillSelectCall( selectCall, cuiCode );
         final ResultSet resultSet = selectCall.executeQuery();
         if ( resultSet.next() ) {
            preferredName = resultSet.getString( 2 );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return preferredName;
   }

   /**
    * @param selectCall jdbc selection call
    * @param cuiCode    cui of interest
    * @return collection of ints (as strings) that are related to cui as obtained with the selectCall
    */
   static private Collection<String> getIntegerCodes( PreparedStatement selectCall, final Long cuiCode ) {
      final Collection<String> codes = new HashSet<>();
      try {
         fillSelectCall( selectCall, cuiCode );
         final ResultSet resultSet = selectCall.executeQuery();
         while ( resultSet.next() ) {
            codes.add( Integer.toString( resultSet.getInt( 2 ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return codes;
   }

   /**
    * @param selectCall jdbc selection call
    * @param cuiCode    cui of interest
    * @return collection of longs (as strings) that are related to cui as obtained with the selectCall
    */
   static private Collection<String> getLongCodes( PreparedStatement selectCall, final Long cuiCode ) {
      final Collection<String> codes = new HashSet<>();
      try {
         fillSelectCall( selectCall, cuiCode );
         final ResultSet resultSet = selectCall.executeQuery();
         while ( resultSet.next() ) {
            codes.add( Long.toString( resultSet.getLong( 2 ) ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return codes;
   }

   /**
    * @param selectCall jdbc selection call
    * @param cuiCode    cui of interest
    * @return collection of strings that are related to cui as obtained with the selectCall
    */
   static private Collection<String> getStringCodes( PreparedStatement selectCall, final Long cuiCode ) {
      final Collection<String> codes = new HashSet<>();
      try {
         fillSelectCall( selectCall, cuiCode );
         final ResultSet resultSet = selectCall.executeQuery();
         while ( resultSet.next() ) {
            codes.add( resultSet.getString( 2 ) );
         }
         // Though the ResultSet interface documentation states that there are automatic closures,
         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
         resultSet.close();
      } catch ( SQLException e ) {
         LOGGER.error( e.getMessage() );
      }
      return codes;
   }

   /**
    * Creates table information objects with table name, concept name, jdbc prepared statement call
    * @param connection -
    * @param conceptTables map of table names to table value types
    * @return table information objects with table name, concept name, jdbc prepared statement call
    * @throws SQLException
    */
   static private Collection<ConceptTableInfo> createTableInfos( final Connection connection,
                                                                 final Map<String,String> conceptTables )
         throws SQLException {
      if ( conceptTables == null || conceptTables.isEmpty() ) {
         return Collections.emptyList();
      }
      final Collection<String> dbTablesNames = getDbTableNames( connection );
      final Collection<ConceptTableInfo> tableInfos = new ArrayList<>();
      for ( Map.Entry<String, String> conceptTable : conceptTables.entrySet() ) {
         String tableName = conceptTable.getKey().trim().toUpperCase();
         if ( !tableName.endsWith( TABLE_KEY_SUFFIX ) || tableName.length() < 6 ) {
            LOGGER.error( "Cannot have a concept table named " + tableName );
            continue;
         }
         if ( !dbTablesNames.contains( tableName ) ) {
            tableName = tableName.substring( 0, tableName.length() - 5 );
            if ( !dbTablesNames.contains( tableName ) ) {
               LOGGER.error( "Table " + tableName + TABLE_KEY_SUFFIX
                             + " and/or " + tableName + " not found in Database" );
               continue;
            }
         }
         final String tableClass = conceptTable.getValue().trim().toUpperCase();
         if ( tableClass.isEmpty()
              || (!tableClass.equals( TUI_CLASS ) && !tableClass.equals( PREFTERM_CLASS )
                  && !tableClass.equals( INT_CLASS ) && !tableClass.equals( LONG_CLASS )
                  && !tableClass.equals( TEXT_CLASS )) ) {
            LOGGER.error( "Cannot have a concept table with class " + tableClass );
            continue;
         }
         String conceptName = conceptTable.getKey().trim();
         conceptName = conceptName.substring( 0, conceptName.length() - 5 );
         final String lookupSql = "SELECT * FROM " + tableName + " WHERE CUI = ?";
         final PreparedStatement statement = connection.prepareStatement( lookupSql );
         tableInfos.add( new ConceptTableInfo( tableName, conceptName, tableClass, statement ) );
         LOGGER.info( "Connected to concept table " + tableName + " with class " + tableClass );
      }
      return tableInfos;
   }

   /**
    * @param connection -
    * @return all table names in the database
    * @throws SQLException if something goes wrong
    */
   static private Collection<String> getDbTableNames( final Connection connection ) throws SQLException {
      final DatabaseMetaData metadata = connection.getMetaData();
      final ResultSet resultSet = metadata.getTables( null, null, "%", null );
      final Collection<String> tableNames = new ArrayList<>();
      while ( resultSet.next() ) {
         tableNames.add( resultSet.getString( "TABLE_NAME" ).toUpperCase() );
      }
      resultSet.close();
      return tableNames;
   }



   /**
    * @param cuiCode -
    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
    */
   static private void fillSelectCall( final PreparedStatement selectCall, final Long cuiCode ) throws SQLException {
      selectCall.clearParameters();
      selectCall.setLong( 1, cuiCode );
   }


}
