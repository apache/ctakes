package org.apache.ctakes.core.cc.jdbc.table;


import org.apache.ctakes.core.cc.jdbc.field.JdbcField;
import org.apache.ctakes.core.cc.jdbc.row.JdbcRow;

import java.sql.PreparedStatement;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
public interface JdbcTable<T> {

   String getTableName();

   Class<T> getDataType();

   JdbcRow<?, ?, ?, ?, ?> getJdbcRow();

   PreparedStatement getPreparedStatement();

   /**
    * @param batchSize batch size limit after which the batch is written to the db table.
    */
   void setBatchSize( final int batchSize );

   /**
    * @return batch size limit after which the batch is written to the db table.
    */
   int getBatchSize();

   default Collection<JdbcField<?>> getFields() {
      return getJdbcRow().getFields();
   }

   void writeValue( final T value ) throws SQLException;

   /**
    * @return -
    * @throws SQLDataException -
    */
   default String createRowInsertSql() throws SQLDataException {
      final List<JdbcField<?>> fields = new ArrayList<>( getFields() );
      if ( fields.isEmpty() ) {
         throw new SQLDataException( "Must set at least one Field to create an sql insert Statement" );
      }
      fields.sort( Comparator.comparingInt( JdbcField::getIndex ) );
      final StringBuilder statement = new StringBuilder( "insert into" );
      final StringBuilder queries = new StringBuilder();
      statement.append( " " ).append( getTableName() );
      statement.append( " (" );
      for ( JdbcField<?> field : fields ) {
         statement.append( field.getName() ).append( "," );
         queries.append( "?," );
      }
      // remove the last comma
      statement.setLength( statement.length() - 1 );
      queries.setLength( queries.length() - 1 );
      statement.append( ") values (" ).append( queries ).append( ")" );
      return statement.toString();
   }

   default void close() throws SQLException {
      getPreparedStatement().close();
   }

}
