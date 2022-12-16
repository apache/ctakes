package org.apache.ctakes.core.cc.jdbc;


import org.apache.ctakes.core.cc.jdbc.db.JdbcDb;
import org.apache.ctakes.core.cc.jdbc.table.JdbcTable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.sql.SQLException;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/12/2019
 */
abstract public class AbstractJdbcWriter<T> extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "AstractJdbcWriter" );

   static public final String PARAM_DB_DRIVER = "DbDriver";
   @ConfigurationParameter(
         name = PARAM_DB_DRIVER,
         description = "JDBC driver ClassName."
   )
   private String _dbDriver;

   static public final String PARAM_DB_URL = "DbUrl";
   @ConfigurationParameter(
         name = PARAM_DB_URL,
         description = "JDBC URL that specifies database network location and name."
   )
   private String _url;

   static public final String PARAM_DB_USER = "DbUser";
   @ConfigurationParameter(
         name = PARAM_DB_USER,
         description = "Username for database authentication."
   )
   private String _user;

   static public final String PARAM_DB_PASS = "DbPass";
   @ConfigurationParameter(
         name = PARAM_DB_PASS,
         description = "Password for database authentication."
   )
   private String _pass;

   static public final String PARAM_KEEP_ALIVE = "KeepAlive";
   @ConfigurationParameter(
         name = PARAM_KEEP_ALIVE,
         description = "Flag that determines whether to keep JDBC connection open no matter what.",
         mandatory = false
   )
   private String _keepAlive;

   static public final String PARAM_BATCH_SIZE = "BatchSize";
   @ConfigurationParameter(
         name = PARAM_BATCH_SIZE,
         description = "Number of statements to use in a batch.  0 or 1 denotes that batches should not be used.",
         mandatory = false
   )
   private String _batchSize;
// TODO Should batchSize be an int ?  Are we getting an exception from ConfigurationParameter?


   // Maximum row count for prepared statement batches
   static private final int MAX_BATCH_SIZE = 256;

   static private final Object DATA_LOCK = new Object();


   private JdbcDb _jdbcDb;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _jdbcDb = createJdbcDb( _dbDriver, _url, _user, _pass, _keepAlive );
      if ( _batchSize != null && !_batchSize.trim().isEmpty() ) {
         try {
            final int batchSize = Integer.decode( _batchSize.trim() );
            _jdbcDb.setBatchSize( batchSize );
         } catch ( NumberFormatException nfE ) {
            LOGGER.error( "Could not parse batch size " + _batchSize );
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      try {
         synchronized ( DATA_LOCK ) {
            createData( jcas );
            final T data = getData();
            if ( data != null ) {
               writeJdbc( data );
               writeComplete( data );
            }
         }
      } catch ( SQLException sqlE ) {
         throw new AnalysisEngineProcessException( sqlE );
      }
   }

   /**
    * Write any remaining patient information
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      try {
         _jdbcDb.close();
      } catch ( SQLException sqlE ) {
         LOGGER.error( sqlE.getMessage() );
      }
   }

   abstract protected JdbcDb createJdbcDb( final String driver,
                                           final String url,
                                           final String user,
                                           final String pass,
                                           final String keepAlive ) throws ResourceInitializationException;

   /**
    * @param jCas the jcas passed to the process( jcas ) method.
    */
   abstract protected void createData( JCas jCas );

   /**
    * @return the data to be written.
    */
   abstract protected T getData();

   /**
    * called after writing is complete
    *
    * @param data -
    */
   abstract protected void writeComplete( T data );

   /**
    * Write information into a table.
    *
    * @param data data to be written.
    * @throws SQLException if anything goes wrong.
    */
   public void writeJdbc( final T data ) throws SQLException {
      for ( JdbcTable<T> table : getJdbcTables( (Class<T>)data.getClass() ) ) {
         table.writeValue( data );
      }
   }

   /**
    * The JdbcDb used by this writer can have multiple tables for various datatypes.
    *
    * @param dataClass class of data for this writer.
    * @return tables in database that can use this data class.
    */
   private Collection<JdbcTable<T>> getJdbcTables( final Class<T> dataClass ) {
      return _jdbcDb.getTables( dataClass );
   }


}
