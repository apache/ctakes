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
package org.apache.ctakes.drugner.fsm.machines.elements;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.core.fsm.condition.DisjoinCondition;
import org.apache.ctakes.core.fsm.condition.IntegerRangeCondition;
import org.apache.ctakes.core.fsm.condition.NumberCondition;
import org.apache.ctakes.core.fsm.condition.RangeCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.state.NonTerminalEndState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.elements.conditions.FrequencyUnitCondition;
import org.apache.ctakes.drugner.fsm.elements.conditions.RangeStrengthCondition;
import org.apache.ctakes.drugner.fsm.elements.conditions.RouteCondition;
import org.apache.ctakes.drugner.fsm.output.elements.FrequencyToken;
import org.apache.ctakes.drugner.fsm.states.util.IndentStartState;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect frequency data in the given
 * input of tokens.
 * 
 * @author Mayo Clinic
 */
public class FrequencyFSM {
	Set iv_frequencySet = new HashSet();

	Set iv_middleTermSet = new HashSet();

	Set iv_periodSet = new HashSet();

	Set iv_hyphenatedSet = new HashSet();

	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	// private DosagesFSM iv_dosages = new DosagesFSM();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public FrequencyFSM() {

		iv_frequencySet.add("once");
		iv_frequencySet.add("twice");
		iv_frequencySet.add("one");
		iv_frequencySet.add("two");
		iv_frequencySet.add("three");
		iv_frequencySet.add("four");
		iv_frequencySet.add("five");
		iv_frequencySet.add("six");
		iv_frequencySet.add("seven");
		iv_frequencySet.add("eight");
		iv_frequencySet.add("nine");

		iv_middleTermSet.add("a");
		iv_middleTermSet.add("an");
		iv_middleTermSet.add("as");
		iv_middleTermSet.add("in");
		iv_middleTermSet.add("the");
		iv_middleTermSet.add("each");
		iv_middleTermSet.add("times");
		iv_middleTermSet.add("time");
		iv_middleTermSet.add("per");
		iv_middleTermSet.add("every");
		iv_middleTermSet.add("at");
		iv_middleTermSet.add("x");

		iv_hyphenatedSet.add("one-time");
		iv_hyphenatedSet.add("two-times");
		iv_hyphenatedSet.add("three-times");
		iv_hyphenatedSet.add("four-times");
		iv_hyphenatedSet.add("five-times");
		iv_hyphenatedSet.add("six-times");
		iv_hyphenatedSet.add("seven-times");
		iv_hyphenatedSet.add("eight-times");
		iv_hyphenatedSet.add("nine-times");
		iv_hyphenatedSet.add("ten-times");

		iv_hyphenatedSet.add("once-a-day");
		iv_hyphenatedSet.add("once-a-week");
		iv_hyphenatedSet.add("twice-a-day");
		iv_hyphenatedSet.add("twice-a-week");
		iv_hyphenatedSet.add("once-daily");
		iv_hyphenatedSet.add("twice-daily");
		iv_hyphenatedSet.add("one-a-day");
		iv_hyphenatedSet.add("two-a-day");
		iv_hyphenatedSet.add("three-a-day");
		iv_hyphenatedSet.add("four-a-day");
		iv_hyphenatedSet.add("five-a-day");
		iv_hyphenatedSet.add("six-a-day");
		iv_hyphenatedSet.add("seven-a-day");
		iv_hyphenatedSet.add("eight-a-day");
		iv_hyphenatedSet.add("nine-a-day");
		iv_hyphenatedSet.add("once-weekly");
		iv_hyphenatedSet.add("twice-weekly");
		iv_hyphenatedSet.add("one-a-week");
		iv_hyphenatedSet.add("two-a-week");
		iv_hyphenatedSet.add("three-a-week");
		iv_hyphenatedSet.add("four-a-week");
		iv_hyphenatedSet.add("five-a-week");
		iv_hyphenatedSet.add("six-a-week");
		iv_hyphenatedSet.add("seven-a-week");
		iv_hyphenatedSet.add("eight-a-week");
		iv_hyphenatedSet.add("nine-a-week");
		iv_hyphenatedSet.add("once-monthly");
		iv_hyphenatedSet.add("twice-monthly");
		iv_hyphenatedSet.add("one-a-month");
		iv_hyphenatedSet.add("two-a-month");
		iv_hyphenatedSet.add("three-a-month");
		iv_hyphenatedSet.add("four-a-month");
		iv_hyphenatedSet.add("five-a-month");
		iv_hyphenatedSet.add("six-a-month");
		iv_hyphenatedSet.add("seven-a-month");
		iv_hyphenatedSet.add("eight-a-month");
		iv_hyphenatedSet.add("nine-a-month");
		iv_hyphenatedSet.add("once-hourly");
		iv_hyphenatedSet.add("twice-hourly");
		iv_hyphenatedSet.add("one-an-hour");
		iv_hyphenatedSet.add("two-an-hour");
		iv_hyphenatedSet.add("three-an-hour");
		iv_hyphenatedSet.add("four-an-hour");
		iv_hyphenatedSet.add("five-an-hour");
		iv_hyphenatedSet.add("six-an-hour");
		iv_hyphenatedSet.add("seven-an-hour");
		iv_hyphenatedSet.add("eight-an-hour");
		iv_hyphenatedSet.add("nine-an-hour");

