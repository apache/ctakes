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
//package org.apache.ctakes.dictionary.lookup2.bsv;
//
//import org.apache.ctakes.dictionary.lookup.Dictionary;
//import org.apache.ctakes.dictionary.lookup.DictionaryException;
//import org.apache.ctakes.dictionary.lookup.MetaDataHit;
//import org.apache.log4j.Logger;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashSet;
//
///**
// * Author: SPF
// * Affiliation: CHIP-NLP
// * Date: 11/20/13
// */
//public class RareWordDictionaryWrapper extends AbstractRareWordDictionary implements Dictionary {
//
//   // LOG4J logger based on class name
//   final private Logger _logger = Logger.getLogger( getClass().getName() );
//
//   final private Dictionary _metaDataHitDictionary;
//
//   /**
//    *
//    * @param name name of the database table to use for lookup.  Used as the simple name for the dictionary
//    * @param entityTypeId the type of term that exists in the dictionary: Anatomical Site, Disease/Disorder, Drug, etc.
//    * @param metaDataHitDictionary older dictionary to wrap for lookup
//    */
//   public RareWordDictionaryWrapper( final String name, final String entityTypeId, final Dictionary metaDataHitDictionary ) {
//      super( name, entityTypeId );
//      _metaDataHitDictionary = metaDataHitDictionary;
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void retainMetaData( final String metaFieldName )  {
//      _metaDataHitDictionary.retainMetaData( metaFieldName );
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public boolean contains( String text ) throws DictionaryException {
//      return _metaDataHitDictionary.contains( text );
//   }
//
//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public Collection<MetaDataHit> getEntries( String text ) throws DictionaryException {
//      return _metaDataHitDictionary.getEntries( text );
//   }
//
//
//
//   /**
//    * Uses metadatahit metafieldvalues of cui tui wordindex tokenlength text rareword
//    *
//    * {@inheritDoc}
//    */
//   @Override
//   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) throws DictionaryException {
//      final Collection<MetaDataHit> metaDataHits = getEntries( rareWordText );
//      if ( metaDataHits == null || metaDataHits.isEmpty() ) {
//         return Collections.emptySet();
//      }
//      final Collection<RareWordTerm> rareWordTerms = new HashSet<RareWordTerm>( metaDataHits.size() );
//      for ( MetaDataHit metaDataHit : metaDataHits ) {
//         final String text = metaDataHit.getMetaFieldValue( "text" );
//         final String rareWord = metaDataHit.getMetaFieldValue( "rareword" );
//         final String cui = metaDataHit.getMetaFieldValue( "cui" );
//         final String tui = metaDataHit.getMetaFieldValue( "tui" );
//         int index = -1;
//         int length = -1;
//         try {
//            index = Integer.parseInt( metaDataHit.getMetaFieldValue( "wordindex" ) );
//            length = Integer.parseInt( metaDataHit.getMetaFieldValue( "tokenlength" ) );
//         } catch ( NumberFormatException nfE ) {
//            _logger.warn( "No wordindex or tokenlength in metaDataHit " + metaDataHit );
//            index = 0;
//            length = text.split( "\\s+" ).length;
//         }
//         if ( index >=0 && length >0 ) {
//            rareWordTerms.add( new RareWordTerm( text, cui, tui, rareWord, index, length ) );
//         }
//      }
//      return rareWordTerms;
//   }
//
//
//}
