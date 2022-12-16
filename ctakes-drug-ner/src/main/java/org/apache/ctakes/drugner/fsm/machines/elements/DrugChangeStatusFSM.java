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
import org.apache.ctakes.drugner.fsm.elements.conditions.StrengthCondition;
import org.apache.ctakes.drugner.fsm.output.elements.DrugChangeStatusToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect change status data in the
 * given input of tokens.
 * 
 * @author Mayo Clinic
 */
public class DrugChangeStatusFSM {

	Set iv_singleStopWordSet = new HashSet();

	Set iv_singleStartWordSet = new HashSet();

	Set iv_singleIncreaseWordSet = new HashSet();

	Set iv_singleDecreaseWordSet = new HashSet();

	Set iv_singleNoChangeWordSet = new HashSet();

	Set iv_singleChangeWordSet = new HashSet();
	
	Set iv_singleSumWordSet = new HashSet();

	Set iv_firstStartDualWordSet = new HashSet();
	
	Set iv_firstStopDualWordSet = new HashSet();
	
	Set iv_firstNoChangeDualWordSet = new HashSet();
	
	Set iv_firstIncreaseDualWordSet = new HashSet();
	
	Set iv_firstDecreaseDualWordSet = new HashSet();
		
	Set iv_secondDualWordSet = new HashSet();
	
	Set iv_secondDualFromWordSet = new HashSet();
	
	Set iv_secondOffDualWordSet = new HashSet();

	Set iv_noChangeWordSet = new HashSet();

	Set iv_changeWordSet = new HashSet();
	
	Set iv_singleMaxWordSet = new HashSet();
	
	Set iv_firstMaxDualWordSet = new HashSet();
	
	Set iv_secondMaxDualWordSet = new HashSet();
	
	Set iv_multiThenWordSet = new HashSet();

	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	private Machine iv_startStatusMachine;

	private Machine iv_stopStatusMachine;
	
	private Machine iv_increaseStatusMachine;
	
	private Machine iv_decreaseStatusMachine;
	
	private Machine iv_increaseFromStatusMachine;
	
	private Machine iv_decreaseFromStatusMachine;
	
	private Machine iv_noChangeStatusMachine;
	
	private Machine iv_changeStatusMachine;
	
	private Machine iv_sumStatusMachine;
	
	private Machine iv_maxStatusMachine;

