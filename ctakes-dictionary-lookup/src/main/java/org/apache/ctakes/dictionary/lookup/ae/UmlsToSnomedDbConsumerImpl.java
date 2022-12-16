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
package org.apache.ctakes.dictionary.lookup.ae;

import org.apache.ctakes.core.resource.JdbcConnectionResource;
import org.apache.uima.UimaContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


/**
 * Implementation that takes UMLS dictionary lookup hits and stores as NamedEntity
 * objects only the ones that have a SNOMED synonym, by looking in a database
 * for SNOMED codes that map to the identified CUI.
 *
 * @author Mayo Clinic
 */
public class UmlsToSnomedDbConsumerImpl extends UmlsToSnomedConsumerImpl {

   static private final String DB_CONN_RESRC_KEY_PRP_KEY = "dbConnExtResrcKey";
   static private final String MAP_PREP_STMT_PRP_KEY = "mapPrepStmt";
   //ohnlp-Bugs-3296301 fix limited search results to fixed 100 records.
   // Added 'MaxListSize'
   private static int _maxListSize;
   final private PreparedStatement _preparedStatement;

   public UmlsToSnomedDbConsumerImpl( final UimaContext uimaContext, final Properties properties, final int maxListSize )
         throws Exception {
      super( uimaContext, properties );
      _maxListSize = maxListSize;
      final String resourceName = props.getProperty( DB_CONN_RESRC_KEY_PRP_KEY );
      final JdbcConnectionResource resrc = (JdbcConnectionResource) uimaContext.getResourceObject( resourceName );

      final String sqlStatement = props.getProperty( MAP_PREP_STMT_PRP_KEY );
      final Connection connection = resrc.getConnection();
      _preparedStatement = connection.prepareStatement( sqlStatement );

   }

   public UmlsToSnomedDbConsumerImpl( final UimaContext uimaContext, final Properties properties )
         throws Exception {
      this( uimaContext, properties, Integer.MAX_VALUE );
   }

   public void close() {
      try {
         if ( _preparedStatement != null && !_preparedStatement.isClosed() ) {
            _preparedStatement.close();
         }
      } catch ( SQLException sqlE ) {
         // Nothing necessary
      }
   }

   /**
    * Queries the given UMLS CUI against the DB. Re`turns a set of SNOMED codes.
    *
    * @param umlsCode -
    * @return          -
    * @throws SQLException
    */
   @Override
   protected Set<String> getSnomedCodes( final String umlsCode ) throws SQLException {
      final Set<String> codeSet = new HashSet<>();
      _preparedStatement.setString( 1, umlsCode );
      try(final ResultSet rs = _preparedStatement.executeQuery()){
        while ( rs.next() ) {
          final String snomedCode = rs.getString( 1 ).trim();
          codeSet.add( snomedCode );
        }
      }
      return codeSet;
   }

}
