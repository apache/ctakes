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

import org.apache.ctakes.core.fsm.token.DecimalToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;

/**
 * Adapts JCas token annotation to interface expected by the Context Dependent
 * Tokenizer.
 *
 * @author Mayo Clinic
 */
public class DecimalTokenAdapter extends NumberTokenAdapter implements
																				DecimalToken {

	private double iv_val;

	static private final String negativeSign = "-";

	public DecimalTokenAdapter( NumToken nta ) {
		super( nta );


		if ( !nta.getCoveredText()
					.isEmpty() ) {
			String numAsString = removeCommas( nta.getCoveredText() );

			try {
				iv_val = Double.parseDouble( numAsString );
			} catch ( NumberFormatException nfE ) {
				if ( numAsString.startsWith( negativeSign ) ) {
					iv_val = -Double.MAX_VALUE;
				} else {
					iv_val = Double.MAX_VALUE;
				}
			}
		} else {
			iv_val = 0;
		}
	}

	public double getValue()
	{
		return iv_val;
	}
}
