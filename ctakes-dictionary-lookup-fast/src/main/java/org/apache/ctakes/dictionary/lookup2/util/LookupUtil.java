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

import java.util.ArrayList;
import java.util.List;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 12/16/13
 * @deprecated use core.util.StringUtil
 */
@Deprecated
final public class LookupUtil {

   private LookupUtil() {
   }

   /**
    * Splits a string using a character.  Faster than String.split( regex )
    *
    * @param line full text to split
    * @param c    character at which to split
    * @return array of substrings or the original line if there are no characters c
    */
   static public String[] fastSplit( final String line, final char c ) {
      int nextSplit = line.indexOf( c );
      if ( nextSplit < 0 ) {
         return new String[] { line };
      }
      final List<String> splits = new ArrayList<String>();
      int lastSplit = -1;
      while ( nextSplit > 0 ) {
         splits.add( line.substring( lastSplit + 1, nextSplit ) );
         lastSplit = nextSplit;
         nextSplit = line.indexOf( c, lastSplit + 1 );
      }
      if ( lastSplit + 1 < line.length() ) {
         splits.add( line.substring( lastSplit + 1 ) );
      }
      return splits.toArray( new String[ splits.size() ] );
   }

}
