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
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.RangeCondition;
import org.apache.ctakes.core.fsm.condition.TextValueCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.elements.conditions.RangeStrengthCondition;
import org.apache.ctakes.drugner.fsm.output.elements.RouteToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect route data in the given
 * input of tokens.
 * @author Mayo Clinic
 */
public class RouteFSM {

	Set iv_middleTermSet = new HashSet();

	Set iv_periodSet = new HashSet();

	Set iv_singleTopicalWordSet = new HashSet();
	
	Set iv_specifiedGastricWordSet = new HashSet();

	Set iv_singleOralWordSet = new HashSet();
	
	Set iv_singleInjectWordSet = new HashSet();

	Set iv_singleRectalWordSet = new HashSet();
	
	Set iv_specifiedInjectWordSet = new HashSet();
	
	Set iv_specifiedOralWordSet = new HashSet();
	
	Set iv_specifiedPatchesWordSet = new HashSet();

	// contains the finite state machines
	private Set iv_machineSet = new HashSet();
	
    private Machine iv_PatchesMachine;
    
    private Machine iv_GastricMachine;
    
    private Machine iv_TopicalMachine;
    
    private Machine iv_OralMachine;
    
    private Machine iv_RectalMachine;
    
    private Machine iv_InjectMachine;
    

	/**
	 * 
	 * Constructor
	 *
	 */
	public RouteFSM() {
		iv_specifiedOralWordSet.add("nebulizer");
		iv_specifiedOralWordSet.add("nebulizers");
		iv_specifiedOralWordSet.add("neb");

		iv_specifiedInjectWordSet.add("injection");
		iv_specifiedInjectWordSet.add("injections");
		iv_specifiedInjectWordSet.add("injected");
	
		iv_specifiedPatchesWordSet.add("transdermal");

		iv_specifiedGastricWordSet.add("gastric");
		iv_specifiedGastricWordSet.add("duodental");
		
		iv_singleTopicalWordSet.add("drop");
		iv_singleTopicalWordSet.add("drops");
		iv_singleTopicalWordSet.add("skin");
		iv_singleTopicalWordSet.add("cream");
		iv_singleTopicalWordSet.add("creams");
		iv_singleTopicalWordSet.add("pv");
		iv_singleTopicalWordSet.add("p.v.");// TokenizerPTB handles abbreviations as tokens now.
		iv_singleTopicalWordSet.add("p.v");
		iv_singleTopicalWordSet.add("topically");
		iv_singleTopicalWordSet.add("topical");
		iv_singleTopicalWordSet.add("vaginally");
		
		
		iv_singleOralWordSet.add("po");
		iv_singleOralWordSet.add("p.o.");// TokenizerPTB handles abbreviations as tokens now.
		iv_singleOralWordSet.add("p.o");
		iv_singleOralWordSet.add("mouth");
		iv_singleOralWordSet.add("orally");
		iv_singleOralWordSet.add("oral");
		
		iv_singleRectalWordSet.add("rectally");
		iv_singleRectalWordSet.add("anally");
		iv_singleRectalWordSet.add("pr");
		iv_singleRectalWordSet.add("p.r.");// TokenizerPTB handles abbreviations as tokens now.
		iv_singleRectalWordSet.add("p.r");
		
		iv_singleInjectWordSet.add("subcutaneously");
		iv_singleInjectWordSet.add("intervenous");
		iv_singleInjectWordSet.add("intervenously");	
		iv_singleInjectWordSet.add("injection");
		iv_singleInjectWordSet.add("injections");
		iv_singleInjectWordSet.add("injected");
		iv_singleInjectWordSet.add("iv");	
		iv_singleInjectWordSet.add("intravenous");
		iv_singleInjectWordSet.add("intravenously");		

		iv_middleTermSet.add("a");
		iv_middleTermSet.add("an");
		iv_middleTermSet.add("as");
		iv_middleTermSet.add("by");
		iv_middleTermSet.add("the");
		iv_middleTermSet.add("via");
		iv_middleTermSet.add("tube");
		iv_middleTermSet.add("in");
		iv_PatchesMachine = getPatchesMachine();
		iv_GastricMachine = getGastricMachine();
		iv_TopicalMachine = getTopicalMachine();
		iv_OralMachine = getOralMachine();
		iv_RectalMachine = getRectalMachine();
		iv_InjectMachine = getInjectionMachine();
		
		iv_machineSet.add(iv_PatchesMachine);
		iv_machineSet.add(iv_GastricMachine);
		iv_machineSet.add(iv_TopicalMachine);
		iv_machineSet.add(iv_OralMachine);
		iv_machineSet.add(iv_RectalMachine);
		iv_machineSet.add(iv_InjectMachine);

	}

	
	/**
	 * Gets a finite state machine that detects the following 
	 * Such as patches
	 * <ol>
	 * 		<li>patches</li>
	 * </ol>
	 * @return
	 */
	private Machine getPatchesMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State leftAbbreviateState = new NamedState("LEFT_FREQ");
	    State leftDosagesState = new NamedState("LEFT_DOSE");
		State lastTextState = new NamedState("RIGHT_FREQ");
		State middleATextState = new NamedState("MID_TEXT");
		State firstDashState = new NamedState("FIRSTDASH");
		State secondDashState = new NamedState("SECONDDASH");
	

