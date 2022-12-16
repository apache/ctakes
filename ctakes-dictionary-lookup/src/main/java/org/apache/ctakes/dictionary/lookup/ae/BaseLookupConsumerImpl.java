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
package org.apache.ctakes.dictionary.lookup.ae;

import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.*;


/**
 * Provides some base functionality for subclasses.
 *
 * @author Mayo Clinic
 */
// TODO rename this class properly: AbstractBaseLookupConsumer.  Requires refactoring outside module
public abstract class BaseLookupConsumerImpl implements LookupConsumer {
   /**
    * Organizes the LookupHit objects by begin and end offsets.
    *
    * @param lookupHitIterator -
    * @return Iterator over Set objects. Each Set object is a collection of
    *         LookupHit objects with the same begin,end offsets.
    */
   static protected Iterator<Set<LookupHit>> organizeByOffset( final Iterator<LookupHit> lookupHitIterator ) {
      final  Map<LookupHitKey, Set<LookupHit>> lookupHitMap = createLookupHitMap( lookupHitIterator );
      return lookupHitMap.values().iterator();
   }

   static protected Map<LookupHitKey, Set<LookupHit>> createLookupHitMap( final Iterator<LookupHit> lookupHitIterator ) {
      final Map<LookupHitKey, Set<LookupHit>> lookupHitMap = new HashMap<LookupHitKey, Set<LookupHit>>();
      while ( lookupHitIterator.hasNext() ) {
         final LookupHit lookupHit = lookupHitIterator.next();
         final LookupHitKey key = new LookupHitKey( lookupHit );
         Set<LookupHit> lookupHits = lookupHitMap.get( key );
         if ( lookupHits == null ) {
            lookupHits = new HashSet<LookupHit>();
            lookupHitMap.put( key, lookupHits );
         }
         lookupHits.add( lookupHit );
      }
      return lookupHitMap;
   }

   /**
    * Using a String as a HashMap Key can be slow as
    * the hashCode is computed per character with each call - ditto for equals
    */
   static protected class LookupHitKey {
      final protected int __start;
      final protected int __end;
      final private int __hashCode;

      private LookupHitKey( final LookupHit lookupHit ) {
         __start = lookupHit.getStartOffset();
         __end = lookupHit.getEndOffset();
         __hashCode = 1000 * __end + __start;
      }

      public int hashCode() {
         return __hashCode;
      }

      public boolean equals( final Object object ) {
         return object instanceof LookupHitKey
               && __start == ((LookupHitKey) object).__start
               && __end == ((LookupHitKey) object).__end;
      }
   }

}
