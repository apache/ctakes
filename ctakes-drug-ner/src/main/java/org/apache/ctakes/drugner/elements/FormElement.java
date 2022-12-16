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

public class FormElement {
	
	private String form = null;
	
	private int begOff = 0;
	
	private int endOff = 0;
    public static final String AEROSOL = "aerosol";
    public static final String CAPSULE = "capsule";
    public static final String CREAM = "cream";
    public static final String ELIXIR = "elixir";
    public static final String EMULSION = "emulsion";
    public static final String ENEMA = "enema";
    public static final String GEL = "gel";
    public static final String IMPLANT = "implant";
    public static final String INHALANT = "inhalant";
    public static final String INJECTION = "injection";
    public static final String LIQUID = "liquid";
    public static final String LOTION = "lotion";
    public static final String LOZENGE = "lozenge";
    public static final String OINTMENT = "ointment";
    public static final String PATCH = "patch";
    public static final String PILL = "pill";
    public static final String POWDER = "powder";
    public static final String SHAMPOO = "shampoo";
    public static final String SOAP = "soap";
    public static final String SOLUTION = "solution";
    public static final String SPRAY = "spray";
    public static final String SUPPOSITORY = "suppository";
    public static final String SYRUP = "syrup";
    public static final String TABLET = "tablet";

	public static final String SYRINGE = "syringe"; 
    
    public FormElement(){
    	
    
		
	}
    
	public FormElement(String fm, int beginOffset, int endOffset){
		form = fm;
		begOff = beginOffset;
		endOff = endOffset;
	}
	
	public String getFormMention(){
		return form;
	}
	
	public int getBeginOffset(){
		return begOff;
	}
	
	public int getEndOffset(){
		return endOff;
	}

}