		Condition firstDashCondition = new PunctuationValueCondition('-');
		Condition secondDashCondition = new PunctuationValueCondition('-');
		Condition rangeCombineCondition = new DisjoinCondition(
		new RangeCondition(),
		new RangeStrengthCondition()
		);
	
	
		Condition initialMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition firstMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition secondMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition thirdMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition fourthMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		
	
		Condition specificWordCondition = new WordSetCondition(
				iv_specifiedPatchesWordSet, false);
		
	
		startState.addTransition(new TextValueCondition("a", true),
				leftAbbreviateState);
		startState.addTransition(initialMiddleTextCondition, leftAbbreviateState);
		startState.addTransition(rangeCombineCondition, leftDosagesState);
		startState.addTransition(specificWordCondition, endState);
		startState.addTransition(new AnyCondition(), startState);
	
		
		leftAbbreviateState.addTransition(firstMiddleTextCondition,
				middleATextState);
		leftAbbreviateState.addTransition(firstDashCondition, firstDashState);
		leftAbbreviateState.addTransition(new AnyCondition(), startState);
	
		firstDashState
				.addTransition(thirdMiddleTextCondition, middleATextState);
		firstDashState.addTransition(new AnyCondition(), startState);
	
		middleATextState
				.addTransition(secondMiddleTextCondition, lastTextState);
		middleATextState.addTransition(secondDashCondition, secondDashState);
		middleATextState.addTransition(new AnyCondition(), startState);
	
		secondDashState.addTransition(fourthMiddleTextCondition, lastTextState);
		secondDashState.addTransition(new AnyCondition(), startState);
	
	
		lastTextState.addTransition(new AnyCondition(), startState);
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following 
	 * medications given through a gastric or duodenal tube.
	 * <ol>
	 * 		<li>gastric tube</li>
	 * </ol>
	 * @return
	 */
	private Machine getGastricMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State leftAbbreviateState = new NamedState("LEFT_FREQ");
	    State leftDosagesState = new NamedState("LEFT_DOSE");
		State lastTextState = new NamedState("RIGHT_FREQ");
		State middleATextState = new NamedState("MID_TEXT");
		State firstDashState = new NamedState("FIRSTDASH");
		State secondDashState = new NamedState("SECONDDASH");
	
	
		Condition firstDashCondition = new PunctuationValueCondition('-');
		Condition secondDashCondition = new PunctuationValueCondition('-');
		Condition rangeCombineCondition = new DisjoinCondition(
		new RangeCondition(),
		new RangeStrengthCondition()
		);
	
	
		Condition initialMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition firstMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition secondMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition thirdMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition fourthMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		
	
		Condition specificWordCondition = new WordSetCondition(
				iv_specifiedGastricWordSet, false);
		
	
		startState.addTransition(new TextValueCondition("a", true),
				leftAbbreviateState);
		startState.addTransition(initialMiddleTextCondition, leftAbbreviateState);
		startState.addTransition(rangeCombineCondition, leftDosagesState);
		startState.addTransition(specificWordCondition, endState);
		startState.addTransition(new AnyCondition(), startState);
	
		
		leftAbbreviateState.addTransition(firstMiddleTextCondition,
				middleATextState);
		leftAbbreviateState.addTransition(firstDashCondition, firstDashState);
		leftAbbreviateState.addTransition(new AnyCondition(), startState);
	
		firstDashState
				.addTransition(thirdMiddleTextCondition, middleATextState);
		firstDashState.addTransition(new AnyCondition(), startState);
	
		middleATextState
				.addTransition(secondMiddleTextCondition, lastTextState);
		middleATextState.addTransition(secondDashCondition, secondDashState);
		middleATextState.addTransition(new AnyCondition(), startState);
	
		secondDashState.addTransition(fourthMiddleTextCondition, lastTextState);
		secondDashState.addTransition(new AnyCondition(), startState);
	

