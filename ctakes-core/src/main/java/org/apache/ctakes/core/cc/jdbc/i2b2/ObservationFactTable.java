package org.apache.ctakes.core.cc.jdbc.i2b2;


import org.apache.ctakes.core.cc.jdbc.row.JdbcRow;
import org.apache.ctakes.core.cc.jdbc.table.AbstractUmlsTable;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import static org.apache.ctakes.core.cc.jdbc.i2b2.ObservationFactTable.CorpusSettings;


/**
 * For the format of the I2B2 Observation_Fact table,
 * see https://www.i2b2.org/software/projects/datarepo/CRC_Design_Doc_13.pdf
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
public class ObservationFactTable extends AbstractUmlsTable<CorpusSettings> {


   private final CorpusSettings _corpusSettings;

   public ObservationFactTable( final Connection connection,
                                final String tableName,
                                final boolean repeatCuis,
                                final CorpusSettings corpusSettings ) throws SQLException {
      super( connection, tableName, repeatCuis );
      _corpusSettings = corpusSettings;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected CorpusSettings getCorpusInitializer( final JCas jCas ) {
      return _corpusSettings;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public JdbcRow<CorpusSettings, JCas, JCas, IdentifiedAnnotation, UmlsConcept> createJdbcRow() {
      return new ObservationFactRow();
   }


   static public class CorpusSettings {
      public enum Marker {
         MARK_NEGATED, MARK_UNCERTAIN, MARK_GENERIC;
      }

      final Collection<Marker> _markers;

      public CorpusSettings( final Marker... markers ) {
         _markers = Arrays.asList( markers );
      }

      boolean markNegated() {
         return _markers.contains( Marker.MARK_NEGATED );
      }

      boolean markUncertain() {
         return _markers.contains( Marker.MARK_UNCERTAIN );
      }

      boolean markGeneric() {
         return _markers.contains( Marker.MARK_GENERIC );
      }
   }

}
