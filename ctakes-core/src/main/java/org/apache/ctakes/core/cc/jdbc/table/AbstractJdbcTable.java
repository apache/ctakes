package org.apache.ctakes.core.cc.jdbc.table;


import javax.annotation.concurrent.NotThreadSafe;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
@NotThreadSafe
abstract public class AbstractJdbcTable<T> implements JdbcTable<T> {

   static private final int DEFAULT_BATCH_LIMIT = 256;

   private final String _tableName;
   private final PreparedStatement _preparedStatement;
   private int _batchSize = DEFAULT_BATCH_LIMIT;
   private int _batchIndex = 0;

   public AbstractJdbcTable( final Connection connection, final String tableName ) throws SQLException {
      _tableName = tableName;
      final String sql = createRowInsertSql();
      _preparedStatement = connection.prepareStatement( sql );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public String getTableName() {
      return _tableName;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public PreparedStatement getPreparedStatement() {
      return _preparedStatement;
   }

   /**
    * @param batchSize batch size limit after which the batch is written to the db table.  Max 10,000.  0 or 1 disable batching.
    */
   @Override
   final public void setBatchSize( final int batchSize ) {
      if ( batchSize >= 0 && batchSize <= 10000 ) {
         _batchSize = batchSize;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public int getBatchSize() {
      return _batchSize;
   }

   /**
    * @return true if the statement batch was written.
    * @throws SQLException -
    */
   protected boolean writeRow() throws SQLException {
      final PreparedStatement statement = getPreparedStatement();
      if ( _batchSize < 2 ) {
         // If the batch limit is 0 or 1 then write each row as it is populated.
         statement.execute();
         return true;
      }
      // Otherwise use a batch.
      statement.addBatch();
      _batchIndex++;
      if ( _batchIndex >= _batchSize ) {
         _batchIndex = 0;
         statement.executeBatch();
         statement.clearBatch();
         return true;
      }
      return false;
   }

}
