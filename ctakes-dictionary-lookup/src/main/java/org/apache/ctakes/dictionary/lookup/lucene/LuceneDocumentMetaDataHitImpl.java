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
package org.apache.ctakes.dictionary.lookup.lucene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.apache.ctakes.dictionary.lookup.AbstractBaseMetaDataHit;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;


/**
 * @author Mayo Clinic
 */
@Immutable
public final class LuceneDocumentMetaDataHitImpl extends AbstractBaseMetaDataHit {
   final private Document _luceneDoc;
   final private Set<String> _nameSet;
   final private Collection<String> _valueList;

   public LuceneDocumentMetaDataHitImpl( final Document luceneDoc ) {
      _luceneDoc = luceneDoc;
      final List<IndexableField> fieldEnumList = _luceneDoc.getFields();
      final Set<String> nameSet = new HashSet<>( fieldEnumList.size() );
      final List<String> valueList = new ArrayList<>( fieldEnumList.size() );
      for ( IndexableField field : fieldEnumList ) {
         nameSet.add( field.name() );
         valueList.add( field.stringValue() );
      }
      _nameSet = Collections.unmodifiableSet( nameSet );
      _valueList = Collections.unmodifiableList( valueList );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getMetaFieldValue( final String metaFieldName ) {
      return _luceneDoc.get( metaFieldName );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Set<String> getMetaFieldNames() {
      return _nameSet;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getMetaFieldValues() {
      return _valueList;
   }
}
