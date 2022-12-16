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

import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.SymbolValueCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.elements.conditions.ContainsSetTextValueCondition;
import org.apache.ctakes.drugner.fsm.output.elements.StrengthUnitCombinedToken;
import org.apache.ctakes.drugner.fsm.output.elements.StrengthUnitToken;
import org.apache.ctakes.drugner.fsm.states.util.IndentStartState;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect dosages in the given
 * input of tokens.
 * @author Mayo Clinic
 */
public class StrengthUnitFSM {
	// text fractions
	Set<String> iv_fullTextSet = new HashSet();
	Set iv_oneOfTwoTextSet = new HashSet();
	Set iv_twoOfTwoTextSet = new HashSet();
	private Machine iv_strengthMachine;
	private Machine iv_strengthCombinedMachine;
	private Set iv_machineSet = new HashSet();

	/**
	 * 
	 * Constructor
	 *
	 */
	public StrengthUnitFSM() {

		iv_oneOfTwoTextSet.add("international");
		
		iv_twoOfTwoTextSet.add("units");
		iv_fullTextSet.add("micrograms");
		iv_fullTextSet.add("microgram");
		iv_fullTextSet.add("teaspoon");
		iv_fullTextSet.add("teaspoons");
		iv_fullTextSet.add("tablespoon");
		iv_fullTextSet.add("tablespoons");
		iv_fullTextSet.add("gram");
		iv_fullTextSet.add("grams");
		iv_fullTextSet.add("centigram");
		iv_fullTextSet.add("centigrams");
		iv_fullTextSet.add("milligram");
		iv_fullTextSet.add("milligrams");
		iv_fullTextSet.add("grain");
		iv_fullTextSet.add("grains");
		iv_fullTextSet.add("puffs");
		iv_fullTextSet.add("puff");
		iv_fullTextSet.add("unit");
		iv_fullTextSet.add("units");

		iv_fullTextSet.add("gtts");
		iv_fullTextSet.add("ui");
		iv_fullTextSet.add("iu");
		iv_fullTextSet.add("meq");
		iv_fullTextSet.add("mcg");
		iv_fullTextSet.add("gr");
		iv_fullTextSet.add("gm");
		iv_fullTextSet.add("tsp");
		iv_fullTextSet.add("tbsp");
		iv_fullTextSet.add("g");
		iv_fullTextSet.add("mg");
		iv_fullTextSet.add("ou");
		
		iv_strengthMachine = getStrengthMachine();
		iv_strengthCombinedMachine = getStrengthCombinedMachine();
		iv_machineSet.add(iv_strengthCombinedMachine);
		iv_machineSet.add(iv_strengthMachine);


	}
	/**
	 * Handles a complex range of strength representations for the discovery of the combined 
	 * quantity and unit value for this element. The following represents the range of strength
	 * types which are discovered:
	 *  <ol>
	 * 		<li>250-mg</li>
	 * 		<li>two-puffs</li>
	 * 		<li>1-cc</li>
	 * 		<li>15.5 mg</li>
	 * 		<li>25.7-30.2 mg</li>
	 *  </ol>
	 */
	private Machine getStrengthMachine(){
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State unitState = new NamedState("UNIT");
		State ntFalseTermState = new IndentStartState("NON TERMINAL START");
		endState.setEndStateFlag(true);
		ntFalseTermState.setEndStateFlag(true);

//		startState.addTransition(new ContainsSetTextValueCondition(
//				iv_fullTextSet, false), endState);
//		startState.addTransition(new WordSetCondition(
//				iv_fullTextSet, false), endState);
		
		startState.addTransition(new WordSetCondition(iv_fullTextSet, false), endState);
		startState.addTransition(new PunctuationValueCondition('-'), unitState);
		startState.addTransition(new SymbolValueCondition('%'), endState);
		startState.addTransition(new AnyCondition(), startState);
		
		unitState.addTransition(new WordSetCondition(iv_fullTextSet, false), ntFalseTermState);
		unitState.addTransition(new SymbolValueCondition('%'), endState);
		unitState.addTransition(new AnyCondition(), startState);

		ntFalseTermState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		Machine m = new Machine(startState);
		return m;
	}
	/**
	 * Handles a complex range of strength representations for the discovery of the combined 
	 * quantity and unit value for this element. The following represents the range of strength
	 * types which are discovered:
	 *  <ol>
	 * 		<li>250mg</li>
	 * 		<li>1-cc</li>
	 * 	 	<li>0.4mg</li>
	 * 		<li>two-3.5mg</li>
	 *  </ol>
	 */
	private Machine getStrengthCombinedMachine(){
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State unitState = new NamedState("UNIT");
		State ntFalseTermState = new IndentStartState("NON TERMINAL START");
		endState.setEndStateFlag(true);
		ntFalseTermState.setEndStateFlag(true);

		startState.addTransition(new ContainsSetTextValueCondition(
				iv_fullTextSet, false), endState);
//		startState.addTransition(new WordSetCondition(
//				iv_fullTextSet, false), endState);
//		
//		startState.addTransition(new WordSetCondition(iv_fullTextSet, false), endState);
		startState.addTransition(new PunctuationValueCondition('-'), unitState);
		startState.addTransition(new SymbolValueCondition('%'), endState);
		startState.addTransition(new AnyCondition(), startState);
		
		unitState.addTransition(new ContainsSetTextValueCondition(iv_fullTextSet, false), ntFalseTermState);
		unitState.addTransition(new SymbolValueCondition('%'), endState);
		unitState.addTransition(new AnyCondition(), startState);

		ntFalseTermState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		Machine m = new Machine(startState);
		return m;
	}
	/**
	 * Executes the finite state machines.
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
			if (overrideOn1 && overrideOn2){
				if (overrideEndOffset1 >= overrideEndOffset2)
					overrideOn1 = false;
				else
					overrideOn2 = false;
			}
			if (overrideOn1) {
				if (token.getStartOffset() >= overrideEndOffset1) {
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
					BaseToken endToken = token;
					if (currentState instanceof IndentStartState) {
						startToken = (BaseToken) tokens
								.get(tokenStartIndex + 1);

					} else {
						startToken = (BaseToken) tokens.get(tokenStartIndex);

					}
					StrengthUnitToken measurementToken = null;
					StrengthUnitCombinedToken measurementCombinedToken = null;
					if (fsm.equals(iv_strengthCombinedMachine)) {
						measurementCombinedToken = new StrengthUnitCombinedToken(startToken
								.getStartOffset(), endToken.getEndOffset());
						measurementSet.add(measurementCombinedToken);
						
					}
					else {
						measurementToken = new StrengthUnitToken(startToken
								.getStartOffset(), endToken.getEndOffset());
						measurementSet.add(measurementToken);
						
					}
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
		Map overrideBeginTokenMap = new HashMap();
		while (overrideTokenItr.hasNext()) {
			BaseToken t = (BaseToken) overrideTokenItr.next();
			Integer key = new Integer(t.getStartOffset());
			overrideTokenMap.put(key, t);
		}

		boolean overrideOn = false;
		int overrideEndOffset = -1;
		int tokenOffset = 0;
		int anchorKey = 0;
		
		for (int i = 0; i < tokens.size(); i++) {
			BaseToken token = (BaseToken) tokens.get(i);

			Integer key = new Integer(token.getStartOffset());

			if (overrideOn) {
				if (token.getStartOffset() >= overrideEndOffset) {
					if (tokenOffset > 0)
						overrideBeginTokenMap.put(new Integer(anchorKey), new Integer(tokenOffset));
					overrideOn = false;
					overrideEndOffset = -1;
				} else {
					tokenOffset++;
					// step to next iteration of for loop
					continue;
				}
			} else {
				if (overrideTokenMap.containsKey(key)) {
					// override one or more tokens until the override
					// token is complete
					anchorKey = key.intValue();
					token = (BaseToken) overrideTokenMap.get(key);
					overrideOn = true;
					overrideEndOffset = token.getEndOffset();
					tokenOffset = 0;
				}
			}

			Iterator machineItr = iv_machineSet.iterator();
			while (machineItr.hasNext()) {
				Machine fsm = (Machine) machineItr.next();

				fsm.input(token);

				State currentState = fsm.getCurrentState();
				if (currentState.getStartStateFlag()) {
					tokenStartMap.put(fsm, new Integer(i));
					tokenOffset = 0;
				}
				if (currentState.getEndStateFlag()) {
					Object o = tokenStartMap.get(fsm);
					int tokenStartIndex;
					if (o == null) {
						// By default, all machines start with
						// token zero.
						tokenStartIndex = 0;
					} else {
						Integer tokenMap = new Integer(0);
											
						BaseToken lookUpOffset = (BaseToken) tokens.get(((Integer) o).intValue());
							
						if (overrideBeginTokenMap.get(new Integer(lookUpOffset.getStartOffset())) != null){
							Integer offSet = (Integer) (overrideBeginTokenMap.get(new Integer(lookUpOffset.getStartOffset())));
							tokenMap = new Integer(offSet.intValue()  + tokenMap.intValue());
						}
						
						
						tokenStartIndex = ((Integer) o).intValue() + tokenMap.intValue();
						// skip ahead over single token we don't want
						tokenStartIndex++;
					}
					BaseToken startToken = null;
					BaseToken endToken = token;
					if (currentState instanceof IndentStartState) {
						startToken = (BaseToken) tokens
								.get(tokenStartIndex + 1);

					} else {
						startToken = (BaseToken) tokens.get(tokenStartIndex);

					}
					StrengthUnitToken measurementToken = null;
					StrengthUnitCombinedToken measurementCombinedToken = null;
					if (fsm.equals(iv_strengthCombinedMachine)) {
						measurementCombinedToken = new StrengthUnitCombinedToken(startToken
								.getStartOffset(), endToken.getEndOffset());
						measurementSet.add(measurementCombinedToken);
						
					}
					else {
						measurementToken = new StrengthUnitToken(startToken
								.getStartOffset(), endToken.getEndOffset());
						measurementSet.add(measurementToken);
						
					}
					
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

