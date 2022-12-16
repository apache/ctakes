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
package org.apache.ctakes.dictionary.lookup2.ae;

import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.Map;

/**
 * Processes an Annotation window in the cas, adding discovered terms to a map.
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 12/5/13
 */
public interface WindowProcessor {

//   /**
//    * Some windows should be skipped entirely, such as "[section *]"
//    *
//    * @param window annotation in which to search for terms
//    * @return true if window should be processed, false if it should not
//    */
//   boolean isWindowOk( Annotation window );


//   /**
//    * Processes a window of annotations for dictionary terms
//    *
//    * @param jcas            -
//    * @param window          annotation in which to search for terms
//    * @param dictionaryTerms map of entity types and terms for those types in the window
//    */
//   void processWindow( JCas jcas,
//                       Annotation window,
//                       Map<RareWordDictionary, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> dictionaryTerms );

   /**
    * Processes a window of annotations for dictionary terms
    *
    * @param jcas            -
    * @param windowBaseTokens baseTokens in window in which to search for terms
    * @param dictionaryTerms map of entity types and terms for those types in the window
    */
   public void processWindow( final JCas jcas, final Collection<BaseToken> windowBaseTokens,
                              final Map<RareWordDictionary, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> dictionaryTerms );

}
