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
package org.apache.ctakes.core.fsm.condition;

import org.apache.ctakes.core.fsm.token.DecimalToken;

import net.openai.util.fsm.Condition;

/**
 * 
 * @author Mayo Clinic
 */
@SuppressWarnings("serial")
public class DecimalValueCondition extends Condition {
	private double iv_num;

	public DecimalValueCondition(double value) {
		iv_num = value;
	}

	/**
	 * Called to check if the conditional meets the criteria defined by this
	 * state.
	 */
	public boolean satisfiedBy(Object conditional) {
		if (conditional instanceof DecimalToken) {
			DecimalToken t = (DecimalToken) conditional;
			if (t.getValue() == iv_num) {
				return true;
			}
		}

		return false;
	}
}
