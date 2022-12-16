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
package org.apache.ctakes.dictionary.lookup.strtable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mayo Clinic
 */
final public class StringTable {
   // key = indexed field name (String), value = VALUE MAP
   final private Map<String, Map<String, Set<StringTableRow>>> iv_nameMap;

   // ROW MAP
   // key = indexed field value (String), value = set of StringTableRows

   public StringTable( final String[] indexedFieldNames ) {
      iv_nameMap = new HashMap<>();
      for ( String fieldName : indexedFieldNames ) {
         iv_nameMap.put( fieldName, new HashMap<String, Set<StringTableRow>>() );
      }
   }

   public void addRow( final StringTableRow strTableRow ) {
      for ( Map.Entry<String,Map<String, Set<StringTableRow>>> stringMapEntry : iv_nameMap.entrySet() ) {
         final Map<String, Set<StringTableRow>> valueMap = stringMapEntry.getValue();
         final String indexedFieldValue = strTableRow.getFieldValue( stringMapEntry.getKey() );

         Set<StringTableRow> rowSet = valueMap.get( indexedFieldValue );
         if ( rowSet == null ) {
            rowSet = new HashSet<>();
         }
         rowSet.add( strTableRow );
         valueMap.put( indexedFieldValue, rowSet );

      }
   }

   public StringTableRow[] getRows( final String indexedFieldName, final String fieldVal ) {
      final Map<String, Set<StringTableRow>> valueMap = iv_nameMap.get( indexedFieldName );
      Set<StringTableRow> rowSet = valueMap.get( fieldVal );
      if ( rowSet != null ) {
         return rowSet.toArray( new StringTableRow[rowSet.size()] );
      }
      return new StringTableRow[0];
   }

   public StringTableRow[] getAllRows() {
      final Set<StringTableRow> allRows = new HashSet<>();
      for ( Map<String, Set<StringTableRow>> valueMap : iv_nameMap.values() ) {
         for ( Set<StringTableRow> rowSet : valueMap.values() ) {
            if ( !rowSet.isEmpty() ) {
               allRows.addAll( rowSet );
            }
         }
      }
      return allRows.toArray( new StringTableRow[allRows.size()] );
   }

}
