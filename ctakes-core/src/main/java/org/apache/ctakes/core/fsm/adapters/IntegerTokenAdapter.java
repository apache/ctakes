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
package org.apache.ctakes.core.fsm.adapters;

import org.apache.ctakes.core.fsm.token.IntegerToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;

/**
 * Adapts JCas token annotation to interface expected by the Context Dependent
 * Tokenizer.
 * 
 * @author Mayo Clinic
 * 
 */
public class IntegerTokenAdapter extends NumberTokenAdapter implements
		IntegerToken
{
	private long iv_val;
	private double iv_double;

	static String negativeSign = "-";

	public IntegerTokenAdapter(NumToken nta)
	{
		super(nta);

		if (nta.getCoveredText().length() > 0) {
			String numAsString = removeCommas(nta.getCoveredText());
			
			// Parse as a long by default
			try {
			    iv_val = Long.parseLong(numAsString);
			} catch (NumberFormatException e) {
			    if (numAsString.startsWith(negativeSign)) {
			    	iv_val = Long.MIN_VALUE;
			    } else {
			    	iv_val = Long.MAX_VALUE;
			    }
			}

			try {
				iv_double = Double.parseDouble(numAsString);
			} catch (NumberFormatException e) {
				if (numAsString.startsWith(negativeSign)) {
					iv_double =  - Double.MAX_VALUE;
				} else {
					iv_double = Double.MAX_VALUE;
				}
			}
		} else {
			iv_val = 0;
			iv_double = 0;
		}
	}

	public long getValue()
	{
		return iv_val;
	}
	
	/* If a string of numbers is too long to be represented by a long, then
	 * this method could be used to get an approximation of the value as a double 
	 * Or use the covered text and process as you wish 
	 */
	public double getValueAsDouble() {
		return iv_double;
	}


}
