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

import org.apache.ctakes.core.fsm.condition.DayNightWordCondition;
import org.apache.ctakes.core.fsm.condition.HourMinuteCondition;
import org.apache.ctakes.core.fsm.condition.IntegerRangeCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.output.TimeToken;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect times in the given input of
 * tokens.
 * 
 * @author Mayo Clinic
 */
public class TimeFSM {
	// constants
	private final int MIN_MINUTE = 0;
	private final int MAX_MINUTE = 59;
	private final int MIN_HOUR = 1;
	private final int MAX_HOUR = 12;

	// Set that consists of AM and PM
	private Set<String> iv_dayNightSet = new HashSet<String>();

	// contains the finite state machines
	private Set<Machine> iv_machineSet = new HashSet<Machine>();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public TimeFSM() {
		iv_dayNightSet.add("am");
		iv_dayNightSet.add("pm");

		iv_machineSet.add(getMachine());
	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * <li>3 PM</li>
	 * <li>3 P.M.</li>
	 * <li>3:05 PM</li>
	 * <li>3:05 P.M.</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State hourNumState = new NamedState("HOUR_NUM");
		State hourMinTextState = new NamedState("HOUR_MIN_TEXT");
		State ampmTextWithPeriodState = new NamedState("AM_PM_PERIOD_TEXT");

		Condition hourNumCondition = new IntegerRangeCondition(MIN_HOUR,
				MAX_HOUR);
		Condition hourMinCondition = new HourMinuteCondition(MIN_HOUR,
				MAX_HOUR, MIN_MINUTE, MAX_MINUTE);
		Condition dayNightCondition = new WordSetCondition(iv_dayNightSet,
				false);
		Condition dayNightWithPeriodCondition = new DayNightWordCondition();
		Condition closingPeriodCondition = new PunctuationValueCondition('.');

		startState.addTransition(hourNumCondition, hourNumState);
		startState.addTransition(hourMinCondition, hourMinTextState);
		startState.addTransition(new AnyCondition(), startState);

		hourMinTextState.addTransition(dayNightCondition, endState);
		hourMinTextState.addTransition(dayNightWithPeriodCondition,
				ampmTextWithPeriodState);
		hourMinTextState.addTransition(new AnyCondition(), startState);

		hourNumState.addTransition(dayNightCondition, endState);
		hourNumState.addTransition(dayNightWithPeriodCondition,
				ampmTextWithPeriodState);
		hourNumState.addTransition(new AnyCondition(), startState);

		ampmTextWithPeriodState.addTransition(closingPeriodCondition, endState);
		ampmTextWithPeriodState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @return Set of TimeToken objects.
	 */
	public Set<TimeToken> execute(List<? extends BaseToken> tokens)
			throws Exception {
		Set<TimeToken> timeSet = new HashSet<TimeToken>();

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
					TimeToken timeToken = new TimeToken(startToken
							.getStartOffset(), endToken.getEndOffset());
					timeSet.add(timeToken);
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

		return timeSet;
	}

}