	/**
	 * 
	 * Constructor
	 * 
	 */
	public DrugChangeStatusFSM() {

		iv_singleStopWordSet.add("discontinued");
		iv_singleStopWordSet.add("stopped");
		iv_singleStopWordSet.add("ended");
		iv_singleStopWordSet.add("finished");
		iv_singleStopWordSet.add("held");
		iv_singleStopWordSet.add("discontinue");
		iv_singleStopWordSet.add("stopping");
		iv_singleStopWordSet.add("ending");
		iv_singleStopWordSet.add("stop");
		iv_singleStopWordSet.add("stops");
		iv_singleStopWordSet.add("end");
		iv_singleStopWordSet.add("ends");
		iv_singleStopWordSet.add("off");
		iv_singleStopWordSet.add("quit");

		iv_singleStartWordSet.add("started");
		iv_singleStartWordSet.add("starting");
		iv_singleStartWordSet.add("begin");
		iv_singleStartWordSet.add("begins");
		iv_singleStartWordSet.add("restart");
		iv_singleStartWordSet.add("restarted");
		iv_singleStartWordSet.add("start");
		iv_singleStartWordSet.add("starts");
		iv_singleStartWordSet.add("reinstitued");

		iv_singleIncreaseWordSet.add("increased");
		iv_singleIncreaseWordSet.add("increase");

		iv_singleDecreaseWordSet.add("lower");
		iv_singleDecreaseWordSet.add("lowers");
		iv_singleDecreaseWordSet.add("lowered");
		iv_singleDecreaseWordSet.add("decreased");
		iv_singleDecreaseWordSet.add("decrease");
		iv_singleDecreaseWordSet.add("decreases");
		iv_singleDecreaseWordSet.add("reduce");
		iv_singleDecreaseWordSet.add("reduces");
		iv_singleDecreaseWordSet.add("reduced");
		
		iv_singleNoChangeWordSet.add("continued");
		iv_singleNoChangeWordSet.add("continue");
		iv_singleNoChangeWordSet.add("continues");

		iv_multiThenWordSet.add("then"); 
		iv_multiThenWordSet.add("taper");
		iv_multiThenWordSet.add("tapers");
		iv_multiThenWordSet.add("tapered");
		iv_multiThenWordSet.add("tapering");
		
		iv_singleChangeWordSet.add("then");
		iv_singleChangeWordSet.add("taper");
		iv_singleChangeWordSet.add("tapers");
		iv_singleChangeWordSet.add("tapered");
		iv_singleChangeWordSet.add("tapering");
		
		iv_firstStartDualWordSet.add("new");
		iv_firstStartDualWordSet.add("admission");
		iv_firstStartDualWordSet.add("reinstituted");
		
		iv_firstStopDualWordSet.add("discontinued");
		iv_firstStopDualWordSet.add("discontinue");
		iv_firstStopDualWordSet.add("on");
		iv_firstStopDualWordSet.add("past");
		iv_firstStopDualWordSet.add("end");
		
		iv_firstNoChangeDualWordSet.add("continued");
		iv_firstNoChangeDualWordSet.add("alter");
		iv_firstNoChangeDualWordSet.add("altered");
		iv_firstNoChangeDualWordSet.add("future");

		iv_firstIncreaseDualWordSet.add("increase");
		iv_firstIncreaseDualWordSet.add("increases");
		iv_firstIncreaseDualWordSet.add("increased");
		iv_firstIncreaseDualWordSet.add("increasing");
		
		iv_firstDecreaseDualWordSet.add("taper");
		iv_firstDecreaseDualWordSet.add("tapering");
		iv_firstDecreaseDualWordSet.add("tapered");
		iv_firstDecreaseDualWordSet.add("decrease");
		iv_firstDecreaseDualWordSet.add("decreased");
		
		iv_firstDecreaseDualWordSet.add("reduced");
		iv_firstDecreaseDualWordSet.add("reduce");
		iv_firstDecreaseDualWordSet.add("lower");
		iv_firstDecreaseDualWordSet.add("lowered");
		
		
		iv_secondDualWordSet.add("medications");
		iv_secondDualWordSet.add("dose");
		iv_secondDualWordSet.add("dosage");
		iv_secondDualWordSet.add("dosages");
		iv_secondDualWordSet.add("medication");
		iv_secondDualWordSet.add("psychotropic");
		iv_secondDualWordSet.add("taper");
		iv_secondDualWordSet.add("tapering");
		iv_secondDualWordSet.add("to");
		iv_secondDualWordSet.add("complete");
		iv_secondDualWordSet.add("completed");
		
		iv_secondDualFromWordSet.add("from");
		
		iv_secondOffDualWordSet.add("off");
		iv_secondOffDualWordSet.add("stop");
		iv_secondOffDualWordSet.add("hold");
		iv_secondOffDualWordSet.add("discontinue");
		iv_secondOffDualWordSet.add("discontinued");
				
		iv_noChangeWordSet.add("no");

		iv_changeWordSet.add("change");
		iv_changeWordSet.add("changed"); 
		
		iv_singleMaxWordSet.add("maximum");
		iv_singleMaxWordSet.add("max");
		iv_singleMaxWordSet.add("total");
		iv_singleMaxWordSet.add("totaling");
		
		iv_firstMaxDualWordSet.add("maximum");
		iv_firstMaxDualWordSet.add("max");
		iv_firstMaxDualWordSet.add("total");
		
		iv_secondMaxDualWordSet.add("of");


		iv_startStatusMachine = getStartStatusMachine();
		iv_stopStatusMachine = getStopStatusMachine();
		iv_increaseStatusMachine = getIncreaseStatusMachine();
		iv_decreaseStatusMachine = getDecreaseStatusMachine();
		iv_noChangeStatusMachine = getNoChangeStatusMachine();
		iv_changeStatusMachine = getChangeStatusMachine();
		iv_sumStatusMachine = getSumStatusMachine();
		iv_maxStatusMachine = getMaximumStatusMachine();
		iv_increaseFromStatusMachine = getIncreaseFromAndTheStatusMachine();
		iv_decreaseFromStatusMachine = getDecreaseFromAndTheStatusMachine();
		
		iv_machineSet.add(iv_startStatusMachine);
		iv_machineSet.add(iv_stopStatusMachine);
		iv_machineSet.add(iv_increaseFromStatusMachine);
		iv_machineSet.add(iv_decreaseFromStatusMachine);
		iv_machineSet.add(iv_increaseStatusMachine);
		iv_machineSet.add(iv_decreaseStatusMachine);
		iv_machineSet.add(iv_noChangeStatusMachine);
		iv_machineSet.add(iv_changeStatusMachine);
		iv_machineSet.add(iv_sumStatusMachine);
		iv_machineSet.add(iv_maxStatusMachine);


	}

