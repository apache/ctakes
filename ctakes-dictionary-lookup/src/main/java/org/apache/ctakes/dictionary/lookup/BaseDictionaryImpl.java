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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Mayo Clinic
 * @deprecated Use {@link AbstractBaseDictionary}
 */
// Renamed AbstractBaseDictionary as this is not a full implementation. - 12/24/2012 SPF
// Added types and removed redundant method declaration - 12/24/2012 SPF
@Deprecated
public abstract class BaseDictionaryImpl implements Dictionary
{
    private Set<String> iv_metaFieldNames = new HashSet<>();

    protected Iterator<String> getMetaFieldNames()
    {
        return iv_metaFieldNames.iterator();
    }

    public void retainMetaData(String metaFieldName)
    {
    	iv_metaFieldNames.add(metaFieldName);
    }

    public abstract boolean contains(String text) throws DictionaryException;

    public abstract Collection<MetaDataHit> getEntries(String str)
        throws DictionaryException;

}
