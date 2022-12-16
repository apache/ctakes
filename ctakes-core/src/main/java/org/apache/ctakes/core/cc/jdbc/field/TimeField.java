package org.apache.ctakes.core.cc.jdbc.field;


import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
final public class TimeField extends AbstractJdbcField<Timestamp> {

   public TimeField( final String name, final int index ) {
      super( name, index );
   }

   public void addToStatement( final PreparedStatement statement, final Timestamp value ) throws SQLException {
      statement.setTimestamp( getIndex(), value );
   }

}
