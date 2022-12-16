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

import java.util.Comparator;

/**
 * Allows two MetaDataHit objects to be compared based on a specific metadata
 * value that they both contain.
 * 
 * @author Mayo Clinic
 */
public class MetaDataHitComparator implements Comparator<MetaDataHit>, MetaDataHitConst
{
    private int iv_type;
    private String iv_metaFieldName;
    private boolean iv_sortAscending;

    /**
     * Constructor
     * @param metaFieldName
     * @param type
     * @param sortAscending
     */
    public MetaDataHitComparator(String metaFieldName, int type, boolean sortAscending)
    {
        iv_metaFieldName = metaFieldName;
        iv_type = type;
        iv_sortAscending = sortAscending;
    }

    /**
     * Implementation
     */
    public int compare(MetaDataHit mdh1, MetaDataHit mdh2)
    {
        String mdv1 = mdh1.getMetaFieldValue(iv_metaFieldName);
        String mdv2 = mdh2.getMetaFieldValue(iv_metaFieldName);

        int comparison;
        switch (iv_type)
        {
        case INTEGER_TYPE:
            Integer int1 = new Integer(mdv1);
            Integer int2 = new Integer(mdv2);
            comparison = int1.compareTo(int2);
            break;
        case FLOAT_TYPE:
            Float float1 = new Float(mdv1);
            Float float2 = new Float(mdv2);
            comparison = float1.compareTo(float2);
            break;
        default:
            comparison = mdv1.compareTo(mdv2);
        	break;
        }
        
        if (iv_sortAscending)
        {
            return comparison;
        }
        return comparison * -1;
    }
}