	/**
	 * Gets a finite state machine that detects the following: 'maximum' or 'total':
	 * <ol>
	 * <li>max of</li>
	 * <li>totaling</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getMaximumStatusMachine() {
		State startState = new NamedState("START1");
		State endState = new NamedState("END1");
		State foundDualFirstState = new NamedState("MAXBEGIN");
		endState.setEndStateFlag(true);
		
		Machine m = new Machine(startState);
		
		startState.addTransition(new WordSetCondition(iv_singleMaxWordSet, false), endState);
		startState.addTransition(new WordSetCondition(iv_firstMaxDualWordSet, false), foundDualFirstState);
		startState.addTransition(new AnyCondition(), startState);
		
		foundDualFirstState.addTransition(new WordSetCondition(iv_secondMaxDualWordSet, false), endState);
		foundDualFirstState.addTransition(new AnyCondition(), startState);
		
		endState.addTransition(new AnyCondition(), startState);
		return m;
	}
	
	/**
	 * Gets a finite state machine that detects the following: 'tapered' or 'then':
	 * <ol>
	 * <li>taper</li>
	 * <li>then</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getChangeStatusMachine() {
		State startState = new NamedState("START2");
		State endState = new NamedState("END2");
		State byState = new NamedState("BYSTATE");
		endState.setEndStateFlag(true);
		
		Machine m = new Machine(startState);
		
		startState.addTransition(new WordSetCondition(iv_singleChangeWordSet, false), endState);
		startState.addTransition(new TextValueCondition("followed", false), byState);
		startState.addTransition(
				new WordSetCondition(iv_changeWordSet, false), endState);
		startState.addTransition(new AnyCondition(), startState);
		
		byState.addTransition(new TextValueCondition("by", false), endState);
		byState.addTransition(new AnyCondition(), startState);
		
		endState.addTransition(new AnyCondition(), startState);
		return m;
	}
	/**
	 * Gets a finite state machine that detects the following: 'and':
	 * <ol>
	 * <li>and</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getSumStatusMachine() {
		State startState = new NamedState("START3");
		State endState = new NamedState("END3");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		startState.addTransition(new WordSetCondition(iv_singleSumWordSet, false), endState);
		startState.addTransition(new AnyCondition(), startState);
		
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}
	
	/**
	 * Gets a finite state machine that detects the following: 'start' or 'restarted':
	 * <ol>
	 * <li>started</li>
	 * <li>restart</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getStartStatusMachine() {
		State startState = new NamedState("START4");
		State endState = new NamedState("END4");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State leftStatusState = new NamedState("LEFT_START_STATUS");
		State leftDosagesState = new NamedState("LEFT_START_DOSE");
		State lastTextState = new NamedState("RIGHT_START_FREQ");
		State middleATextState = new NamedState("MID_START_TEXT");
		State firstDashState = new NamedState("FIRSTDASHSTART");
		State beginEndState = new NamedState("STOPBEGINSTART");
		State endEndState = new NamedState("STARTENDSTART");
		State sectionStatusState = new NamedState("DUALSTARTSTART");
		State thenStatusState = new NamedState("DUALSTARTTHENSTART");
		
		Condition rangeCombineCondition = new DisjoinCondition(
				new RangeCondition(), new RangeStrengthCondition());
	
		Condition sectionBracket = new PunctuationValueCondition('[');
		Condition soloCondition = new WordSetCondition(iv_singleStartWordSet,
				false);
		Condition soloEndCondition = new WordSetCondition(
				iv_singleStartWordSet, false);
		Condition firstDualCondition = new WordSetCondition(
				iv_firstStartDualWordSet, false);
		Condition secondDualCondition = new WordSetCondition(
				iv_secondDualWordSet, false);
		Condition thenCondition = new WordSetCondition(
				iv_multiThenWordSet, false);
		Condition containsDosagesCondition = new StrengthCondition();
	
		startState.addTransition(thenCondition, thenStatusState);
		startState.addTransition(firstDualCondition, sectionStatusState);
		startState.addTransition(sectionBracket, beginEndState);
		startState.addTransition(rangeCombineCondition, leftDosagesState);
		startState.addTransition(containsDosagesCondition, leftStatusState);
		startState.addTransition(soloCondition, endState);
		startState.addTransition(new AnyCondition(), startState);

		thenStatusState.addTransition(firstDualCondition, sectionStatusState);
		thenStatusState.addTransition(soloCondition, endState);
		thenStatusState.addTransition(new AnyCondition(), startState);
		
		sectionStatusState.addTransition(secondDualCondition, endState);
		sectionStatusState.addTransition(new AnyCondition(), startState);
	
		beginEndState.addTransition(soloEndCondition, endEndState);
		beginEndState.addTransition(new AnyCondition(), startState);
	
		endEndState.addTransition(new TextValueCondition("section", true),
				leftStatusState);
		endEndState.addTransition(new AnyCondition(), startState);
	
		leftStatusState.addTransition(soloCondition, endState);
		leftStatusState.addTransition(new AnyCondition(), startState);
	
		firstDashState.addTransition(new AnyCondition(), startState);
	
		middleATextState.addTransition(new AnyCondition(), startState);
	
		lastTextState.addTransition(new AnyCondition(), startState);
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following 'current':
	 * <ol>
	 * <li>current medications</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getNoChangeStatusMachine() {
		State startState = new NamedState("START5");
		State endState = new NamedState("END5");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State leftStatusState = new NamedState("LEFT_STATUS_NOCHANGE");
		State leftDosagesState = new NamedState("LEFT_DOSE_NOCHANGE");
		State lastTextState = new NamedState("RIGHT_FREQ_NOCHANGE");
		State middleATextState = new NamedState("MID_TEXT_NOCHANGE");
		State firstDashState = new NamedState("FIRSTDASHNOCHANGE");
		State thenStatusState = new NamedState("DUALSTARTINCREASE");
		
	
		State sectionStatusState = new NamedState("DUALSTARTNOCHANGE");
		State dualWordState = new NamedState("START_NOCHANGE");
	
		Condition rangeCombineCondition = new DisjoinCondition(
				new RangeCondition(), new RangeStrengthCondition());
	
		Condition soloCondition = new WordSetCondition(iv_singleNoChangeWordSet,
				false);
		Condition firstDualCondition = new WordSetCondition(
				iv_firstNoChangeDualWordSet, false);
		Condition secondDualCondition = new WordSetCondition(
				iv_secondDualWordSet, false);
		Condition thenCondition = new WordSetCondition(
				iv_multiThenWordSet, false);
		Condition containsDosagesCondition = new StrengthCondition();
	
		startState.addTransition(thenCondition, thenStatusState);
		startState.addTransition(soloCondition, endState);
		startState.addTransition(firstDualCondition, sectionStatusState);
		startState.addTransition(rangeCombineCondition, leftDosagesState);
		startState.addTransition(containsDosagesCondition, leftStatusState);
		startState.addTransition(
				new WordSetCondition(iv_noChangeWordSet, false), dualWordState);
		startState.addTransition(new AnyCondition(), startState);
	
		thenStatusState.addTransition(firstDualCondition, sectionStatusState);
		thenStatusState.addTransition(soloCondition, endState);
		thenStatusState.addTransition(new AnyCondition(), startState);
		
		sectionStatusState.addTransition(secondDualCondition, endState);
		sectionStatusState.addTransition(new AnyCondition(), startState);
	
		
		leftStatusState.addTransition(soloCondition, endState);
		leftStatusState.addTransition(new AnyCondition(), startState);
	
		firstDashState.addTransition(new AnyCondition(), startState);
	
		dualWordState.addTransition(new WordSetCondition(iv_changeWordSet,
				false), endState);
		dualWordState.addTransition(new AnyCondition(), startState);
		middleATextState.addTransition(new AnyCondition(), startState);
	
		lastTextState.addTransition(new AnyCondition(), startState);
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following : 'increase':
	 * 
	 * <ol>
	 * <li>increase</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getIncreaseStatusMachine() {
		State startState = new NamedState("START6");
		State endState = new NamedState("END6");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State thenStatusState = new NamedState("DUALSTARTTHENINCREASE");
		State sectionStatusState = new NamedState("DUALSTARTINCREASE");
		
		Condition soloCondition = new WordSetCondition(iv_singleIncreaseWordSet,
				false);
		Condition firstDualCondition = new WordSetCondition(
				iv_firstIncreaseDualWordSet, false);
		Condition secondDualCondition = new WordSetCondition(
				iv_secondDualWordSet, false);
		Condition thenCondition = new WordSetCondition(
				iv_multiThenWordSet, false);
		
		startState.addTransition(thenCondition, thenStatusState);
		startState.addTransition(soloCondition, endState);
		startState.addTransition(firstDualCondition, sectionStatusState);
		startState.addTransition(new AnyCondition(), startState);
		
		
		thenStatusState.addTransition(soloCondition, endState);
		thenStatusState.addTransition(firstDualCondition, sectionStatusState);
		thenStatusState.addTransition(new AnyCondition(), startState);
	
		sectionStatusState.addTransition(secondDualCondition, endState);
		sectionStatusState.addTransition(new AnyCondition(), startState);
		
	
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following : 'increase from':
	 * 
	 * <ol>
	 * <li>increase from</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getIncreaseFromAndTheStatusMachine() {
		State startState = new NamedState("START7");
		State endState = new NamedState("END7");
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
	
		State thenStatusState = new NamedState("DUALSTARTTHENINCREASEFROM");
		State sectionStatusState = new NamedState("DUALFROMINCREASE");

		Condition firstDualCondition = new WordSetCondition(
				iv_firstIncreaseDualWordSet, false);
		Condition secondDualCondition = new WordSetCondition(
				iv_secondDualFromWordSet, false);
		Condition thenCondition = new WordSetCondition(
				iv_multiThenWordSet, false);
		
		startState.addTransition(thenCondition, thenStatusState);
		startState.addTransition(firstDualCondition, sectionStatusState);
		startState.addTransition(new AnyCondition(), startState);
		
		thenStatusState.addTransition(firstDualCondition, sectionStatusState);
		thenStatusState.addTransition(new AnyCondition(), startState);
		
		sectionStatusState.addTransition(secondDualCondition, endState);
		sectionStatusState.addTransition(new AnyCondition(), startState);
		
		endState.addTransition(new AnyCondition(), startState);
	
		return m;
	}
	/**
	 * Gets a finite state machine that detects the following : 'decrease' or 'lowered':
	 * <ol>
	 * <li>decrease</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getDecreaseStatusMachine() {
		State startState = new NamedState("START8");
		State endState = new NamedState("END8");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State leftStatusState = new NamedState("LEFT_STATUS_DECREASE");
		State thenStatusState = new NamedState("DUALSTARTTHENDECREASE");
		State sectionStatusState = new NamedState("DUALSTARTDECREASE");

		Condition soloCondition = new WordSetCondition(iv_singleDecreaseWordSet,
				false);
		Condition firstDualCondition = new WordSetCondition(
				iv_firstDecreaseDualWordSet, false);
		Condition secondDualCondition = new WordSetCondition(
				iv_secondDualWordSet, false);
		Condition thenCondition = new WordSetCondition(
				iv_multiThenWordSet, false);
	
		startState.addTransition(thenCondition, thenStatusState);
		startState.addTransition(soloCondition, endState);
		startState.addTransition(firstDualCondition, sectionStatusState);
		startState.addTransition(new AnyCondition(), startState);
		
		thenStatusState.addTransition(soloCondition, endState);
		thenStatusState.addTransition(firstDualCondition, sectionStatusState);
		thenStatusState.addTransition(new AnyCondition(), startState);

		sectionStatusState.addTransition(secondDualCondition, endState);
		sectionStatusState.addTransition(new AnyCondition(), startState);

		
		leftStatusState.addTransition(soloCondition, endState);
		leftStatusState.addTransition(new AnyCondition(), startState);



		endState.addTransition(new AnyCondition(), startState);

		return m;
	}
	
	/**
	 * Gets a finite state machine that detects the following : 'decrease from' or 'lowered from':
	 * <ol>
	 * <li>decrease from</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getDecreaseFromAndTheStatusMachine() {
		State startState = new NamedState("START9");
		State endState = new NamedState("END9");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State thenStatusState = new NamedState("DUALSTARTTHENDECREASEFROM");
		State sectionStatusState = new NamedState("DUALFROMDECREASE");
//		State firstTheState = new NamedState("FIRSTTHEDECREASE");
//		State secondTheState = new NamedState("SECONDTHEDECREASE");
		
		Condition firstDualCondition = new WordSetCondition(
				iv_firstDecreaseDualWordSet, false);
		Condition secondDualCondition = new WordSetCondition(
				iv_secondDualFromWordSet, false);
		Condition thenCondition = new WordSetCondition(
				iv_multiThenWordSet, false);

		startState.addTransition(thenCondition, thenStatusState);
		startState.addTransition(firstDualCondition, sectionStatusState);
		startState.addTransition(new AnyCondition(), startState);

		thenStatusState.addTransition(firstDualCondition, sectionStatusState);
		thenStatusState.addTransition(new AnyCondition(), startState);
		
		sectionStatusState.addTransition(secondDualCondition, endState);
		sectionStatusState.addTransition(new AnyCondition(), startState);

				
		endState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Gets a finite state machine that detects the following 'stop', 'stopped',
	 * or 'discontinue':
	 * <ol>
	 * <li>stopped/li>
	 * <li>discontinued</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getStopStatusMachine() {
		State startState = new NamedState("START10");
		State endState = new NamedState("END10");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);
		
		State thenStatusState = new NamedState("DUALSTARTTHENSTOP");
		State leftStatusState = new NamedState("LEFT_STATUS_STOP");
		State leftDosagesState = new NamedState("LEFT_DOSE_STOP");
		State lastTextState = new NamedState("RIGHT_FREQ_STOP");
		State middleATextState = new NamedState("MID_TEXT_STOP");
		State firstDashState = new NamedState("FIRSTDASHSTOP");
		State beginEndState = new NamedState("STOPBEGINSTOP");
		State endEndState = new NamedState("STOPENDSTOP");
		State sectionStatusState = new NamedState("DUALSTARTSTOP");

		Condition rangeCombineCondition = new DisjoinCondition(
				new RangeCondition(), new RangeStrengthCondition());

		Condition sectionBracket = new PunctuationValueCondition('[');
		Condition soloCondition = new WordSetCondition(iv_singleStopWordSet,
				false);
		Condition soloEndCondition = new WordSetCondition(iv_singleStopWordSet,
				false);
		Condition firstDualCondition = new WordSetCondition(
				iv_firstStopDualWordSet, false);
		Condition secondOffDualCondition = new WordSetCondition(
				iv_secondOffDualWordSet, false);
		Condition secondDualCondition = new WordSetCondition(
				iv_secondDualWordSet, false);
		Condition thenCondition = new WordSetCondition(
				iv_multiThenWordSet, false);
		
		Condition containsDosagesCondition = new StrengthCondition();

		startState.addTransition(thenCondition, thenStatusState);
		startState.addTransition(soloCondition, endState);
		startState.addTransition(firstDualCondition, sectionStatusState);
		startState.addTransition(sectionBracket, beginEndState);
		startState.addTransition(rangeCombineCondition, leftDosagesState);
		startState.addTransition(containsDosagesCondition, leftStatusState);
		startState.addTransition(new AnyCondition(), startState);
		
		thenStatusState.addTransition(soloCondition, endState);
		thenStatusState.addTransition(firstDualCondition, sectionStatusState);
		thenStatusState.addTransition(new AnyCondition(), startState);
		
		sectionStatusState.addTransition(secondDualCondition, endState);
		sectionStatusState.addTransition(secondOffDualCondition, endState);
		sectionStatusState.addTransition(new AnyCondition(), startState);

		beginEndState.addTransition(soloEndCondition, endEndState);
		beginEndState.addTransition(new AnyCondition(), startState);

		endEndState.addTransition(new TextValueCondition("section", true),
				leftStatusState);
		endEndState.addTransition(new AnyCondition(), startState);

		leftStatusState.addTransition(soloCondition, endState);
		leftStatusState.addTransition(new AnyCondition(), startState);

		firstDashState.addTransition(new AnyCondition(), startState);

		middleATextState.addTransition(new AnyCondition(), startState);

		lastTextState.addTransition(new AnyCondition(), startState);

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
					DrugChangeStatusToken fractionToken = null;
					if (fsm.equals(iv_startStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(),DrugChangeStatusToken.START);
					} else if (fsm.equals(iv_stopStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.STOP);
					} else if (fsm.equals(iv_increaseFromStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.INCREASEFROM);
					} else if (fsm.equals(iv_decreaseFromStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.DECREASEFROM);
					}  else if (fsm.equals(iv_increaseStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.INCREASE);
					} else if (fsm.equals(iv_decreaseStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.DECREASE);
					} else if (fsm.equals(iv_noChangeStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.NOCHANGE);
						
					} else if (fsm.equals(iv_changeStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.OTHER);
					} else if (fsm.equals(iv_sumStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.SUM);
						
					} else if (fsm.equals(iv_maxStatusMachine)) {
						fractionToken = new DrugChangeStatusToken(startToken
								.getStartOffset(), endToken.getEndOffset(), DrugChangeStatusToken.MAX);
						
					}

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
