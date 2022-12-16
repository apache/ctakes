/*
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
package org.apache.ctakes.dictionary.lookup2.dictionary;

import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;

import java.util.Collection;

/**
 * A RareWordDictionary that uses a HashMap of Rare Words and Terms for lookup
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/9/14
 */
final public class MemRareWordDictionary extends AbstractRareWordDictionary {

   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   final private CollectionMap<String, RareWordTerm, ? extends Collection<RareWordTerm>> _rareWordTermMap;

   /**
    * {@inheritDoc}
    *
    * @param rareWordTermMap Map with a Rare Word (tokens) as key, and RareWordTerm Collection as value
    */
   public MemRareWordDictionary( final String name,
                                 final CollectionMap<String, RareWordTerm, ? extends Collection<RareWordTerm>> rareWordTermMap ) {
      super( name );
      _rareWordTermMap = rareWordTermMap;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
      return _rareWordTermMap.getCollection( rareWordText );
   }

}