		lastTextState.addTransition(new AnyCondition(), startState);
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following 
	 * medications applied onto the skin, asthma medications,
	 * enema, eye or ear drops, decongestants, vaginal creams.
	 * <ol>
	 * 		<li>drops</li>
	 * </ol>
	 * @return
	 */
	private Machine getTopicalMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State leftAbbreviateState = new NamedState("LEFT_FREQ");
	    State leftDosagesState = new NamedState("LEFT_DOSE");
		State lastTextState = new NamedState("RIGHT_FREQ");
		State middleATextState = new NamedState("MID_TEXT");
		State firstDashState = new NamedState("FIRSTDASH");
		State secondDashState = new NamedState("SECONDDASH");

		State leftAbbreviatePState = new NamedState("LEFT_P");
    	State rightAbbreviatePVState = new NamedState("RIGHT_PV");
    	State firstDotPState = new NamedState("FIRSTDOTP");

    	//Condition secondPVDotCondition = new PunctuationValueCondition('.');
		Condition firstDashCondition = new PunctuationValueCondition('-');
		Condition secondDashCondition = new PunctuationValueCondition('-');
		Condition rangeCombineCondition = new DisjoinCondition(
		new RangeCondition(),
		new RangeStrengthCondition()
		);
	
	
		Condition initialMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition firstMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition secondMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition thirdMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition fourthMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		
		Condition soloCondition = new WordSetCondition(iv_singleTopicalWordSet, true);
	
		
	
		startState.addTransition(new TextValueCondition("a", true),
				leftAbbreviateState);
		startState.addTransition(new TextValueCondition("p", true),
				leftAbbreviatePState);
		startState.addTransition(initialMiddleTextCondition, leftAbbreviateState);
		startState.addTransition(rangeCombineCondition, leftDosagesState);
		startState.addTransition(soloCondition, endState);
	
		startState.addTransition(new AnyCondition(), startState);
	
		leftAbbreviatePState.addTransition(new PunctuationValueCondition('.'), firstDotPState);
		leftAbbreviatePState.addTransition(new AnyCondition(), startState);

		firstDotPState.addTransition(soloCondition, endState);
		firstDotPState.addTransition(new TextValueCondition("v", true),
				rightAbbreviatePVState);
		firstDotPState.addTransition(new AnyCondition(), startState);
		leftAbbreviateState.addTransition(firstMiddleTextCondition,
				middleATextState);
		leftAbbreviateState.addTransition(firstDashCondition, firstDashState);
		leftAbbreviateState.addTransition(soloCondition, endState);
		leftAbbreviateState.addTransition(new AnyCondition(), startState);
	
		firstDashState
				.addTransition(thirdMiddleTextCondition, middleATextState);
		firstDashState.addTransition(new AnyCondition(), startState);
	
		middleATextState
				.addTransition(secondMiddleTextCondition, lastTextState);
		middleATextState.addTransition(secondDashCondition, secondDashState);
		middleATextState.addTransition(new AnyCondition(), startState);
	
		secondDashState.addTransition(fourthMiddleTextCondition, lastTextState);
		secondDashState.addTransition(new AnyCondition(), startState);
	
		lastTextState.addTransition(new AnyCondition(), startState);
	
		rightAbbreviatePVState.addTransition(new PunctuationValueCondition('.'), endState);
		rightAbbreviatePVState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following 
	 * Most drugs taken by mouth such as tablets or capsules.
	 * <ol>
	 * 		<li>tabs</li>
	 * </ol>
	 * @return
	 */
	private Machine getOralMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State leftAbbreviateState = new NamedState("LEFT_FREQ");
	    State leftDosagesState = new NamedState("LEFT_DOSE");
		State lastTextState = new NamedState("RIGHT_FREQ");
		State middleATextState = new NamedState("MID_TEXT");
		State firstDashState = new NamedState("FIRSTDASH");
		State secondDashState = new NamedState("SECONDDASH");
		State leftAbbreviatePState = new NamedState("LEFT_P");
    	State rightAbbreviatePOState = new NamedState("RIGHT_PO");
    	State firstDotPState = new NamedState("FIRSTDOTP");


