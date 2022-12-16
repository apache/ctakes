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

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.uima.analysis_component.AnalysisComponent;

import java.util.Collection;
import java.util.List;

/**
 * An annotator that looks up dictionary terms in text using
 * {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary} s
 * and adds them as entities to the Cas using a {@link org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer}
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 2/24/14
 */
public interface JCasTermAnnotator extends AnalysisComponent {

   /**
    * @deprecated replaced by {@link ConfigParameterConstants#PARAM_LOOKUP_XML }
    */
   @Deprecated
   String DICTIONARY_DESCRIPTOR_KEY = "DictionaryDescriptor";
   /**
    * specifies the type of window to use for lookup
    */
   String PARAM_WINDOW_ANNOT_KEY = "windowAnnotations";
   /**
    * optional part of speech tags for tokens that should not be used for lookup
    */
   String PARAM_EXC_TAGS_KEY = "exclusionTags";
   /**
    * optional minimum span for tokens that should not be used for lookup
    */
   String PARAM_MIN_SPAN_KEY = "minimumSpan";


   String DEFAULT_LOOKUP_WINDOW = "org.apache.ctakes.typesystem.type.textspan.Sentence";
   String DEFAULT_EXCLUSION_TAGS
         = "VB,VBD,VBG,VBN,VBP,VBZ,CC,CD,DT,EX,IN,LS,MD,PDT,POS,PP,PP$,PRP,PRP$,RP,TO,WDT,WP,WPS,WRB";
   int DEFAULT_MINIMUM_SPAN = 3;
   String DEFAULT_DICT_DESC_PATH = "org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab.xml";

   /**
    * @return Dictionaries that are used by this WindowProcessor
    */
   public Collection<RareWordDictionary> getDictionaries();

   /**
    * Given a dictionary, tokens, and lookup token indices, populate a terms collection with discovered terms
    *
    * @param dictionary          -
    * @param allTokens           -
    * @param lookupTokenIndices  -
    * @param termsFromDictionary -
    */
   void findTerms( RareWordDictionary dictionary,
                   List<FastLookupToken> allTokens,
                   List<Integer> lookupTokenIndices,
                   CollectionMap<TextSpan, Long, ? extends Collection<Long>> termsFromDictionary );

}
