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

import org.apache.ctakes.core.fsm.condition.IntegerCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.RomanNumeralCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.output.RangeToken;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect ranges in the given input of
 * tokens.
 * 
 * @author Mayo Clinic
 */
public class RangeFSM {
	// text fractions
	Set<String> iv_textNumberSet = new HashSet<String>();

	// contains the finite state machines
	private Set<Machine> iv_machineSet = new HashSet<Machine>();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public RangeFSM() {
		iv_textNumberSet.add("one");
		iv_textNumberSet.add("two");
		iv_textNumberSet.add("three");
		iv_textNumberSet.add("four");
		iv_textNumberSet.add("five");
		iv_textNumberSet.add("six");
		iv_textNumberSet.add("seven");
		iv_textNumberSet.add("eight");
		iv_textNumberSet.add("nine");
		iv_textNumberSet.add("ten");

		iv_machineSet.add(getMachine());
	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * <li>250-300</li>
	 * <li>I-IV</li>
	 * <li>two-three</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);
		State leftNumIntegerState = new NamedState("LEFT_NUM_INTEGER");
		State leftNumRomanState = new NamedState("LEFT_NUM_ROMAN");
		State leftNumTextState = new NamedState("LEFT_NUM_TEXT");
		State dashState = new NamedState("DASH");

		Condition leftIntCondition = new IntegerCondition();
		Condition rightIntCondition = new IntegerCondition();
		Condition dashCondition = new PunctuationValueCondition('-');
		Condition leftRomanNumeralCondition = new RomanNumeralCondition();
		Condition rightRomanNumeralCondition = new RomanNumeralCondition();
		Condition leftNumTextCondition = new WordSetCondition(iv_textNumberSet,
				false);
		Condition rightNumTextCondition = new WordSetCondition(
				iv_textNumberSet, false);

		startState.addTransition(leftIntCondition, leftNumIntegerState);
		startState.addTransition(leftRomanNumeralCondition, leftNumRomanState);
		startState.addTransition(leftNumTextCondition, leftNumTextState);
		startState.addTransition(new AnyCondition(), startState);

		leftNumIntegerState.addTransition(dashCondition, dashState);
		leftNumIntegerState.addTransition(new AnyCondition(), startState);

		leftNumRomanState.addTransition(dashCondition, dashState);
		leftNumRomanState.addTransition(new AnyCondition(), startState);

		leftNumTextState.addTransition(dashCondition, dashState);
		leftNumTextState.addTransition(new AnyCondition(), startState);

		dashState.addTransition(rightIntCondition, endState);
		dashState.addTransition(rightRomanNumeralCondition, endState);
		dashState.addTransition(rightNumTextCondition, endState);
		dashState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @return Set of RangeToken objects.
	 */
	public Set<RangeToken> execute(List<? extends BaseToken> tokens,
			Set<? extends BaseToken> overrideSet) throws Exception {
		Set<RangeToken> rangeSet = new HashSet<RangeToken>();

		// maps a fsm to a token start index
		// key = fsm , value = token start index
		Map<Machine, Integer> tokenStartMap = new HashMap<Machine, Integer>();

		Iterator<? extends BaseToken> overrideTokenItr = overrideSet.iterator();
		// key = start offset, value = override BaseToken object
		Map<Integer, BaseToken> overrideTokenMap = new HashMap<Integer, BaseToken>();
		while (overrideTokenItr.hasNext()) {
			BaseToken t = overrideTokenItr.next();
			Integer key = new Integer(t.getStartOffset());
			overrideTokenMap.put(key, t);
		}

		boolean overrideOn = false;
		int overrideEndOffset = -1;
		for (int i = 0; i < tokens.size(); i++) {
			BaseToken token = tokens.get(i);

			Integer key = new Integer(token.getStartOffset());

			if (overrideOn) {
				if (token.getStartOffset() >= overrideEndOffset) {
					overrideOn = false;
					overrideEndOffset = -1;
				} else {
					// step to next iteration of for loop
					continue;
				}
			} else {
				if (overrideTokenMap.containsKey(key)) {
					// override one or more tokens until the override
					// token is complete
					token = overrideTokenMap.get(key);
					overrideOn = true;
					overrideEndOffset = token.getEndOffset();
				}
			}

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
					RangeToken rangeToken = new RangeToken(startToken
							.getStartOffset(), endToken.getEndOffset());
					rangeSet.add(rangeToken);
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

		return rangeSet;
	}
}
