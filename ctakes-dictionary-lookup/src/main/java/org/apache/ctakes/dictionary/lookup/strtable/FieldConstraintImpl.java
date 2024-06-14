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
/*
 * Created on May 20, 2005
 *
 */
package org.apache.ctakes.dictionary.lookup.strtable;

/**
 * @author Mayo Clinic
 * 
 */
@SuppressWarnings( "unchecked" )
public class FieldConstraintImpl implements FieldConstraint
{
    public static final int EQ_OP = 0;
    public static final int LT_OP = 1;
    public static final int LTEQ_OP = 2;
    public static final int GT_OP = 3;
    public static final int GTEQ_OP = 4;

    private final String iv_fieldName;
    private final Object iv_fieldValue;
    private final int iv_op;
    private final Class<?> iv_fieldValueClass;

    public FieldConstraintImpl(String fieldName, int op, String fieldValue,
            Class<?> fieldValueClass)
    {
        iv_fieldName = fieldName;
        iv_op = op;
        iv_fieldValueClass = fieldValueClass;
        iv_fieldValue = convertFieldValue(fieldValue);
    }

    public boolean isConstrained(String fieldName, String fieldValue)
    {
        if (iv_fieldName.equals(fieldName))
        {
            Object curfieldValueObj = convertFieldValue(fieldValue);

            Comparable<Object> c1 = (Comparable<Object>) iv_fieldValue;
            Comparable<Object> c2 = (Comparable<Object>) curfieldValueObj;

            int comparison = c2.compareTo(c1);

            if ((comparison == 0)
                    && ((iv_op == EQ_OP) || (iv_op == LTEQ_OP) || (iv_op == GTEQ_OP)))
            {
                return true;
            }
            else if ((comparison < 0)
                    && ((iv_op == LT_OP) || (iv_op == LTEQ_OP)))
            {
                return true;
            }
            else if ((comparison > 0)
                    && ((iv_op == GT_OP) || (iv_op == GTEQ_OP)))
            {
                return true;
            }
        }

        return false;
    }

    private Object convertFieldValue(String str)
    {
        try {
            if ( iv_fieldValueClass.equals( Integer.class ) ) {
                return Integer.parseInt( str );
            }
            else if ( iv_fieldValueClass.equals( Float.class ) ) {
                return Float.parseFloat( str );
            }
            else if ( iv_fieldValueClass.equals( Double.class ) ) {
                return Double.parseDouble( str );
            }
        } catch ( NumberFormatException nfE ) {
            return str;
        }
        return str;
    }
}