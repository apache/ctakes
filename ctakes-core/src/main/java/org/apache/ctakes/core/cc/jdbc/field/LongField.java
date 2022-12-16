package org.apache.ctakes.core.cc.jdbc.field;


import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
final public class LongField extends AbstractJdbcField<Long> {

   public LongField( final String name, final int index ) {
      super( name, index );
   }

   public void addToStatement( final PreparedStatement statement, final Long value ) throws SQLException {
      statement.setLong( getIndex(), value );
   }

}
