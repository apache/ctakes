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
import org.apache.ctakes.dictionary.lookup.MetaDataHitConst;

/**
 * A filter that uses a value to filter against a MetaDataHit.
 * 
 * @author Mayo Clinic
 */
public class SetValuePostLookupFilterImpl
        implements PostLookupFilter, MetaDataHitConst
{
    private Object iv_value;
    private String iv_metaFieldName;
    private int iv_metaFieldType;
    private int iv_metaFieldOp;
    private boolean iv_excludeMatches = false;

    public SetValuePostLookupFilterImpl(String metaFieldName,
            int metaFieldType, int metaFieldOp, boolean excludeMatches)
    {
        iv_metaFieldName = metaFieldName;
        iv_metaFieldType = metaFieldType;
        iv_metaFieldOp = metaFieldOp;
        iv_excludeMatches = excludeMatches;
    }

    public void setValue(String value)
    {
        if (iv_metaFieldType == MetaDataHitConst.INTEGER_TYPE)
        {
            iv_value = new Integer(value);
        }
        else if (iv_metaFieldType == MetaDataHitConst.FLOAT_TYPE)
        {
            iv_value = new Float(value);
        }
        else 
        {
            iv_value = value;
        }
    }

    public boolean contains(MetaDataHit mdh) throws FilterException
    {
        if (iv_value == null)
        {
            throw new FilterException(new Exception(
                    "Value has not been set for PostLookupFilter."));
        }

        String mdVal = getMetaDataValue(mdh);

        boolean isContained = isContained(mdVal);
        if (iv_excludeMatches)
        {
            return isContained;
        }
        return !isContained;
    }

    /**
     * Helper method
     * 
     * @param mdVal
     * @return
     */
    private boolean isContained(String mdVal)
    {
        if (iv_metaFieldType == MetaDataHitConst.INTEGER_TYPE)
        {
            Integer mdValInteger = new Integer(mdVal);
            Integer refValInteger = (Integer) iv_value;

            int comparison = refValInteger.compareTo(mdValInteger);
            return applyOp(comparison);
        }
        else if (iv_metaFieldType == MetaDataHitConst.FLOAT_TYPE)
        {
            Float mdValFloat = new Float(mdVal);
            Float refValFloat = (Float) iv_value;

            int comparison = refValFloat.compareTo(mdValFloat);
            return applyOp(comparison);
        }
        else if (iv_metaFieldType == MetaDataHitConst.STRING_TYPE)
        {
            String refValStr = (String) iv_value;

            int comparison = refValStr.compareTo(mdVal);
            return applyOp(comparison);
        }

        return false;
    }

    /**
     * Checks the comparison value against the specified operation. Based on
     * this information, being filtered or not can be determined.
     * 
     * @param comparisonVal
     * @return
     */
    private boolean applyOp(int comparisonVal)
    {
        if (((iv_metaFieldOp == MetaDataHitConst.EQ_OP)
                || (iv_metaFieldOp == MetaDataHitConst.LTEQ_OP) || (iv_metaFieldOp == MetaDataHitConst.GTEQ_OP))
                && (comparisonVal == 0))
        {
            return true;
        }
        else if (((iv_metaFieldOp == MetaDataHitConst.LT_OP) || (iv_metaFieldOp == MetaDataHitConst.LTEQ_OP))
                && (comparisonVal > 0))
        {
            return true;
        }
        else if (((iv_metaFieldOp == MetaDataHitConst.GT_OP) || (iv_metaFieldOp == MetaDataHitConst.GTEQ_OP))
                && (comparisonVal < 0))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private String getMetaDataValue(MetaDataHit mdh) throws FilterException
    {
        String mdVal = mdh.getMetaFieldValue(iv_metaFieldName);
        if (mdVal != null)
        {
            return mdVal;
        }
        throw new FilterException(new Exception(
                "Unable to extract meta data from MetaDataHit object."));
    }
}