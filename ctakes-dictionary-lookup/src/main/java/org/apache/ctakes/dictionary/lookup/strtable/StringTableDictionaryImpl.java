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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ctakes.dictionary.lookup.AbstractBaseDictionary;
import org.apache.ctakes.dictionary.lookup.DictionaryException;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;


/**
 * @author Mayo Clinic
 */
public class StringTableDictionaryImpl extends AbstractBaseDictionary {
   final private StringTable iv_strTable;
   final private String iv_lookupFieldName;

   public StringTableDictionaryImpl( final StringTable strTable, final String lookupFieldName ) {
      iv_strTable = strTable;
      iv_lookupFieldName = lookupFieldName;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean contains( final String text ) throws DictionaryException {
      return iv_strTable.getRows( iv_lookupFieldName, text ).length > 0;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<MetaDataHit> getEntries( final String text ) throws DictionaryException {
      final StringTableRow[] strTableRows = iv_strTable.getRows( iv_lookupFieldName, text );
      if ( strTableRows.length == 0 ) {
         return Collections.emptySet();
      }
      final Set<MetaDataHit> metaDataHitSet = new HashSet<>();
      for ( StringTableRow tableRow : strTableRows ) {
         final MetaDataHit metaDataHit = new StringTableRowMetaDataHitImpl( tableRow );
         metaDataHitSet.add( metaDataHit );
      }
      return metaDataHitSet;
   }

}
