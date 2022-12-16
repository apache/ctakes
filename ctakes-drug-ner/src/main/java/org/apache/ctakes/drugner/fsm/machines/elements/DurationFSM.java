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

import org.apache.ctakes.core.fsm.condition.NumberCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.elements.conditions.RangeStrengthCondition;
import org.apache.ctakes.drugner.fsm.output.elements.DurationToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect duration data in the given
 * input of tokens.
 * 
 * @author Mayo Clinic
 */
public class DurationFSM {

	Set iv_middleNumericTermSet = new HashSet();

	Set iv_periodSet = new HashSet();

	Set iv_appendWordSet = new HashSet();

	Set iv_specifiedWordSet = new HashSet();
	
	Set iv_combinedSet = new HashSet();

	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public DurationFSM() {

		iv_middleNumericTermSet.add("one");
		iv_middleNumericTermSet.add("two");
		iv_middleNumericTermSet.add("three");
		iv_middleNumericTermSet.add("four");
		iv_middleNumericTermSet.add("five");
		iv_middleNumericTermSet.add("six");
		iv_middleNumericTermSet.add("seven");
		iv_middleNumericTermSet.add("eight");
		iv_middleNumericTermSet.add("nine");
		iv_middleNumericTermSet.add("ten");
		iv_middleNumericTermSet.add("eleven");
		iv_middleNumericTermSet.add("twelve");
		iv_middleNumericTermSet.add("half");
		iv_middleNumericTermSet.add("few");
		iv_middleNumericTermSet.add("couple");
		iv_middleNumericTermSet.add("once");
		iv_middleNumericTermSet.add("twice");
		
		iv_combinedSet.add("one-day");
		iv_combinedSet.add("two-days");
		iv_combinedSet.add("three-days");
		iv_combinedSet.add("four-days");
		iv_combinedSet.add("five-days");
		iv_combinedSet.add("six-days");
		iv_combinedSet.add("seven-days");
		iv_combinedSet.add("eight-days");
		iv_combinedSet.add("nine-days");
		iv_combinedSet.add("half-days");
		iv_combinedSet.add("few-days");
		iv_combinedSet.add("couple-days");
		iv_combinedSet.add("once-a-day");
		iv_combinedSet.add("twice-a-day");
		
		iv_combinedSet.add("one-week");
		iv_combinedSet.add("1-week");
		iv_combinedSet.add("two-weeks");
		iv_combinedSet.add("2-weeks");
		iv_combinedSet.add("three-weeks");
		iv_combinedSet.add("3-weeks");
		iv_combinedSet.add("four-weeks");
		iv_combinedSet.add("4-weeks");
		iv_combinedSet.add("five-weeks");
		iv_combinedSet.add("5-weeks");
		iv_combinedSet.add("six-weeks");
		iv_combinedSet.add("six-weeks");
		iv_combinedSet.add("seven-weeks");
		iv_combinedSet.add("7-weeks");
		iv_combinedSet.add("eight-weeks");
		iv_combinedSet.add("8-weeks");
		iv_combinedSet.add("nine-weeks");
		iv_combinedSet.add("9-weeks");
		iv_combinedSet.add("half-weeks");
		iv_combinedSet.add("few-weeks");
		iv_combinedSet.add("couple-weeks");
		iv_combinedSet.add("once-a-week");
		iv_combinedSet.add("twice-a-week");
		
		iv_combinedSet.add("one-month");
		iv_combinedSet.add("one-day");
		iv_combinedSet.add("1-month");
		iv_combinedSet.add("two-months");
		iv_combinedSet.add("2-months");
		iv_combinedSet.add("three-months");
		iv_combinedSet.add("four-months");
		iv_combinedSet.add("five-months");
		iv_combinedSet.add("six-months");
		iv_combinedSet.add("seven-months");
		iv_combinedSet.add("eight-months");
		iv_combinedSet.add("nine-months");
		iv_combinedSet.add("half-months");
		iv_combinedSet.add("few-months");
		iv_combinedSet.add("couple-months");
		iv_combinedSet.add("once-a-month");
		iv_combinedSet.add("twice-a-month");

		iv_specifiedWordSet.add("for");
		iv_specifiedWordSet.add("until");
		iv_specifiedWordSet.add("continue");
		iv_specifiedWordSet.add("continued");
		iv_specifiedWordSet.add("over");
		iv_specifiedWordSet.add("and");
		iv_specifiedWordSet.add("of");
		iv_specifiedWordSet.add("X");

		iv_appendWordSet.add("an");
		iv_appendWordSet.add("a");
		iv_appendWordSet.add("the");
		iv_appendWordSet.add("next");
		iv_appendWordSet.add("up");
		iv_appendWordSet.add("about");
		iv_appendWordSet.add("additional");
		iv_appendWordSet.add("approximately");

		iv_periodSet.add("year");
		iv_periodSet.add("one-year");
		iv_periodSet.add("years");
		iv_periodSet.add("month");
		iv_periodSet.add("months");
		iv_periodSet.add("week");
		iv_periodSet.add("weeks");
		iv_periodSet.add("day");
		iv_periodSet.add("days");
		iv_periodSet.add("yr");
		iv_periodSet.add("yrs");
		iv_periodSet.add("mo");
		iv_periodSet.add("wk");
		iv_periodSet.add("mos");
		iv_periodSet.add("wks");

		iv_machineSet.add(geDurationMachine());
		iv_machineSet.add(geDuration2ndMachine());

	}

