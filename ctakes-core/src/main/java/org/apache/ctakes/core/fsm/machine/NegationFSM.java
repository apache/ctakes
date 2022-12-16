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

import org.apache.ctakes.core.fsm.condition.DisjoinCondition;
import org.apache.ctakes.core.fsm.condition.NegateCondition;
import org.apache.ctakes.core.fsm.condition.TextSetCondition;
import org.apache.ctakes.core.fsm.output.NegationIndicator;
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
public class NegationFSM {

	// regular modal verb
	private Set<String> iv_modalVerbsSet = new HashSet<String>();
	// negative particle
	private Set<String> iv_negParticlesSet = new HashSet<String>();
	// regular verbs requiring negation particle
	private Set<String> iv_regVerbsSet = new HashSet<String>();
	// neagive verbs that contain negation in them
	private Set<String> iv_negVerbsSet = new HashSet<String>();
	// negation preposition
	private Set<String> iv_negPrepositionsSet = new HashSet<String>();
	// negatively charged determiners
	private Set<String> iv_negDeterminersSet = new HashSet<String>();
	// regular nouns - indicators
	private Set<String> iv_regNounsSet = new HashSet<String>();
	// regular prepositions
	private Set<String> iv_regPrepositionsSet = new HashSet<String>();
	// negative adjectives
	private Set<String> iv_negAdjectivesSet = new HashSet<String>();
	// negative collocations
	private Set<String> iv_negCollocSet = new HashSet<String>();
	// NEGATIVE COLLOCATION PARTICLE
	private Set<String> iv_negColPartSet = new HashSet<String>();

	// contains the finite state machines
	private Set<Machine> iv_machineSet = new HashSet<Machine>();

	/**
	 * 
	 * Constructor
	 * 
	 */
	public NegationFSM() {
		iv_modalVerbsSet.add("can");
		iv_modalVerbsSet.add("ca");
		iv_modalVerbsSet.add("will");
		iv_modalVerbsSet.add("must");
		iv_modalVerbsSet.add("could");
		iv_modalVerbsSet.add("would");
		iv_modalVerbsSet.add("should");
		iv_modalVerbsSet.add("shall");
		iv_modalVerbsSet.add("did");

		iv_negParticlesSet.add("not");
		iv_negColPartSet.add("out");
		iv_negParticlesSet.add("n't");
		iv_negParticlesSet.add("'t");

		iv_negCollocSet.add("rule");
		iv_negCollocSet.add("rules");
		iv_negCollocSet.add("ruled");
		iv_negCollocSet.add("ruling");
		iv_negCollocSet.add("rule-out");

		iv_regVerbsSet.add("reveal");
		iv_regVerbsSet.add("reveals");
		iv_regVerbsSet.add("revealed");
		iv_regVerbsSet.add("revealing");
		iv_regVerbsSet.add("have");
		iv_regVerbsSet.add("had");
		iv_regVerbsSet.add("has");
		iv_regVerbsSet.add("feel");
		iv_regVerbsSet.add("feels");
		iv_regVerbsSet.add("felt");
		iv_regVerbsSet.add("feeling");
		iv_regVerbsSet.add("complain");
		iv_regVerbsSet.add("complains");
		iv_regVerbsSet.add("complained");
		iv_regVerbsSet.add("complaining");
		iv_regVerbsSet.add("demonstrate");
		iv_regVerbsSet.add("demonstrates");
		iv_regVerbsSet.add("demonstrated");
		iv_regVerbsSet.add("demonstrating");
		iv_regVerbsSet.add("appear");
		iv_regVerbsSet.add("appears");
		iv_regVerbsSet.add("appeared");
		iv_regVerbsSet.add("appearing");
		iv_regVerbsSet.add("caused");
		iv_regVerbsSet.add("cause");
		iv_regVerbsSet.add("causing");
		iv_regVerbsSet.add("causes");
		iv_regVerbsSet.add("find");
		iv_regVerbsSet.add("finds");
		iv_regVerbsSet.add("found");
		iv_regVerbsSet.add("discover");
		iv_regVerbsSet.add("discovered");
		iv_regVerbsSet.add("discovers");

		iv_negVerbsSet.add("deny");
		iv_negVerbsSet.add("denies");
		iv_negVerbsSet.add("denied");
		iv_negVerbsSet.add("denying");
		iv_negVerbsSet.add("fail");
		iv_negVerbsSet.add("fails");
		iv_negVerbsSet.add("failed");
		iv_negVerbsSet.add("failing");
		iv_negVerbsSet.add("decline");
		iv_negVerbsSet.add("declines");
		iv_negVerbsSet.add("declined");
		iv_negVerbsSet.add("declining");
		iv_negVerbsSet.add("exclude");
		iv_negVerbsSet.add("excludes");
		iv_negVerbsSet.add("excluding");
		iv_negVerbsSet.add("excluded");

		iv_negPrepositionsSet.add("without");
		iv_negPrepositionsSet.add("absent");
		iv_negPrepositionsSet.add("none");

		iv_negDeterminersSet.add("no");
		iv_negDeterminersSet.add("any");
		iv_negDeterminersSet.add("neither");
		iv_negDeterminersSet.add("nor");
		iv_negDeterminersSet.add("never");

		iv_regNounsSet.add("evidence");
		iv_regNounsSet.add("indication");
		iv_regNounsSet.add("indications");
		iv_regNounsSet.add("sign");
		iv_regNounsSet.add("signs");
		iv_regNounsSet.add("symptoms");
		iv_regNounsSet.add("symptom");
		iv_regNounsSet.add("sx");
		iv_regNounsSet.add("dx");
		iv_regNounsSet.add("diagnosis");
		iv_regNounsSet.add("history");
		iv_regNounsSet.add("hx");
		iv_regNounsSet.add("findings");

		iv_regPrepositionsSet.add("of");
		iv_regPrepositionsSet.add("in");
		iv_regPrepositionsSet.add("for");
		iv_regPrepositionsSet.add("with");

		iv_negAdjectivesSet.add("unremarkable");
		iv_negAdjectivesSet.add("unlikely");
		iv_negAdjectivesSet.add("negative");

		iv_machineSet.add(getAspectualNegIndicatorMachine());
		iv_machineSet.add(getNominalNegIndicatorMachine());
		iv_machineSet.add(getAdjNegIndicatorMachine());

	}

	private Machine getAspectualNegIndicatorMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State anyState = new NamedState("ANY");

		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);

		Machine m = new Machine(startState);
		State regModalState = new NamedState("REG_MODAL");
		State negPartState = new NamedState("NEG_PART");
		State negVerbState = new NamedState("NEG_VERB");
		State negCollocState = new NamedState("NEG_COLLOC");
		State negColPartState = new NamedState("NEG_COLPART");

		Condition regModalC = new TextSetCondition(iv_modalVerbsSet, false);
		Condition negPartC = new TextSetCondition(iv_negParticlesSet, false);
		Condition regVerbC = new TextSetCondition(iv_regVerbsSet, false);
		Condition negVerbC = new TextSetCondition(iv_negVerbsSet, false);
		Condition negDetC = new TextSetCondition(iv_negDeterminersSet, false);
		Condition negCollocC = new TextSetCondition(iv_negCollocSet, false);
		Condition negColPartC = new TextSetCondition(iv_negColPartSet, false);

		Condition notCollocC = new NegateCondition(negCollocC);

		startState.addTransition(negVerbC, negVerbState);
		startState.addTransition(negCollocC, negCollocState); // rule

		startState.addTransition(new DisjoinCondition(regModalC, regVerbC),
				regModalState); // start with a modal
		startState.addTransition(new DisjoinCondition(negPartC, negDetC),
				negPartState);

		startState.addTransition(new AnyCondition(), startState);

		regModalState.addTransition(negCollocC, negCollocState);
		negCollocState.addTransition(negColPartC, negColPartState); // out
		negColPartState.addTransition(new AnyCondition(), ntEndState);
		negCollocState.addTransition(new AnyCondition(), startState);

		regModalState.addTransition(new DisjoinCondition(negPartC, negDetC),
				negPartState);
		regModalState.addTransition(new AnyCondition(), anyState);

		anyState.addTransition(new DisjoinCondition(negPartC, negDetC),
				negPartState);
		anyState.addTransition(new AnyCondition(), startState);

		negPartState.addTransition(notCollocC, ntEndState);
		negVerbState.addTransition(notCollocC, ntEndState);
		negPartState.addTransition(new AnyCondition(), startState);
		negVerbState.addTransition(new AnyCondition(), startState);

		negPartState.addTransition(new AnyCondition(), ntEndState);
		negVerbState.addTransition(new AnyCondition(), ntEndState);

		ntEndState.addTransition(new AnyCondition(), endState);

		return m;
	}

	
	/**
	 *  should recognize:
	 *  <ul><li>A</li>
	 *  	<li>B</li>
	 *  	<li>B C</li>
	 *  	<li>B D* C</li>
	 *  </ul>
	 *  <p>where A is one of
	 *  <ul><li>without</li>
	 *  	<li>absent</li>
	 *  	<li>none</li>
	 *  </ul>
	 *  <p> and B is one of
	 *  <ul>
	 *   	<li>no</li>
	 *  	<li>any</li>
	 *  	<li>neither</li>
	 *  	<li>nor</li>
	 *  	<li>never</li>
	 *  </ul>
	 *  <p> and C is one of
	 *  <ul>
	 *   	<li>evidence</li>
	 *  	<li>indication</li>
	 *  	<li>indications</li>
	 *  	<li>sign</li>
	 *  	<li>signs</li>
	 *  	<li>symptoms</li>
	 *  	<li>symptom</li>
	 *  	<li>sx</li>
	 *  	<li>dx</li>
	 *  	<li>diagnosis</li>
	 *  	<li>history</li>
	 *  	<li>hx</li>
	 *  	<li>findings</li>
	 *  </ul>
	 * <p> and D is anything
	 * @return
	 */
	private Machine getNominalNegIndicatorMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State anyState = new NamedState("ANY");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);

		Machine m = new Machine(startState);
		State negPrepState = new NamedState("NEG_PREP");
		State negDetState = new NamedState("NEG_DET");
		State regNounState = new NamedState("REG_NOUN");

		Condition negPrepC = new TextSetCondition(iv_negPrepositionsSet, false);
		Condition negDetC = new TextSetCondition(iv_negDeterminersSet, false);
		Condition regNounC = new TextSetCondition(iv_regNounsSet, false);

		startState.addTransition(negDetC, negDetState); // start with a modal
		startState.addTransition(negPrepC, negPrepState);
		startState.addTransition(new AnyCondition(), startState);

		negPrepState.addTransition(new AnyCondition(), ntEndState);
		negDetState.addTransition(regNounC, regNounState);
		negDetState.addTransition(new AnyCondition(), ntEndState);
		negDetState.addTransition(new AnyCondition(), anyState);

		anyState.addTransition(regNounC, regNounState);
		anyState.addTransition(new AnyCondition(), anyState);

		regNounState.addTransition(new AnyCondition(), ntEndState);

		ntEndState.addTransition(new AnyCondition(), endState);

		return m;
	}

	
	/**
	 * recognizes "A B ..."
	 * where A is unremarkable, unlikely, or negative
	 * and B is of, in, for, or with
	 */

	private Machine getAdjNegIndicatorMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		endState.setEndStateFlag(true);
		ntEndState.setEndStateFlag(true);

		Machine m = new Machine(startState);
		State regPrepState = new NamedState("REG_PREP");
		State negAdjState = new NamedState("NEG_ADJ");

		Condition regPrepC = new TextSetCondition(iv_regPrepositionsSet, false);
		Condition negAdjC = new TextSetCondition(iv_negAdjectivesSet, false);

		startState.addTransition(negAdjC, negAdjState); // start with a modal
		startState.addTransition(new AnyCondition(), startState);

		negAdjState.addTransition(regPrepC, regPrepState);
		regPrepState.addTransition(new AnyCondition(), ntEndState);
		negAdjState.addTransition(new AnyCondition(), startState);

		ntEndState.addTransition(new AnyCondition(), endState);

		return m;
	}

	/**
	 * Executes the finite state machines.
	 * 
	 * @return Set of DateToken objects.
	 */
	public Set<NegationIndicator> execute(List<?> tokens) throws Exception {
		Set<NegationIndicator> outSet = new HashSet<NegationIndicator>();

		// maps a fsm to a token start index
		// key = fsm , value = token start index
		Map<Machine, Integer> tokenStartMap = new HashMap<Machine, Integer>();

		for (int i = 0; i < tokens.size(); i++) {
			BaseToken token = (BaseToken) tokens.get(i);

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
						endToken = (BaseToken) tokens.get(i - 1);
					} else {
						endToken = token;
					}

					BaseToken startToken = (BaseToken) tokens
							.get(tokenStartIndex);
					NegationIndicator neg = new NegationIndicator(startToken
							.getStartOffset(), endToken.getEndOffset());
					outSet.add(neg);
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

		return outSet;
	}
}