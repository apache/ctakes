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
public class FrequencyUnitToken extends BaseTokenImpl
{
    public static final double QUANTITY_PRN = 0;
    public static final double QUANTITY_ONE = 1;
    public static final double QUANTITY_TWO = 2;
    public static final double QUANTITY_THREE = 3;
    public static final double QUANTITY_FOUR = 4;
    public static final double QUANTITY_FIVE = 5;
    public static final double QUANTITY_SIX = 6;
    public static final double QUANTITY_SEVEN = 7;
    public static final double QUANTITY_EIGHT = 8;
    public static final double QUANTITY_NINE = 9;
    public static final double QUANTITY_TEN = 10;
    public static final double QUANTITY_ELEVEN = 11;
    public static final double QUANTITY_24 = 24;
    public static final double QUANTITY_WEEKLY = 0.14;
    public static final double QUANTITY_BIWEEKLY = 0.07;
    public static final double QUANTITY_MONTHLY = 0.03;
    public static final double QUANTITY_EVERY_OTHER_DAY = 0.5;
    public static final double QUANTITY_YEARLY = 0.003;
        
    
    private double iv_quantity;

	public FrequencyUnitToken(int startOffset, int endOffset, double quantity)
	{
		super(startOffset, endOffset);
	    iv_quantity = quantity;	
	}

    public double getFrequencyUnitQuantity()
    {
        return iv_quantity;
    }

}