	/**
	 * Gets a finite state machine that detects the following ('once', 'twice', #
	 * or text#) a day/week/month/year:
	 * <ol>
	 * <li>for 3 years</li>
	 * <li>until gone</li>
	 * <li>for-six-months</li>
	 * <li>for an additional eight weeks</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine geDuration2ndMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State startAbbreviateState = new NamedState("START_ABBR");
		State middleTermState = new NamedState("MID_TERM");
		State finalTextState = new NamedState("FIN_TEXT");
		State finalTermState = new NamedState("FIN_TERM");
		State finalAppendState = new NamedState("FIN_APPEND");
		State anotherAppendState = new NamedState("ANOTHER_APPEND");
		
	//	State firstDashState = new NamedState("FIRST_DASH");
	//	State secondDashState = new NamedState("SECOND_DASH");
	
	//	Condition firstDashCondition = new PunctuationValueCondition('-');
	//	Condition secondDashCondition = new PunctuationValueCondition('-');
	
		Condition numericTextCondition = new WordSetCondition(
				iv_middleNumericTermSet, true);
		Condition periodCondition = new WordSetCondition(iv_periodSet, true);
		Condition periodAdditionalCondition = new WordSetCondition(iv_periodSet, true);
		Condition periodAdditional2Condition = new WordSetCondition(iv_periodSet, true);
		Condition specificWordCondition = new WordSetCondition(
				iv_specifiedWordSet, false);
		Condition containsAppendTermCondition = new WordSetCondition(
				iv_appendWordSet, true);
		Condition containsSecondAppendTermCondition = new WordSetCondition(
				iv_appendWordSet, true);
	
		startState.addTransition(specificWordCondition, startAbbreviateState);
		startState.addTransition(new AnyCondition(), startState);
	
		startAbbreviateState.addTransition(containsAppendTermCondition,
				middleTermState);

	//	startAbbreviateState.addTransition(firstDashCondition, firstDashState);
	    startAbbreviateState.addTransition(new NumberCondition(),
				finalTermState);
		
	
		startAbbreviateState.addTransition(new AnyCondition(), startState);
	
	
		middleTermState.addTransition(new RangeStrengthCondition(), anotherAppendState);
		middleTermState.addTransition(containsSecondAppendTermCondition, finalTermState);
		middleTermState.addTransition(numericTextCondition, finalAppendState);
		middleTermState.addTransition(new NumberCondition(), anotherAppendState);
		
		middleTermState.addTransition(new AnyCondition(), startState);
	
	//	secondDashState.addTransition(containsSecondAppendTermCondition, finalTextState);
	//	secondDashState.addTransition(periodCondition, endState);
	//	secondDashState.addTransition(new AnyCondition(), startState);
	
		//finalTermState.addTransition(periodCondition, endState);
		finalTermState.addTransition(new RangeStrengthCondition(), finalTextState);
		finalTermState.addTransition(numericTextCondition, finalTextState);
		finalTermState.addTransition(new NumberCondition(), finalTextState);
		
		finalTermState.addTransition(new AnyCondition(), startState);
		
		finalAppendState.addTransition(periodAdditionalCondition, endState);
		finalAppendState.addTransition(new AnyCondition(), startState);
		
		anotherAppendState.addTransition(periodAdditional2Condition, endState);
		anotherAppendState.addTransition(new AnyCondition(), startState);
		
		finalTextState.addTransition(periodCondition, endState);
		finalTextState.addTransition(new AnyCondition(), startState);
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following ('once', 'twice', #
	 * or text#) a day/week/month/year:
	 * <ol>
	 * <li>for 3 years</li>
	 * <li>until gone</li>
	 * <li>for-six-months</li>
	 * <li>for an additional eight weeks</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine geDurationMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State leftAbbreviateState = new NamedState("LEFT_ABBR");
		State lastTextState = new NamedState("LAST_TEXT");
		State middleTextState = new NamedState("MID_TEXT");
		State firstDashState = new NamedState("FIRSTDASH");
		State secondDashState = new NamedState("SECONDDASH");

		Condition firstDashCondition = new PunctuationValueCondition('-');
		Condition secondDashCondition = new PunctuationValueCondition('-');

		Condition firstMiddleTextCondition = new WordSetCondition(
				iv_middleNumericTermSet, true);
		Condition secondMiddleTextCondition = new WordSetCondition(
				iv_middleNumericTermSet, true);
		Condition thirdMiddleTextCondition = new WordSetCondition(
				iv_middleNumericTermSet, true);
		Condition fourthMiddleTextCondition = new WordSetCondition(
				iv_middleNumericTermSet, true);

		Condition periodCondition = new WordSetCondition(iv_periodSet, true);
		Condition specificWordCondition = new WordSetCondition(
				iv_specifiedWordSet, false);


		startState.addTransition(specificWordCondition, leftAbbreviateState);
		startState.addTransition(new AnyCondition(), startState);

		leftAbbreviateState.addTransition(new RangeStrengthCondition(), middleTextState);
		leftAbbreviateState.addTransition(firstMiddleTextCondition,
				middleTextState);
		leftAbbreviateState.addTransition(firstDashCondition, firstDashState);
		leftAbbreviateState.addTransition(new NumberCondition(),
				middleTextState);
		
        leftAbbreviateState.addTransition(new WordSetCondition(
				iv_combinedSet, false), endState);
		leftAbbreviateState.addTransition(new AnyCondition(), startState);

		middleTextState.addTransition(new RangeStrengthCondition(), lastTextState);
		middleTextState.addTransition(secondMiddleTextCondition, lastTextState);
		middleTextState.addTransition(firstDashCondition, firstDashState);
		middleTextState.addTransition(secondDashCondition, secondDashState);
	
		middleTextState.addTransition(periodCondition, endState);
		middleTextState.addTransition(new AnyCondition(), startState);

		firstDashState.addTransition(thirdMiddleTextCondition, middleTextState);
		firstDashState.addTransition(new AnyCondition(), startState);


		secondDashState.addTransition(fourthMiddleTextCondition, lastTextState);
		secondDashState.addTransition(periodCondition, endState);
		secondDashState.addTransition(new AnyCondition(), startState);

		lastTextState.addTransition(fourthMiddleTextCondition, endState);
		lastTextState.addTransition(periodCondition, endState);
		lastTextState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @param tokens
	 * @return Set DurationToken objects.
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
					tokenStartMap.put(fsm, Integer.valueOf(i));
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
					BaseToken startToken = (BaseToken) tokens
							.get(tokenStartIndex);
					BaseToken endToken = token;
					DurationToken segmentToken = new DurationToken(startToken
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
					tokenStartMap.put(fsm, Integer.valueOf(i));
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
					BaseToken startToken = (BaseToken) tokens
							.get(tokenStartIndex);
					BaseToken endToken = token;
					DurationToken fractionToken = new DurationToken(startToken
							.getStartOffset(), endToken.getEndOffset());
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
}
