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

import org.apache.ctakes.core.fsm.condition.IntegerCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.TextValueCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.elements.conditions.ContainsSetTextValueCondition;
import org.apache.ctakes.drugner.fsm.output.util.SuffixFrequencyToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect frequency suffix data in the given
 * input of tokens.
 * @author Mayo Clinic
 */
public class SuffixFrequencyFSM {
	// text fractions
	Set iv_textSuffixSet = new HashSet();

	Set iv_textPrefixSet = new HashSet();

	Set iv_frequencySet = new HashSet();

	Set iv_middleTermSet = new HashSet();

	Set iv_periodSet = new HashSet();

	Set iv_hyphenatedSet = new HashSet();

	Set iv_singleWordSet = new HashSet();

	Set iv_specifiedWordSet = new HashSet();

	// contains the finite state machines
	private Set iv_machineSet = new HashSet();

	
	/**
	 * 
	 * Constructor
	 *
	 */
	public SuffixFrequencyFSM() {
		iv_specifiedWordSet.add("noon");
		iv_specifiedWordSet.add("lunch");
		iv_specifiedWordSet.add("breakfast");
		iv_specifiedWordSet.add("dinner");
		iv_specifiedWordSet.add("morning");
		iv_specifiedWordSet.add("afternoon");
		iv_specifiedWordSet.add("evening");
		iv_specifiedWordSet.add("am");
		iv_specifiedWordSet.add("pm");
		iv_specifiedWordSet.add("specified");

		iv_singleWordSet.add("weekly");
		iv_singleWordSet.add("monthly");
		iv_singleWordSet.add("biweekly");
		iv_singleWordSet.add("daily");
		iv_singleWordSet.add("nightly");
		iv_singleWordSet.add("bid");
		iv_singleWordSet.add("od");
		iv_singleWordSet.add("qd");
		iv_singleWordSet.add("hs");
		iv_singleWordSet.add("prn");
		iv_singleWordSet.add("tid");
		iv_singleWordSet.add("q");

		iv_textSuffixSet.add("d");
		iv_textSuffixSet.add("y");
		iv_textSuffixSet.add("m");
		iv_textSuffixSet.add("mo");
		iv_textSuffixSet.add("yr");
		iv_textSuffixSet.add("day");
		iv_textSuffixSet.add("daily");
		iv_textSuffixSet.add("wk");
		iv_textSuffixSet.add("week");
		iv_textSuffixSet.add("weeks");
		iv_textSuffixSet.add("h");
		iv_textSuffixSet.add("hour");
		iv_textSuffixSet.add("hours");
		iv_textSuffixSet.add("min");
		iv_textSuffixSet.add("month");
		iv_textSuffixSet.add("months");
		iv_textSuffixSet.add("year");

		iv_textPrefixSet.add("every");

		iv_frequencySet.add("once");
		iv_frequencySet.add("twice");
		iv_frequencySet.add("one");
		iv_frequencySet.add("two");
		iv_frequencySet.add("three");
		iv_frequencySet.add("four");
		iv_frequencySet.add("five");
		iv_frequencySet.add("six");
		iv_frequencySet.add("seven");
		iv_frequencySet.add("eight");
		iv_frequencySet.add("nine");

		iv_middleTermSet.add("a");
		iv_middleTermSet.add("an");
		iv_middleTermSet.add("as");
		iv_middleTermSet.add("in");
		iv_middleTermSet.add("the");
		iv_middleTermSet.add("each");
		iv_middleTermSet.add("times");
		iv_middleTermSet.add("time");
		iv_middleTermSet.add("per");
		iv_middleTermSet.add("every");
		iv_middleTermSet.add("at");

		iv_hyphenatedSet.add("once-a-day");
		iv_hyphenatedSet.add("once-a-week");
		iv_hyphenatedSet.add("twice-a-day");
		iv_hyphenatedSet.add("twice-a-week");
		iv_hyphenatedSet.add("once-daily");
		iv_hyphenatedSet.add("twice-daily");
		iv_hyphenatedSet.add("one-a-day");
		iv_hyphenatedSet.add("two-a-day");
		iv_hyphenatedSet.add("three-a-day");
		iv_hyphenatedSet.add("four-a-day");
		iv_hyphenatedSet.add("five-a-day");
		iv_hyphenatedSet.add("six-a-day");
		iv_hyphenatedSet.add("seven-a-day");
		iv_hyphenatedSet.add("eight-a-day");
		iv_hyphenatedSet.add("nine-a-day");
		iv_hyphenatedSet.add("once-weekly");
		iv_hyphenatedSet.add("twice-weekly");
		iv_hyphenatedSet.add("one-a-week");
		iv_hyphenatedSet.add("two-a-week");
		iv_hyphenatedSet.add("three-a-week");
		iv_hyphenatedSet.add("four-a-week");
		iv_hyphenatedSet.add("five-a-week");
		iv_hyphenatedSet.add("six-a-week");
		iv_hyphenatedSet.add("seven-a-week");
		iv_hyphenatedSet.add("eight-a-week");
		iv_hyphenatedSet.add("nine-a-week");
		iv_hyphenatedSet.add("once-monthly");
		iv_hyphenatedSet.add("twice-monthly");
		iv_hyphenatedSet.add("one-a-month");
		iv_hyphenatedSet.add("two-a-month");
		iv_hyphenatedSet.add("three-a-month");
		iv_hyphenatedSet.add("four-a-month");
		iv_hyphenatedSet.add("five-a-month");
		iv_hyphenatedSet.add("six-a-month");
		iv_hyphenatedSet.add("seven-a-month");
		iv_hyphenatedSet.add("eight-a-month");
		iv_hyphenatedSet.add("nine-a-month");
		iv_hyphenatedSet.add("once-hourly");
		iv_hyphenatedSet.add("twice-hourly");
		iv_hyphenatedSet.add("one-an-hour");
		iv_hyphenatedSet.add("two-an-hour");
		iv_hyphenatedSet.add("three-an-hour");
		iv_hyphenatedSet.add("four-an-hour");
		iv_hyphenatedSet.add("five-an-hour");
		iv_hyphenatedSet.add("six-an-hour");
		iv_hyphenatedSet.add("seven-an-hour");
		iv_hyphenatedSet.add("eight-an-hour");
		iv_hyphenatedSet.add("nine-an-hour");

		iv_hyphenatedSet.add("once-nightly");
		iv_hyphenatedSet.add("as-needed");
		iv_hyphenatedSet.add("twice-nightly");
		iv_hyphenatedSet.add("once-every-day");
		iv_hyphenatedSet.add("once-daily");
		iv_hyphenatedSet.add("twice-daily");
		iv_hyphenatedSet.add("one-time-a-day");
		iv_hyphenatedSet.add("two-times-a-day");
		iv_hyphenatedSet.add("three-times-a-day");
		iv_hyphenatedSet.add("four-times-a-day");
		iv_hyphenatedSet.add("five-times-a-day");
		iv_hyphenatedSet.add("six-times-a-day");
		iv_hyphenatedSet.add("seven-times-a-day");
		iv_hyphenatedSet.add("eight-times-a-day");
		iv_hyphenatedSet.add("nine-times-a-day");
		iv_hyphenatedSet.add("once-every-week");
		iv_hyphenatedSet.add("twice-every-day");
		iv_hyphenatedSet.add("one-time-a-week");
		iv_hyphenatedSet.add("two-times-a-week");
		iv_hyphenatedSet.add("three-times-a-week");
		iv_hyphenatedSet.add("four-times-a-week");
		iv_hyphenatedSet.add("five-times-a-week");
		iv_hyphenatedSet.add("six-times-a-week");
		iv_hyphenatedSet.add("seven-times-a-week");
		iv_hyphenatedSet.add("eight-times-a-week");
		iv_hyphenatedSet.add("nine-times-a-week");
		iv_hyphenatedSet.add("once-every-hour");
		iv_hyphenatedSet.add("twice-every-hour");
		iv_hyphenatedSet.add("one-time-a-month");
		iv_hyphenatedSet.add("two-times-a-month");
		iv_hyphenatedSet.add("three-times-a-month");
		iv_hyphenatedSet.add("four-times-a-month");
		iv_hyphenatedSet.add("five-times-a-month");
		iv_hyphenatedSet.add("six-times-a-month");
		iv_hyphenatedSet.add("seven-times-a-month");
		iv_hyphenatedSet.add("eight-times-a-month");
		iv_hyphenatedSet.add("nine-times-a-month");
		iv_hyphenatedSet.add("one-time-each-hour");
		iv_hyphenatedSet.add("two-times-each-hour");
		iv_hyphenatedSet.add("three-times-each-hour");
		iv_hyphenatedSet.add("four-times-each-hour");
		iv_hyphenatedSet.add("five-times-each-hour");
		iv_hyphenatedSet.add("six-times-each-hour");
		iv_hyphenatedSet.add("seven-times-each-hour");
		iv_hyphenatedSet.add("eight-times-each-hour");
		iv_hyphenatedSet.add("nine-times-each-hour");

		iv_machineSet.add(getLatin3AbbreviationMachine());
		iv_machineSet.add(getLatin2AbbreviationMachine());
		iv_machineSet.add(getFrequencyMachine());

	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * 		<li>40mg/d</li>
	 * 		<li>32.1-47.3mg/wk</li>
	 * </ol>
	 * @return
	 */
	private Machine getLatin3AbbreviationMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State leftAbbreviateQState = new NamedState("LEFT_Q");
		State leftAbbreviateBState = new NamedState("LEFT_B");
		State leftAbbreviatePState = new NamedState("LEFT_P");
		State leftAbbreviateTState = new NamedState("LEFT_T");

		State middleAbbreviateQtoAState = new NamedState("MID_Q2A");
		State middleAbbreviateQtoDState = new NamedState("MID_Q2D");
		State middleAbbreviateQtoHState = new NamedState("MID_Q2H");
		State middleAbbreviateQtoIState = new NamedState("MID_Q2I");
		State middleAbbreviateQtoMState = new NamedState("MID_Q2M");
		State middleAbbreviateQtoOState = new NamedState("MID_Q2O");
		State middleAbbreviateQtoWState = new NamedState("MID_Q2W");
		State middleAbbreviateQtoPState = new NamedState("MID_Q2P");

		State middleAbbreviatePtoRState = new NamedState("MID_P2R");
		State middleAbbreviateTtoIState = new NamedState("MID_T2I");

		State middleAbbreviateBtoIState = new NamedState("MID_B2I");

		State rightAbbreviateQIDState = new NamedState("RIGHT_QID");
		State rightAbbreviateQADState = new NamedState("RIGHT_QAD");
		State rightAbbreviateQDSState = new NamedState("RIGHT_QDS");
		State rightAbbreviateQHSState = new NamedState("RIGHT_QHS");
		State rightAbbreviateQWKState = new NamedState("RIGHT_QWK");
		State rightAbbreviateQODState = new NamedState("RIGHT_QOD");
		State rightAbbreviateQAMState = new NamedState("RIGHT_QAM");
		State rightAbbreviateQPMState = new NamedState("RIGHT_QPM");
		State rightAbbreviateQMTState = new NamedState("RIGHT_QMT");
		State rightAbbreviateBIDState = new NamedState("RIGHT_BID");
		State rightAbbreviatePRNState = new NamedState("RIGHT_PRN");
		State rightAbbreviateTIDState = new NamedState("RIGHT_TID");

		State firstDotQState = new NamedState("FIRSTDOTQ");
		State firstDotBState = new NamedState("FIRSTDOTB");
		State firstDotPState = new NamedState("FIRSTDOTP");
		State firstDotTState = new NamedState("FIRSTDOTT");

		State secondDotQtoAState = new NamedState("SECONDDOTQ2A");
		State secondDotQtoDState = new NamedState("SECONDDOTQ2D");
		State secondDotQtoHState = new NamedState("SECONDDOTQ2H");
		State secondDotQtoIState = new NamedState("SECONDDOTQ2I");
		State secondDotQtoMState = new NamedState("SECONDDOTQ2M");
		State secondDotQtoOState = new NamedState("SECONDDOTQ2O");
		State secondDotQtoWState = new NamedState("SECONDDOTQ2W");
		State secondDotQtoPState = new NamedState("SECONDDOTQ2P");
		State secondDotBtoIState = new NamedState("SECONDDOTB2I");
		State secondDotPtoRState = new NamedState("SECONDDOTP2R");
		State secondDotTtoIState = new NamedState("SECONDDOTT2I");

		Condition firstDotConditionQ = new PunctuationValueCondition('.');
		Condition firstDotConditionB = new PunctuationValueCondition('.');
		Condition firstDotConditionP = new PunctuationValueCondition('.');
		Condition firstDotConditionT = new PunctuationValueCondition('.');
		Condition secondDotConditionQH = new PunctuationValueCondition('.');
		Condition secondDotConditionQI = new PunctuationValueCondition('.');
		Condition secondDotConditionQA = new PunctuationValueCondition('.');
		Condition secondDotConditionQD = new PunctuationValueCondition('.');
		Condition secondDotConditionQM = new PunctuationValueCondition('.');
		Condition secondDotConditionQO = new PunctuationValueCondition('.');
		Condition secondDotConditionQW = new PunctuationValueCondition('.');
		Condition secondDotConditionQP = new PunctuationValueCondition('.');
		Condition secondDotConditionBI = new PunctuationValueCondition('.');
		Condition secondDotConditionPR = new PunctuationValueCondition('.');
		Condition secondDotConditionTI = new PunctuationValueCondition('.');
		Condition thirdDotConditionQHS = new PunctuationValueCondition('.');
		Condition thirdDotConditionQAD = new PunctuationValueCondition('.');
		Condition thirdDotConditionQID = new PunctuationValueCondition('.');
		Condition thirdDotConditionQDS = new PunctuationValueCondition('.');
		Condition thirdDotConditionQMT = new PunctuationValueCondition('.');
		Condition thirdDotConditionQOD = new PunctuationValueCondition('.');
		Condition thirdDotConditionQWK = new PunctuationValueCondition('.');
		Condition thirdDotConditionQAM = new PunctuationValueCondition('.');
		Condition thirdDotConditionQPM = new PunctuationValueCondition('.');
		Condition thirdDotConditionBID = new PunctuationValueCondition('.');
		Condition thirdDotConditionPRN = new PunctuationValueCondition('.');
		Condition thirdDotConditionTID = new PunctuationValueCondition('.');

		startState.addTransition(new TextValueCondition("q", true),
				leftAbbreviateQState);
		startState.addTransition(new TextValueCondition("b", true),
				leftAbbreviateBState);
		startState.addTransition(new TextValueCondition("p", true),
				leftAbbreviatePState);
		startState.addTransition(new TextValueCondition("t", true),
				leftAbbreviateTState);
		startState.addTransition(new AnyCondition(), startState);

		leftAbbreviateQState.addTransition(firstDotConditionQ, firstDotQState);
		leftAbbreviateQState.addTransition(new AnyCondition(), startState);

		firstDotQState.addTransition(new TextValueCondition("a", true),
				middleAbbreviateQtoAState);
		firstDotQState.addTransition(new TextValueCondition("d", true),
				middleAbbreviateQtoDState);
		firstDotQState.addTransition(new TextValueCondition("h", true),
				middleAbbreviateQtoHState);
		firstDotQState.addTransition(new TextValueCondition("i", true),
				middleAbbreviateQtoIState);
		firstDotQState.addTransition(new TextValueCondition("m", true),
				middleAbbreviateQtoMState);
		firstDotQState.addTransition(new TextValueCondition("o", true),
				middleAbbreviateQtoOState);
		firstDotQState.addTransition(new TextValueCondition("w", true),
				middleAbbreviateQtoWState);
		firstDotQState.addTransition(new TextValueCondition("p", true),
				middleAbbreviateQtoPState);
		firstDotQState.addTransition(new AnyCondition(), startState);

		middleAbbreviateQtoAState.addTransition(secondDotConditionQA,
				secondDotQtoAState);
		middleAbbreviateQtoAState.addTransition(new AnyCondition(), startState);

		middleAbbreviateQtoDState.addTransition(secondDotConditionQD,
				secondDotQtoDState);
		middleAbbreviateQtoDState.addTransition(new AnyCondition(), startState);

		middleAbbreviateQtoHState.addTransition(secondDotConditionQH,
				secondDotQtoHState);
		middleAbbreviateQtoHState.addTransition(new AnyCondition(), startState);

		middleAbbreviateQtoIState.addTransition(secondDotConditionQI,
				secondDotQtoIState);
		middleAbbreviateQtoIState.addTransition(new AnyCondition(), startState);

		middleAbbreviateQtoMState.addTransition(secondDotConditionQM,
				secondDotQtoMState);
		middleAbbreviateQtoMState.addTransition(new AnyCondition(), startState);

		middleAbbreviateQtoOState.addTransition(secondDotConditionQO,
				secondDotQtoOState);
		middleAbbreviateQtoOState.addTransition(new AnyCondition(), startState);

		middleAbbreviateQtoWState.addTransition(secondDotConditionQW,
				secondDotQtoWState);
		middleAbbreviateQtoWState.addTransition(new AnyCondition(), startState);

		middleAbbreviateQtoPState.addTransition(secondDotConditionQP,
				secondDotQtoPState);
		middleAbbreviateQtoPState.addTransition(new AnyCondition(), startState);

		secondDotQtoAState.addTransition(new TextValueCondition("d", true),
				rightAbbreviateQADState);
		secondDotQtoAState.addTransition(new AnyCondition(), startState);

		secondDotQtoDState.addTransition(new TextValueCondition("s", true),
				rightAbbreviateQDSState);
		secondDotQtoDState.addTransition(new AnyCondition(), startState);

		secondDotQtoHState.addTransition(new TextValueCondition("s", true),
				rightAbbreviateQHSState);
		secondDotQtoHState.addTransition(new AnyCondition(), startState);

		secondDotQtoIState.addTransition(new TextValueCondition("d", true),
				rightAbbreviateQIDState);
		secondDotQtoIState.addTransition(new AnyCondition(), startState);

		secondDotQtoMState.addTransition(new TextValueCondition("t", true),
				rightAbbreviateQMTState);
		secondDotQtoMState.addTransition(new AnyCondition(), startState);

		secondDotQtoOState.addTransition(new TextValueCondition("d", true),
				rightAbbreviateQODState);
		secondDotQtoOState.addTransition(new AnyCondition(), startState);

		secondDotQtoWState.addTransition(new TextValueCondition("k", true),
				rightAbbreviateQWKState);
		secondDotQtoWState.addTransition(new AnyCondition(), startState);

		secondDotQtoAState.addTransition(new TextValueCondition("m", true),
				rightAbbreviateQAMState);
		secondDotQtoAState.addTransition(new AnyCondition(), startState);

		secondDotQtoPState.addTransition(new TextValueCondition("m", true),
				rightAbbreviateQPMState);
		secondDotQtoPState.addTransition(new AnyCondition(), startState);

		secondDotBtoIState.addTransition(new TextValueCondition("d", true),
				endState);
		secondDotBtoIState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQADState.addTransition(thirdDotConditionQAD, endState);
		rightAbbreviateQADState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQDSState.addTransition(thirdDotConditionQDS, endState);
		rightAbbreviateQDSState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQHSState.addTransition(thirdDotConditionQHS, endState);
		rightAbbreviateQHSState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQIDState.addTransition(thirdDotConditionQID, endState);
		rightAbbreviateQIDState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQMTState.addTransition(thirdDotConditionQMT, endState);
		rightAbbreviateQMTState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQODState.addTransition(thirdDotConditionQOD, endState);
		rightAbbreviateQODState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQWKState.addTransition(thirdDotConditionQWK, endState);
		rightAbbreviateQWKState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQAMState.addTransition(thirdDotConditionQAM, endState);
		rightAbbreviateQAMState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQPMState.addTransition(thirdDotConditionQPM, endState);
		rightAbbreviateQPMState.addTransition(new AnyCondition(), startState);

		leftAbbreviateBState.addTransition(firstDotConditionB, firstDotBState);
		leftAbbreviateBState.addTransition(new AnyCondition(), startState);

		firstDotBState.addTransition(new TextValueCondition("i", true),
				middleAbbreviateBtoIState);
		firstDotBState.addTransition(new AnyCondition(), startState);

		middleAbbreviateBtoIState.addTransition(secondDotConditionBI,
				secondDotBtoIState);
		middleAbbreviateBtoIState.addTransition(new AnyCondition(), startState);

		secondDotBtoIState.addTransition(new TextValueCondition("d", true),
				rightAbbreviateBIDState);
		secondDotBtoIState.addTransition(new AnyCondition(), startState);

		rightAbbreviateBIDState.addTransition(thirdDotConditionBID, endState);

		leftAbbreviatePState.addTransition(firstDotConditionP, firstDotPState);
		leftAbbreviatePState.addTransition(new AnyCondition(), startState);

		leftAbbreviateTState.addTransition(firstDotConditionT, firstDotTState);
		leftAbbreviateTState.addTransition(new AnyCondition(), startState);

		firstDotPState.addTransition(new TextValueCondition("r", true),
				middleAbbreviatePtoRState);
		firstDotPState.addTransition(new AnyCondition(), startState);

		firstDotTState.addTransition(new TextValueCondition("i", true),
				middleAbbreviateTtoIState);
		firstDotTState.addTransition(new AnyCondition(), startState);

		middleAbbreviatePtoRState.addTransition(secondDotConditionPR,
				secondDotPtoRState);
		middleAbbreviatePtoRState.addTransition(new AnyCondition(), startState);

		middleAbbreviateTtoIState.addTransition(secondDotConditionTI,
				secondDotTtoIState);
		middleAbbreviateTtoIState.addTransition(new AnyCondition(), startState);

		secondDotPtoRState.addTransition(new TextValueCondition("n", true),
				rightAbbreviatePRNState);
		secondDotPtoRState.addTransition(new AnyCondition(), startState);

		secondDotTtoIState.addTransition(new TextValueCondition("d", true),
				rightAbbreviateTIDState);
		secondDotTtoIState.addTransition(new AnyCondition(), startState);

		rightAbbreviatePRNState.addTransition(thirdDotConditionPRN, endState);
		rightAbbreviateTIDState.addTransition(thirdDotConditionTID, endState);

		endState.addTransition(new AnyCondition(), startState);
		return m;
	}

