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

import java.util.Collection;

/**
 * Dictionary used to lookup terms by the most rare word within them
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
public interface RareWordDictionary {

   /**
    * The Type identifier and Name are used to maintain a collection of dictionaries,
    * so the combination of Type and Name should be unique for each dictionary if possible.
    *
    * @return simple name for the dictionary
    */
   public String getName();

   /**
    * Any single token can exist in zero or more terms in the dictionary.  It may exist as its -own- form or as an
    * alternate canonical variant.  This method will check the dictionary for both
    *
    * @param fastLookupToken a single-word token
    * @return zero or more terms that contain the lookup token
    */
   public Collection<RareWordTerm> getRareWordHits( final FastLookupToken fastLookupToken );

   /**
    * Get all terms within the dictionary that contain a given rare word
    *
    * @param rareWordText text of the rare word
    * @return all terms within the dictionary that contain {@code rareWordText}
    */
   public Collection<RareWordTerm> getRareWordHits( final String rareWordText );

}
