///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//package org.apache.ctakes.dictionary.lookup2.relation;
//
//import org.apache.log4j.Logger;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import static org.apache.ctakes.dictionary.lookup2.consumer.WsdTermConsumer.RelatedCui;
//
///**
// * TODO  -- work in progress for use in WSD
// *
// *
// *
//* Author: SPF
//* Affiliation: CHIP-NLP
//* Date: 12/16/13
//*/
//public class CuiRelationsJdbc {
//
//   /**
//    * Column (field) indices in the database.  Notice that these are constant and not configurable.
//    * If a configurable implementation is desired then create an extension.
//    */
//   static private enum FIELD_INDEX {
//      CUI( 1 ), RELATION_TYPE( 2 );
//      final private int __index;
//      private FIELD_INDEX( final int index ) {
//         __index = index;
//      }
//   }
//
//   // LOG4J logger based on class name
//   final private Logger _logger = Logger.getLogger( getClass().getName() );
//
//   final private Connection _connection;
//   final private String _tableName;
//   private PreparedStatement _metadataStatement;
//
//   /**
//    *
//    */
//   public CuiRelationsJdbc( final Connection connection, final String tableName ) {
//      _connection = connection;
//      _tableName = tableName;
//   }
//
//   /**
//    * @param cui cui to check for relations
//    * @return all relations (cui and relation)
//    */
//   public Collection<RelatedCui> getCuiRelations( final String cui ) {
//      final List<RelatedCui> relatedCuis = new ArrayList<RelatedCui>();
//      try {
//         initMetaDataStatement( cui );
//         final ResultSet resultSet = _metadataStatement.executeQuery();
//         while ( resultSet.next() ) {
//            final RelatedCui relatedCui = new RelatedCui( resultSet.getString( FIELD_INDEX.CUI.__index),
//                                                          resultSet.getString( FIELD_INDEX.RELATION_TYPE.__index ) );
//            relatedCuis.add( relatedCui );
//         }
//         // Though the ResultSet interface documentation states that there are automatic closures,
//         // it is up to the driver to implement this behavior ...  historically some drivers have not done so
//         resultSet.close();
//      } catch ( SQLException e ) {
//         _logger.error( e.getMessage() );
//      }
//      return relatedCuis;
//   }
//
//   /**
//    *
//    * @param cui text of the rare word to use for term lookup
//    * @return an sql call to use for term lookup
//    * @throws SQLException if the {@code PreparedStatement} could not be created or changed
//    */
//   private PreparedStatement initMetaDataStatement( final String cui ) throws SQLException {
//      if ( _metadataStatement == null ) {
//         final String lookupSql = "SELECT * FROM " + _tableName + " WHERE RWORD = ?";
//         _metadataStatement = _connection.prepareStatement( lookupSql );
//      }
//      _metadataStatement.clearParameters();
//      _metadataStatement.setString( 1, cui );
//      return _metadataStatement;
//   }
//
//}
