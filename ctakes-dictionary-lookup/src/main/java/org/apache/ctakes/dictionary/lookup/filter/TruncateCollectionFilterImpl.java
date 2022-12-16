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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Truncates objects in the collection. The collection is first sorted and then
 * the top N objects are retained. The rest of the objects are truncated.
 * 
 * @author Mayo Clinic
 */
public class TruncateCollectionFilterImpl implements CollectionFilter
{
    private int iv_mdhCount;
    private Comparator<MetaDataHit> iv_mdhComparator;

    /**
     * Constructor
     * 
     * @param mdhComparator
     *            Comparator for sorting MetaDataHit objects.
     * @param mdhCount
     *            Number of objects (N) to retain.
     */
    public TruncateCollectionFilterImpl(Comparator<MetaDataHit> mdhComparator, int mdhCount)
    {
        iv_mdhComparator = mdhComparator;
        iv_mdhCount = mdhCount;
    }

    /**
     * Implementation
     */
    public Collection<MetaDataHit> applyFilter(final Collection<MetaDataHit> metaDataHitCol) throws FilterException
    {
        if (iv_mdhCount >= metaDataHitCol.size())
        {
            return metaDataHitCol;
        }
        
        final List<MetaDataHit> mdhList = new ArrayList<>(metaDataHitCol);

        // sort the hits
        Collections.sort(mdhList, iv_mdhComparator);

        // truncate off the hits
        mdhList.subList(iv_mdhCount, mdhList.size()).clear();
        
        return mdhList;
    }
}
