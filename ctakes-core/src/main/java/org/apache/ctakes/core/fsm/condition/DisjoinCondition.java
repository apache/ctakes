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

import net.openai.util.fsm.Condition;

/**
 * 
 * @author Mayo Clinic
 */
@SuppressWarnings("serial")
public class DisjoinCondition extends Condition {
	private Condition iv_condition1;
	private Condition iv_condition2;

	public DisjoinCondition(Condition c1, Condition c2) {
		iv_condition1 = c1;
		iv_condition2 = c2;
	}

	/**
	 * Called to check if the conditional meets the criteria defined by this
	 * state.
	 */
	public boolean satisfiedBy(Object conditional) {
		return (iv_condition1.satisfiedBy(conditional) || iv_condition2
				.satisfiedBy(conditional));
	}
}
