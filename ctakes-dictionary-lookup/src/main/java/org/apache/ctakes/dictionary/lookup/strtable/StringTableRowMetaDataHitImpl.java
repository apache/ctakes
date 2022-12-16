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
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.apache.ctakes.dictionary.lookup.AbstractBaseMetaDataHit;


/**
 * @author Mayo Clinic
 */
@Immutable
final public class StringTableRowMetaDataHitImpl extends AbstractBaseMetaDataHit {
   final private StringTableRow _strTableRow;

   public StringTableRowMetaDataHitImpl( final StringTableRow strTableRow ) {
      _strTableRow = strTableRow;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getMetaFieldValue( final String metaFieldName ) {
      return _strTableRow.getFieldValue( metaFieldName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<String> getMetaFieldNames() {
      return _strTableRow.getNames();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getMetaFieldValues() {
      return _strTableRow.getValues();
   }
}
