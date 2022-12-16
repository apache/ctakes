package org.apache.ctakes.core.cr.jdbc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.structured.Demographics;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.structured.Metadata;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import sqlWrapper.WrappedConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;


/**
 * Collection Reader that pulls documents to be processed from a database.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/24/2018
 */
@PipeBitInfo(
      name = "JDBC Note Table Reader",
      description = "Reads document texts from database table's fields.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
final public class JdbcNotesReader extends JCasCollectionReader_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "JdbcNoteTableReader" );


   static public final String PARAM_DB_DRIVER = "DbDriver";
   @ConfigurationParameter(
         name = PARAM_DB_DRIVER,
         description = "JDBC driver ClassName."
   )
   private String _dbDriver;

   static public final String PARAM_DB_DECRYPTOR = "DbDecryptor";
   @ConfigurationParameter(
         name = PARAM_DB_DECRYPTOR,
         description = "JDBC decryptor ClassName.",
         mandatory = false
   )
   private String _dbDecryptor;

   static public final String PARAM_DECRYPT_PASS = "DecryptPass";
   @ConfigurationParameter(
         name = PARAM_DECRYPT_PASS,
         description = "Password for text decryption.",
         mandatory = false
   )
   private String _decryptPass;

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

   static public final String PARAM_SQL = "SqlStatement";
   @ConfigurationParameter(
         name = PARAM_SQL,
         description = "SQL statement to retrieve the document."
   )
   private String _sqlStatement;


   /**
    * Name of column from resultset that contains the document text. Supported
    * column types are CHAR, VARCHAR, and CLOB.
    */
   static public final String PARAM_DOCTEXT_COL = "DocColumn";
   @ConfigurationParameter(
         name = PARAM_DOCTEXT_COL,
         description = "Name of column that contains the document text."
   )
   private String _docTextColumn;

   static public final String PARAM_DOCID_COLS = "IdColumns";
   @ConfigurationParameter(
         name = PARAM_DOCID_COLS,
         description = "Specifies column names that will be used to form a document ID.",
         mandatory = false
   )
   private String[] _docIdColumns;

   static public final String PARAM_DOCID_DELIMITER = "IdDelimiter";
   @ConfigurationParameter(
         name = PARAM_DOCID_DELIMITER,
         description = "Specifies delimiter used when document ID is built.",
         mandatory = false
   )
   private String _docIdDelimiter = "_";


   //Metadata
   static public final String PARAM_PATIENT_COLUMN = "PatientColumn";
   @ConfigurationParameter(
         name = PARAM_PATIENT_COLUMN,
         description = "Name of column that contains the patient identifier.",
         mandatory = false
   )
   private String _patientIdentifier;

   static public final String PARAM_PATIENT_ID = "PatientIdColumn";
   @ConfigurationParameter(
         name = PARAM_PATIENT_ID,
         description = "Name of column that contains the patient id.",
         mandatory = false
   )
   private String _patientId;


   // SourceData

   static public final String PARAM_NOTE_TYPE = "NoteTypeColumn";
   @ConfigurationParameter(
         name = PARAM_NOTE_TYPE,
         description = "Name of column that contains the note type.",
         mandatory = false
   )
   private String _noteTypeCode;

   static public final String PARAM_NOTE_SUBTYPE = "NoteSubtypeColumn";
   @ConfigurationParameter(
         name = PARAM_NOTE_SUBTYPE,
         description = "Name of column that contains the note subtype.",
         mandatory = false
   )
   private String _noteSubtypeCode;

   static public final String PARAM_SPECIALTY = "SpecialtyColumn";
   @ConfigurationParameter(
         name = PARAM_SPECIALTY,
         description = "Name of column that contains the author specialty.",
         mandatory = false
   )
   private String _authorSpecialty;

   static public final String PARAM_DOC_STANDARD = "StandardColumn";
   @ConfigurationParameter(
         name = PARAM_DOC_STANDARD,
         description = "Name of column that contains the document standard.",
         mandatory = false
   )
   private String _documentStandard;

   static public final String PARAM_INSTANCE_ID = "InstanceIdColumn";
   @ConfigurationParameter(
         name = PARAM_INSTANCE_ID,
         description = "Name of column that contains the document instance id.",
         mandatory = false
   )
   private String _sourceInstanceId;

   static public final String PARAM_REVISION = "RevisionColumn";
   @ConfigurationParameter(
         name = PARAM_REVISION,
         description = "Name of column that contains the document revision number.",
         mandatory = false
   )
   private String _sourceRevisionNumber;

   static public final String PARAM_REVISION_DATE = "RevisionDateColumn";
   @ConfigurationParameter(
         name = PARAM_REVISION_DATE,
         description = "Name of column that contains the document revision date.",
         mandatory = false
   )
   private String _sourceRevisionDate;

   static public final String PARAM_DATE_COLUMN = "DateColumn";
   @ConfigurationParameter(
         name = PARAM_DATE_COLUMN,
         description = "Name of column that contains the document original date.",
         mandatory = false
   )
   private String _sourceOriginalDate;

   static public final String PARAM_INSTITUTE = "InstituteColumn";
   @ConfigurationParameter(
         name = PARAM_INSTITUTE,
         description = "Name of column that contains the source institution.",
         mandatory = false
   )
   private String _sourceInstitution;

   static public final String PARAM_ENCOUNTER = "EncounterIdColumn";
   @ConfigurationParameter(
         name = PARAM_ENCOUNTER,
         description = "Name of column that contains the encounter id.",
         mandatory = false
   )
   private String _sourceEncounterId;

   //Demographics
   static public final String PARAM_BIRTHDAY = "BirthColumn";
   @ConfigurationParameter(
         name = PARAM_BIRTHDAY,
         description = "Name of column that contains the patient birth date.",
         mandatory = false
   )
   private String _birthDate;

   static public final String PARAM_DEATHDAY = "DeathColumn";
   @ConfigurationParameter(
         name = PARAM_DEATHDAY,
         description = "Name of column that contains the patient death date.",
         mandatory = false
   )
   private String _deathDate;

   static public final String PARAM_GENDER = "GenderColumn";
   @ConfigurationParameter(
         name = PARAM_GENDER,
         description = "Name of column that contains the patient gender.",
         mandatory = false
   )
   private String _gender;

   static public final String PARAM_FIRST_NAME = "FirstNameColumn";
   @ConfigurationParameter(
         name = PARAM_FIRST_NAME,
         description = "Name of column that contains the patient first name.",
         mandatory = false
   )
   private String _firstName;

   static public final String PARAM_MIDDLE_NAME = "MiddleNameColumn";
   @ConfigurationParameter(
         name = PARAM_MIDDLE_NAME,
         description = "Name of column that contains the patient middle name.",
         mandatory = false
   )
   private String _middleName;

   static public final String PARAM_LAST_NAME = "LastNameColumn";
   @ConfigurationParameter(
         name = PARAM_LAST_NAME,
         description = "Name of column that contains the patient last name.",
         mandatory = false
   )
   private String _lastName;

   static public final String PARAM_FIRST_SOUNDEX = "FirstSoundexColumn";
   @ConfigurationParameter(
         name = PARAM_FIRST_SOUNDEX,
         description = "Name of column that contains the patient first name soundex.",
         mandatory = false
   )
   private String _firstNameSoundex;

   static public final String PARAM_LAST_SOUNDEX = "LastSoundexColumn";
   @ConfigurationParameter(
         name = PARAM_LAST_SOUNDEX,
         description = "Name of column that contains the patient last name soundex.",
         mandatory = false
   )
   private String _lastNameSoundex;


   private Connection _connection;

   private Decryptor _decryptor;

   private PreparedStatement _preparedStatement;
   private ResultSet _resultSet;
   private int _docColumnType;
   private String _docColumnTypeName;

   private long _startMillis;
   private int _totalRowCount = 0;
   private int _rowIndex = 0;
   private String _docId;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      LOGGER.info( "Using Sql Statement:\n" + _sqlStatement );
      _connection = createConnection( _dbDriver, _url, _user, _pass, _keepAlive );
      _decryptor = createDecryptor( _dbDecryptor );
      _preparedStatement = createSqlStatement( _connection );
      _startMillis = System.currentTimeMillis();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean hasNext() throws IOException, CollectionException {
      if ( _resultSet == null ) {
         try {
            fillResultSet();
            setupDocColumnType();
         } catch ( SQLException sqlE ) {
            // thrown by createResultSet() and setupDocColumnType(), rethrow as declared CollectionException
            throw new CollectionException( sqlE );
         }
      }
      boolean hasAnotherRow;
      try {
         hasAnotherRow = _resultSet.next();
         if ( hasAnotherRow ) {
            _docId = createDocId();
         } else {
            // it's important to close ResultSets as they can accumulate
            // in the JVM heap. Too many open result sets can inadvertently
            // cause the DB conn to be closed by the server.
            _resultSet.close();
         }
      } catch ( SQLException sqlE ) {
         // thrown by ResultSet.next() and ResultSet.close()
         throw new CollectionException( sqlE );
      }
      return hasAnotherRow;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void getNext( final JCas jCas ) throws IOException, CollectionException {
      _rowIndex++;
      if ( jCas == null ) {
         throw new CollectionException( new NullPointerException( "Null CAS " + _rowIndex
                                                                  + " in " + getClass().getName() +
                                                                  ".getNext( JCAS )" ) );
      }
      // pull doc text from resultset - may throw IOException
      final String clobDocument = getClobDocument();
      // get the plain text version of the clob document
      final String document = getTextDocument( clobDocument );
      try {
         jCas.setDocumentText( document );
      } catch ( CASRuntimeException casRTE ) {
         // thrown by JCas.setDocumentText(..) , rethrow as declared CollectionException
         throw new CollectionException( casRTE );
      }
      // Put doc Id in the cas
      final DocumentID docIdAnnot = new DocumentID( jCas );
      docIdAnnot.setDocumentID( _docId );
      docIdAnnot.addToIndexes();
      LOGGER.info( "Reading document number " + _rowIndex + " with ID " + _docId );
      // Set the rest of the patient and doc info
      try {
         setMetadata( jCas );
      } catch ( SQLException sqlE ) {
         // thrown by setMetaData(..) inner calls to ResultSet.get*(..) , rethrow as declared IOException
         throw new IOException( sqlE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Progress[] getProgress() {
      final Progress p = new ProgressImpl( _rowIndex, _totalRowCount, Progress.ENTITIES );
      return new Progress[] { p };
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void close() throws IOException {
      final long totalMillis = System.currentTimeMillis() - _startMillis;
      final long totalSeconds = totalMillis / 1000l;
      final long hourSeconds = 60 * 60;
      final long daySeconds = 24 * hourSeconds;
      final long days = totalSeconds / daySeconds;
      final long hours = (totalSeconds - days * daySeconds) / hourSeconds;
      final long minutes = (totalSeconds - days * daySeconds - hours * hourSeconds) / 60;
      final long seconds = totalSeconds % 60;
      LOGGER.info( getClass().getName() + " read " + _totalRowCount + " documents in "
                   + days + " days, " + hours + " hours, " + minutes + " minutes and " + seconds + " seconds" );
      try {
         if ( _resultSet != null && !_resultSet.isClosed() ) {
            // Some jdbc drivers may not close the ResultSet when the PreparedStatement is closed
            _resultSet.close();
         }
         if ( _preparedStatement != null && !_preparedStatement.isClosed() ) {
            _preparedStatement.close();
         }
      } catch ( SQLException sqlE ) {
         // thrown by ResultSet.close() and Statement.close()
         // rethrow as IOException to fit the declared exception type
         throw new IOException( sqlE );
      }
   }

   /**
    * @param connection -
    * @return a prepared statement
    * @throws ResourceInitializationException -
    */
   private PreparedStatement createSqlStatement( final Connection connection ) throws ResourceInitializationException {
      try {
         _preparedStatement = connection.prepareStatement( _sqlStatement );
         _totalRowCount = getTotalRowCount( connection, _sqlStatement );
      } catch ( SQLException sqlE ) {
         // thrown by Connection.prepareStatement(..) and getTotalRowCount(..)
         LOGGER.error( "Could not interact with Database" );
         throw new ResourceInitializationException( sqlE );
      }
      return _preparedStatement;
   }

   /**
    * Slice up the query SQL and rebuild a SQL statement that gets a row count;
    *
    * @param connection -
    * @param querySql   the sql specified by the user for the full data fetch.
    * @return total row count
    * @throws SQLException -
    */
   private int getTotalRowCount( final Connection connection, final String querySql ) throws SQLException {
      final PreparedStatement countStatement = createCountSql( connection, querySql );
      int totalRowCount;
      totalRowCount = getTotalRowCount( countStatement );
      if ( !countStatement.isClosed() ) {
         countStatement.close();
      }
      LOGGER.info( "Processing row count:" + totalRowCount );
      return totalRowCount;
   }

   /**
    * Slice up the query SQL and rebuild a SQL statement that gets a row count;
    *
    * @param connection -
    * @param querySql   the sql specified by the user for the full data fetch.
    * @return a select count statement
    * @throws SQLException -
    */
   static private PreparedStatement createCountSql( final Connection connection,
                                                    final String querySql ) throws SQLException {
      final StringBuilder sb = new StringBuilder();
      sb.append( "SELECT COUNT(*) " );
      final int fromIndex = querySql.toUpperCase().indexOf( "FROM" );
      sb.append( querySql.subSequence( fromIndex, querySql.length() ) );
      return connection.prepareStatement( sb.toString() );
   }

   /**
    * @param countStatement a select count statement
    * @return number of rows that satisfy the user's select call
    * @throws SQLException -
    */
   static private int getTotalRowCount( final PreparedStatement countStatement ) throws SQLException {
      final ResultSet resultSet = countStatement.executeQuery();
      resultSet.next();
      final int count = resultSet.getInt( 1 );
      // Some jdbc drivers may not close the ResultSet when the PreparedStatement is closed
      resultSet.close();
      countStatement.close();
      return count;
   }

   /**
    * Fetches all of the data from the db
    *
    * @throws SQLException -
    */
   private void fillResultSet() throws SQLException {
      LOGGER.info( "SQL: " + _preparedStatement.toString() );
      _resultSet = _preparedStatement.executeQuery();
   }

   /**
    * Attempts to automatically determine the datatype of the column containing document text.
    *
    * @throws SQLException -
    */
   private void setupDocColumnType() throws SQLException {
      final ResultSetMetaData rsMetaData = _resultSet.getMetaData();
      final int colIdx = _resultSet.findColumn( _docTextColumn );
      _docColumnType = rsMetaData.getColumnType( colIdx );
      _docColumnTypeName = rsMetaData.getColumnTypeName( colIdx );
   }


   /**
    * Builds a document ID from one or more pieces of query data.
    * If the query data is not specified OR if an SQLException is caught, the next row # is used.
    * This method should not throw an exception that stops the entire run when a row index can be used as an identifier
    *
    * @return document ID
    */
   private String createDocId() {
      if ( _docIdColumns == null ) {
         return String.valueOf( _rowIndex + 1 );
      }
      final StringBuilder sb = new StringBuilder();
      // use flag to determine the first iteration in the loop, used for delimiter
      boolean firstColumn = true;
      try {
         for ( String columnName : _docIdColumns ) {
            if ( !firstColumn ) {
               sb.append( _docIdDelimiter );
            } else {
               firstColumn = false;
            }
            final String columnValue = _resultSet.getObject( columnName ).toString();
            sb.append( columnValue );
         }
      } catch ( SQLException sqlE ) {
         // thrown by ResultSet.getObject(..) and should be handled in this method createDocumentID(..)
         // do not throw an exception here if there is default behavior, which is to use row number
         return String.valueOf( _rowIndex );
      }
      return sb.toString();

   }

   /**
    * @return raw document text
    * @throws IOException -
    */
   private String getClobDocument() throws IOException {
      // pull doc text from resultset
      String document;
      try {
         if ( _docColumnType == Types.CHAR || _docColumnType == Types.VARCHAR ) {
            document = _resultSet.getString( _docTextColumn );
         } else if ( _docColumnType == Types.CLOB ) {
            document = convertToString( _resultSet.getClob( _docTextColumn ) );
         } else {
            if ( !_docColumnTypeName.equals( "text" ) ) {
               LOGGER.warn( "Inferring document text column as string type: " + _docColumnTypeName );
            }
            document = _resultSet.getString( _docTextColumn );
         }
      } catch ( SQLException sqlE ) {
         // thrown by ResultSet.getString(..) and ResultSet.getClob(..) and convertToString(..)
         // rethrow as declared IOException
         throw new IOException( sqlE );
         // IOException thrown by convertToString(..) , ignoring as it will be passed through as declared
      }
      return document;
   }

   /**
    * @param clobDocument raw document text
    * @return decrypted document text
    * @throws IOException -
    */
   private String getTextDocument( final String clobDocument ) throws IOException {
      if ( _decryptPass == null || _decryptPass.trim().isEmpty() ) {
         // Assume that the clob document is not encrypted
         return clobDocument;
      }
      //Decrypt the encrypted doc
      try {
         return _decryptor.decrypt( _decryptPass, clobDocument );
      } catch ( Exception e ) {
         // raw Exception thrown by decrypt(..) , rethrow as declared IOException
         throw new IOException( e );
      }
   }

   /**
    * Loads the clob data into a String object.
    *
    * @param clob -
    * @return clob as single string with \n as line separator
    * @throws SQLException -
    * @throws IOException  -
    */
   static private String convertToString( final Clob clob ) throws SQLException, IOException {
      final StringBuilder sb = new StringBuilder();
      final BufferedReader br = new BufferedReader( clob.getCharacterStream() );
      String line = br.readLine();
      while ( line != null ) {
         sb.append( line );
         sb.append( '\n' );
         line = br.readLine();
      }
      br.close();
      return sb.toString();
   }


   /**
    * Fill the document metadata information
    *
    * @param jCas ye olde ...
    * @throws SQLException -
    */
   private void setMetadata( final JCas jCas ) throws SQLException {
      final Metadata metadata = new Metadata( jCas );
      metadata.setPatientIdentifier( getResult( _patientIdentifier ) );
      final Long patientId = getResultLong( _patientId );
      if ( patientId != null ) {
         metadata.setPatientID( patientId );
      }

      final SourceData sourcedata = createSourceData( jCas );
      metadata.setSourceData( sourcedata );

      final Demographics demographics = createDemographics( jCas );
      metadata.setDemographics( demographics );

      jCas.addFsToIndexes( metadata );
   }

   /**
    * @param jCas ye olde ...
    * @return data about note source
    * @throws SQLException -
    */
   private SourceData createSourceData( final JCas jCas ) throws SQLException {
      final SourceData sourcedata = new SourceData( jCas );
      sourcedata.setNoteTypeCode( getResult( _noteTypeCode ) );
      sourcedata.setNoteSubTypeCode( getResult( _noteSubtypeCode ) );
      sourcedata.setAuthorSpecialty( getResult( _authorSpecialty ) );
      sourcedata.setDocumentStandard( getResult( _documentStandard ) );
      sourcedata.setSourceInstanceId( getResult( _sourceInstanceId ) );
      final Integer revision = getResultInt( _sourceRevisionNumber );
      if ( revision != null ) {
         sourcedata.setSourceRevisionNbr( revision );
      }
      final Timestamp revisionDate = getResultDate( _sourceRevisionDate );
      if ( revisionDate != null ) {
         sourcedata.setSourceRevisionDate( revisionDate.toString() );
      }
      final Timestamp originalDate = getResultDate( _sourceOriginalDate );
      if ( originalDate != null ) {
         sourcedata.setSourceOriginalDate( originalDate.toString() );
      }
      sourcedata.setSourceInstitution( getResult( _sourceInstitution ) );
      sourcedata.setSourceEncounterId( getResult( _sourceEncounterId ) );
      return sourcedata;
   }

   /**
    * @param jCas ye olde ...
    * @return data about note patient
    * @throws SQLException -
    */
   private Demographics createDemographics( final JCas jCas ) throws SQLException {
      final Demographics demographics = new Demographics( jCas );
      final Timestamp birthDate = getResultDate( _birthDate );
      if ( birthDate != null ) {
         demographics.setBirthDate( birthDate.toString() );
      }
      final Timestamp deathDate = getResultDate( _deathDate );
      if ( deathDate != null ) {
         demographics.setDeathDate( deathDate.toString() );
      }
      demographics.setGender( getResult( _gender ) );
      demographics.setFirstName( getResult( _firstName ) );
      demographics.setMiddleName( getResult( _middleName ) );
      demographics.setLastName( getResult( _lastName ) );
      demographics.setFirstNameSoundex( getResult( _firstNameSoundex ) );
      demographics.setLastNameSoundex( getResult( _lastNameSoundex ) );
      return demographics;
   }

   /**
    * @param column column name
    * @return text in column or empty if column name is actually not specified
    * @throws SQLException -
    */
   private String getResult( final String column ) throws SQLException {
      if ( column == null || column.isEmpty() ) {
         return "";
      }
      return _resultSet.getString( column );
   }

   /**
    * @param column column name
    * @return int in column or null if column name is actually not specified
    * @throws SQLException -
    */
   private Integer getResultInt( final String column ) throws SQLException {
      if ( column == null || column.isEmpty() ) {
         return null;
      }
      return _resultSet.getInt( column );
   }

   /**
    * @param column column name
    * @return long in column or null if column name is actually not specified
    * @throws SQLException -
    */
   private Long getResultLong( final String column ) throws SQLException {
      if ( column == null || column.isEmpty() ) {
         return null;
      }
      return _resultSet.getLong( column );
   }

   /**
    * @param column column name
    * @return date and time in column or null if column name is actually not specified
    * @throws SQLException -
    */
   private Timestamp getResultDate( final String column ) throws SQLException {
      if ( column == null || column.isEmpty() ) {
         return null;
      }
      return _resultSet.getTimestamp( column );
   }

   /**
    * @param driver    -
    * @param url       -
    * @param user      -
    * @param pass      -
    * @param keepAlive -
    * @return a connection to the database
    * @throws ResourceInitializationException -
    */
   static private Connection createConnection( final String driver,
                                               final String url,
                                               final String user,
                                               final String pass,
                                               final String keepAlive ) throws ResourceInitializationException {
      final Object[] emptyObjectArray = new Object[ 0 ];
      try {
         if ( keepAlive != null && !keepAlive.isEmpty() && Boolean.valueOf( keepAlive ) ) {
            return new WrappedConnection( user, pass, driver, url );
         }
         final Class driverClass = Class.forName( driver );
         return DriverManager.getConnection( url, user, pass );
      } catch ( ClassNotFoundException | SQLException multE ) {
         throw new ResourceInitializationException( "Could not construct " + driver,
               emptyObjectArray, multE );
      }
   }

   /**
    * @param decryptorClassName user-specified class name for a decrypter
    * @return a class that can be used to decrypt notes or the PassThroughDecryptor if the user didn't specify one.
    * @throws ResourceInitializationException -
    */
   static private Decryptor createDecryptor( final String decryptorClassName ) throws ResourceInitializationException {
      if ( decryptorClassName == null || decryptorClassName.isEmpty() ) {
         return new PassThroughDecryptor();
      }
      final Object[] emptyObjectArray = new Object[ 0 ];
      Class decryptorClass;
      try {
         decryptorClass = Class.forName( decryptorClassName );
      } catch ( ClassNotFoundException cnfE ) {
         throw new ResourceInitializationException( "Unknown class " + decryptorClassName, emptyObjectArray, cnfE );
      }
      if ( !Decryptor.class.isAssignableFrom( decryptorClass ) ) {
         return createWrappedDecryptor( decryptorClass );
      }
      final Constructor[] constructors = decryptorClass.getConstructors();
      for ( Constructor constructor : constructors ) {
         try {
            if ( constructor.getParameterTypes().length == 0 ) {
               return (Decryptor)constructor.newInstance();
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException multE ) {
            throw new ResourceInitializationException( "Could not construct " + decryptorClassName,
                  emptyObjectArray, multE );
         }
      }
      throw new ResourceInitializationException( "No Constructor for " + decryptorClassName, emptyObjectArray );
   }

   /**
    * @param decryptorThingClass -
    * @return a decryptor
    * @throws ResourceInitializationException -
    */
   static private Decryptor createWrappedDecryptor( final Class decryptorThingClass )
         throws ResourceInitializationException {
      final Object[] emptyObjectArray = new Object[ 0 ];
      final Class[] methodParameters = { String.class, String.class };
      Method decryptMethod;
      try {
         decryptMethod = decryptorThingClass.getDeclaredMethod( "decrypt", methodParameters );
         if ( !decryptMethod.getReturnType().equals( String.class ) ) {
            throw new ResourceInitializationException( decryptorThingClass.getName()
                                                       + ".decrypt( key, note ) method does not return text",
                  emptyObjectArray );
         }
      } catch ( NoSuchMethodException nsmE ) {
         throw new ResourceInitializationException( decryptorThingClass.getName()
                                                    + " has no decrypt( key, note ) method",
               emptyObjectArray );
      }
      final Constructor[] constructors = decryptorThingClass.getConstructors();
      for ( Constructor constructor : constructors ) {
         try {
            if ( constructor.getParameterTypes().length == 0 ) {
               LOGGER.info( "Wrapping " + decryptorThingClass.getName() + " in a Decryptor" );
               return new DecryptorWrapper( constructor.newInstance(), decryptMethod );
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException multE ) {
            throw new ResourceInitializationException( "Could not construct " + decryptorThingClass.getName(),
                  emptyObjectArray, multE );
         }
      }
      throw new ResourceInitializationException( "No Constructor for " + decryptorThingClass.getName(),
            emptyObjectArray );
   }


}
