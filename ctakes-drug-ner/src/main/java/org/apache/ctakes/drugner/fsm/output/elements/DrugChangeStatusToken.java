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
package org.apache.ctakes.drugner.fsm.output.elements;



/**
 *
 * @author Mayo Clinic
 */
public class DrugChangeStatusToken extends BaseTokenImpl
{
   
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String INCREASEFROM = "increasefrom";
    public static final String DECREASEFROM = "decreasefrom";
    public static final String INCREASE = "increase";
    public static final String DECREASE = "decrease";
    public static final String NOCHANGE = "noChange";
    public static final String SUM = "add";
    public static final String MAX = "maximum";
    public static final String OTHER = "change";
    
    private String status = NOCHANGE;
    
    public DrugChangeStatusToken(int startOffset, int endOffset, String statusChange)
	{
		super(startOffset, endOffset);
		status = statusChange;
	}
    
    public String getDrugChangeStatus(){
    	return status;
    }
    
    public void setDrugChangeStatus(String localStatus){
    	status = localStatus;
    }
}


