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
package org.apache.ctakes.dictionary.lookup.filter;

import javax.annotation.concurrent.Immutable;
import java.util.Set;

/**
 * @author Mayo Clinic
 */
@Immutable
final public class StringPreLookupFilterImpl implements PreLookupFilter {
   final private Set<String> _strSet;

   public StringPreLookupFilterImpl( final Set<String> strSet ) {
      _strSet = strSet;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean contains( final String text ) throws FilterException {
      return _strSet.contains( text );
   }

}
