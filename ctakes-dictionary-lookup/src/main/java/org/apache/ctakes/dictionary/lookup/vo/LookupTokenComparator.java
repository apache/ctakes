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
package org.apache.ctakes.dictionary.lookup.vo;


import java.util.Comparator;


/**
 * Comparator that sorts LookupToken objects by their start and end offsets.
 *
 * @author Mayo Clinic
 */
public enum LookupTokenComparator implements Comparator<LookupToken> {
   INSTANCE;

   static public LookupTokenComparator getInstance() {
      return INSTANCE;
   }

   public int compare( final LookupToken token1, final LookupToken token2 ) {
      if ( token1.getStartOffset() < token2.getStartOffset() ) {
         return -1;
      } else if ( token1.getStartOffset() > token2.getStartOffset() ) {
         return 1;
      } else {
         // equal start offsets, now look at end offsets
         if ( token1.getEndOffset() < token2.getEndOffset() ) {
            return -1;
         } else if ( token1.getEndOffset() > token2.getEndOffset() ) {
            return 1;
         } else {
            // all offsets are equal
            return 0;
         }
      }
   }
}
