package org.apache.ctakes.core.cc.jdbc.table;


import org.apache.ctakes.core.cc.jdbc.row.JdbcRow;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Table that has one row per UmlsConcept in the JCas.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/14/2019
 */
abstract public class AbstractUmlsTable<C> extends AbstractJdbcTable<JCas> {

   private JdbcRow<C, JCas, JCas, IdentifiedAnnotation, UmlsConcept> _jdbcRow;
   private final boolean _repeatCuis;

   public AbstractUmlsTable( final Connection connection, final String tableName, final boolean repeatCuis )
         throws SQLException {
      super( connection, tableName );
      _repeatCuis = repeatCuis;
   }

   /**
    * @param jCas ye olde ...
    * @return some class that can be used to initialize the table.
    */
   abstract protected C getCorpusInitializer( final JCas jCas );

   /**
    * @return some row that uses JCas, Identified annotation, and a UmlsConcept per row.
    */
   abstract protected JdbcRow<C, JCas, JCas, IdentifiedAnnotation, UmlsConcept> createJdbcRow();

   /**
    * {@inheritDoc}
    */
   @Override
   final public Class<JCas> getDataType() {
      return JCas.class;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public JdbcRow<C, JCas, JCas, IdentifiedAnnotation, UmlsConcept> getJdbcRow() {
      if ( _jdbcRow == null ) {
         _jdbcRow = createJdbcRow();
      }
      return _jdbcRow;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public void writeValue( final JCas value ) throws SQLException {
      final PreparedStatement statement = getPreparedStatement();
      final C corpusInitializer = getCorpusInitializer( value );
      final JdbcRow<C, JCas, JCas, IdentifiedAnnotation, UmlsConcept> row = getJdbcRow();

      row.initializeCorpus( corpusInitializer );
      row.initializePatient( value );
      row.initializeDocument( value );

      final Collection<UmlsConcept> removals = new ArrayList<>();
      final Collection<String> cuis = new HashSet<>();
      boolean batchWritten = false;
      final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( value, IdentifiedAnnotation.class );
      if ( annotations.isEmpty() ) {
         return;
      }
      int rowCount = 0;
      for ( IdentifiedAnnotation annotation : annotations ) {
         row.initializeEntity( annotation );
         final Collection<UmlsConcept> umlsConcepts = OntologyConceptUtil.getUmlsConcepts( annotation );
         if ( !_repeatCuis && umlsConcepts.size() > 1 ) {
            for ( UmlsConcept concept : umlsConcepts ) {
               if ( cuis.contains( concept.getCui() ) ) {
                  removals.add( concept );
               }
               cuis.add( concept.getCui() );
            }
            umlsConcepts.removeAll( removals );
            removals.clear();
            cuis.clear();
         }
         for ( UmlsConcept concept : umlsConcepts ) {
            row.addToStatement( statement, concept );
            batchWritten = writeRow();
            rowCount++;
         }
      }
      if ( !batchWritten && rowCount > 0 ) {
         // The current batch has not been written to db.  Do so now.
         statement.executeBatch();
         statement.clearBatch();
      }
   }


}
