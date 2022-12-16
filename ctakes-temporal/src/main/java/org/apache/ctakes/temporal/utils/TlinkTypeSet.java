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
package org.apache.ctakes.temporal.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
* Author: SPF
* Affiliation: CHIP-NLP
* Date: 4/17/13
*/
public class TlinkTypeSet implements Iterable<TlinkType> {

   private Set<TlinkType> _tlinkTypes;

   public boolean add( final TlinkType tlinkType ) {
      if ( _tlinkTypes == null ) {
         _tlinkTypes = new HashSet<TlinkType>( 1 );
      }
      return _tlinkTypes.add( tlinkType );
   }

   public int size() {
      return _tlinkTypes == null ? 0 : _tlinkTypes.size();
   }

   public boolean isEmpty() {
      return _tlinkTypes == null || _tlinkTypes.isEmpty();
   }

   public boolean contains( final TlinkType tlinkType ) {
      return _tlinkTypes != null && _tlinkTypes.contains( tlinkType );
   }

   public TlinkTypeSet createReciprocals() {
      final TlinkTypeSet reciprocals = new TlinkTypeSet();
      for ( TlinkType tlinkType : this ) {
         reciprocals.add( tlinkType.getReciprocal() );
      }
      return reciprocals;
   }


   /**
    * {@inheritDoc}
    * @return "NONE," if empty, else a series of & separated time relation type names
    */
   @Override
   public String toString() {
      if ( isEmpty() ) {
         return "NONE,";
      }
      final StringBuilder sb = new StringBuilder();
      for ( TlinkType type : _tlinkTypes ) {
         sb.append( type.name() ).append( " & " );
      }
      sb.setLength( sb.length()-3 );
      sb.append( "," );
      return sb.toString();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<TlinkType> iterator() {
      if ( isEmpty() ) {
         return EMPTY_ITERATOR;
      }
      return _tlinkTypes.iterator();
   }

   static private final Iterator<TlinkType> EMPTY_ITERATOR = new Iterator<TlinkType>() {
      @Override
      public boolean hasNext() {
         return false;
      }

      @Override
      public TlinkType next() {
         return null;
      }

      @Override
      public void remove() {
      }
   };



   //   /**
   //    * Agreement between two Time Relation sets.
   //    * 0 indicates no agreement, 1 indicates perfect agreement, anything between indicates partial agreement.
   //    * @param otherTimeRelationTypes time relation type set to contrast with this one
   //    * @return fraction of agreement: # common time relations(*2) divided by # total time relations
   //    */
   //   public double contrast( final TlinkTypeSet otherTimeRelationTypes ) {
   //      if ( otherTimeRelationTypes == null || isEmpty() || otherTimeRelationTypes.isEmpty() ) {
   //         return 0d;
   //      }
   //      final int total = size() + otherTimeRelationTypes.size();
   //      final Set<TlinkType> commonSet = new HashSet<TlinkType>( _tlinkTypes );
   //      commonSet.retainAll( otherTimeRelationTypes._tlinkTypes );
   //      return 2d * (double) commonSet.size() / (double) total;
   //   }



}
