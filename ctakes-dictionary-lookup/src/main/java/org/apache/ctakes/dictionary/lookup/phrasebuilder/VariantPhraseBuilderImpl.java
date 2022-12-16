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
package org.apache.ctakes.dictionary.lookup.phrasebuilder;

import org.apache.ctakes.dictionary.lookup.vo.LookupToken;

import java.util.*;


/**
 * Builds phrases based on various variants of a LookupToken. For instance, a
 * single LookupToken may have a spell corrected variant, abbreviation expansion
 * variant, etc...
 *
 * @author Mayo Clinic
 */
public class VariantPhraseBuilderImpl implements PhraseBuilder {

   final private List<TextExtractor> iv_textExtractorList;

   /**
    * Constructor
    *
    * @param variantAttrNames Key names of the variant attributes attached to the
    *                         LookupToken objects.
    * @param useOriginalText  flag that determines whether to use the original text or not.
    */
   public VariantPhraseBuilderImpl( final String[] variantAttrNames, final boolean useOriginalText ) {
      iv_textExtractorList = new ArrayList<>();

      if ( useOriginalText ) {
         // use original text as a variant
         iv_textExtractorList.add( new OriginalTextImpl() );
      }
      // add variants
      for ( String name : variantAttrNames ) {
         iv_textExtractorList.add( new AttributeTextImpl( name ) );
      }
   }

   public String[] getPhrases( final List<LookupToken> lookupTokenList ) {
      final Set<String> phraseSet = new HashSet<>();
      for ( TextExtractor extractor : iv_textExtractorList ) {
         final StringBuilder sb = new StringBuilder();
         LookupToken previousLt = null;
         for ( LookupToken lt : lookupTokenList ) {
            String variant = extractor.getText( lt );
            if ( variant == null ) {
               variant = lt.getText();
            }
            if ( previousLt != null && previousLt.getEndOffset() != lt.getStartOffset() ) {
               // check delta between previous token and current token
               // this delta represents whitespace between tokens
               // insert whitespace
               sb.append( ' ' );
            }
            sb.append( variant );
            previousLt = lt;
         }
         final String phrase = sb.toString().trim();
         phraseSet.add( phrase );
      }
      return phraseSet.toArray( new String[phraseSet.size()] );
   }

   /**
    * Common interface to extract text from a LookupToken.
    *
    * @author Mayo Clinic
    */
   private interface TextExtractor {
      public String getText( LookupToken lt );
   }

   /**
    * Implementation that extracts text from the original text of a
    * LookupToken.
    *
    * @author Mayo Clinic
    */
   class OriginalTextImpl implements TextExtractor {
      public String getText( LookupToken lt ) {
         return lt.getText();
      }
   }

   /**
    * Implementation that extracts text from an attribute of a LookupToken.
    *
    * @author Mayo Clinic
    */
   class AttributeTextImpl implements TextExtractor {
      private String iv_varAttrName;

      /**
       * Constructor
       *
       * @param varAttrName
       */
      public AttributeTextImpl( String varAttrName ) {
         iv_varAttrName = varAttrName;
      }

      public String getText( LookupToken lt ) {
         return lt.getStringAttribute( iv_varAttrName );
      }
   }
}
