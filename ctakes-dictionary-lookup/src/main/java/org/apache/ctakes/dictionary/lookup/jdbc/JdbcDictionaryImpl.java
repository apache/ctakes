/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dictionary.lookup.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.dictionary.lookup.AbstractBaseDictionary;
import org.apache.ctakes.dictionary.lookup.DictionaryException;
import org.apache.ctakes.dictionary.lookup.GenericMetaDataHitImpl;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;


/**
 * @author Mayo Clinic
 */
public class JdbcDictionaryImpl extends AbstractBaseDictionary {
   final private Connection iv_dbConn;
   final private String iv_tableName;
   final private String iv_lookupFieldName;
   private PreparedStatement iv_mdPrepStmt;
   private PreparedStatement iv_cntPrepStmt;

   public JdbcDictionaryImpl( final Connection conn, final String tableName, final String lookupFieldName ) {
      iv_dbConn = conn;
      iv_tableName = tableName;
      iv_lookupFieldName = lookupFieldName;
   }

   private PreparedStatement initCountPrepStmt( final String text ) throws SQLException {
      if ( iv_cntPrepStmt == null ) {
         final StringBuilder sb = new StringBuilder();
         sb.append( "SELECT COUNT(*) " );

         sb.append( " FROM " );
         sb.append( iv_tableName );

         sb.append( " WHERE " );
         sb.append( iv_lookupFieldName );
         sb.append( " = ?" );

         iv_cntPrepStmt = iv_dbConn.prepareStatement( sb.toString() );
      }

      iv_cntPrepStmt.clearParameters();
      iv_cntPrepStmt.setString( 1, text );

      return iv_cntPrepStmt;
   }

   private PreparedStatement initMetaDataPrepStmt( final String text ) throws SQLException {
      if ( iv_mdPrepStmt == null ) {
         final StringBuilder sb = new StringBuilder();
         sb.append( "SELECT " );

         // translate meta data field names into columns
         // to be returned in the result set
         final Iterator<String> metaFieldNameItr = getMetaFieldNames();
         while ( metaFieldNameItr.hasNext() ) {
            String mdFieldName = metaFieldNameItr.next();
            sb.append( mdFieldName );
            sb.append( ',' );
         }
         // chomp off the last comma
         sb.deleteCharAt( sb.length() - 1 );

         sb.append( " FROM " );
         sb.append( iv_tableName );

         sb.append( " WHERE " );
         sb.append( iv_lookupFieldName );
         sb.append( " = ?" );

         iv_mdPrepStmt = iv_dbConn.prepareStatement( sb.toString() );
      }

      iv_mdPrepStmt.clearParameters();
      iv_mdPrepStmt.setString( 1, text );

      return iv_mdPrepStmt;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean contains( final String text ) throws DictionaryException {
      try {
         final PreparedStatement prepStmt = initCountPrepStmt( text );
         final ResultSet rs = prepStmt.executeQuery();
         rs.next();
         final int count = rs.getInt( 1 );
         return count > 0;
      } catch ( SQLException e ) {
         throw new DictionaryException( e );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<MetaDataHit> getEntries( final String text ) throws DictionaryException {
      final Set<MetaDataHit> metaDataHitSet = new HashSet<>();
      try {
         final PreparedStatement prepStmt = initMetaDataPrepStmt( text );
         final ResultSet rs = prepStmt.executeQuery();

         while ( rs.next() ) {
            final Map<String, String> nameValMap = new HashMap<>();
            final Iterator<String> metaFieldNameItr = getMetaFieldNames();
            while ( metaFieldNameItr.hasNext() ) {
               final String metaFieldName = metaFieldNameItr.next();
               final String metaFieldValue = rs.getString( metaFieldName );
               nameValMap.put( metaFieldName, metaFieldValue );
            }
            final MetaDataHit mdh = new GenericMetaDataHitImpl( nameValMap );
            metaDataHitSet.add( mdh );
         }
         return metaDataHitSet;
      } catch ( SQLException e ) {
         throw new DictionaryException( e );
      }
   }
}
