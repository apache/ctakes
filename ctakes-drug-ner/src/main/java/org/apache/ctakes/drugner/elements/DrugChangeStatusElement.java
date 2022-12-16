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
package org.apache.ctakes.drugner.elements;

public class DrugChangeStatusElement {
	
	private String drugChange = null;
	
	private int begOff = 0;
	
	private int endOff = 0;
	public static final String STOP_STATUS = "stop";
	public static final String START_STATUS = "start";
	public static final String INCREASE_STATUS = "increase";
	public static final String DECREASE_STATUS = "decrease";
	public static final String NOCHANGE_STATUS = "noChange";
	public static final String SUMMATION_STATUS = "add";
	public static final String MAXIMUM_STATUS = "maximum";
	
    public DrugChangeStatusElement(){
    	
    
		
	}
    
	public DrugChangeStatusElement(String status, int beginOffset, int endOffset){
		drugChange = status;
		begOff = beginOffset;
		endOff = endOffset;
	}
	
	public String getDrugChangeStatus(){
		return drugChange;
	}
	
	public int getBeginOffset(){
		return begOff;
	}
	
	public int getEndOffset(){
		return endOff;
	}

}
