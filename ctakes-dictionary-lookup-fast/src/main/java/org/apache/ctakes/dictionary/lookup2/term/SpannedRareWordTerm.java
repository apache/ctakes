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

import org.apache.ctakes.dictionary.lookup2.textspan.DefaultTextSpan;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;

import javax.annotation.concurrent.Immutable;

/**
 * A {@link RareWordTerm} tied to a text span
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/18/13
 */
// TODO No longer used - remove
@Immutable
final public class SpannedRareWordTerm {

   final private TextSpan _textSpan;
   final private RareWordTerm _rareWordTerm;
   final private int _hashCode;

   /**
    * @param rareWordTerm contains a term from a {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary}
    * @param startOffset  the start index of the term
    * @param endOffset    the end index of the term
    */
   public SpannedRareWordTerm( final RareWordTerm rareWordTerm, final int startOffset, final int endOffset ) {
      this( rareWordTerm, new DefaultTextSpan( startOffset, endOffset ) );
   }

   /**
    * @param rareWordTerm contains a term from a {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary}
    * @param spanKey      the span of the term
    */
   public SpannedRareWordTerm( final RareWordTerm rareWordTerm, final TextSpan spanKey ) {
      _rareWordTerm = rareWordTerm;
      _textSpan = spanKey;
      _hashCode = _rareWordTerm.hashCode() + _textSpan.hashCode();
   }

   /**
    * @return a span with the start and end indices used for this lookup token
    */
   public TextSpan getTextSpan() {
      return _textSpan;
   }

   /**
    * @return the term that was discovered in this span
    */
   public RareWordTerm getRareWordTerm() {
      return _rareWordTerm;
   }

   /**
    * Override default equals method. Two SpannedRareWordTerm objects are equal if their
    * offsets match and their RareWordTerm objects are equal.
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      if ( value instanceof SpannedRareWordTerm ) {
         final SpannedRareWordTerm other = (SpannedRareWordTerm)value;
         return _textSpan.equals( other._textSpan ) && _rareWordTerm.equals( other.getRareWordTerm() );
      }
      return false;
   }

   /**
    * Override default equals method. Two SpannedRareWordTerm objects are equal if their
    * offsets match and their RareWordTerm objects are equal.
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _hashCode;
   }

}
