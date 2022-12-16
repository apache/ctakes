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
 * Created on Aug 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.apache.ctakes.drugner.fsm.output.util;

import org.apache.ctakes.core.fsm.output.BaseTokenImpl;

/**
 * @author Mayo Clinic
 *
 */
public class SubSectionIndicator extends BaseTokenImpl
{
	public static final int CONFIRMED_STATUS = 0;
    public static final int HISTORY_STATUS = 1;
    public static final int FAMILY_HISTORY_STATUS = 2;
    public static final int PROBABLE_STATUS = 3;
    private int iv_status;
    
    public SubSectionIndicator(int start, int end, int status)
    {
    	super(start, end);
        iv_status = status;
    }

    public int getStatus()
    {
        return iv_status;
    }
}
