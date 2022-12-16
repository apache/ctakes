package org.apache.ctakes.core.cc.jdbc.field;


import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
final public class IntField extends AbstractJdbcField<Integer> {

   public IntField( final String name, final int index ) {
      super( name, index );
   }

   public void addToStatement( final PreparedStatement statement, final Integer value ) throws SQLException {
      statement.setInt( getIndex(), value );
   }

}