	/**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * 		<li>40mg/d</li>
	 * 		<li>32.1-47.3mg/wk</li>
	 * </ol>
	 * @return
	 */
	private Machine getLatin2AbbreviationMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State leftAbbreviateQState = new NamedState("LEFT_Q");
		State leftAbbreviateAState = new NamedState("LEFT_A");
		State leftAbbreviateOState = new NamedState("LEFT_O");
		State leftAbbreviateHState = new NamedState("LEFT_H");
		State leftAbbreviatePState = new NamedState("LEFT_P");

		State rightAbbreviateQDState = new NamedState("RIGHT_QD");
		State rightAbbreviateQHState = new NamedState("RIGHT_QH");
		State rightAbbreviateAMState = new NamedState("RIGHT_AM");
		State rightAbbreviateODState = new NamedState("RIGHT_OD");
		State rightAbbreviateHSState = new NamedState("RIGHT_HS");
		State rightAbbreviatePMState = new NamedState("RIGHT_PM");

		State firstDotQState = new NamedState("FIRSTDOTQ");
		State firstDotAState = new NamedState("FIRSTDOTA");
		State firstDotOState = new NamedState("FIRSTDOTO");
		State firstDotHState = new NamedState("FIRSTDOTH");
		State firstDotPState = new NamedState("FIRSTDOTP");

