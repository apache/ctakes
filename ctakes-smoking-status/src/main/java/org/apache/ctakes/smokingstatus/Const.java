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
package org.apache.ctakes.smokingstatus;

/**
 * Constants used by the SmokingStatus project.
 * @author Mayo Clinic
 *
 */
public interface Const
{
    // TODO make this an enum
    public static final String CLASS_PAST_SMOKER = "PAST_SMOKER";
    public static final String CLASS_CURR_SMOKER = "CURRENT_SMOKER";
    public static final String CLASS_SMOKER = "SMOKER";
    public static final String CLASS_NON_SMOKER = "NON_SMOKER";
    public static final String CLASS_UNKNOWN = "UNKNOWN";    
    public static final String CLASS_KNOWN = "KNOWN";
    
    public static final int CLASS_CURR_SMOKER_INT = 1;
    public static final int CLASS_PAST_SMOKER_INT = 2;
    public static final int CLASS_SMOKER_INT = 3;
}