		iv_hyphenatedSet.add("once-nightly");
		iv_hyphenatedSet.add("twice-nightly");
		iv_hyphenatedSet.add("once-every-day");
		iv_hyphenatedSet.add("once-daily");
		iv_hyphenatedSet.add("twice-daily");
		iv_hyphenatedSet.add("one-time-a-day");
		iv_hyphenatedSet.add("two-times-a-day");
		iv_hyphenatedSet.add("three-times-a-day");
		iv_hyphenatedSet.add("four-times-a-day");
		iv_hyphenatedSet.add("five-times-a-day");
		iv_hyphenatedSet.add("six-times-a-day");
		iv_hyphenatedSet.add("seven-times-a-day");
		iv_hyphenatedSet.add("eight-times-a-day");
		iv_hyphenatedSet.add("nine-times-a-day");
		iv_hyphenatedSet.add("once-every-week");
		iv_hyphenatedSet.add("twice-every-day");
		iv_hyphenatedSet.add("one-time-a-week");
		iv_hyphenatedSet.add("two-times-a-week");
		iv_hyphenatedSet.add("three-times-a-week");
		iv_hyphenatedSet.add("four-times-a-week");
		iv_hyphenatedSet.add("five-times-a-week");
		iv_hyphenatedSet.add("six-times-a-week");
		iv_hyphenatedSet.add("seven-times-a-week");
		iv_hyphenatedSet.add("eight-times-a-week");
		iv_hyphenatedSet.add("nine-times-a-week");
		iv_hyphenatedSet.add("once-every-hour");
		iv_hyphenatedSet.add("twice-every-hour");
		iv_hyphenatedSet.add("one-time-a-month");
		iv_hyphenatedSet.add("two-times-a-month");
		iv_hyphenatedSet.add("three-times-a-month");
		iv_hyphenatedSet.add("four-times-a-month");
		iv_hyphenatedSet.add("five-times-a-month");
		iv_hyphenatedSet.add("six-times-a-month");
		iv_hyphenatedSet.add("seven-times-a-month");
		iv_hyphenatedSet.add("eight-times-a-month");
		iv_hyphenatedSet.add("nine-times-a-month");
		iv_hyphenatedSet.add("one-time-each-hour");
		iv_hyphenatedSet.add("two-times-each-hour");
		iv_hyphenatedSet.add("three-times-each-hour");
		iv_hyphenatedSet.add("four-times-each-hour");
		iv_hyphenatedSet.add("five-times-each-hour");
		iv_hyphenatedSet.add("six-times-each-hour");
		iv_hyphenatedSet.add("seven-times-each-hour");
		iv_hyphenatedSet.add("eight-times-each-hour");
		iv_hyphenatedSet.add("nine-times-each-hour");