		Condition firstQDDotCondition = new PunctuationValueCondition('.');
		Condition secondQDDotCondition = new PunctuationValueCondition('.');
		Condition firstODDotCondition = new PunctuationValueCondition('.');
		Condition secondQHDotCondition = new PunctuationValueCondition('.');
		Condition secondODDotCondition = new PunctuationValueCondition('.');
		Condition firstAMDotCondition = new PunctuationValueCondition('.');
		Condition firstPMDotCondition = new PunctuationValueCondition('.');
		Condition secondAMDotCondition = new PunctuationValueCondition('.');
		Condition secondPMDotCondition = new PunctuationValueCondition('.');
		Condition firstHSDotCondition = new PunctuationValueCondition('.');
		Condition secondHSDotCondition = new PunctuationValueCondition('.');

		Condition soloCondition = new WordSetCondition(iv_singleWordSet, true);

		startState.addTransition(new TextValueCondition("q", true),
				leftAbbreviateQState);
		startState.addTransition(new TextValueCondition("a", true),
				leftAbbreviateAState);
		startState.addTransition(new TextValueCondition("o", true),
				leftAbbreviateOState);
		startState.addTransition(new TextValueCondition("h", true),
				leftAbbreviateHState);
		startState.addTransition(new TextValueCondition("p", true),
				leftAbbreviatePState);
		startState.addTransition(new AnyCondition(), startState);

