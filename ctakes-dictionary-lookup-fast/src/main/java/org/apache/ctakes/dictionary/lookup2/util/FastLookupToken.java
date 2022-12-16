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
package org.apache.ctakes.dictionary.lookup2.util;


import org.apache.ctakes.dictionary.lookup2.textspan.DefaultTextSpan;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.jcas.tcas.Annotation;

import javax.annotation.concurrent.Immutable;

/**
 * Container class that holds a text span, actual text, and possible variant text for a lookup token.
 * This class maintains (forces) lowercase text for lookup
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/29/13
 */
@Immutable
final public class FastLookupToken {

   final private TextSpan _textSpan;
   final private String _text;
   private String _variant;

   public FastLookupToken( final Annotation jcasAnnotation ) {
      _textSpan = new DefaultTextSpan( jcasAnnotation.getBegin(), jcasAnnotation.getEnd() );
      _text = jcasAnnotation.getCoveredText().toLowerCase();
      if ( jcasAnnotation instanceof WordToken ) {
         final String canonicalForm = ((WordToken)jcasAnnotation).getCanonicalForm();
         // If canonical is not null AND not the same as the plain text then it is a valid variant for lookup
         if ( canonicalForm != null && !canonicalForm.equals( _text ) ) {
            _variant = canonicalForm;
         }
      }
   }

   /**
    * @return a span with the start and end indices used for this lookup token
    */
   public TextSpan getTextSpan() {
      return _textSpan;
   }

   /**
    * @return the start index used for this lookup token
    */
   public int getStart() {
      return _textSpan.getStart();
   }

   /**
    * @return the end index used for this lookup token
    */
   public int getEnd() {
      return _textSpan.getEnd();
   }

   /**
    * @return the length of the text span in characters
    */
   public int getLength() {
      return _textSpan.getLength();
   }

   /**
    * @return the actual text in the document for the lookup token, in lowercase
    */
   public String getText() {
      return _text;
   }

   /**
    * @return possible canonical variant text for the lookup token, in lowercase, or null if none
    */
   public String getVariant() {
      return _variant;
   }

   /**
    * Two lookup tokens are equal iff the spans are equal.
    *
    * @param value -
    * @return true if {@code value} is a {@code FastLookupToken} and has a span equal to this token's span
    */
   public boolean equals( final Object value ) {
      return value != null && value instanceof FastLookupToken
             && _textSpan.equals( ((FastLookupToken)value).getTextSpan() );
   }

   /**
    * @return hashCode created from the Span
    */
   public int hashCode() {
      return _textSpan.hashCode();
   }

}
