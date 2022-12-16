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

public class RouteElement {
	
	private String route = null;
	
	private int begOff = 0;
	
	private int endOff = 0;
    public static final String TOPICAL = "Topical";
    public static final String ORAL = "Enteral_Oral";//) Most drugs taken by mouth such as tablets or capsules.
    public static final String GASTRIC = "Enteral_Gastric"; // Examples include medications given through a gastric or duodenal tube.
    public static final String RECTAL = "Enteral_Rectal"; // Drugs in suppositories or enema form.
    public static final String INTRAVENOUS = "Parenteral_Intravenous";
    public static final String INTRAARTERIAL = "Parenteral_Intra-arterial";
    public static final String INTRAMUSCULAR = "Parenteral_Intramuscular";
    public static final String INTRACARDIAC = "Parenteral_Intracardiac";
    public static final String SUBCUTANEOUS = "Parenteral_Subcutaneous";// Medications such as insulin.
    public static final String INTRATHECAL = "Parenteral_Intrathecal"; // Into the spinal canal.
    public static final String INTRAPERITONEAL = "Parenteral_Intraperitoneal";  //In the abdominal region
    public static final String TRANSDERMAL = "Parenteral_Transdermal"; // Such as patches.
    public static final String TRANSMUCOSAL = "Parenteral_Transmucosal"; 
    
    
    public RouteElement(){
    	

		
	}
    
	public RouteElement(String rt, int beginOffset, int endOffset){
		route = rt;
		begOff = beginOffset;
		endOff = endOffset;
	}
	
	public String getRouteMention(){
		return route;
	}
	
	public int getBeginOffset(){
		return begOff;
	}
	
	public int getEndOffset(){
		return endOff;
	}

}