		leftAbbreviateQState.addTransition(firstQDDotCondition, firstDotQState);

		leftAbbreviateQState.addTransition(new AnyCondition(), startState);

		leftAbbreviateAState.addTransition(firstAMDotCondition, firstDotAState);
		leftAbbreviateAState.addTransition(new AnyCondition(), startState);

		leftAbbreviateOState.addTransition(firstODDotCondition, firstDotOState);
		leftAbbreviateOState.addTransition(new AnyCondition(), startState);

		leftAbbreviateHState.addTransition(firstHSDotCondition, firstDotHState);
		leftAbbreviateHState.addTransition(new AnyCondition(), startState);

		leftAbbreviatePState.addTransition(firstPMDotCondition, firstDotPState);
		leftAbbreviatePState.addTransition(new AnyCondition(), startState);

		firstDotQState.addTransition(soloCondition, endState);
		firstDotQState.addTransition(new TextValueCondition("d", true),
				rightAbbreviateQDState);
		firstDotQState.addTransition(new TextValueCondition("h", true),
				rightAbbreviateQHState);
		firstDotQState.addTransition(new AnyCondition(), startState);

		firstDotAState.addTransition(new TextValueCondition("m", true),
				rightAbbreviateAMState);
		firstDotAState.addTransition(new AnyCondition(), startState);

