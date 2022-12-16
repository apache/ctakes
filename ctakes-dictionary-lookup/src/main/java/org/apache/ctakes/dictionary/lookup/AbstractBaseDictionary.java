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
package org.apache.ctakes.dictionary.lookup;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Mayo Clinic
 */
public abstract class AbstractBaseDictionary implements Dictionary {

    private Set<String> _metaFieldNames = new HashSet<>();

   /**
    * TODO - get rid of the using class iterators and use Collection foreach
    * @return iterator of all metaField names
    */
    protected Iterator<String> getMetaFieldNames() {
        return _metaFieldNames.iterator();
    }

   /**
    * {@inheritDoc}
    */
   @Override
    public void retainMetaData( final String metaFieldName )  {
    	_metaFieldNames.add( metaFieldName );
    }

}
