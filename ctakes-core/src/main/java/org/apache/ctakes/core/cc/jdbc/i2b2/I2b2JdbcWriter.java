package org.apache.ctakes.core.cc.jdbc.i2b2;

import org.apache.ctakes.core.cc.jdbc.AbstractJCasJdbcWriter;
import org.apache.ctakes.core.cc.jdbc.db.JdbcDb;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.sql.SQLException;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
@PipeBitInfo(
      name = "I2b2JdbcWriter",
      description = "Writes UMLS Concepts to a standard I2B2 Observation_Fact table.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION
)
public class I2b2JdbcWriter extends AbstractJCasJdbcWriter {

   static private final Logger LOGGER = Logger.getLogger( "I2b2JdbcWriter" );

   static public final String PARAM_TABLE_NAME = "FactOutputTable";
   @ConfigurationParameter(
         name = PARAM_TABLE_NAME,
         description = "Name of the Observation_Fact table for writing output."
   )
   private String _tableName;

   static public final String PARAM_REPEAT_CUIS = "RepeatCuis";
   @ConfigurationParameter(
         name = PARAM_REPEAT_CUIS,
         description = "Repeat Concepts with the same Cui but possibly different Semantic Type or Preferred Text.",
         mandatory = false
   )
   private boolean _repeatCuis;

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Writing to Database ..." );
      super.process( jcas );
   }

   final protected String getTableName() {
      return _tableName;
   }

   final protected boolean isRepeatCuis() {
      return _repeatCuis;
   }

   protected JdbcDb createJdbcDb( final String driver,
                                        final String url,
                                        final String user,
                                        final String pass,
                                        final String keepAlive ) throws ResourceInitializationException {
      LOGGER.info( "Initializing connection to " + url + " ..." );
      final I2b2Db db = new I2b2Db( driver, url, user, pass, keepAlive );
      try {
         db.addObservationFact( getTableName(), isRepeatCuis() );
      } catch ( SQLException sqlE ) {
         throw new ResourceInitializationException( sqlE );
      }
      return db;
   }

}
