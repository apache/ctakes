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
package org.apache.ctakes.dictionary.lookup;

import org.apache.ctakes.dictionary.lookup.filter.CollectionFilter;
import org.apache.ctakes.dictionary.lookup.filter.FilterException;
import org.apache.ctakes.dictionary.lookup.filter.PostLookupFilter;
import org.apache.ctakes.dictionary.lookup.filter.PreLookupFilter;

import java.util.*;

/**
 * The engine adds additional functionality to executing a dictionary.
 * <ol>
 * <li>Pre-lookup filtering. Filters the input text.</li>
 * <li>Post-lookup filtering. Filters individual dictionary hits based on
 * metadata.</li>
 * <li>Collection filtering. Filters the collection of dictionary hits based on
 * metadata and/or collection information.</li>
 * <li>Caching. Caches dictionary hits.</li>
 * <li>Case Sensitivity. Keeps case or makes case irrelevant to lookup.</li>
 * </ol>
 */
public class DictionaryEngine {
   final private Dictionary _dictionary;
   final private boolean _keepCase;

   // use Lists to store filters to maintain order
   private List<PreLookupFilter> _preLookupFilterList = new ArrayList<>();
   private List<PostLookupFilter> _postLookupFilterList = new ArrayList<>();
   private List<CollectionFilter> _collectionFilterList = new ArrayList<>();

   // cache objs
   // key = String text, value = Boolean
   private Map<String, Boolean> _binaryLookupCacheMap = new HashMap<>();
   // key = String text, value = Collection of MetaDataHits
   private Map<String, Collection<MetaDataHit>> _metaLookupCacheMap = new HashMap<>();


   /**
    * @param dictionary Dictionary to use for lookup operations.
    * @param keepCase   Determines whether to keep character Upper or Lower casing.
    *                   False indicates that casing will be ignored by lower casing
    *                   all lookups.
    */
   public DictionaryEngine( final Dictionary dictionary, final boolean keepCase ) {
      _dictionary = dictionary;
      _keepCase = keepCase;
   }

   /**
    * Adds a Pre-lookup filter to the engine. Filters will be applied in the
    * order of addition.
    *
    * @param preLookupFilter PreLookupFilter to add.
    */
   public void addPreLookupFilter( final PreLookupFilter preLookupFilter ) {
      _preLookupFilterList.add( preLookupFilter );
   }

   /**
    * Adds a Post-lookup filter to the engine. Filters will be applied in the
    * order of addition.
    *
    * @param postLookupFilter PostLookupFilter to add.
    */
   public void addPostLookupFilter( final PostLookupFilter postLookupFilter ) {
      _postLookupFilterList.add( postLookupFilter );
   }

   /**
    * Adds a Collection filter to the engine. Filters will be applied in the
    * order of addition. Filter will be applied after PostLookupFiltering.
    *
    * @param collectionFilter CollectionFilter to add.
    */
   public void addCollectionFilter( final CollectionFilter collectionFilter ) {
      _collectionFilterList.add( collectionFilter );
   }

   /**
    * Adds an entry to the dictionary lookup cache. The given text will be
    * cached immediately when this method is invoked. This cache does not
    * expire.
    *
    * @param text to add to the caches as a hit or miss
    */
   public void addCacheEntry( String text ) throws DictionaryException, FilterException {
      if ( !_keepCase ) {
         text = text.toLowerCase();
      }
      final boolean isHit = binaryLookup( text );
      _binaryLookupCacheMap.put( text, isHit );

      final Collection<MetaDataHit> metaDataHits = metaLookup( text );
      _metaLookupCacheMap.put( text, metaDataHits );
   }

   /**
    * Gets a collection of MetaDataHits from the Dictionary based on the input
    * text. Both Pre-lookup and Post-lookup filtering are applied.
    *
    * @param text text to check for in the filters
    * @return Collection of MetaDataHit objects
    * @throws DictionaryException
    * @throws FilterException
    */
   public Collection<MetaDataHit> metaLookup( String text ) throws DictionaryException, FilterException {
      if ( !_keepCase ) {
         text = text.toLowerCase();
      }
      // apply pre-filtering
      if ( isFilteredByPreLookup( text ) ) {
         // return empty Collection
         return Collections.emptySet();
      }
      // not part of filter
      Collection<MetaDataHit> metaDataHitCol = _metaLookupCacheMap.get( text );
      if ( metaDataHitCol == null ) {
         // not part of cache, go ahead and do lookup
         metaDataHitCol = _dictionary.getEntries( text );
      }
      // apply post-filtering
      if ( !_postLookupFilterList.isEmpty() ) {
         final Set<MetaDataHit> mdhRemovalSet = new HashSet<>();
         for ( MetaDataHit metaDataHit : metaDataHitCol ) {
            // check the mdhRemoval set before iterating over the entire filter list (and filter .contains calls)
            if ( !mdhRemovalSet.contains( metaDataHit ) ) {
               for ( PostLookupFilter postLookupFilter : _postLookupFilterList ) {
                  if ( postLookupFilter.contains( metaDataHit ) ) {
                     mdhRemovalSet.add( metaDataHit );
                  }
               }
            }
         }
         metaDataHitCol.removeAll( mdhRemovalSet );
      }
      // apply collection filtering
      if ( !_collectionFilterList.isEmpty() ) {
         for  ( CollectionFilter collectionFilter : _collectionFilterList ) {
            metaDataHitCol = collectionFilter.applyFilter( metaDataHitCol );
         }
      }
      return metaDataHitCol;
   }

   /**
    * Determines whether the input text is contained by the Dictionary. Only
    * pre-lookup filtering is applied.
    *
    * @param text The input text.
    * @return true if contained by Dictionary, false otherwise
    * @throws DictionaryException
    * @throws FilterException
    */
   public boolean binaryLookup( String text ) throws DictionaryException, FilterException {
      if ( !_keepCase ) {
         text = text.toLowerCase();
      }
      // apply pre-filtering
      if ( isFilteredByPreLookup( text ) ) {
         return false;
      }
      // not part of filter, go ahead and do lookup
      final Boolean isHit = _binaryLookupCacheMap.get( text );
      if ( isHit != null ) {
         return isHit;
      }
      // not part of cache, go ahead and do lookup
      return _dictionary.contains( text );
   }

   /**
    * Helper method that applies Pre-lookup filtering to the input text.
    *
    * @param text text to be filtered, possible case change must have already been accounted for by calling method.
    * @return true if filtered, false otherwise
    * @throws FilterException
    */
   private boolean isFilteredByPreLookup( final String text ) throws FilterException {
      for  ( PreLookupFilter preLookupFilter : _preLookupFilterList ) {
         if ( preLookupFilter.contains( text ) ) {
            // text is part of filter
            return true;
         }
      }
      return false;
   }

}
