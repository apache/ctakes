package org.apache.ctakes.core.cc.jdbc.field;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
public interface JdbcField<T> {

   String getName();

   int getIndex();

   void addToStatement( final PreparedStatement statement, final T value ) throws SQLException;

}
