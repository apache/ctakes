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
package org.apache.ctakes.drugner.fsm.machines.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.TextSetCondition;
import org.apache.ctakes.core.fsm.condition.TextValueCondition;
import org.apache.ctakes.core.fsm.machine.FSM;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.state.NonTerminalEndState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.output.util.SubSectionIndicator;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect sub-sections in the given input of
 * tokens.
 * 
 * @author Mayo Clinic
 */
public class SubSectionIndicatorFSM implements FSM {

	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	private Set iv_probableSubBeginSet = new HashSet();

	private Set iv_probableSubNextSet = new HashSet();
	
	private Set iv_confirmedSubBeginSet = new HashSet();

	private Set iv_confirmedSubNextSet = new HashSet();
	
	private Set iv_historySubBeginSet = new HashSet();

	private Set iv_historySubNextSet = new HashSet();
	private Set iv_historySubMidSet = new HashSet();
	
	private Set iv_middleWordSet = new HashSet();
	
	private Set iv_probableSubEndSet = new HashSet();

	private Machine iv_subSectionIDProbableMachine = new Machine();

	private Machine iv_subSectionIDHistoryMachine = new Machine();
	
	private Machine iv_subSectionIDConfirmMachine = new Machine();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public SubSectionIndicatorFSM() {

		
		iv_probableSubBeginSet.add("taper");
		iv_probableSubBeginSet.add("tapered");
		iv_probableSubBeginSet.add("changed");
		iv_probableSubBeginSet.add("altered");
		iv_probableSubBeginSet.add("alter");
		iv_probableSubBeginSet.add("patient");
		
		iv_probableSubNextSet.add("medication");
		iv_probableSubNextSet.add("medications");
		iv_probableSubNextSet.add("take");

		iv_probableSubEndSet.add("discontinued");
		iv_probableSubEndSet.add("discontinue");
		
		iv_historySubBeginSet.add("discontinued");
		iv_historySubBeginSet.add("discontinue");
		iv_historySubBeginSet.add("past");
		iv_historySubBeginSet.add("recently");
		iv_historySubBeginSet.add("medication");
		iv_historySubBeginSet.add("radiology");
		iv_historySubBeginSet.add("food");
	
		iv_historySubNextSet.add("medication");
		iv_historySubNextSet.add("medications");
		iv_historySubNextSet.add("hold");

		
		iv_historySubMidSet.add("psychotropic");
		iv_historySubMidSet.add("discontinued");
		iv_historySubMidSet.add("on");
		
		iv_confirmedSubBeginSet.add("new");
		iv_confirmedSubBeginSet.add("dose");
		iv_confirmedSubBeginSet.add("reinstituted");
		iv_confirmedSubBeginSet.add("continue");
		iv_confirmedSubBeginSet.add("prn");
		iv_confirmedSubBeginSet.add("continued");
		
		iv_confirmedSubNextSet.add("adjustment");
		iv_confirmedSubNextSet.add("dose");
		iv_confirmedSubNextSet.add("dosage");
		iv_confirmedSubNextSet.add("dosages");
		iv_confirmedSubNextSet.add("adjustments");
		iv_confirmedSubNextSet.add("medication");
		iv_confirmedSubNextSet.add("medications");
		
		iv_middleWordSet.add("and");
		iv_middleWordSet.add("may");
		iv_subSectionIDProbableMachine = getProbableSubSectionMachine();
		iv_subSectionIDHistoryMachine = getHistorySubSectionMachine();
		iv_subSectionIDConfirmMachine = getConfirmSubSectionMachine();
		iv_machineSet.add(iv_subSectionIDProbableMachine);
		iv_machineSet.add(iv_subSectionIDHistoryMachine);
		iv_machineSet.add(iv_subSectionIDConfirmMachine);

	}

	private Machine getHistorySubSectionMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State medState = new NamedState("MEDHIST");
		State midWordState = new NamedState("MIDDLE");

		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		Condition subFirstBegin = new TextSetCondition(iv_historySubBeginSet,
				false);
		Condition subFirstMid = new TextSetCondition(iv_historySubMidSet,
				false);
		Condition subFirstNext = new TextSetCondition(iv_historySubNextSet,
				false);

		startState.addTransition(subFirstBegin, medState);
	    startState.addTransition(new AnyCondition(), startState);
	    
		medState.addTransition(subFirstNext, endState);
		medState.addTransition(subFirstMid, midWordState);
		medState.addTransition(new PunctuationValueCondition(':'), endState);
		//medState.addTransition(new PunctuationValueCondition('/'), ntEndState);
		medState.addTransition(new AnyCondition(), startState);
		
