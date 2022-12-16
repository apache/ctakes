/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.core.resource.JdbcConnectionResource;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Collection Reader that pulls documents to be processed from a database.
 *
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "JDBC Reader",
      description = "Reads document texts from database text fields.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
public class JdbcCollectionReader extends JCasCollectionReader_ImplBase {

   // LOG4J logger based on class name
   private Logger logger = Logger.getLogger( getClass().getName() );

   /**
    * SQL statement to retrieve the document.
    */
   public static final String PARAM_SQL = "SqlStatement";
   @ConfigurationParameter(
         name = PARAM_SQL,
         description = "SQL statement to retrieve the document."
   )
   private String _sqlStatement;

   /**
    * Name of column from resultset that contains the document text. Supported
    * column types are CHAR, VARCHAR, and CLOB.
    */
   public static final String PARAM_DOCTEXT_COL = "DocTextColName";
   @ConfigurationParameter(
         name = PARAM_DOCTEXT_COL,
         description = "Name of column from resultset that contains the document text."
   )
   private String _docTextColName;

   /**
    * Name of external resource for database connection.
    */
   public static final String PARAM_DB_CONN_RESRC = "DbConnResrcName";
   @ConfigurationParameter(
         name = PARAM_DB_CONN_RESRC,
         description = "Name of external resource for database connection."
   )
   String _resrcName;

   /**
    * Optional parameter. Specifies column names that will be used to form a
    * document ID.  Optional, will remain null if not set.
    */
   public static final String PARAM_DOCID_COLS = "DocIdColNames";
   @ConfigurationParameter(
         name = PARAM_DOCID_COLS,
         description = "Specifies column names that will be used to form a document ID.",
         mandatory = false
   )
   private String[] _docIdColNames;


   /**
    * Optional parameter. Specifies delimiter used when document ID is built.  Default is an underscore.
    */
   public static final String PARAM_DOCID_DELIMITER = "DocIdDelimiter";
   @ConfigurationParameter(
         name = PARAM_DOCID_DELIMITER,
         description = "Specifies delimiter used when document ID is built.",
         mandatory = false
   )
   private String _docIdDelimiter = "_";


   /**
    * Optional parameter. Name of external resource for prepared statement
    * value file. Each line of this file represents prepared statement values
    * that will be used to substitute for the "?" placeholders. TAB character
    * \t is used to delimit the values on a single line. The prepared statement
    * will be called once for each line in this file.
    */
   public static final String PARAM_VALUE_FILE_RESRC = "ValueFileResrcName";
   @ConfigurationParameter(
         name = PARAM_VALUE_FILE_RESRC,
         description = "Name of external resource for prepared statement value file.",
         mandatory = false
   )
   private String _fileResrcName;


   private PreparedStatement _preparedStatement;
   private ResultSet _resultSet;

   private int _docColType;
   private String _docColTypeName;


   private int _totalRowCount = 0;
   private int _currRowCount = 0;

