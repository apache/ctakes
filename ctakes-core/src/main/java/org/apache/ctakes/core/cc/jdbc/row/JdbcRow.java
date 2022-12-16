package org.apache.ctakes.core.cc.jdbc.row;

import org.apache.ctakes.core.cc.jdbc.field.JdbcField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/14/2019
 */
public interface JdbcRow<C, P, D, E, T> {

   Collection<JdbcField<?>> getFields();

   default void initializeCorpus( final C corpusValue ) {
   }

   default void initializePatient( final P patientValue ) {
   }

   default void initializeDocument( final D documentValue ) {
   }

   default void initializeEntity( final E entityValue ) {
   }

   void addToStatement( final PreparedStatement statement, final T value ) throws SQLException;


}