		firstDotOState.addTransition(new TextValueCondition("d", true),
				rightAbbreviateODState);
		firstDotOState.addTransition(new AnyCondition(), startState);

		firstDotHState.addTransition(new TextValueCondition("s", true),
				rightAbbreviateHSState);
		firstDotHState.addTransition(new AnyCondition(), startState);

		firstDotPState.addTransition(new TextValueCondition("m", true),
				rightAbbreviatePMState);
		firstDotPState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQHState.addTransition(secondQHDotCondition, endState);
		rightAbbreviateQHState.addTransition(new AnyCondition(), startState);

		rightAbbreviateAMState.addTransition(secondAMDotCondition, endState);
		rightAbbreviateAMState.addTransition(new AnyCondition(), startState);

		rightAbbreviateODState.addTransition(secondODDotCondition, endState);
		rightAbbreviateODState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQDState.addTransition(secondQDDotCondition, endState);
		rightAbbreviateQDState.addTransition(new AnyCondition(), startState);

		rightAbbreviatePMState.addTransition(secondPMDotCondition, endState);
		rightAbbreviatePMState.addTransition(new AnyCondition(), startState);

		rightAbbreviateHSState.addTransition(secondHSDotCondition, endState);
		rightAbbreviateHSState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Gets a finite state machine that detects the following 
	 * ('once', 'twice', # or text#) a day/week/month/year:
	 * <ol>
	 * 		<li>once a day</li>
	 * 		<li>three times a day</li>
	 * 		<li>once-a-day</li>
	 * </ol>
	 * @return
	 */
	private Machine getFrequencyMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State leftAbbreviateState = new NamedState("LEFT_FREQ");

		State lastTextState = new NamedState("RIGHT_FREQ");
		State middleATextState = new NamedState("MID_TEXT");
		State firstDashState = new NamedState("FIRSTDASH");
		State secondDashState = new NamedState("SECONDDASH");

		Condition integerCondition = new IntegerCondition();
		Condition firstDashCondition = new PunctuationValueCondition('-');
		Condition secondDashCondition = new PunctuationValueCondition('-');

		Condition numericStartCondition = new WordSetCondition(iv_frequencySet,
				false);
		Condition hyphenatedCondition = new WordSetCondition(iv_hyphenatedSet,
				false);
		Condition firstMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition secondMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition thirdMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition fourthMiddleTextCondition = new WordSetCondition(
				iv_middleTermSet, true);
		Condition lastTextCondition = new WordSetCondition(iv_textSuffixSet,
				false);
		Condition firstTextCondition = new WordSetCondition(iv_textPrefixSet,
				false);
		Condition soloCondition = new WordSetCondition(iv_singleWordSet, true);
		Condition specificWordCondition = new WordSetCondition(
				iv_specifiedWordSet, false);
		Condition containsSoloTermCondition = new ContainsSetTextValueCondition(
				iv_singleWordSet, true);

		startState.addTransition(numericStartCondition, leftAbbreviateState);
		startState.addTransition(firstTextCondition, leftAbbreviateState);
		startState.addTransition(new TextValueCondition("a", true),
				leftAbbreviateState);
		startState.addTransition(integerCondition, leftAbbreviateState);
		startState.addTransition(hyphenatedCondition, endState);
		startState.addTransition(containsSoloTermCondition, endState);
		startState.addTransition(soloCondition, endState);

		startState.addTransition(new AnyCondition(), startState);

		leftAbbreviateState.addTransition(firstMiddleTextCondition,
				middleATextState);
		leftAbbreviateState.addTransition(firstDashCondition, firstDashState);
		leftAbbreviateState.addTransition(soloCondition, endState);
		leftAbbreviateState.addTransition(specificWordCondition, endState);
		leftAbbreviateState.addTransition(hyphenatedCondition, endState);
		leftAbbreviateState.addTransition(new AnyCondition(), startState);

		firstDashState
				.addTransition(thirdMiddleTextCondition, middleATextState);
		firstDashState.addTransition(new AnyCondition(), startState);

		middleATextState
				.addTransition(secondMiddleTextCondition, lastTextState);
		middleATextState.addTransition(secondDashCondition, secondDashState);
		middleATextState.addTransition(lastTextCondition, endState);
		middleATextState.addTransition(new AnyCondition(), startState);

		secondDashState.addTransition(fourthMiddleTextCondition, lastTextState);
		secondDashState.addTransition(lastTextCondition, endState);
		secondDashState.addTransition(new AnyCondition(), startState);

		lastTextState.addTransition(lastTextCondition, endState);
		lastTextState.addTransition(new AnyCondition(), startState);

		endState.addTransition(new AnyCondition(), startState);

		return m;
	}

	/**
	 * Executes the finite state machines.
	 * @param tokens
	 * @return Set SuffixFrequencyToken objects.
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
					SuffixFrequencyToken segmentToken = new SuffixFrequencyToken(
							startToken.getStartOffset(), endToken
									.getEndOffset());
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
					SuffixFrequencyToken fractionToken = new SuffixFrequencyToken(
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
