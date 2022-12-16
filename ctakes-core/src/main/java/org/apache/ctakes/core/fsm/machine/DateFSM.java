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

import org.apache.ctakes.core.fsm.condition.CombineCondition;
import org.apache.ctakes.core.fsm.condition.IntegerRangeCondition;
import org.apache.ctakes.core.fsm.condition.NegateCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.output.DateToken;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.state.NonTerminalEndState;
import org.apache.ctakes.core.fsm.token.BaseToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect dates in the given input of
 * tokens.
 * 
 * @author Mayo Clinic
 */
public class DateFSM {
	// constants
	private final int MIN_MONTH = 1;
	private final int MAX_MONTH = 12;
	private final int MIN_DAY = 1;
	private final int MAX_DAY = 31;
	private final int MIN_YEAR = 1;
	private final int MAX_YEAR = 2999;

	// month names in FULL and SHORT formats
	private Set<String> iv_monthFullNameSet = new HashSet<String>();
	private Set<String> iv_monthShortNameSet = new HashSet<String>();

	// contains the finite state machines
	private Set<Machine> iv_machineSet = new HashSet<Machine>();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public DateFSM() {
		iv_monthFullNameSet.add("january");
		iv_monthFullNameSet.add("february");
		iv_monthFullNameSet.add("march");
		iv_monthFullNameSet.add("april");
		iv_monthFullNameSet.add("may");
		iv_monthFullNameSet.add("june");
		iv_monthFullNameSet.add("july");
		iv_monthFullNameSet.add("august");
		iv_monthFullNameSet.add("september");
		iv_monthFullNameSet.add("october");
		iv_monthFullNameSet.add("november");
		iv_monthFullNameSet.add("december");

		iv_monthShortNameSet.add("jan");
		iv_monthShortNameSet.add("feb");
		iv_monthShortNameSet.add("mar");
		iv_monthShortNameSet.add("apr");
		iv_monthShortNameSet.add("may");
		iv_monthShortNameSet.add("jun");
		iv_monthShortNameSet.add("jul");
		iv_monthShortNameSet.add("aug");
		iv_monthShortNameSet.add("sep");
		iv_monthShortNameSet.add("sept");
		iv_monthShortNameSet.add("oct");
		iv_monthShortNameSet.add("nov");
		iv_monthShortNameSet.add("dec");

		iv_machineSet.add(getNumericDateMachine());
		iv_machineSet.add(getTextualDateMachine());
	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * <li>10/15/2002</li>
	 * <li>10/15</li>
	 * <li>10-15-2002</li>
	 * <li>10-15</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getNumericDateMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);

		Machine machine = new Machine(startState);
		State monthNumState = new NamedState("MONTH_NUM");
		State monthDaySeparatorState = new NamedState("MONTH_DAY_SEP");
		State dayNumState = new NamedState("DAY_NUM");
		State dayYearSeparatorState = new NamedState("DAY_YEAR_SEP");

		Condition monthNumCondition = new IntegerRangeCondition(MIN_MONTH,
				MAX_MONTH);
		Condition mdFslashCondition = new PunctuationValueCondition('/');
		Condition mdDashCondition = new PunctuationValueCondition('-');
		Condition dyFslashCondition = new PunctuationValueCondition('/');
		Condition dyDashCondition = new PunctuationValueCondition('-');
		Condition dayNumCondition = new IntegerRangeCondition(MIN_DAY, MAX_DAY);
		Condition yearNumCondition = new IntegerRangeCondition(MIN_YEAR,
				MAX_YEAR);

		// condition that is not a fslash or dash
		Condition notFslashNotDashCondition = new CombineCondition(
				new NegateCondition(mdFslashCondition), new NegateCondition(
						mdDashCondition));

		startState.addTransition(monthNumCondition, monthNumState);
		startState.addTransition(new AnyCondition(), startState);

		monthNumState.addTransition(mdFslashCondition, monthDaySeparatorState);
		monthNumState.addTransition(mdDashCondition, monthDaySeparatorState);
		monthNumState.addTransition(new AnyCondition(), startState);

		monthDaySeparatorState.addTransition(dayNumCondition, dayNumState);
		monthDaySeparatorState.addTransition(new AnyCondition(), startState);

