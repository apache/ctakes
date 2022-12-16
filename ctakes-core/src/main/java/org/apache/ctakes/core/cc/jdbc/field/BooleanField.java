package org.apache.ctakes.core.cc.jdbc.field;


import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
final public class BooleanField extends AbstractJdbcField<Boolean> {

   public BooleanField( final String name, final int index ) {
      super( name, index );
   }

   public void addToStatement( final PreparedStatement statement, final Boolean value ) throws SQLException {
      statement.setBoolean( getIndex(), value );
   }

}
