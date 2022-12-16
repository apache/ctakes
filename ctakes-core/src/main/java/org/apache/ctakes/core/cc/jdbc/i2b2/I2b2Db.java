package org.apache.ctakes.core.cc.jdbc.i2b2;

import org.apache.ctakes.core.cc.jdbc.db.AbstractJdbcDb;
import org.apache.log4j.Logger;
import org.apache.uima.resource.ResourceInitializationException;

import java.sql.SQLException;

import static org.apache.ctakes.core.cc.jdbc.i2b2.ObservationFactTable.CorpusSettings;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/15/2019
 */
public class I2b2Db extends AbstractJdbcDb {

   static private final Logger LOGGER = Logger.getLogger( "I2b2Db" );

   public I2b2Db( final String driver,
                  final String url,
                  final String user,
                  final String pass,
                  final String keepAlive ) throws ResourceInitializationException {
      super( driver, url, user, pass, keepAlive );
   }

   /**
    * @param tableName name of the output observation fact table.
    * @return observation fact table with only negation marked by a negative (-) sign before concept codes.
    * @throws SQLException -
    */
   public ObservationFactTable addObservationFact( final String tableName, final boolean repeatCuis )
         throws SQLException {
      final ObservationFactTable.CorpusSettings settings
            = new ObservationFactTable.CorpusSettings( ObservationFactTable.CorpusSettings.Marker.MARK_NEGATED );
      return addObservationFact( tableName, repeatCuis, settings );
   }

   /**
    * {@inheritDoc}
    */
//   @Override
   public ObservationFactTable addObservationFact( final String tableName,
                                                      final boolean repeatCuis,
                                                      final CorpusSettings corpusSettings ) throws SQLException {
      final ObservationFactTable table
            = new ObservationFactTable( getConnection(), tableName, repeatCuis, corpusSettings );
      addTable( table );
      LOGGER.info( "Added Observation Fact table " + tableName );
      return table;
   }

}
