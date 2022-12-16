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
import org.apache.ctakes.core.fsm.condition.DisjoinCondition;
import org.apache.ctakes.core.fsm.condition.IntegerValueCondition;
import org.apache.ctakes.core.fsm.condition.NumberCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.TextValueCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.state.NonTerminalEndState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.core.fsm.token.adapter.PunctuationTokenAdapter;
import org.apache.ctakes.drugner.fsm.elements.conditions.FormCondition;
import org.apache.ctakes.drugner.fsm.elements.conditions.FractionStrengthCondition;
import org.apache.ctakes.drugner.fsm.elements.conditions.RangeStrengthCondition;
import org.apache.ctakes.drugner.fsm.elements.conditions.RouteCondition;
import org.apache.ctakes.drugner.fsm.output.elements.DosageToken;
import org.apache.ctakes.drugner.fsm.states.util.IndentStartState;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect dosages in the given input
 * of tokens.
 * 
 * @author Mayo Clinic
 */
public class DosagesFSM {

	Set iv_soloTextSet = new HashSet();

	Set iv_textNumberSet = new HashSet();

	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public DosagesFSM() {

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
		iv_textNumberSet.add("one-half");
		iv_textNumberSet.add("one-and-a-half");
		iv_textNumberSet.add("one-and-a-half-tablets");
		iv_textNumberSet.add("one-half-tablet");
		iv_textNumberSet.add("one-fourth");
		iv_textNumberSet.add("one-third");
		iv_textNumberSet.add("half-tablet");
		iv_textNumberSet.add("one-to-two");
		iv_textNumberSet.add("two-to-three");
		iv_textNumberSet.add("three-quarter");
		iv_textNumberSet.add("three-quarters");
		iv_textNumberSet.add("three-fourths");
		iv_textNumberSet.add("one-quarter");
		
		iv_soloTextSet.add("one-half-tablet");
		iv_soloTextSet.add("half-tablet");

		iv_machineSet.add(getDosageQuantityMachine());

	}

	/**
	 * - * Gets a finite state machine that detects the following:
	 * <ol>
	 * <li>one milligram</li>
	 * <li>one mg</li>
	 * <li>1 milligram</li>
	 * <li>1 mg</li>
	 * <li>10mg</li>
	 * <li>0.5 mg</li>
	 * <li>1-5 milligrams</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getDosageQuantityMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State ofState = new NamedState("OF");
		State aState = new NamedState("A");
		State numState = new NamedState("NUMVALUE");
		State formState = new NamedState("FORM");
		State leftParenState = new NamedState("LEFTPAREN");
		State ntFalseTermState = new IndentStartState("NON TERMINAL START");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntFalseTermState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);

		Machine m = new Machine(startState);
		State hyphState = new NamedState("HYPHTERM");
		State dosageState = new NamedState("DOSAGE");

		Condition decimalCondition = new DecimalCondition();
		Condition numberCondition = new NumberCondition();
		Condition strengthFormCondition = new DisjoinCondition(
				new RouteCondition(), new FormCondition());
		Condition numberTextCondition = new WordSetCondition(iv_textNumberSet,
				false);
		Condition rangeCondition = new RangeStrengthCondition();
		Condition fractionRangeCondition = new FractionStrengthCondition();
		Condition decimalStart = new DisjoinCondition(
				new IntegerValueCondition(0), new NumberCondition());

		startState.addTransition(new WordSetCondition(iv_soloTextSet, true),
				endState);
		startState.addTransition(numberCondition, dosageState);
		startState.addTransition(decimalCondition, dosageState);
		startState.addTransition(rangeCondition, dosageState);
		startState.addTransition(fractionRangeCondition, dosageState);
		startState.addTransition(numberTextCondition, dosageState);
		startState.addTransition(decimalStart, dosageState);
		/*startState.addTransition(new DisjoinCondition(new StrengthCondition(),
				new FormCondition()), formState);*/

		startState.addTransition(new AnyCondition(), startState);

		formState.addTransition(new WordSetCondition(iv_textNumberSet, true),
				ntFalseTermState);
		formState.addTransition(new AnyCondition(), startState);

		dosageState.addTransition(strengthFormCondition, ntEndState);
		dosageState
				.addTransition(new PunctuationValueCondition('-'), hyphState);

		dosageState.addTransition(new PunctuationValueCondition('('),
				leftParenState);
		dosageState.addTransition(new TextValueCondition("of", false), ofState);
		dosageState.addTransition(new AnyCondition(), startState);

		ofState.addTransition(new TextValueCondition("a", false), aState);
		ofState.addTransition(new AnyCondition(), startState);

		aState.addTransition(new DisjoinCondition(new RouteCondition(),
				new FormCondition()), ntEndState);
		aState.addTransition(new AnyCondition(), startState);

		hyphState.addTransition(new WordSetCondition(iv_soloTextSet, true),
				endState);
		hyphState.addTransition(new NumberCondition(), numState);
		hyphState.addTransition(new AnyCondition(), startState);

		numState.addTransition(new DisjoinCondition(new RouteCondition(),
				new FormCondition()), ntEndState);
		numState.addTransition(new AnyCondition(), startState);

		leftParenState.addTransition(new DisjoinCondition(
				new RouteCondition(), new FormCondition()), ntEndState);
		leftParenState.addTransition(new AnyCondition(), startState);

		ntEndState.addTransition(new AnyCondition(), startState);
		ntFalseTermState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);

		return m;
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
					int globalOffset = 0;
					int tokenStartIndex;
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
					BaseToken endToken = token;
					if (currentState instanceof NonTerminalEndState) {
						endToken = (BaseToken) tokens.get(i - 1);

						if (endToken instanceof PunctuationTokenAdapter) {
							endToken = (BaseToken) tokens.get(i - 2);
						}
					} else {
						endToken = token;
					}
					DosageToken measurementToken = new DosageToken(startToken
							.getStartOffset(), endToken.getEndOffset());
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

	/**
	 * Executes the finite state machines.
	 * 
	 * @param tokens
	 * @return Set of RangeToken objects.
	 * @throws Exception
	 */
	public Set execute(List tokens, Set overrideSet) throws Exception {
		Set measurementSet = new HashSet();

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
						if (endToken instanceof PunctuationTokenAdapter) {
							endToken = (BaseToken) tokens.get(i - 2);
						}
					} else {
						endToken = token;
					}
					// BaseToken endToken = token;
					DosageToken measurementToken = new DosageToken(startToken
							.getStartOffset(), endToken.getEndOffset());

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
