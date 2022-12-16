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

import org.apache.ctakes.dictionary.lookup.DictionaryEngine;
import org.apache.ctakes.dictionary.lookup.DictionaryException;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;

/**
 *
 * @author Mayo Clinic
 */
public class MetaDataPostLookupFilterImpl implements PostLookupFilter
{
    private String[] iv_metaFieldNames;
    private DictionaryEngine iv_dictEngine;
    private boolean iv_excludeMatches = false;

    public MetaDataPostLookupFilterImpl(
        DictionaryEngine dictEngine,
        String[] metaFieldNames,
        boolean excludeMatches)
    {
        iv_dictEngine = dictEngine;
        iv_metaFieldNames = metaFieldNames;
        iv_excludeMatches = excludeMatches;
    }

    public boolean contains(MetaDataHit mdh) throws FilterException
    {
        String mdVal = getMetaDataValue(mdh);
        
        try
        {
            boolean isContained = iv_dictEngine.binaryLookup(mdVal);
            if (iv_excludeMatches)
            {
                return isContained;
            }
            return !isContained;
        }
        catch (DictionaryException ge)
        {
            throw new FilterException(ge);
        }
    }

    private String getMetaDataValue(MetaDataHit mdh) throws FilterException
    {
        for (int i = 0; i < iv_metaFieldNames.length; i++)
        {
            String mdVal = mdh.getMetaFieldValue(iv_metaFieldNames[i]);
            if (mdVal != null)
            {
                return mdVal;
            }
        }
        throw new FilterException(
            new Exception("Unable to extract meta data from MetaDataHit object."));
    }

}
