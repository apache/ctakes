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

import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.output.util.HyphenatedDrugToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect hyphenated drug information
 * adjoined to dosage data in the given input of tokens.
 * 
 * @author Mayo Clinic
 */
public class HyphenatedDrugFSM {
	// text fractions
	Set iv_hyphenDrugSet = new HashSet();

	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public HyphenatedDrugFSM() {
		iv_hyphenDrugSet.add("alka-seltzer");
		iv_hyphenDrugSet.add("abilify");
		iv_hyphenDrugSet.add("actiq");

		iv_hyphenDrugSet.add("ambi-1000");
		iv_hyphenDrugSet.add("almacone-2");
		iv_hyphenDrugSet.add("alexan-100");
		iv_hyphenDrugSet.add("azo-100");
		iv_hyphenDrugSet.add("allerhist-1");
		iv_hyphenDrugSet.add("acne-5");
		iv_hyphenDrugSet.add("andriod-10");
		iv_hyphenDrugSet.add("andriod-25");
		iv_hyphenDrugSet.add("andriod-50");
		iv_hyphenDrugSet.add("acys-5");
		iv_hyphenDrugSet.add("aldoclor-250");
		iv_hyphenDrugSet.add("aerolin-400");
		iv_hyphenDrugSet.add("alka-seltzer plus");
		iv_hyphenDrugSet.add("allegra-d");

		iv_hyphenDrugSet.add("b-complex");
		iv_hyphenDrugSet.add("bactroban");
		iv_hyphenDrugSet.add("chlor-trimeton");

		iv_hyphenDrugSet.add("claritin-d");
		iv_hyphenDrugSet.add("cleocin-t");
		iv_hyphenDrugSet.add("daranide");

		iv_hyphenDrugSet.add("depo-medrol");
		iv_hyphenDrugSet.add("depo-provera");
		iv_hyphenDrugSet.add("effexor-xr");

		iv_hyphenDrugSet.add("glucosamine-chondroitin");
		iv_hyphenDrugSet.add("fml-s");
		iv_hyphenDrugSet.add("hep-lock");
		iv_hyphenDrugSet.add("l-arginine");
		iv_hyphenDrugSet.add("l-thyroxine");
		iv_hyphenDrugSet.add("lupron-depot");
		iv_hyphenDrugSet.add("metro cream");
		iv_hyphenDrugSet.add("micro-k");
		iv_hyphenDrugSet.add("multi-vitamin");
		iv_hyphenDrugSet.add("muro-128");
		iv_hyphenDrugSet.add("naphcon-a");
		iv_hyphenDrugSet.add("neo-synephrine");
		iv_hyphenDrugSet.add("no-doz");
		iv_hyphenDrugSet.add("noctec");
		iv_hyphenDrugSet.add("nor-qd");
		iv_hyphenDrugSet.add("ocuflox");
		iv_hyphenDrugSet.add("ortho tri-cyclen");
		iv_hyphenDrugSet.add("ortho tri-cyclen lo");
		iv_hyphenDrugSet.add("ortho-cept");
		iv_hyphenDrugSet.add("ortho-novum");
		iv_hyphenDrugSet.add("os-cal D");
		iv_hyphenDrugSet.add("os-cal 500");
		iv_hyphenDrugSet.add("os-cal");
		iv_hyphenDrugSet.add("peg-intron");
		iv_hyphenDrugSet.add("phospho-soda");
		iv_hyphenDrugSet.add("pred-g");
		iv_hyphenDrugSet.add("retin-a");
		iv_hyphenDrugSet.add("robitussin-dm");
		iv_hyphenDrugSet.add("senokot-s");
		iv_hyphenDrugSet.add("serentil");
		iv_hyphenDrugSet.add("sleep-eze");
		iv_hyphenDrugSet.add("tegretol-xr");
		iv_hyphenDrugSet.add("theo-dur");
		iv_hyphenDrugSet.add("tilade");
		iv_hyphenDrugSet.add("tri-sprintec");
		iv_hyphenDrugSet.add("vasocon-a");
		iv_hyphenDrugSet.add("vivelle-dot");
		iv_hyphenDrugSet.add("vira-a");
		iv_hyphenDrugSet.add("vitamin-a");
		iv_hyphenDrugSet.add("vitamin-b6");
		iv_hyphenDrugSet.add("vitamin-b complex");
		iv_hyphenDrugSet.add("vitamin-b");
		iv_hyphenDrugSet.add("vitamin-b1");
		iv_hyphenDrugSet.add("vitamin-c");
		iv_hyphenDrugSet.add("vitamin-d");
		iv_hyphenDrugSet.add("vitamin-d2");
		iv_hyphenDrugSet.add("vitamin-e");

		iv_machineSet.add(getDashMachine());

	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * <li>zyrtec-d</li>
	 * <li>alka-seltzer plus</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getDashMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State hyphenatedDrugState = new NamedState("HYPH_DRUG");
		Condition hyphenatedDrugCondition = new WordSetCondition(
				iv_hyphenDrugSet, false);

		startState.addTransition(hyphenatedDrugCondition, endState);//hyphenatedDrugState);
		startState.addTransition(new AnyCondition(), startState);

		hyphenatedDrugState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);

		return m;
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
					BaseToken startToken = (BaseToken) tokens
							.get(tokenStartIndex);
					BaseToken endToken = token;
					HyphenatedDrugToken fractionToken = new HyphenatedDrugToken(
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
}
