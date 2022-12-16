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


import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/25/13
 */
abstract public class AbstractRareWordDictionary implements RareWordDictionary {

   final private String _name;


   /**
    * @param name simple name for the dictionary
    */
   public AbstractRareWordDictionary( final String name ) {
      _name = name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final FastLookupToken fastLookupToken ) {
      if ( fastLookupToken.getVariant() == null ) {
         return getRareWordHits( fastLookupToken.getText() );
      }
      final Collection<RareWordTerm> terms = new ArrayList<RareWordTerm>();
      terms.addAll( getRareWordHits( fastLookupToken.getText() ) );
      terms.addAll( getRareWordHits( fastLookupToken.getVariant() ) );
      return terms;
   }

}
