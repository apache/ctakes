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

/**
 * @author Mayo Clinic
 */
public interface MetaDataHitConst
{
    public static final int INTEGER_TYPE = 0;
    public static final int FLOAT_TYPE = 1;
    public static final int STRING_TYPE = 2;
    
    public static final int EQ_OP = 0;
    public static final int LT_OP = 1;
    public static final int LTEQ_OP = 2;
    public static final int GT_OP = 3;
    public static final int GTEQ_OP = 4;    
}