		midWordState.addTransition(subFirstNext, endState);
		midWordState.addTransition(new PunctuationValueCondition(':'), endState);
		midWordState.addTransition(new AnyCondition(), startState);
		
		ntEndState.addTransition(new AnyCondition(),  startState);
		endState.addTransition(new AnyCondition(), startState);
		return m;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @param tokens
	 * @return Set of DateToken objects.
	 * @throws Exception
	 */
	public Set execute(List tokens) throws Exception {
		Set outSet = new HashSet();

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
					BaseToken endToken = null;
					if (currentState instanceof NonTerminalEndState) {
						endToken = (BaseToken) tokens.get(i - 1);
					} else {
						endToken = token;
					}

					BaseToken startToken = (BaseToken) tokens
							.get(tokenStartIndex);
					SubSectionIndicator subs = null;
					if (fsm.equals(iv_subSectionIDProbableMachine)) {
						subs = new SubSectionIndicator(startToken
								.getStartOffset(), endToken.getEndOffset(),
								SubSectionIndicator.PROBABLE_STATUS);
					} else if (fsm.equals(iv_subSectionIDHistoryMachine)) {
						subs = new SubSectionIndicator(startToken
								.getStartOffset(), endToken.getEndOffset(),
								SubSectionIndicator.HISTORY_STATUS);
					} else if (fsm.equals(iv_subSectionIDConfirmMachine)) {
						subs = new SubSectionIndicator(startToken
								.getStartOffset(), endToken.getEndOffset(),
								SubSectionIndicator.CONFIRMED_STATUS);
					} else
						subs = new SubSectionIndicator(startToken
								.getStartOffset(), endToken.getEndOffset(),
								SubSectionIndicator.FAMILY_HISTORY_STATUS);
					outSet.add(subs);

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
	
		return outSet;
	}


	private Machine getProbableSubSectionMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State medState = new NamedState("PROBHIST");
	
		State endWordState = new NamedState("ENDWORD");

		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		Condition subFirstBegin = new TextSetCondition(iv_probableSubBeginSet,
				false);
		Condition subFirstNext = new TextSetCondition(iv_probableSubNextSet,
				false);

		startState.addTransition(subFirstBegin, medState);
	
		startState.addTransition(new AnyCondition(), startState);
		
			
		medState.addTransition(subFirstNext, endState);
		medState.addTransition(new TextSetCondition(iv_middleWordSet, false), endWordState);
		medState.addTransition(new AnyCondition(), startState);
		
		endWordState.addTransition(new TextSetCondition(iv_probableSubEndSet, false), endState);
		endWordState.addTransition(new AnyCondition(), startState);
		
		endState.addTransition(new AnyCondition(), startState);
		return m;
	}

	private Machine getConfirmSubSectionMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State medState = new NamedState("MEDHIST");
		State firstDotState = new NamedState("FIRSTDOT");
		State rState = new NamedState("RSTATE");
		State secondDotState = new NamedState("SECONDDOT");
		State nState = new NamedState("NSTATE");
		State thirdDotState = new NamedState("THIRDDOT");
	
	
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		Condition subFirstBegin = new TextSetCondition(iv_confirmedSubBeginSet,
				false);
		Condition subFirstNext = new TextSetCondition(iv_confirmedSubNextSet,
				false);
	
		startState.addTransition(subFirstBegin, medState);
	    startState.addTransition(new TextValueCondition("p", false), firstDotState);
	    startState.addTransition(new AnyCondition(), startState);
	    
	    firstDotState.addTransition(new PunctuationValueCondition('.'), rState);
	    firstDotState.addTransition(new AnyCondition(), startState);
	    
	    rState.addTransition(new TextValueCondition("r", false), secondDotState);
	    rState.addTransition(new AnyCondition(), startState);
	    
	    secondDotState.addTransition(new PunctuationValueCondition('.'), nState);
	    secondDotState.addTransition(new AnyCondition(), startState);
	    
	    nState.addTransition(new TextValueCondition("n", false), thirdDotState);
	    nState.addTransition(new AnyCondition(), startState);
	    
	    thirdDotState.addTransition(new PunctuationValueCondition('.'), medState);
	    thirdDotState.addTransition(new AnyCondition(), startState);
	    
		medState.addTransition(subFirstNext, endState);
		medState.addTransition(new AnyCondition(), startState);
	
		endState.addTransition(new AnyCondition(), startState);
		return m;
	}

}