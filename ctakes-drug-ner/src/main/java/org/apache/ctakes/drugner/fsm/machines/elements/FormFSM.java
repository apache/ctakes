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

import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.output.elements.FormToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect form tokens in the given
 * input of tokens.
 * @author Mayo Clinic
 */
public class FormFSM {
	// text fractions
	Set iv_fullTextSet = new HashSet();





	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	/**
	 * 
	 * Constructor
	 *
	 */
	public FormFSM() {

		iv_fullTextSet.add("cream");
		iv_fullTextSet.add("creams");
		iv_fullTextSet.add("patch");
		iv_fullTextSet.add("patches");
		iv_fullTextSet.add("tablet");
		iv_fullTextSet.add("tablets");
		iv_fullTextSet.add("capsules");
		iv_fullTextSet.add("capsule");
		iv_fullTextSet.add("caplets");
		iv_fullTextSet.add("nebulizer");
		iv_fullTextSet.add("supplementation");
		iv_fullTextSet.add("supplement");
		iv_fullTextSet.add("cap");
		iv_fullTextSet.add("caps");
		iv_fullTextSet.add("tabs");
		iv_fullTextSet.add("tab");
	    iv_fullTextSet.add("aerosol");
	    iv_fullTextSet.add("elixir");
	    iv_fullTextSet.add("emulsion");
	    iv_fullTextSet.add("enema");
	    iv_fullTextSet.add("gel");
	    iv_fullTextSet.add("implant");
	    iv_fullTextSet.add("inhalant");
	    iv_fullTextSet.add("injection");
	    iv_fullTextSet.add("liquid");
	    iv_fullTextSet.add("lotion");
	    iv_fullTextSet.add("lozenge");
	    iv_fullTextSet.add("lozenges");
	    iv_fullTextSet.add("ointment");
	    iv_fullTextSet.add("packet");
	    iv_fullTextSet.add("packets");
	    iv_fullTextSet.add("pill");
	    iv_fullTextSet.add("pills");
	    iv_fullTextSet.add("powder");
	    iv_fullTextSet.add("shampoo");
	    iv_fullTextSet.add("soap");
	    iv_fullTextSet.add("unit");
	    iv_fullTextSet.add("units");
	    iv_fullTextSet.add("solution");
	    iv_fullTextSet.add("spray");
	    iv_fullTextSet.add("suppository");
	    iv_fullTextSet.add("syrup");
	    iv_fullTextSet.add("drop");
		iv_fullTextSet.add("drops");
		iv_fullTextSet.add("dl");
		iv_fullTextSet.add("cl");
		iv_fullTextSet.add("cc");
		iv_fullTextSet.add("ml");
		iv_fullTextSet.add("liter");
		iv_fullTextSet.add("liters");
		iv_fullTextSet.add("centiliter");
		iv_fullTextSet.add("centiliters");
		iv_fullTextSet.add("milliliter");
		iv_fullTextSet.add("milliliters");
 
		iv_machineSet.add(getFormMachine());
		

	}

	/**
	 -     * Gets a finite state machine that detects the following:
	 * <ol>
	 * 		<li>tablet</li>
	 * 		<li>caps</li>
	 * </ol>
	 * @return
	 */
	private Machine getFormMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);
	

	
		Condition fullTextCondition = new WordSetCondition(iv_fullTextSet,
				false);

		startState.addTransition(fullTextCondition, endState);
		startState.addTransition(new AnyCondition(), startState);


		endState.addTransition(new AnyCondition(), startState);

		return m;
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
					FormToken measurementToken = new FormToken(startToken
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
	 * @param tokens
	 * @return Set of FractionToken objects.
	 * @throws Exception
	 */
	public List execute(List tokens) throws Exception {
	
		// maps a fsm to a token start index
		// key = fsm , value = token start index
		Map tokenStartMap = new HashMap();

		for (int i = 0; i < tokens.size(); i++) {
			Object token = tokens.get(i);

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
					tokens.remove(o);
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

		return tokens;
	}
	  
}