    	Condition firstPODotCondition = new PunctuationValueCondition('.');
    	Condition secondPODotCondition = new PunctuationValueCondition('.');
		Condition firstDashCondition = new PunctuationValueCondition('-');
		Condition secondDashCondition = new PunctuationValueCondition('-');
		Condition rangeCombineCondition = new DisjoinCondition(
		new RangeCondition(),
		new RangeStrengthCondition()
		);
	
	
		Condition initialMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition firstMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition secondMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition thirdMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition fourthMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		
		Condition soloCondition = new WordSetCondition(iv_singleOralWordSet, true);
		Condition specificWordCondition = new WordSetCondition(
				iv_specifiedOralWordSet, false);
		
	
		startState.addTransition(new TextValueCondition("a", true),
				leftAbbreviateState);
		startState.addTransition(new TextValueCondition("p", true),
				leftAbbreviatePState);
		startState.addTransition(initialMiddleTextCondition, leftAbbreviateState);
		startState.addTransition(rangeCombineCondition, leftDosagesState);
		startState.addTransition(soloCondition, endState);
		startState.addTransition(specificWordCondition, endState);
		startState.addTransition(new AnyCondition(), startState);
		
		leftAbbreviatePState.addTransition(firstPODotCondition, firstDotPState);
		leftAbbreviatePState.addTransition(new AnyCondition(), startState);

		firstDotPState.addTransition(soloCondition, endState);
				
		firstDotPState.addTransition(new TextValueCondition("o", true),
				rightAbbreviatePOState);
		firstDotPState.addTransition(new AnyCondition(), startState);
		
		leftAbbreviateState.addTransition(firstMiddleTextCondition,
				middleATextState);
		leftAbbreviateState.addTransition(firstDashCondition, firstDashState);
		leftAbbreviateState.addTransition(soloCondition, endState);
		leftAbbreviateState.addTransition(new AnyCondition(), startState);
	
		firstDashState
				.addTransition(thirdMiddleTextCondition, middleATextState);
		firstDashState.addTransition(new AnyCondition(), startState);
	
		middleATextState
				.addTransition(secondMiddleTextCondition, lastTextState);
		middleATextState.addTransition(secondDashCondition, secondDashState);
		middleATextState.addTransition(new AnyCondition(), startState);
	
		secondDashState.addTransition(fourthMiddleTextCondition, lastTextState);
		secondDashState.addTransition(new AnyCondition(), startState);
	
		lastTextState.addTransition(new AnyCondition(), startState);
		rightAbbreviatePOState.addTransition(secondPODotCondition, endState);
		rightAbbreviatePOState.addTransition(new AnyCondition(), startState);	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following 
	 * Drugs in suppositories or enema form.
	 * <ol>
	 * 		<li>enema</li>
	 * </ol>
	 * @return
	 */
	private Machine getRectalMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
		
		Condition soloCondition = new WordSetCondition(iv_singleRectalWordSet, true);
	
		startState.addTransition(soloCondition, endState);
		startState.addTransition(new AnyCondition(), startState);
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}
	
	/**
	 * Gets a finite state machine that detects the following 
	 * Drugs in inject or intervenous form.
	 * <ol>
	 * 		<li>intervenously</li>
	 * </ol>
	 * @return
	 */
	private Machine getInjectionMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
		
		Condition soloCondition = new WordSetCondition(iv_singleInjectWordSet, true);
	
		startState.addTransition(soloCondition, endState);
		startState.addTransition(new AnyCondition(), startState);
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Executes the finite state machines.
	 * @param tokens
	 * @return Set RouteToken objects.
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
					BaseToken startToken = (BaseToken) tokens
							.get(tokenStartIndex);
					BaseToken endToken = token;
					RouteToken segmentToken = null;
					
					if (fsm.equals(iv_PatchesMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.TRANSDERMAL);
					}
					else if (fsm.equals(iv_GastricMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.GASTRIC);
					}
					else if (fsm.equals(iv_TopicalMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.TOPICAL);
					}
					else if (fsm.equals(iv_OralMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.ORAL);
					}
					else if (fsm.equals(iv_RectalMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.RECTAL);
					}
					else if (fsm.equals(iv_InjectMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.INTRAVENOUS);
					}
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
					RouteToken segmentToken = null;
					
					if (fsm.equals(iv_PatchesMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.TRANSDERMAL);
					}
					else if (fsm.equals(iv_GastricMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.GASTRIC);
					}
					else if (fsm.equals(iv_TopicalMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.TOPICAL);
					}
					else if (fsm.equals(iv_OralMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.ORAL);
					}
					else if (fsm.equals(iv_RectalMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.RECTAL);
					}
					else if (fsm.equals(iv_InjectMachine)){
						segmentToken = new RouteToken(
								startToken.getStartOffset(), endToken
										.getEndOffset());
						segmentToken.setFormMethod(RouteToken.INTRAVENOUS);
					}
					
					fractionSet.add(segmentToken);
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
