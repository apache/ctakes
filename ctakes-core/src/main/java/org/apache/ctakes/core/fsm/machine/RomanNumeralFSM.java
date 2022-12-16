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
package org.apache.ctakes.core.fsm.machine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.core.fsm.output.RomanNumeralToken;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.core.fsm.token.WordToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect roman numerals in the given
 * input of tokens.
 * 
 * @author Mayo Clinic
 */
public class RomanNumeralFSM {

	// contains the finite state machines
	private Set<Machine> iv_machineSet = new HashSet<Machine>();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public RomanNumeralFSM() {
		iv_machineSet.add(getMachine());
	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * <li>III</li>
	 * <li>iii</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		startState.addTransition(new RomanNumeralCondition(), endState);
		startState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @return Set of RomanNumeralToken objects.
	 */
	public Set<RomanNumeralToken> execute(List<? extends BaseToken> tokens)
			throws Exception {
		Set<RomanNumeralToken> romanNumeralSet = new HashSet<RomanNumeralToken>();

		// maps a fsm to a token start index
		// key = fsm , value = token start index
		Map<Machine, Integer> tokenStartMap = new HashMap<Machine, Integer>();

		for (int i = 0; i < tokens.size(); i++) {
			BaseToken token = tokens.get(i);

			Iterator<Machine> machineItr = iv_machineSet.iterator();
			while (machineItr.hasNext()) {
				Machine fsm = machineItr.next();

				fsm.input(token);

				State currentState = fsm.getCurrentState();
				if (currentState.getStartStateFlag()) {
					tokenStartMap.put(fsm, new Integer(i));
				}
				if (currentState.getEndStateFlag()) {
					Object o = tokenStartMap.get(fsm);
					int tokenStartIndex;
					if (o == null) {
						// By default, all machines start with
						// token zero.
						tokenStartIndex = 0;
					} else {
						tokenStartIndex = ((Integer) o).intValue();
						// skip ahead over single token we don't want
						tokenStartIndex++;
					}

					BaseToken startToken = tokens.get(tokenStartIndex);
					BaseToken endToken = token;
					RomanNumeralToken rnToken = new RomanNumeralToken(
							startToken.getStartOffset(), endToken
									.getEndOffset());
					romanNumeralSet.add(rnToken);
					fsm.reset();
				}
			}
		}

		// cleanup
		tokenStartMap.clear();

		// reset machines
		Iterator<Machine> itr = iv_machineSet.iterator();
		while (itr.hasNext()) {
			Machine fsm = itr.next();
			fsm.reset();
		}

		return romanNumeralSet;
	}

	@SuppressWarnings("serial")
	class RomanNumeralCondition extends Condition {
		public boolean satisfiedBy(Object conditional) {
			if (conditional instanceof WordToken) {
				WordToken wt = (WordToken) conditional;
				return isRomanNumeral(wt.getText());
			}

			return false;
		}

		/**
		 * Validates whether the given string is a roman numeral.
		 * 
		 * @param str
		 * @return
		 */
		private boolean isRomanNumeral(String str) {
			str = str.toUpperCase();
			for (int i = 0; i < str.length(); i++) {
				char currentChar = str.charAt(i);
				if ((currentChar != 'I') && (currentChar != 'V')
						&& (currentChar != 'X') && (currentChar != 'L')
						&& (currentChar != 'C') && (currentChar != 'D')
						&& (currentChar != 'M')) {
					return false;
				}
			}
			return true;
		}
	}

}
