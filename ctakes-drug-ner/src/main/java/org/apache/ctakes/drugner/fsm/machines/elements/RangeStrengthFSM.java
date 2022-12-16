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

import org.apache.ctakes.core.fsm.condition.DecimalCondition;
import org.apache.ctakes.core.fsm.condition.NumberCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.RangeCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.elements.conditions.FractionStrengthCondition;
import org.apache.ctakes.drugner.fsm.output.util.RangeStrengthToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect range strength tokens in the given
 * input of tokens.
 * 
 * @author Mayo Clinic
 */
public class RangeStrengthFSM 
{
	// text fractions
	Set iv_textNumberSet = new HashSet();
	// range text
	Set iv_rangeSet = new HashSet();

	Set iv_hyphenatedSet = new HashSet();
	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	/**
	 * Constructor
	 */
	public RangeStrengthFSM()
	{
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
		iv_rangeSet.add("to");
		iv_rangeSet.add("through");
		iv_rangeSet.add("thru");
		
		iv_hyphenatedSet.add("one-two");
		iv_hyphenatedSet.add("one-three");
		iv_hyphenatedSet.add("one-four");
		iv_hyphenatedSet.add("one-five");
		iv_hyphenatedSet.add("one-six");
		iv_hyphenatedSet.add("one-seven");
		iv_hyphenatedSet.add("one-eight");
		iv_hyphenatedSet.add("one-nine");
		iv_hyphenatedSet.add("one-ten");
		

		iv_hyphenatedSet.add("two-three");
		iv_hyphenatedSet.add("two-four");
		iv_hyphenatedSet.add("two-five");
		iv_hyphenatedSet.add("two-six");
		iv_hyphenatedSet.add("two-seven");
		iv_hyphenatedSet.add("two-eight");
		iv_hyphenatedSet.add("two-nine");
		iv_hyphenatedSet.add("two-ten");	
		

		iv_hyphenatedSet.add("three-four");
		iv_hyphenatedSet.add("three-five");
		iv_hyphenatedSet.add("three-six");
		iv_hyphenatedSet.add("three-seven");
		iv_hyphenatedSet.add("three-eight");
		iv_hyphenatedSet.add("three-nine");
		iv_hyphenatedSet.add("three-ten");
		
		iv_hyphenatedSet.add("four-five");
		iv_hyphenatedSet.add("four-six");
		iv_hyphenatedSet.add("four-seven");
		iv_hyphenatedSet.add("four-eight");
		iv_hyphenatedSet.add("four-nine");
		iv_hyphenatedSet.add("four-ten");
		
		iv_hyphenatedSet.add("five-six");
		iv_hyphenatedSet.add("five-seven");
		iv_hyphenatedSet.add("five-eight");
		iv_hyphenatedSet.add("five-nine");
		iv_hyphenatedSet.add("five-ten");
		
		iv_hyphenatedSet.add("six-ten");
		iv_hyphenatedSet.add("six-seven");
		iv_hyphenatedSet.add("six-eight");
		iv_hyphenatedSet.add("six-nine");
		
		iv_hyphenatedSet.add("seven-eight");
		iv_hyphenatedSet.add("seven-nine");
		iv_hyphenatedSet.add("seven-ten");
		
		iv_hyphenatedSet.add("eight-nine");
		iv_hyphenatedSet.add("eight-ten");
		
		iv_hyphenatedSet.add("nine-ten");
		
		iv_hyphenatedSet.add("one-to-two");
		iv_hyphenatedSet.add("one-to-three");
		iv_hyphenatedSet.add("one-to-four");
		iv_hyphenatedSet.add("one-to-five");
		iv_hyphenatedSet.add("one-to-six");
		iv_hyphenatedSet.add("one-to-seven");
		iv_hyphenatedSet.add("one-to-eight");
		iv_hyphenatedSet.add("one-to-nine");
		iv_hyphenatedSet.add("one-to-ten");
		

		iv_hyphenatedSet.add("two-to-three");
		iv_hyphenatedSet.add("two-to-four");
		iv_hyphenatedSet.add("two-to-five");
		iv_hyphenatedSet.add("two-to-six");
		iv_hyphenatedSet.add("two-to-seven");
		iv_hyphenatedSet.add("two-to-eight");
		iv_hyphenatedSet.add("two-to-nine");
		iv_hyphenatedSet.add("two-to-ten");	
		

		iv_hyphenatedSet.add("three-to-four");
		iv_hyphenatedSet.add("three-to-five");
		iv_hyphenatedSet.add("three-to-six");
		iv_hyphenatedSet.add("three-to-seven");
		iv_hyphenatedSet.add("three-to-eight");
		iv_hyphenatedSet.add("three-to-nine");
		iv_hyphenatedSet.add("three-to-ten");
		
		iv_hyphenatedSet.add("four-to-five");
		iv_hyphenatedSet.add("four-to-six");
		iv_hyphenatedSet.add("four-to-seven");
		iv_hyphenatedSet.add("four-to-eight");
		iv_hyphenatedSet.add("four-to-nine");
		iv_hyphenatedSet.add("four-to-ten");
		
		iv_hyphenatedSet.add("five-to-six");
		iv_hyphenatedSet.add("five-to-seven");
		iv_hyphenatedSet.add("five-to-eight");
		iv_hyphenatedSet.add("five-to-nine");
		iv_hyphenatedSet.add("five-to-ten");
		
		iv_hyphenatedSet.add("six-to-ten");
		iv_hyphenatedSet.add("six-to-seven");
		iv_hyphenatedSet.add("six-to-eight");
		iv_hyphenatedSet.add("six-to-nine");
		
		iv_hyphenatedSet.add("seven-to-eight");
		iv_hyphenatedSet.add("seven-to-nine");
		iv_hyphenatedSet.add("seven-to-ten");
		
		iv_hyphenatedSet.add("eight-to-nine");
		iv_hyphenatedSet.add("eight-to-ten");
		
		iv_hyphenatedSet.add("nine-to-ten");
		
		iv_machineSet.add(getDashMachine());
		iv_machineSet.add(getDotDashMachine());
		iv_machineSet.add(getDashDashMachine());

	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * 		<li>25.4-30.4</li>
	 * 		<li>32.1-three</li>
	 * </ol>
	 * @return
	 */
	private Machine getDashDashMachine()
	{
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
		
		State leftNumTextState = new NamedState("LEFT_NUM_TEXT");
		State rightNumTextState = new NamedState("RIGHT_NUM_TEXT");
		State middleDash = new NamedState("MIDDASH");
		State dashAnotherState = new NamedState("DASH_2");
		State dash2State = new NamedState("DASH2");
		
		Condition rightIntCondition = new NumberCondition();
		
		Condition rightNumTextCondition =
			new WordSetCondition(iv_textNumberSet, false);
	
		
		startState.addTransition(new WordSetCondition(iv_textNumberSet, false), leftNumTextState);
		startState.addTransition(new AnyCondition(), startState);
	
		leftNumTextState.addTransition(new PunctuationValueCondition('-'), dash2State);
		leftNumTextState.addTransition(new WordSetCondition(iv_rangeSet, false), rightNumTextState);
		leftNumTextState.addTransition(new AnyCondition(), startState);
	
			
	  	dash2State.addTransition(rightIntCondition, endState);
		dash2State.addTransition(rightNumTextCondition, endState);
		dash2State.addTransition(new WordSetCondition(iv_rangeSet, false), middleDash);
		dash2State.addTransition(new AnyCondition(), startState);
		
		middleDash.addTransition(new PunctuationValueCondition('-'), dashAnotherState);
		middleDash.addTransition(new AnyCondition(), startState);
		
		rightNumTextState.addTransition(new NumberCondition(), endState);
		rightNumTextState.addTransition(new WordSetCondition(iv_textNumberSet, false), endState);
		rightNumTextState.addTransition(new AnyCondition(), startState);
		
		dashAnotherState.addTransition(new WordSetCondition(iv_textNumberSet, false), endState);
		dashAnotherState.addTransition(new NumberCondition(), endState);
		
		dashAnotherState.addTransition(new AnyCondition(), startState); 
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * 		<li>25.4-30.4</li>
	 * 		<li>32.1-three</li>
	 * </ol>
	 * @return
	 */
	private Machine getDashMachine()
	{
	    State startState = new NamedState("START");
	    State endState = new NamedState("END");
	    endState.setEndStateFlag(true);
	
	    Machine m = new Machine(startState);
	    State leftNumIntegerState = new NamedState("LEFT_NUM_INTEGER");
	    State leftNumTextState = new NamedState("LEFT_NUM_TEXT");
	    State dashState = new NamedState("DASH1");
	    State dash1State = new NamedState("DASH_1");
		
	    Condition leftIntCondition = new NumberCondition();
	    Condition rightIntCondition = new NumberCondition();
	
	    startState.addTransition(leftIntCondition, leftNumIntegerState);
	    startState.addTransition(new WordSetCondition(iv_hyphenatedSet, false), endState);
	    startState.addTransition(new WordSetCondition(iv_textNumberSet, false), leftNumTextState);
	    startState.addTransition(new AnyCondition(), startState);
	
	    leftNumIntegerState.addTransition(new PunctuationValueCondition('-'), dashState);
	    leftNumIntegerState.addTransition(new AnyCondition(), startState);
	
	    leftNumTextState.addTransition(new PunctuationValueCondition('-'), dash1State);
	    leftNumTextState.addTransition(new AnyCondition(), startState);
	
	    dashState.addTransition(rightIntCondition, endState);
	    dashState.addTransition(new WordSetCondition(iv_textNumberSet, false), endState);
	
	    dashState.addTransition(new AnyCondition(), startState);
		
	    dash1State.addTransition(rightIntCondition, endState);
	    dash1State.addTransition(new WordSetCondition(iv_textNumberSet, false), endState);
	
	    dash1State.addTransition(new AnyCondition(), startState);
	
	    endState.addTransition(new AnyCondition(), startState);
	
	    return m;
	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * 		<li>250-300</li>
	 * 		<li>I-IV</li>
	 * 		<li>two-three</li>
	 * 		<li>two-to-three</li>
	 * </ol>
	 * @return
	 */
	private Machine getDotDashMachine()
	
	{
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
		State leftNumIntegerState = new NamedState("LEFT_N2_INT");
		State decPartNumState = new NamedState("DECIMAL_NUM");

		State dashState = new NamedState("DASH");
		State dotState = new NamedState("DOT");

		Condition rangeCondition = new RangeCondition();
		Condition leftIntCondition = new NumberCondition();
		Condition decIntCondition = new NumberCondition();
		Condition rightIntCondition = new NumberCondition();

		Condition rightDecimalCondition = new DecimalCondition();
		Condition dashCondition = new PunctuationValueCondition('-');
		
		Condition dotCondition = new PunctuationValueCondition('.');

		Condition rightDoseagesCondition = new FractionStrengthCondition();
	
		startState.addTransition(leftIntCondition, leftNumIntegerState);
		startState.addTransition(rangeCondition, leftNumIntegerState);

	
		startState.addTransition(new AnyCondition(), startState);
		
			
		leftNumIntegerState.addTransition(dotCondition, dotState);
		leftNumIntegerState.addTransition(new AnyCondition(), startState);
	
		dotState.addTransition(decIntCondition, decPartNumState);
		dotState.addTransition(new AnyCondition(), startState);
		
		decPartNumState.addTransition(dashCondition, dashState);
		decPartNumState.addTransition(new AnyCondition(), startState);

	
		dashState.addTransition(rightIntCondition, endState);
		dashState.addTransition(rightDecimalCondition, endState);
		dashState.addTransition(rightDoseagesCondition, endState);
	   
		dashState.addTransition(new AnyCondition(), startState);
	
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Executes the finite state machines.
	 * @param tokens
	 * @return Set of RangeToken objects.
	 * @throws Exception
	 */
	public Set execute(List tokens, Set overrideSet) throws Exception
	{
		Set rangeSet = new HashSet();

		// maps a fsm to a token start index
		// key = fsm , value = token start index
		Map tokenStartMap = new HashMap();

		Iterator overrideTokenItr = overrideSet.iterator();
		// key = start offset, value = override BaseToken object
		Map overrideTokenMap = new HashMap();
		while (overrideTokenItr.hasNext())
		{
			BaseToken t = (BaseToken)overrideTokenItr.next();
			Integer key = new Integer(t.getStartOffset());
			overrideTokenMap.put(key, t);
		}

		boolean overrideOn = false;
		int overrideEndOffset = -1;
		for (int i = 0; i < tokens.size(); i++)
		{
			BaseToken token = (BaseToken) tokens.get(i);

			Integer key = new Integer(token.getStartOffset());						
			
			if (overrideOn)
			{
				if (token.getStartOffset() >= overrideEndOffset)
				{
					overrideOn = false;
					overrideEndOffset = -1;						
				}
				else
				{
					// step to next iteration of for loop
					continue;
				}
			}			
			else
			{
				if (overrideTokenMap.containsKey(key))
				{
					// override one or more tokens until the override
					// token is complete
					token = (BaseToken)overrideTokenMap.get(key);
					overrideOn = true;
					overrideEndOffset = token.getEndOffset();
				}
			}

			Iterator machineItr = iv_machineSet.iterator();
			while (machineItr.hasNext())
			{
				Machine fsm = (Machine) machineItr.next();

				fsm.input(token);

				State currentState = fsm.getCurrentState();
				if (currentState.getStartStateFlag())
				{
					tokenStartMap.put(fsm, new Integer(i));
				}
				if (currentState.getEndStateFlag())
				{
					Object o = tokenStartMap.get(fsm);
					int tokenStartIndex;
					if (o == null)
					{
						// By default, all machines start with
						// token zero.
						tokenStartIndex = 0;
					}
					else
					{
						tokenStartIndex = ((Integer) o).intValue();
						// skip ahead over single token we don't want
						tokenStartIndex++;						
					}
					BaseToken startToken =
						(BaseToken) tokens.get(tokenStartIndex);
					BaseToken endToken = token;
					RangeStrengthToken segmentToken =
						new RangeStrengthToken(
							startToken.getStartOffset(),
							endToken.getEndOffset());
					rangeSet.add(segmentToken);
					fsm.reset();
				}
			}
		}

		// cleanup
		tokenStartMap.clear();

		// reset machines
		Iterator itr = iv_machineSet.iterator();
		while (itr.hasNext())
		{
			Machine fsm = (Machine) itr.next();
			fsm.reset();
		}

		return rangeSet;
	}

	/**
	 * Executes the finite state machines.
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
					BaseToken startToken = (BaseToken) tokens
							.get(tokenStartIndex);
					BaseToken endToken = token;
					RangeStrengthToken fractionToken = new RangeStrengthToken(startToken
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
