package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.resource.JdbcConnectionResource;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import java.io.IOException;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Write cas to a database using jdbc
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2015
 */
abstract public class AbstractJdbcWriter extends CasConsumer_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "AbstractJdbcWriter" );

   // Parameter names for the desc file
   static public final String PARAM_DB_CONN_RESRC = "DbConnResrcName";

   // Maximum row count for prepared statement batches
   static private final int MAX_BATCH_SIZE = 100;


   protected interface TableInfo {
      String getTableName();

      FieldInfo[] getFieldInfos();
   }

   protected interface FieldInfo {
      String getFieldName();

      int getFieldIndex();

      Class<?> getValueClass();
   }

   static protected class TableSqlInfo {
      final private PreparedStatement __preparedStatement;
      private int __batchCount;

      protected TableSqlInfo( final Connection connection, final TableInfo tableInfo ) throws SQLException {
         final String sql = createRowInsertSql( tableInfo.getTableName(), tableInfo.getFieldInfos() );
         __preparedStatement = connection.prepareStatement( sql );
      }

      protected PreparedStatement getPreparedStatement() {
         return __preparedStatement;
      }

      protected void setBatchCount( final int batchCount ) {
         __batchCount = batchCount;
      }

      protected int getBatchCount() {
         return __batchCount;
      }
   }


   final protected Map<String, TableSqlInfo> _tableSqlInfoMap = new HashMap<>();


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize() throws ResourceInitializationException {
      final String resourceName = (String)getConfigParameterValue( PARAM_DB_CONN_RESRC );
      JdbcConnectionResource resource;
      try {
         resource = (JdbcConnectionResource)getUimaContext().getResourceObject( resourceName );
      } catch ( ResourceAccessException raE ) {
         // thrown by UimaContext.getResourceObject(..)
         throw new ResourceInitializationException( raE );
      }
      final Connection connection = resource.getConnection();
      final Collection<TableInfo> tableInfos = getTableInfos();
      try {
         for ( TableInfo tableInfo : tableInfos ) {
            _tableSqlInfoMap.put( tableInfo.getTableName(), new TableSqlInfo( connection, tableInfo ) );
         }
      } catch ( SQLException sqlE ) {
         // thrown by Connection.prepareStatement(..)
         throw new ResourceInitializationException( sqlE );
      }
   }

   /**
    * {@inheritDoc}
    * closes the PreparedStatements
    */
   @Override
   public void collectionProcessComplete( ProcessTrace arg0 )
         throws ResourceProcessException, IOException {
      try {
         for ( TableSqlInfo tableSqlInfo : _tableSqlInfoMap.values() ) {
            tableSqlInfo.__preparedStatement.close();
         }
      } catch ( SQLException sqlE ) {
         LOGGER.warn( sqlE.getMessage() );
      }
      super.collectionProcessComplete( arg0 );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void processCas( final CAS aCAS ) throws ResourceProcessException {
      JCas jcas;
      try {
         jcas = aCAS.getJCas();
      } catch ( CASException casE ) {
         throw new ResourceProcessException( casE );
      }
      final SourceData sourceData = SourceMetadataUtil.getSourceData( jcas );
      if ( sourceData == null ) {
         LOGGER.error( "Missing source metadata for document." );
         return;
      }
      final long patientNum = SourceMetadataUtil.getPatientNum( jcas );
      final int encounterNum = SourceMetadataUtil.getEncounterNum( sourceData );
      final String providerId = SourceMetadataUtil.getProviderId( sourceData );
      final Timestamp startDate = SourceMetadataUtil.getStartDate( sourceData );
      try {

         writeJCasInformation( jcas, encounterNum, patientNum, providerId, startDate );

         for ( TableSqlInfo tableSqlInfo : _tableSqlInfoMap.values() ) {
            if ( tableSqlInfo.getBatchCount() > 0 ) {
               tableSqlInfo.getPreparedStatement().executeBatch();
               // Not all drivers automatically clearCollection the batch.  This is considered by some to be a feature, by most a bug.
               tableSqlInfo.getPreparedStatement().clearBatch();
               tableSqlInfo.setBatchCount( 0 );
            }
         }
      } catch ( SQLException sqlE ) {
         // thrown by PreparedStatement methods
         throw new ResourceProcessException( sqlE );
      }
   }


   /**
    * Called from initialize()
    *
    * @return Table Info Objects for all tables of interest
    */
   abstract protected Collection<TableInfo> getTableInfos();

   /**
    * The main "process" method, called from processCas
    *
    * @param jcas         -
    * @param encounterNum -
    * @param patientNum   -
    * @param providerId   -
    * @param startDate    -
    * @throws SQLException if implementations throw SQLException
    */
   abstract protected void writeJCasInformation( final JCas jcas, final int encounterNum,
                                                 final long patientNum, final String providerId,
                                                 final Timestamp startDate ) throws SQLException;


   /**
    * @return the map of table name to table sql info objects
    */
   protected Map<String, TableSqlInfo> getTableSqlInfoMap() {
      return _tableSqlInfoMap;
   }

   /**
    * This is a safety method to set values of fieldInfoMaps instead of doing a direct .put in the map.
    * an IllegalArgumentException will be thrown if the given value is not the same class type as what the given
    * FieldInfo wants
    *
    * @param fieldInfoMap map in which to set the value
    * @param fieldInfo    key
    * @param value        value
    */
   static protected void setFieldInfoValue( final Map<FieldInfo, Object> fieldInfoMap,
                                            final FieldInfo fieldInfo, final Object value ) {
      final Class<?> valueClass = fieldInfo.getValueClass();
      if ( !valueClass.isInstance( value ) ) {
         throw new IllegalArgumentException( "Invalid Value for Field " + fieldInfo.getFieldName() );
      }
      fieldInfoMap.put( fieldInfo, value );
   }

   /**
    * Adds a new row of values to a batch in the prepared statement.  If the number of rows hits a maximum size (100)
    * then the batch is executed.
    *
    * @param preparedStatement -
    * @param batchSize         the current batch row count in the prepared statement
    * @param fieldInfoMap      for row value assignment
    * @return new batchCount (incremented by one or reset to zero)
    * @throws SQLException if a PreparedStatement call throws one or if there is a type, value mismatch in fieldInfoMap
    */
   static protected int writeTableRow( final PreparedStatement preparedStatement, final int batchSize,
                                       final Map<? extends FieldInfo, Object> fieldInfoMap ) throws SQLException {
      for ( Map.Entry<? extends FieldInfo, Object> fieldInfoEntry : fieldInfoMap.entrySet() ) {
         final int fieldIndex = fieldInfoEntry.getKey().getFieldIndex();
         final Class<?> valueClass = fieldInfoEntry.getKey().getValueClass();
         final Object value = fieldInfoEntry.getValue();
         if ( valueClass.isAssignableFrom( String.class ) && String.class.isInstance( value ) ) {
            preparedStatement.setString( fieldIndex, (String)value );
         } else if ( valueClass.isAssignableFrom( Integer.class ) && Integer.class.isInstance( value ) ) {
            preparedStatement.setInt( fieldIndex, (Integer)value );
         } else if ( valueClass.isAssignableFrom( Long.class ) && Long.class.isInstance( value ) ) {
            preparedStatement.setLong( fieldIndex, (Long)value );
         } else if ( valueClass.isAssignableFrom( Float.class ) && Float.class.isInstance( value ) ) {
            preparedStatement.setFloat( fieldIndex, (Float)value );
         } else if ( valueClass.isAssignableFrom( Double.class ) && Double.class.isInstance( value ) ) {
            preparedStatement.setDouble( fieldIndex, (Double)value );
         } else if ( valueClass.isAssignableFrom( Boolean.class ) && Boolean.class.isInstance( value ) ) {
            preparedStatement.setBoolean( fieldIndex, (Boolean)value );
         } else if ( valueClass.isAssignableFrom( Timestamp.class ) && Timestamp.class.isInstance( value ) ) {
            preparedStatement.setTimestamp( fieldIndex, (Timestamp)value );
         } else {
            throw new SQLDataException( "Invalid Value Class for Field " + fieldInfoEntry.getKey().getFieldName() );
         }
      }
      preparedStatement.addBatch();
      if ( batchSize + 1 >= MAX_BATCH_SIZE ) {
         preparedStatement.executeBatch();
         // Not all drivers automatically clear the batch.  This is considered by some to be a feature, by most a bug.
         preparedStatement.clearBatch();
         return 0;
      }
      return batchSize + 1;
   }

   /**
    * @param tableName  -
    * @param fieldInfos -
    * @return -
    * @throws SQLDataException
    */
   static protected String createRowInsertSql( final String tableName,
                                               final FieldInfo... fieldInfos ) throws SQLDataException {
      if ( fieldInfos.length == 0 ) {
         throw new SQLDataException( "Must set at least one Field to create an sql insert Statement" );
      }
      final StringBuilder statement = new StringBuilder( "insert into" );
      final StringBuilder queries = new StringBuilder();
      statement.append( " " ).append( tableName );
      statement.append( " (" );
      for ( FieldInfo fieldInfo : fieldInfos ) {
         statement.append( fieldInfo.getFieldName() ).append( "," );
         queries.append( "?," );
      }
      // remove the last comma
      statement.setLength( statement.length() - 1 );
      queries.setLength( queries.length() - 1 );
      statement.append( ") values (" ).append( queries ).append( ")" );
      return statement.toString();
   }


}
