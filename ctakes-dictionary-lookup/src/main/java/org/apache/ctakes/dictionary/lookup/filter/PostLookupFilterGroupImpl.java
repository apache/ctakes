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

import org.apache.ctakes.dictionary.lookup.MetaDataHit;

/**
 * Groups multiple PostLookupFilter objects together and applies boolean logic
 * to them.
 * 
 * @author Mayo Clinic
 */
public class PostLookupFilterGroupImpl implements PostLookupFilter
{
    public static final int OR_RELATION = 0;
    public static final int AND_RELATION = 1;

    private int iv_relation;
    private PostLookupFilter[] iv_plfArr;
    private boolean iv_excludeMatches;

    /**
     * Constructor
     * 
     * @param plfArr
     * @param relation
     */
    public PostLookupFilterGroupImpl(PostLookupFilter[] plfArr, int relation, boolean excludeMatches)
    {
        iv_relation = relation;
        iv_plfArr = plfArr;
        iv_excludeMatches = excludeMatches;
    }

    public boolean contains(MetaDataHit mdh) throws FilterException
    {
        boolean isContained = false;
        if (iv_relation == OR_RELATION)
        {
            isContained = false;
            for (int i = 0; i < iv_plfArr.length; i++)
            {
                if (iv_plfArr[i].contains(mdh))
                {
                    isContained = true;
                }
            }
        }
        else if (iv_relation == AND_RELATION)
        {
            isContained = true;
            for (int i = 0; i < iv_plfArr.length; i++)
            {
                if (iv_plfArr[i].contains(mdh) == false)
                {
                    isContained = false;
                }
            }
        }
        
        
        if (iv_excludeMatches)
        {
            return isContained;
        }
        return !isContained;    
    }
}