		iv_machineSet.add(getFrequencyMachine());


	}

	/**
	 * Gets a finite state machine that detects the following ('once', 'twice', #
	 * or text#) a day/week/month/year:
	 * <ol>
	 * <li>once a day</li>
	 * <li>three times a day</li>
	 * <li>once-a-day</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getFrequencyMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State ntFalseTermState = new IndentStartState("NON TERMINAL START");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntFalseTermState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State leftAbbreviateState = new NamedState("LEFT_FREQ");

		State lastTextState = new NamedState("RIGHT_FREQ");
		State middleATextState = new NamedState("MID_TEXT");
		State midTermState = new NamedState("MID_TERM");
		State termState = new NamedState("SKIP_TERM");

		Condition integerCondition = new IntegerRangeCondition(0,5);
		
		Condition rangeCombineCondition = new DisjoinCondition(
				new RangeCondition(), new RangeStrengthCondition());

//		Condition hyphenatedCondition = new WordSetCondition(iv_hyphenatedSet,
//				false);

		Condition secondMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, false);

		startState.addTransition(rangeCombineCondition, leftAbbreviateState);
		startState.addTransition(new WordSetCondition(iv_frequencySet,
				false), leftAbbreviateState);
		startState.addTransition(integerCondition, leftAbbreviateState);
		startState.addTransition(new WordSetCondition(iv_hyphenatedSet,
				false), endState);
		startState.addTransition(new AnyCondition(), startState);

		leftAbbreviateState.addTransition(new WordSetCondition(
				iv_middleTermSet, false), middleATextState);
		leftAbbreviateState.addTransition(new NumberCondition(),
				middleATextState);
		leftAbbreviateState.addTransition(new WordSetCondition(iv_frequencySet,
				false), midTermState);
		leftAbbreviateState.addTransition(new WordSetCondition(iv_hyphenatedSet,
				false), endState);
		leftAbbreviateState.addTransition(new FrequencyUnitCondition(),
				ntEndState);
		leftAbbreviateState.addTransition(new AnyCondition(), startState);


		midTermState.addTransition(new WordSetCondition(iv_middleTermSet, false), termState);
		midTermState.addTransition(new AnyCondition(),  startState);
		
		middleATextState
				.addTransition(secondMiddleTextCondition, lastTextState);

		middleATextState
				.addTransition(new FrequencyUnitCondition(), ntEndState);
		middleATextState.addTransition(new RouteCondition(), endState);
		middleATextState.addTransition(new AnyCondition(), startState);
		

		termState
				.addTransition(new FrequencyUnitCondition(), ntFalseTermState);
		termState.addTransition(new RouteCondition(), ntFalseTermState);
		termState.addTransition(new AnyCondition(), startState); 


		lastTextState.addTransition(new FrequencyUnitCondition(), ntEndState);
		lastTextState.addTransition(new RouteCondition(), ntEndState);
		lastTextState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);
		ntFalseTermState.addTransition(new AnyCondition(), startState);
		ntEndState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @param tokens
	 * @return Set FrequencyToken objects.
	 * @throws Exception
	 */
	public Set execute(List tokens, Set overrideSet) throws Exception {
		Set rangeSet = new HashSet();

		// maps a fsm to a token start index
		// key = fsm , value = token start index
		Map tokenStartMap = new HashMap();

		Iterator overrideTokenItr = overrideSet.iterator();
		// key = start offset, value = override BaseToken object
		Map overrideTokenMap = new HashMap();
		while (overrideTokenItr.hasNext()) {
			BaseToken t = (BaseToken) overrideTokenItr.next();
			Integer key = new Integer(t.getStartOffset());
			overrideTokenMap.put(key, t);
		}

		boolean overrideOn = false;
		int overrideEndOffset = -1;
		for (int i = 0; i < tokens.size(); i++) {
			BaseToken token = (BaseToken) tokens.get(i);

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
					token = (BaseToken) overrideTokenMap.get(key);
					overrideOn = true;
					overrideEndOffset = token.getEndOffset();
				}
			}

			Iterator machineItr = iv_machineSet.iterator();
			while (machineItr.hasNext()) {
				Machine fsm = (Machine) machineItr.next();

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
										BaseToken startToken = null;
					if (currentState instanceof IndentStartState) {
						startToken = (BaseToken) tokens
								.get(tokenStartIndex + 1);

					} else {
						startToken = (BaseToken) tokens.get(tokenStartIndex);

					}
					BaseToken endToken = null;
					if (currentState instanceof NonTerminalEndState) {
						endToken = (BaseToken) tokens.get(i - 1);
					} else {
						endToken = token;
					}
					FrequencyToken segmentToken = new FrequencyToken(startToken
							.getStartOffset(), endToken.getEndOffset());
					rangeSet.add(segmentToken);
					fsm.reset();
				}
			}
		}

		// cleanup
		tokenStartMap.clear();

		// reset machines
		Iterator itr = iv_machineSet.iterator();
		while (itr.hasNext()) {
			Machine fsm = (Machine) itr.next();
			fsm.reset();
		}

		return rangeSet;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @param tokens
	 * @return Set of FractionToken objects.
	 * @throws Exception
	 */
	public Set execute(List tokens) throws Exception {
		Set fractionSet = new HashSet();

		// maps a fsm to a token start index
		// key = fsm , value = token start index
		Map tokenStartMap = new HashMap();

		for (int i = 0; i < tokens.size(); i++) {
			BaseToken token = (BaseToken) tokens.get(i);

			Iterator machineItr = iv_machineSet.iterator();
			while (machineItr.hasNext()) {
				Machine fsm = (Machine) machineItr.next();

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
					BaseToken startToken = null;
					if (currentState instanceof IndentStartState) {
						startToken = (BaseToken) tokens
								.get(tokenStartIndex + 1);

					} else {
						startToken = (BaseToken) tokens.get(tokenStartIndex);

					}
					BaseToken endToken = null;
					if (currentState instanceof NonTerminalEndState) {
						endToken = (BaseToken) tokens.get(i - 1);
					} else {
						endToken = token;
					}
					FrequencyToken fractionToken = new FrequencyToken(
							startToken.getStartOffset(), endToken
									.getEndOffset());
					fractionSet.add(fractionToken);
					fsm.reset();
				}
			}
		}

		// cleanup
		tokenStartMap.clear();

		// reset machines
		Iterator itr = iv_machineSet.iterator();
		while (itr.hasNext()) {
			Machine fsm = (Machine) itr.next();
			fsm.reset();
		}

		return fractionSet;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @param tokens
	 * @return Set of RangeToken objects.
	 * @throws Exception
	 */
	public Set execute(List tokens, Set overrideSet1, Set overrideSet2)
			throws Exception {
		Set measurementSet = new HashSet();

		// maps a fsm to a token start index
		// key = fsm , value = token start index
		Map tokenStartMap = new HashMap();

		Iterator overrideTokenItr1 = overrideSet1.iterator();
		Iterator overrideTokenItr2 = overrideSet2.iterator();
		// key = start offset, value = override BaseToken object
		Map overrideTokenMap1 = new HashMap();
		Map overrideTokenMap2 = new HashMap();
		Map overrideBeginTokenMap1 = new HashMap();
		Map overrideBeginTokenMap2 = new HashMap();
		while (overrideTokenItr1.hasNext()) {
			BaseToken t = (BaseToken) overrideTokenItr1.next();
			Integer key = new Integer(t.getStartOffset());
			overrideTokenMap1.put(key, t);
		}

		while (overrideTokenItr2.hasNext()) {
			BaseToken t = (BaseToken) overrideTokenItr2.next();
			Integer key = new Integer(t.getStartOffset());
			overrideTokenMap2.put(key, t);
		}

		boolean overrideOn1 = false;
		boolean overrideOn2 = false;
		int overrideEndOffset1 = -1;
		int overrideEndOffset2 = -1;
		int tokenOffset1 = 0;
		int tokenOffset2 = 0;
		int anchorKey1 = 0;
		int anchorKey2 = 0;

		for (int i = 0; i < tokens.size(); i++) {
			BaseToken token = (BaseToken) tokens.get(i);

			Integer key = new Integer(token.getStartOffset());
			if (overrideOn1 && overrideOn2) {
				if (overrideEndOffset1 >= overrideEndOffset2)
					overrideOn1 = false;
				else
					overrideOn2 = false;
			}
			if (overrideOn1) {
				if (token.getStartOffset() >= overrideEndOffset1) {
					if (tokenOffset1 > 0)
						overrideBeginTokenMap1.put(new Integer(anchorKey1), new Integer(tokenOffset1));
					overrideOn1 = false;
					overrideEndOffset1 = -1;
				} else {
					tokenOffset1++;
					// step to next iteration of for loop
					continue;
				}
			} else if (overrideOn2) {
				if (token.getStartOffset() >= overrideEndOffset2) {
					if (tokenOffset2 > 0)
						overrideBeginTokenMap2.put(new Integer(anchorKey2), new Integer(tokenOffset2));
					overrideOn2 = false;
					overrideEndOffset2 = -1;
				} else {
					tokenOffset2++;
					// step to next iteration of for loop
					continue;
				}
			} else {
				if (overrideTokenMap1.containsKey(key)) {
					// override one or more tokens until the override
					// token is complete
					anchorKey1 = key.intValue();
					token = (BaseToken) overrideTokenMap1.get(key);
					overrideOn1 = true;
					overrideEndOffset1 = token.getEndOffset();
					tokenOffset1 = 0;
				}
				if (overrideTokenMap2.containsKey(key)) {
					// override one or more tokens until the override
					// token is complete
					anchorKey2 = key.intValue();
					token = (BaseToken) overrideTokenMap2.get(key);
					overrideOn2 = true;
					overrideEndOffset2 = token.getEndOffset();
					tokenOffset2 = 0;
				}
			}

			Iterator machineItr = iv_machineSet.iterator();
			while (machineItr.hasNext()) {
				Machine fsm = (Machine) machineItr.next();

				fsm.input(token);
				State currentState = fsm.getCurrentState();
				if (currentState.getStartStateFlag()) {
					tokenStartMap.put(fsm, new Integer(i));
					tokenOffset1 = 0;
					tokenOffset2 = 0;
				}
				if (currentState.getEndStateFlag()) {
					Object o = tokenStartMap.get(fsm);
					int tokenStartIndex;
					int globalOffset = 0;
					if (o == null) {
						// By default, all machines start with
						// token zero.
						tokenStartIndex = 0;
					} else {
						Integer tokenMap1 = new Integer(0);
						Integer tokenMap2 = new Integer(0);
					
						BaseToken lookUpOffset = (BaseToken) tokens.get(((Integer) o).intValue());
							
						if (overrideBeginTokenMap1.get(new Integer(lookUpOffset.getStartOffset())) != null){
							Integer offSet = (Integer) (overrideBeginTokenMap1.get(new Integer(lookUpOffset.getStartOffset())));
							tokenMap1 = new Integer(offSet.intValue()  + tokenMap1.intValue());
						}
						if (overrideBeginTokenMap2.get(new Integer(lookUpOffset.getStartOffset())) != null){
							Integer offSet = (Integer) (overrideBeginTokenMap2.get(new Integer(lookUpOffset.getStartOffset())));
							tokenMap2 = new Integer(offSet.intValue() + tokenMap2.intValue());
							}
						

						globalOffset = tokenMap1.intValue() + tokenMap2.intValue();
						tokenStartIndex = ((Integer) o).intValue() + globalOffset;
						// skip ahead over single token we don't want
						tokenStartIndex++;
					}

					BaseToken startToken = null;
					if (currentState instanceof IndentStartState) {
						startToken = (BaseToken) tokens
								.get(tokenStartIndex + 1);

					} else {
						startToken = (BaseToken) tokens.get(tokenStartIndex);

					}
					BaseToken endToken = null;
					if (currentState instanceof NonTerminalEndState) {
						endToken = (BaseToken) tokens.get(i - 1);
					} else {
						endToken = token;
					}
					FrequencyToken measurementToken = new FrequencyToken(
							startToken.getStartOffset(), endToken
									.getEndOffset());
					measurementSet.add(measurementToken);
					fsm.reset();
				}
			}
		}

		// cleanup
		tokenStartMap.clear();

		// reset machines
		Iterator itr = iv_machineSet.iterator();
		while (itr.hasNext()) {
			Machine fsm = (Machine) itr.next();
			fsm.reset();
		}

		return measurementSet;
	}
}