   // optional, will remain null if not set
   // Array of List objects. Each List objects represents a list of prepared
   // stmt values.
   private List<String>[] _prepStmtValArr = null;
   private int _prepStmtValArrIdx = 0;
   private boolean _usePrepStmtVals = false;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      try {
         final JdbcConnectionResource connectionResource
               = (JdbcConnectionResource)context.getResourceObject( _resrcName );
         final Connection connection = connectionResource.getConnection();
         _preparedStatement = connection.prepareStatement( _sqlStatement );

         if ( _fileResrcName != null && !_fileResrcName.trim().isEmpty() ) {
            FileResource fileResrc = (FileResource)getUimaContext().getResourceObject( _fileResrcName );
            if ( fileResrc != null ) {
               loadValueFile( fileResrc.getFile() );
               _usePrepStmtVals = true;
            } else {
               logger.error( "Failed to get " + _fileResrcName + " from ResourceManager" );
               throw new ResourceInitializationException();
            }
         }

         _totalRowCount = getRowCount( connection, _sqlStatement );
      } catch ( ResourceAccessException | SQLException | IOException multE ) {
         throw new ResourceInitializationException( multE );
      }
   }

   /**
    * Loads the prepared statement value file.
    *
    * @param valueFile -
    * @throws IOException -
    */
   private void loadValueFile( File valueFile ) throws IOException {
      List<String> lineList = new ArrayList<>();
      BufferedReader br = new BufferedReader( new FileReader( valueFile ) );
      String line = br.readLine();
      while ( line != null && line.trim().length() > 0 ) {
         lineList.add( line );
         line = br.readLine();
      }
      br.close();

      _prepStmtValArr = new List[ lineList.size() ];
      for ( int i = 0; i < lineList.size(); i++ ) {
         String currLine = lineList.get( i );
         List<String> valList = new ArrayList<>();
         StringTokenizer st = new StringTokenizer( currLine, "\t" );
         while ( st.hasMoreTokens() ) {
            String token = st.nextToken().trim();
            valList.add( token );
         }
         _prepStmtValArr[ i ] = valList;
      }
      logger.info( "Loaded " + lineList.size() + " lines from value file: "
                   + valueFile.getAbsolutePath() );
   }

   /**
    * Slice up the query SQL and rebuild a SQL statement that gets a row count;
    *
    * @param querySql -
    * @return -
    */
   private int getRowCount( Connection conn, String querySql )
         throws SQLException {
      StringBuffer sb = new StringBuffer();
      sb.append( "SELECT COUNT(*) " );
      int idx = querySql.toUpperCase().indexOf( "FROM" );
      sb.append( querySql.subSequence( idx, querySql.length() ) );
      PreparedStatement cntStmt = conn.prepareStatement( sb.toString() );

      if ( _usePrepStmtVals ) {
         int totalCnt = 0;
         for ( int i = 0; i < _prepStmtValArr.length; i++ ) {
            List<String> valList = _prepStmtValArr[ i ];
            setPrepStmtValues( cntStmt, valList );
            ResultSet rs = cntStmt.executeQuery();
            rs.next();
            totalCnt += rs.getInt( 1 );
         }
         return totalCnt;
      } else {
         ResultSet rs = cntStmt.executeQuery();
         rs.next();
         return rs.getInt( 1 );
      }
   }

   /**
    * Helper method that sets the prepared statement values.
    *
    * @param prepStmt -
    * @param valList  -
    */
   private void setPrepStmtValues( PreparedStatement prepStmt, List<String> valList )
         throws SQLException {
      prepStmt.clearParameters();
      for ( int i = 0; i < valList.size(); i++ ) {
         Object valObj = valList.get( i );
         prepStmt.setObject( i + 1, valObj );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void getNext( final JCas jcas ) throws IOException, CollectionException {
      _currRowCount++;
      try {
         // pull doc text from resultset
         String document = null;
         if ( (_docColType == Types.CHAR)
              || (_docColType == Types.VARCHAR) ) {
            document = _resultSet.getString( _docTextColName );
         } else if ( _docColType == Types.CLOB ) {
            document = convertToString( _resultSet.getClob( _docTextColName ) );
         } else {
            throw new Exception( "Unsupported document text column type: "
                                 + _docColTypeName );
         }

         try {
            // No CAS Initiliazer, so set document text ourselves.
            // put document in CAS (assume CAS)
            jcas.setDocumentText( document );

            DocumentID docIdAnnot = new DocumentID( jcas );
            docIdAnnot.setDocumentID( getDocumentID( _resultSet ) );
            docIdAnnot.addToIndexes();

            logger.info( "Reading document with ID="
                         + docIdAnnot.getDocumentID() );
         } catch ( Exception e ) {
            logger.error( "CasInitializer failed to process document: " );
            logger.error( document );
            throw e;
         }
      } catch ( Exception e ) {
         throw new CollectionException( e );
      }
   }

   /**
    * Builds a document ID from one or more pieces of query data. If the query
    * data is not specified, the current row # is used.
    *
    * @param rs
    * @return
    */
   private String getDocumentID( ResultSet rs ) throws SQLException {
      if ( _docIdColNames != null ) {
         StringBuffer sb = new StringBuffer();
         for ( int i = 0; i < _docIdColNames.length; i++ ) {
            String val = rs.getObject( _docIdColNames[ i ] ).toString();
            sb.append( val );
            if ( i != (_docIdColNames.length - 1) ) {
               sb.append( _docIdDelimiter );
            }
         }
         return sb.toString();
      } else {
         // default is to return row num
         return String.valueOf( _currRowCount );
      }
   }

   /**
    * Loads the clob data into a String object.
    *
    * @param clob
    * @return
    * @throws SQLException
    * @throws IOException
    */
   private String convertToString( Clob clob ) throws SQLException, IOException {
      StringBuffer sb = new StringBuffer();
      BufferedReader br = new BufferedReader( clob.getCharacterStream() );
      String line = br.readLine();
      while ( line != null ) {
         sb.append( line );
         sb.append( '\n' );
         line = br.readLine();
      }
      br.close();
      return sb.toString();
   }

   /*
    * (non-Javadoc)
    *
    * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
    */
   @Override
   public boolean hasNext() throws IOException, CollectionException {
      try {

         if ( _resultSet == null ) {
            if ( _usePrepStmtVals ) {
               List<String> valList = _prepStmtValArr[ _prepStmtValArrIdx ];
               setPrepStmtValues( _preparedStatement, valList );
               _prepStmtValArrIdx++;
            }

            _resultSet = _preparedStatement.executeQuery();

            // TODO only needs to be done once
            ResultSetMetaData rsMetaData = _resultSet.getMetaData();
            int colIdx = _resultSet.findColumn( _docTextColName );
            _docColType = rsMetaData.getColumnType( colIdx );
            _docColTypeName = rsMetaData.getColumnTypeName( 1 );
         }

         boolean hasAnotherRow = _resultSet.next();
         if ( hasAnotherRow == false ) {
            // it's important to close ResultSets as they can accumlate
            // in the JVM heap. Too many open result sets can inadvertently
            // cause the DB conn to be closed by the server.
            _resultSet.close();
         }

         if ( _usePrepStmtVals ) {
            if ( (hasAnotherRow == false)
                 && (_prepStmtValArrIdx < _prepStmtValArr.length) ) {
               // the results for the previous prepared statement execution
               // have been exhausted, so the statement needs to be
               // executed with the next set of values

               // reset the _resultSet instance variable to NULL
               _resultSet = null;
               // re-invoke the hasNext() method so the prepared
               // statement gets executed again with the next set of values
               return this.hasNext();
            }
         }

         return hasAnotherRow;
      } catch ( Exception e ) {
         throw new CollectionException( e );
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
    */
   @Override
   public Progress[] getProgress() {
      Progress p = new ProgressImpl( _currRowCount, _totalRowCount,
            Progress.ENTITIES );
      return new Progress[] { p };
   }

   /*
    * (non-Javadoc)
    *
    * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
    */
   @Override
   public void close() throws IOException {
      try {
         _preparedStatement.close();
      } catch ( Exception e ) {
         throw new IOException( e.getMessage() );
      }
   }
}