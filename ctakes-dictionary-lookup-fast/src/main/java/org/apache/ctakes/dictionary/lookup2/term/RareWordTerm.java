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
package org.apache.ctakes.dictionary.lookup2.term;

import javax.annotation.concurrent.Immutable;

/**
 * Container class for terms in a {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary}
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/18/13
 */
@Immutable
final public class RareWordTerm {

   final private String _text;
   final private Long _cuiCode;
   final private String _rareWord;
   final private int _rareWordIndex;
   final private int _tokenCount;
   final private int _hashCode;

   /**
    * @param text          full text of term
    * @param cuiCode       umls cui for the term
    * @param rareWord      rare word in the term that is used for lookup
    * @param rareWordIndex index of the rare word within the term
    * @param tokenCount    number of tokens within the term
    */
   public RareWordTerm( final String text, final Long cuiCode,
                        final String rareWord, final int rareWordIndex,
                        final int tokenCount ) {
      _text = text;
      _cuiCode = cuiCode;
      _rareWord = rareWord;
      _rareWordIndex = rareWordIndex;
      _tokenCount = tokenCount;
      _hashCode = (_cuiCode + _text).hashCode();
   }

   /**
    * @return full text of term
    */
   public String getText() {
      return _text;
   }

   /**
    * @return umls cui for the term
    */
   public Long getCuiCode() {
      return _cuiCode;
   }

   /**
    * @return rare word in the term that is used for lookup
    */
   public String getRareWord() {
      return _rareWord;
   }

   /**
    * @return index of the rare word within the term
    */
   public int getRareWordIndex() {
      return _rareWordIndex;
   }

   /**
    * @return number of tokens within the term
    */
   public int getTokenCount() {
      return _tokenCount;
   }

   /**
    * @return each token in the term as a separate String
    */
   public String[] getTokens() {
      final String[] tokens = new String[ _tokenCount ];
      int tokenIndex = 0;
      int previousSpaceIndex = -1;
      int spaceIndex = _text.indexOf( ' ' );
      while ( spaceIndex > 0 && tokenIndex < _tokenCount ) {
         tokens[ tokenIndex ] = _text.substring( previousSpaceIndex + 1, spaceIndex );
         tokenIndex++;
         previousSpaceIndex = spaceIndex;
         spaceIndex = _text.indexOf( ' ', previousSpaceIndex + 1 );
      }
      if ( previousSpaceIndex + 1 < _text.length() ) {
         tokens[ _tokenCount - 1 ] = _text.substring( previousSpaceIndex + 1 );
      }
      return tokens;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      if ( !(value instanceof RareWordTerm) ) {
         return false;
      }
      final RareWordTerm other = (RareWordTerm)value;
      return other.getCuiCode().equals( _cuiCode ) && other.getText().equals( _text );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _hashCode;
   }

}
