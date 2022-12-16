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

import java.util.Set;

import org.apache.ctakes.core.fsm.token.TextToken;

import net.openai.util.fsm.Condition;

/**
 * 
 * @author Mayo Clinic
 */
@SuppressWarnings("serial")
public class TextSetCondition extends Condition {
	private Set<String> iv_textSet;
	private boolean iv_isCaseSensitive;

	public TextSetCondition(Set<String> textSet, boolean isCaseSensitive) {
		iv_textSet = textSet;
	}

	/**
	 * Called to check if the conditional meets the criteria defined by this
	 * state.
	 */
	public boolean satisfiedBy(Object conditional) {
		if (conditional instanceof TextToken) {
			TextToken t = (TextToken) conditional;
			String text = t.getText();
			if (!iv_isCaseSensitive) {
				text = text.toLowerCase();
			}

			if (iv_textSet.contains(text)) {
				return true;
			}
		}

		return false;
	}
}
