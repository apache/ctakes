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
public class RouteToken extends BaseTokenImpl {
	
    public static final int TOPICAL = 0;
    public static final int ORAL = 1;
    public static final int GASTRIC = 2;
    public static final int RECTAL = 3;
    public static final int INTRAVENOUS = 4;
    public static final int INTRAARTERIAL = 5;
    public static final int INTRAMUSCULAR = 6;
    public static final int INTRACARDIAC = 7;
    public static final int SUBCUTANEOUS = 8;
    public static final int INTRATHECAL = 9;
    public static final int INTRAPERITONEAL = 10;
    public static final int TRANSDERMAL = 11;
    public static final int TRANSMUCOSAL = 12;
    
    private int current = 0;
    
	public RouteToken(int startOffset, int endOffset) {
		super(startOffset, endOffset);
	}
	
	public int getFormMethod(){
		return current;
	}
	
	public void setFormMethod(int localCurrent){
		current = localCurrent;
	}
}