		dayNumState.addTransition(dyFslashCondition, dayYearSeparatorState);
		dayNumState.addTransition(dyDashCondition, dayYearSeparatorState);
		dayNumState.addTransition(notFslashNotDashCondition, ntEndState);
		dayNumState.addTransition(new AnyCondition(), startState);

		dayYearSeparatorState.addTransition(yearNumCondition, endState);
		dayYearSeparatorState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);
		ntEndState.addTransition(new AnyCondition(), startState);

		return machine;
	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * <li>October 15, 2002</li>
	 * <li>October 15 2002</li>
	 * <li>October 15</li>
	 * <li>October 2002</li>
	 * <li>Oct 15, 2002</li>
	 * <li>Oct 15 2002</li>
	 * <li>Oct 15</li>
	 * <li>Oct 2002</li>
	 * <li>Oct. 15, 2002</li>
	 * <li>Oct. 15 2002</li>
	 * <li>Oct. 15</li>
	 * <li>Oct. 2002</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getTextualDateMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State monthFullTextState = new NamedState("MONTH_FULL_TEXT");
		State monthShortTextState = new NamedState("MONTH_SHORT_TEXT");
		State dayNumState = new NamedState("DAY_NUM");
		State commaState = new NamedState("COMMA");
		State periodState = new NamedState("PERIOD");

		Condition monthFullTextCondition = new WordSetCondition(
				iv_monthFullNameSet, false);
		Condition monthShortTextCondition = new WordSetCondition(
				iv_monthShortNameSet, false);
		Condition dayNumCondition = new IntegerRangeCondition(MIN_DAY, MAX_DAY);
		Condition periodCondition = new PunctuationValueCondition('.');
		Condition yearNumCondition = new IntegerRangeCondition(MIN_YEAR,
				MAX_YEAR);
		Condition commaCondition = new PunctuationValueCondition(',');

		// condition for not being a comma or year number
		Condition notCommaNotYearNumCondition = new CombineCondition(
				new NegateCondition(commaCondition), new NegateCondition(
						yearNumCondition));

		// condition for being a year num and not a day num
		Condition yearNotDayNumCondition = new CombineCondition(
				yearNumCondition, new NegateCondition(dayNumCondition));

		startState.addTransition(monthFullTextCondition, monthFullTextState);
		startState.addTransition(monthShortTextCondition, monthShortTextState);
		startState.addTransition(new AnyCondition(), startState);

		monthFullTextState.addTransition(dayNumCondition, dayNumState);
		monthFullTextState.addTransition(yearNotDayNumCondition, endState);
		monthFullTextState.addTransition(new AnyCondition(), startState);

		monthShortTextState.addTransition(dayNumCondition, dayNumState);
		monthShortTextState.addTransition(periodCondition, periodState);
		monthShortTextState.addTransition(yearNotDayNumCondition, endState);
		monthShortTextState.addTransition(new AnyCondition(), startState);

		periodState.addTransition(dayNumCondition, dayNumState);
		periodState.addTransition(yearNotDayNumCondition, endState);
		periodState.addTransition(new AnyCondition(), startState);

		dayNumState.addTransition(yearNumCondition, endState);
		dayNumState.addTransition(commaCondition, commaState);
		dayNumState.addTransition(notCommaNotYearNumCondition, ntEndState);
		dayNumState.addTransition(new AnyCondition(), startState);

		commaState.addTransition(yearNumCondition, endState);
		commaState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);
		ntEndState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @return Set of DateToken objects.
	 */
	public Set<DateToken> execute(List<? extends BaseToken> tokens) throws Exception {
		Set<DateToken> dateSet = new HashSet<DateToken>();

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
					BaseToken endToken = null;
					if (currentState instanceof NonTerminalEndState) {
						endToken = tokens.get(i - 1);
					} else {
						endToken = token;
					}

					BaseToken startToken = tokens
							.get(tokenStartIndex);
					DateToken dateToken = new DateToken(startToken
							.getStartOffset(), endToken.getEndOffset());
					dateSet.add(dateToken);
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

		return dateSet;
	}

}
