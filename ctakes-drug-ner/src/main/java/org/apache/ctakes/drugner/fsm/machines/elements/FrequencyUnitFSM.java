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

import org.apache.ctakes.core.fsm.condition.CombineCondition;
import org.apache.ctakes.core.fsm.condition.DisjoinCondition;
import org.apache.ctakes.core.fsm.condition.IntegerCondition;
import org.apache.ctakes.core.fsm.condition.IntegerRangeCondition;
import org.apache.ctakes.core.fsm.condition.IntegerValueCondition;
import org.apache.ctakes.core.fsm.condition.NegateCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.TextSetCondition;
import org.apache.ctakes.core.fsm.condition.TextValueCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.state.NonTerminalEndState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.elements.conditions.ContainsSetTextValueCondition;
import org.apache.ctakes.drugner.fsm.elements.conditions.TimeCondition;
import org.apache.ctakes.drugner.fsm.output.elements.FrequencyUnitToken;
import org.apache.ctakes.drugner.fsm.states.util.IndentStartState;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect frequency unit data in the given
 * input of tokens.
 * 
 * @author Mayo Clinic
 */
public class FrequencyUnitFSM {

	Set iv_textSuffixSet = new HashSet();
	
	Set iv_weeklySuffixSet = new HashSet();
	
	Set iv_hourlySuffixSet = new HashSet();
	
	Set iv_yearlySuffixSet = new HashSet();
	
	Set iv_monthlySuffixSet = new HashSet();
	
	Set iv_dailySuffixSet = new HashSet();

	Set iv_textPrefixSet = new HashSet();

	Set iv_periodSet = new HashSet();

	Set iv_hyphenatedSet = new HashSet();

	Set iv_singleWordSet = new HashSet();
	
	Set iv_prnWordSet = new HashSet();

	Set iv_postFourWordSet = new HashSet();
	
	Set iv_postSixWordSet = new HashSet();
	
	Set iv_postEightWordSet = new HashSet();
	
	Set iv_fourTimesPerDayWordSet = new HashSet();
	
	Set iv_fiveTimesPerDayWordSet = new HashSet();
	
	Set iv_sixTimesPerDayWordSet = new HashSet();
	
	Set iv_eightTimesPerDayWordSet = new HashSet();
	
	Set iv_perDayWordSet = new HashSet();
	
	Set iv_perWeekWordSet = new HashSet();

	Set iv_dailyWordSet = new HashSet();
	
	Set iv_everyOtherDayWordSet = new HashSet();
	
	Set iv_everyOtherHourWordSet = new HashSet();
	
	Set iv_threeTimesADayWordSet = new HashSet();
	
	Set iv_twiceADayWordSet = new HashSet();
	
	// contains the finite state machines
	private Set iv_machineSet = new HashSet();
	
    private Machine iv_DailyMachine;
    
    private Machine iv_HourlyMachine;
    
    private Machine iv_ThreeTimesADayMachine;
    
    private Machine iv_FourTimesADayMachine;
    
    private Machine iv_FiveTimesADayMachine;
    
    private Machine iv_SixTimesADayMachine;
    
    private Machine iv_EveryOtherHourMachine;
    
    private Machine iv_EveryOtherDayMachine;
    
    private Machine iv_TwiceADayMachine;
    
    private Machine iv_DailySuffixMachine;
    
    private Machine iv_HourlySuffixMachine;
    
    private Machine iv_WeeklySuffixMachine;
    
    private Machine iv_MonthlySuffixMachine;
    
    private Machine iv_YearlySuffixMachine;

    private Machine iv_WeeklyMachine;
    
	private Machine iv_PrnMachine;


	/**
	 * 
	 * Constructor
	 * 
	 */
	public FrequencyUnitFSM() {
		iv_dailyWordSet.add("noon");
		iv_dailyWordSet.add("lunch");
		iv_dailyWordSet.add("breakfast");
		iv_dailyWordSet.add("dinner");
		iv_dailyWordSet.add("morning");
		iv_dailyWordSet.add("afternoon");
		iv_dailyWordSet.add("evening");
		iv_dailyWordSet.add("am");
		iv_dailyWordSet.add("pm");
		iv_dailyWordSet.add("a.m.");// TokenizerPTB handles abbreviations as tokens now.
		iv_dailyWordSet.add("p.m.");
		iv_dailyWordSet.add("q.a.m.");
		iv_dailyWordSet.add("q.p.m.");
		iv_dailyWordSet.add("a.m");
		iv_dailyWordSet.add("p.m");
		iv_dailyWordSet.add("q.a.m");
		iv_dailyWordSet.add("q.p.m");
		iv_dailyWordSet.add("bedtime");
		iv_dailyWordSet.add("specified");
		iv_dailyWordSet.add("day");
		
		iv_perDayWordSet.add("day");
		iv_perDayWordSet.add("qday");
		iv_perDayWordSet.add("24-hours");
		iv_perDayWordSet.add("qd");
		iv_perDayWordSet.add("qn");
		iv_perDayWordSet.add("qhs");
		iv_perDayWordSet.add("daily");
		iv_perDayWordSet.add("nightly");
		iv_perDayWordSet.add("od");		
		iv_perDayWordSet.add("q.d.");// TokenizerPTB handles abbreviations as tokens now.
		iv_perDayWordSet.add("q.d");
		iv_perDayWordSet.add("q.n.");
		iv_perDayWordSet.add("q.n");
		iv_perDayWordSet.add("q.h.s.");
		iv_perDayWordSet.add("q.h.s");
		iv_perDayWordSet.add("q.day");
		iv_perDayWordSet.add("o.d.");	
		iv_perDayWordSet.add("o.d");
		
		iv_postEightWordSet.add("nine");
		iv_postEightWordSet.add("ten");
		
		iv_postFourWordSet.add("five");
		iv_postFourWordSet.add("six");
		iv_postFourWordSet.add("seven");
		iv_postFourWordSet.add("eight");
		iv_postFourWordSet.add("nine");

		iv_postSixWordSet.add("seven");
		iv_postSixWordSet.add("eight");
		iv_postSixWordSet.add("nine");

		iv_fourTimesPerDayWordSet.add("qid");
		iv_fourTimesPerDayWordSet.add("q.i.d.");
		iv_fourTimesPerDayWordSet.add("q.i.d");
		iv_fourTimesPerDayWordSet.add("q6");
		iv_fourTimesPerDayWordSet.add("q6h");
		iv_fourTimesPerDayWordSet.add("qds");
		iv_fourTimesPerDayWordSet.add("6h");
		iv_fourTimesPerDayWordSet.add("6hr");
		iv_fourTimesPerDayWordSet.add("6hrs");
		iv_fourTimesPerDayWordSet.add("q6-q8");
		iv_fourTimesPerDayWordSet.add("6-q8h");
		iv_fourTimesPerDayWordSet.add("q6-8");
		iv_fourTimesPerDayWordSet.add("q6-8h");
		iv_fourTimesPerDayWordSet.add("q5-q6");
		iv_fourTimesPerDayWordSet.add("5-q6h");
		iv_fourTimesPerDayWordSet.add("5-6h");
		iv_fourTimesPerDayWordSet.add("q5-6");
		iv_fourTimesPerDayWordSet.add("q5-6h");
		iv_fourTimesPerDayWordSet.add("q5-6h");
		iv_fourTimesPerDayWordSet.add("5-6");
		
		iv_perWeekWordSet.add("week");
		iv_perWeekWordSet.add("weekly");

		iv_twiceADayWordSet.add("q12h");
		iv_twiceADayWordSet.add("q12");
		iv_twiceADayWordSet.add("12h");
		iv_twiceADayWordSet.add("12hr");
		iv_twiceADayWordSet.add("12hrs");
		iv_twiceADayWordSet.add("q10-12h");
		iv_twiceADayWordSet.add("q10-12");
		iv_twiceADayWordSet.add("q10h-12h");	
		iv_twiceADayWordSet.add("10-12h");
		iv_twiceADayWordSet.add("10-12");
		iv_twiceADayWordSet.add("10h-12h");
		iv_twiceADayWordSet.add("bid");
		iv_twiceADayWordSet.add("b.i.d.");
		iv_twiceADayWordSet.add("b.i.d");
		
		iv_threeTimesADayWordSet.add("q8-12h");
		iv_threeTimesADayWordSet.add("q8-12");
		iv_threeTimesADayWordSet.add("q8h-12h");	
		iv_threeTimesADayWordSet.add("8-12h");
		iv_threeTimesADayWordSet.add("8-12");
		iv_threeTimesADayWordSet.add("8h-12h");
		iv_threeTimesADayWordSet.add("tid");
		iv_threeTimesADayWordSet.add("t.i.d.");
		iv_threeTimesADayWordSet.add("t.i.d");
		iv_threeTimesADayWordSet.add("q8");
		iv_threeTimesADayWordSet.add("q8h");
		iv_threeTimesADayWordSet.add("8h");
		iv_threeTimesADayWordSet.add("8hr");
		iv_threeTimesADayWordSet.add("8hrs");

		iv_threeTimesADayWordSet.add("q4-8h");
		iv_threeTimesADayWordSet.add("q7-8h");
		
		iv_sixTimesPerDayWordSet.add("q4-q6");
		iv_sixTimesPerDayWordSet.add("4-q6h");
		iv_sixTimesPerDayWordSet.add("4-q6hr");
		iv_sixTimesPerDayWordSet.add("4-q6hrs");
		iv_sixTimesPerDayWordSet.add("4-6h");
		iv_sixTimesPerDayWordSet.add("q4-6");
		iv_sixTimesPerDayWordSet.add("q4-6h");
		iv_sixTimesPerDayWordSet.add("q4-6hr");
		iv_sixTimesPerDayWordSet.add("q4-6hrs");
		iv_sixTimesPerDayWordSet.add("q4-6h");
		iv_sixTimesPerDayWordSet.add("q4");
		iv_sixTimesPerDayWordSet.add("q4h");
		iv_sixTimesPerDayWordSet.add("4h");
		iv_sixTimesPerDayWordSet.add("4hr");
		iv_sixTimesPerDayWordSet.add("4hrs");
		
		iv_eightTimesPerDayWordSet.add("q3-q4");
		iv_eightTimesPerDayWordSet.add("3-q4h");
		iv_eightTimesPerDayWordSet.add("3-4h");
		iv_eightTimesPerDayWordSet.add("q3-4");
		iv_eightTimesPerDayWordSet.add("q3-4h");
		iv_eightTimesPerDayWordSet.add("q3-4h");
		
		iv_everyOtherHourWordSet.add("q2-q4");
		iv_everyOtherHourWordSet.add("2-4h");
		iv_everyOtherHourWordSet.add("q2-4hr");
		iv_everyOtherHourWordSet.add("q2-4hrs");
		iv_everyOtherHourWordSet.add("2-q4h");
		iv_everyOtherHourWordSet.add("2-4h");
		iv_everyOtherHourWordSet.add("q2-4");
		iv_everyOtherHourWordSet.add("q2-4h");
		iv_everyOtherHourWordSet.add("q2-4h");
		
		iv_dailySuffixSet.add("d");
		iv_dailySuffixSet.add("day");
		iv_dailySuffixSet.add("daily");
		
		iv_weeklySuffixSet.add("wk");
		iv_weeklySuffixSet.add("week");
		iv_weeklySuffixSet.add("weekly");
		iv_weeklySuffixSet.add("weeks");
		
		iv_yearlySuffixSet.add("y");
		iv_yearlySuffixSet.add("year");
		iv_yearlySuffixSet.add("yr");

		iv_hourlySuffixSet.add("h");
		iv_hourlySuffixSet.add("hr");
		iv_hourlySuffixSet.add("hrs");
		iv_hourlySuffixSet.add("hour");
		iv_hourlySuffixSet.add("hours");
		
		iv_monthlySuffixSet.add("month");
		iv_monthlySuffixSet.add("months");
		iv_monthlySuffixSet.add("m");
		iv_monthlySuffixSet.add("mo");
		
		iv_textSuffixSet.add("min");

		iv_singleWordSet.add("hs");
		iv_singleWordSet.add("h.s.");
		iv_singleWordSet.add("q");
		iv_singleWordSet.add("monthly");
		iv_singleWordSet.add("biweekly");
		
		iv_prnWordSet.add("prn");
		iv_prnWordSet.add("p.r.n.");
		iv_prnWordSet.add("p.r.n");
		iv_prnWordSet.add("as-needed");
		iv_prnWordSet.add("asneeded");
		
		iv_textPrefixSet.add("every");

		iv_everyOtherDayWordSet.add("every-other-day");
		iv_everyOtherDayWordSet.add("every-other-night");
		iv_everyOtherDayWordSet.add("every-other-evening");
		iv_everyOtherDayWordSet.add("every-other-morning");

		// 2 letter latin abbreviations
		iv_DailyMachine = getDailyMachine();
		iv_SixTimesADayMachine = getSixTimesADayMachine();
		iv_FiveTimesADayMachine = getFiveTimesADayMachine();
        iv_ThreeTimesADayMachine = getThreeTimesADayMachine();
        iv_FourTimesADayMachine = getFourTimesADayMachine();
        iv_EveryOtherHourMachine = getEveryOtherHourMachine();
        iv_EveryOtherDayMachine = getEveryOtherDayMachine();
        iv_TwiceADayMachine = getTwiceADayMachine();
        iv_DailySuffixMachine = getDailySuffixMachine();
        iv_WeeklyMachine = getWeeklyMachine();
        iv_HourlySuffixMachine = getHourlySuffixMachine();
        iv_WeeklySuffixMachine = getWeeklySuffixMachine();
        iv_MonthlySuffixMachine = getMonthlySuffixMachine();
        iv_YearlySuffixMachine = getYearlySuffixMachine();
        iv_PrnMachine = getAsNeededMachine();
        
		iv_machineSet.add(iv_DailyMachine);
		iv_machineSet.add(iv_SixTimesADayMachine);
		iv_machineSet.add(iv_FiveTimesADayMachine);
		iv_machineSet.add(iv_HourlySuffixMachine);
		iv_machineSet.add(iv_WeeklySuffixMachine);
		iv_machineSet.add(iv_MonthlySuffixMachine);
		iv_machineSet.add(iv_DailySuffixMachine);
		iv_machineSet.add(iv_YearlySuffixMachine);
		iv_machineSet.add(iv_ThreeTimesADayMachine);
		iv_machineSet.add(iv_FourTimesADayMachine);
		iv_machineSet.add(iv_TwiceADayMachine);
		iv_machineSet.add(iv_EveryOtherHourMachine);
		iv_machineSet.add(iv_EveryOtherDayMachine);
		iv_machineSet.add(iv_WeeklyMachine);
		iv_machineSet.add(iv_PrnMachine);
		


	}
/**
		 * Gets a finite state machine that detects per week via the following:
		 * <ol>
		 * <li>q.w.k.</li>
		 * </ol>
		 * 
		 * @return
		 */
		private Machine getWeeklyMachine() {
			State startState = new NamedState("START");
			State endState = new NamedState("END");
			State ntEndState = new NonTerminalEndState("NON TERMINAL END");
			ntEndState.setEndStateFlag(true);
			endState.setEndStateFlag(true);
			State leftAbbreviateState = new NamedState("LEFT_FREQ");
			
			Machine m = new Machine(startState);
	
			State leftAbbreviateQState = new NamedState("LEFT_Q");

			State middleAbbreviateQtoWState = new NamedState("MID_Q2W");
	
	
			State rightAbbreviateQWKState = new NamedState("RIGHT_QWK");
	
		
			State firstDotQState = new NamedState("FIRSTDOTQ");

	
			State secondDotQtoWState = new NamedState("SECONDDOTQ2W");
	
	
			Condition firstDotConditionQ = new PunctuationValueCondition('.');
			Condition secondDotConditionQW = new PunctuationValueCondition('.');
			Condition thirdDotConditionQWK = new PunctuationValueCondition('.');

			Condition soloCondition = new TextSetCondition(iv_perWeekWordSet, false);
	
	
			startState.addTransition(new TextValueCondition("q", false),
					leftAbbreviateQState); 
			startState.addTransition(new TextValueCondition("a", false),
					leftAbbreviateState);
			startState.addTransition(new TextValueCondition("at", false),
					leftAbbreviateState);
			startState.addTransition(new TextValueCondition("per", false),
					leftAbbreviateState);
	
			startState.addTransition(soloCondition, endState);
			startState.addTransition(new AnyCondition(), startState);
	
			leftAbbreviateQState.addTransition(firstDotConditionQ, firstDotQState);
			leftAbbreviateQState.addTransition(new AnyCondition(), startState);
	
			
			leftAbbreviateState.addTransition(new ContainsSetTextValueCondition(
					iv_perWeekWordSet, false), endState);
			leftAbbreviateState.addTransition(new AnyCondition(), startState);
	
			firstDotQState.addTransition(new TextValueCondition("w", false),
					middleAbbreviateQtoWState);

			firstDotQState.addTransition(new AnyCondition(), startState);
	
	
	
			middleAbbreviateQtoWState.addTransition(secondDotConditionQW,
					secondDotQtoWState);
			middleAbbreviateQtoWState.addTransition(new AnyCondition(), startState);
	
		
			secondDotQtoWState.addTransition(new TextValueCondition("k", false),
					rightAbbreviateQWKState);
			secondDotQtoWState.addTransition(new AnyCondition(), startState);
	
		
			rightAbbreviateQWKState.addTransition(thirdDotConditionQWK, endState);
		    rightAbbreviateQWKState.addTransition(new AnyCondition(), startState);
		//	rightAbbreviateQWKState.addTransition(new AnyCondition(), startState);
	
			ntEndState.addTransition(new AnyCondition(),  startState);
			endState.addTransition(new AnyCondition(), startState);
			return m;
		}
	/**
	 * Gets a finite state machine that detects three times a day as in the following:
	 * <ol>
	 * <li>t.i.d.</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getThreeTimesADayMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);
		State hyphState = new NamedState("HYPHSTATE");
		State rangeState = new NamedState("RANGESTAGE");
		State handleRangeState = new NamedState("HANDLERANGE");
		State leftAbbreviateTState = new NamedState("LEFT_T");
		State middleAbbreviateTtoIState = new NamedState("MID_T2I");
		State rightAbbreviateTIDState = new NamedState("RIGHT_TID");
		State firstDotTState = new NamedState("FIRSTDOTT");
		State secondDotTtoIState = new NamedState("SECONDDOTT2I");
		State eightHourState = new NamedState("EIGHTHOUR");

		Condition firstDotConditionT = new PunctuationValueCondition('.');
		Condition secondDotConditionTI = new PunctuationValueCondition('.');
		Condition thirdDotConditionTID = new PunctuationValueCondition('.');
		
		Condition TTDCondition = new TextSetCondition(iv_threeTimesADayWordSet, false);
	
		startState.addTransition(new TextValueCondition("t", false),
				leftAbbreviateTState);
		startState.addTransition(new TextValueCondition("q", false),
				eightHourState);
		startState.addTransition(new IntegerValueCondition(8), eightHourState);
		startState.addTransition(TTDCondition, endState);
		startState.addTransition(new DisjoinCondition(new CombineCondition(new NegateCondition(new IntegerRangeCondition(1,7)), new IntegerCondition()), new IntegerValueCondition(8)), handleRangeState);
		startState.addTransition(new DisjoinCondition(new TextSetCondition(iv_postEightWordSet, false), new TextValueCondition("eight", false)), eightHourState);
	
		startState.addTransition(new AnyCondition(), startState);

		handleRangeState.addTransition(new PunctuationValueCondition('-'), hyphState);
		handleRangeState.addTransition(new AnyCondition(), startState);
		
		eightHourState.addTransition(new PunctuationValueCondition('-'), hyphState);
		eightHourState.addTransition(new TextValueCondition("to", false), hyphState);
		eightHourState.addTransition(new TextSetCondition(iv_hourlySuffixSet, false), endState);
		eightHourState.addTransition(new AnyCondition(), startState);

		leftAbbreviateTState.addTransition(firstDotConditionT, firstDotTState);
		leftAbbreviateTState.addTransition(new AnyCondition(), startState);

		hyphState.addTransition(new IntegerRangeCondition(9,10), rangeState);
		hyphState.addTransition(new TextValueCondition("ten", false), rangeState);
		hyphState.addTransition(new TextValueCondition("nine", false), rangeState);
		hyphState.addTransition(new AnyCondition(), startState);

		firstDotTState.addTransition(new TextValueCondition("i", false),
				middleAbbreviateTtoIState);
		firstDotTState.addTransition(new AnyCondition(), startState);

		rangeState.addTransition(new TextSetCondition(iv_hourlySuffixSet, false), endState);
		rangeState.addTransition(new AnyCondition(), startState);

		middleAbbreviateTtoIState.addTransition(secondDotConditionTI,
				secondDotTtoIState);
		middleAbbreviateTtoIState.addTransition(new AnyCondition(), startState);


		secondDotTtoIState.addTransition(new TextValueCondition("d", false),
				rightAbbreviateTIDState);
		secondDotTtoIState.addTransition(new AnyCondition(), startState);


		rightAbbreviateTIDState.addTransition(thirdDotConditionTID, endState);
		rightAbbreviateTIDState.addTransition(new AnyCondition(), startState);
		
		ntEndState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		return m;
	}
	/**
	 * Gets a finite state machine that detects twice a day as in the following:
	 * <ol>
	 * <li>b.i.d.</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getTwiceADayMachine() {
		State startState = new NamedState("START");
		State endState = new NamedState("END");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);

		Machine m = new Machine(startState);

		State twelveHourState = new NamedState("TWELVEHOUR");
		State leftAbbreviateBState = new NamedState("LEFT_B");
		State middleAbbreviateBtoIState = new NamedState("MID_B2I");
		State rightAbbreviateBIDState = new NamedState("RIGHT_BID");
		State firstDotBState = new NamedState("FIRSTDOTB");
		State secondDotBtoIState = new NamedState("SECONDDOTB2I");
		Condition TDCondition = new TextSetCondition(iv_twiceADayWordSet, false);
		
		startState.addTransition(new TextValueCondition("b", false),
				leftAbbreviateBState);
		startState.addTransition(new TextValueCondition("q", false), twelveHourState);
		startState.addTransition(new IntegerValueCondition(12), twelveHourState);
		startState.addTransition(new TextValueCondition("twelve", false), twelveHourState);
		startState.addTransition(TDCondition, endState);
		startState.addTransition(new AnyCondition(), startState);

        twelveHourState.addTransition(new TextSetCondition(iv_hourlySuffixSet, false), endState);
        twelveHourState.addTransition(new TextSetCondition(iv_twiceADayWordSet, false), endState);
       // twelveHourState.addTransition(new RangeStrengthCondition(), endState);
        twelveHourState.addTransition(new AnyCondition(), startState);
        
		leftAbbreviateBState.addTransition(new PunctuationValueCondition('.'), firstDotBState);
		leftAbbreviateBState.addTransition(new AnyCondition(), startState);

		firstDotBState.addTransition(new TextValueCondition("i", false),
				middleAbbreviateBtoIState);
		firstDotBState.addTransition(new AnyCondition(), startState);

		middleAbbreviateBtoIState.addTransition(new PunctuationValueCondition('.'),
				secondDotBtoIState);
		middleAbbreviateBtoIState.addTransition(new AnyCondition(), startState);
		
		secondDotBtoIState.addTransition(new TextValueCondition("d", false),
				rightAbbreviateBIDState);
		secondDotBtoIState.addTransition(new AnyCondition(), startState);


		rightAbbreviateBIDState.addTransition(new PunctuationValueCondition('.'), endState);
		rightAbbreviateBIDState.addTransition(new AnyCondition (), startState);
		
		ntEndState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		return m;
	}
	
	/**
	 * Gets a fsm that detects every other day
	 * <ol>
	 * <li>q.a.d.</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getEveryOtherDayMachine() {
		State startState = new NamedState("START_EODM");
		State endState = new NamedState("END_EODM");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
		State leftAbbreviateQState = new NamedState("LEFT_Q_EODM");
		State middleAbbreviateQtoAState = new NamedState("MID_Q2A_EODM");
		State rightAbbreviateQADState = new NamedState("RIGHT_QAD_EODM");
		State secondDotQtoAState = new NamedState("SECONDDOTQ2A_EODM");
		State firstDotQState = new NamedState("FIRSTDOTQ_EODM");
		State EODState = new NamedState("FIRSTDOTQ_EODM");
		
		Condition EODCondition = new TextSetCondition(iv_everyOtherDayWordSet, false);
		
		startState.addTransition(new TextValueCondition("q", false),
				leftAbbreviateQState);
		startState.addTransition(EODCondition, endState);
		startState.addTransition(new TextValueCondition("every-other", false), EODState);
		startState.addTransition(new AnyCondition(), startState);
		
		leftAbbreviateQState.addTransition(new PunctuationValueCondition('.'),
				firstDotQState);
	//leftAbbreviateQState.addTransition(new RangeStrengthCondition(), endState);
		leftAbbreviateQState.addTransition(new AnyCondition(), startState);
		
		firstDotQState.addTransition(new TextValueCondition("a", false),
				middleAbbreviateQtoAState);
		firstDotQState.addTransition(new AnyCondition(), startState);
		
		middleAbbreviateQtoAState.addTransition(new PunctuationValueCondition('.'),
				secondDotQtoAState);
		middleAbbreviateQtoAState.addTransition(new AnyCondition(), startState);
		
		secondDotQtoAState.addTransition(new TextValueCondition("d", false),
				rightAbbreviateQADState);
		secondDotQtoAState.addTransition(new AnyCondition(), startState);
		
		EODState.addTransition(new TextSetCondition(iv_dailyWordSet, false), endState);
		EODState.addTransition(new AnyCondition(), startState);
		
		rightAbbreviateQADState.addTransition(new PunctuationValueCondition('.'), endState);
		rightAbbreviateQADState.addTransition(new AnyCondition(), startState);
		
		
		ntEndState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		
		return m;
	
		
	}
	/**
	 * Gets a fsm that detects every other hour
	 * <ol>
	 * <li>q.o.d.</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getEveryOtherHourMachine() {
		State startState = new NamedState("START_EOHM");
		State endState = new NamedState("END_EOHM");
		State leftAbbreviateQState = new NamedState("LEFT_Q_EOHM");
		State middleAbbreviateQtoOState = new NamedState("MID_Q2O");
		State rightAbbreviateQODState = new NamedState("RIGHT_QOD");
		State firstDotQState = new NamedState("FIRSTDOTQ_EOHM");
		State secondDotQtoOState = new NamedState("SECONDDOTQ2O");

		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
		
		startState.addTransition(new TextValueCondition("q", false),
				leftAbbreviateQState);
	    startState.addTransition(new TextSetCondition(iv_everyOtherHourWordSet, false), endState);
		startState.addTransition(new AnyCondition(), startState);
		
		leftAbbreviateQState.addTransition(new PunctuationValueCondition('.'), firstDotQState);
		leftAbbreviateQState.addTransition(new AnyCondition(), startState);
		
		firstDotQState.addTransition(new TextValueCondition("o", false),
				middleAbbreviateQtoOState);
	    firstDotQState.addTransition(new AnyCondition(),  startState);
	    
		middleAbbreviateQtoOState.addTransition(new PunctuationValueCondition('.'),
				secondDotQtoOState);
		middleAbbreviateQtoOState.addTransition(new AnyCondition(), startState);
		
		secondDotQtoOState.addTransition(new TextValueCondition("d", false),
				rightAbbreviateQODState);
		secondDotQtoOState.addTransition(new AnyCondition(), startState);
		
		rightAbbreviateQODState.addTransition(new PunctuationValueCondition('.'), endState);
		rightAbbreviateQODState.addTransition(new AnyCondition(), startState);
		
	//	ntEndState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		
		return m;
	
		
	}
	/**
	 * Gets a fsm that detects every other hour
	 * <ol>
	 * <li>prn.</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getAsNeededMachine() {
		State startState = new NamedState("START_PRN");
		State endState = new NamedState("END_PRN");
		State asNeededState = new NamedState("PRN");
		State asNeededHyphState = new NamedState("HYPHPRN");
        State startPState = new NamedState("PSTATE");
        State startPDOTState = new NamedState("PDOTSTATE");
        State startRState = new NamedState("RSTATE");
        State startRDOTState = new NamedState("RDOTSTATE");
        State startNState = new NamedState("NSTATE");

        State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
	
		Machine m = new Machine(startState);
		
		startState.addTransition(new TextValueCondition("as", false),
				asNeededState);
	    startState.addTransition(new TextSetCondition(iv_prnWordSet, false), endState);
	    startState.addTransition(new TextValueCondition("p", false), startPState);
		startState.addTransition(new AnyCondition(), startState);
		
		startPState.addTransition(new PunctuationValueCondition('.'), startPDOTState);
		startPState.addTransition(new AnyCondition(), startState);
		
		startPDOTState.addTransition(new TextValueCondition("r", false), startRState);
		startPDOTState.addTransition(new AnyCondition(), startState);
			
		startRState.addTransition(new PunctuationValueCondition('.'), startRDOTState);
		startRState.addTransition(new AnyCondition(), startState);
	
		startRDOTState.addTransition(new TextValueCondition("n", false), startNState);
		startRDOTState.addTransition(new AnyCondition(), startState);
		
		startNState.addTransition(new PunctuationValueCondition('.'), endState);
		startNState.addTransition(new AnyCondition(), startState);
		
		
		asNeededState.addTransition(new TextValueCondition("needed", false), endState);
		asNeededState.addTransition(new PunctuationValueCondition('-'), asNeededHyphState);
		asNeededState.addTransition(new AnyCondition(), startState);
		
		asNeededHyphState.addTransition(new TextValueCondition("needed", false), endState);
		asNeededHyphState.addTransition(new AnyCondition(), startState);
		
		endState.addTransition(new AnyCondition(), startState);
		
		return m;
	
		
	}
	/**
	 * Gets a fsm that detects four times a day
	 * <ol>
	 * <li>q.i.d.</li>
	 * <li>6-8 hours</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getFourTimesADayMachine() {
		State startState = new NamedState("START_4TDM");
		State endState = new NamedState("END_4TDM");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
		State leftAbbreviateQState = new NamedState("LEFT_Q_FTD");
		State middleAbbreviateQtoIState = new NamedState("MID_Q2I");
		State middleAbbreviateQtoDState = new NamedState("MID_Q2D");
		State rightAbbreviateQDSState = new NamedState("RIGHT_QDS");
		State rightAbbreviateQIDState = new NamedState("RIGHT_QID");
		State handleRangeState = new NamedState("HANDLERANGE");
		State sixHourState = new NamedState("SIXHOUR");
		State rangeHourState = new NamedState("RANGEHOUR");
		State eightSuffixState = new NamedState("EIGHTSUFFIX");
		State firstDotQState = new NamedState("FIRSTDOTQ");
		State secondDotQtoDState = new NamedState("SECONDDOTQ2D");
		State secondDotQtoIState = new NamedState("SECONDDOTQ2I");
		Machine m = new Machine(startState);
		
		startState.addTransition(new TextValueCondition("q", false),
				leftAbbreviateQState);
		startState.addTransition(new TextSetCondition(
				iv_fourTimesPerDayWordSet, false) , endState);
		startState.addTransition(new IntegerValueCondition(6), sixHourState);
		startState.addTransition(new DisjoinCondition(new CombineCondition(new NegateCondition(new IntegerRangeCondition(1,5)), new IntegerCondition()), new IntegerValueCondition(6)), handleRangeState);
		startState.addTransition(new DisjoinCondition(new TextSetCondition(iv_postSixWordSet, false), new TextValueCondition("six", false)), sixHourState);

		startState.addTransition(new AnyCondition(), startState);
		
		handleRangeState.addTransition(new PunctuationValueCondition('-'), rangeHourState);
		handleRangeState.addTransition(new AnyCondition(), startState);
		
	    leftAbbreviateQState.addTransition(new PunctuationValueCondition('.'), firstDotQState);
	    leftAbbreviateQState.addTransition(new TextValueCondition("six", false), sixHourState);
	    leftAbbreviateQState.addTransition(new IntegerValueCondition(6), sixHourState);

	    leftAbbreviateQState.addTransition(new TextSetCondition(
				iv_fourTimesPerDayWordSet, false) , sixHourState);
	    leftAbbreviateQState.addTransition(new AnyCondition(), startState);
	    
	    
	    rangeHourState.addTransition(new IntegerRangeCondition(7,10), eightSuffixState);
	    rangeHourState.addTransition(new TextValueCondition("seven", false), eightSuffixState);
	    rangeHourState.addTransition(new TextValueCondition("eight", false), eightSuffixState);
	    rangeHourState.addTransition(new TextValueCondition("nine", false), eightSuffixState);
	    rangeHourState.addTransition(new TextValueCondition("ten", false), eightSuffixState);
	    rangeHourState.addTransition(new AnyCondition(), startState);
	    
	    eightSuffixState.addTransition(new TextSetCondition(iv_hourlySuffixSet, false), endState);
	    eightSuffixState.addTransition(new AnyCondition(), startState);
		
		
		sixHourState.addTransition(new PunctuationValueCondition('-'), rangeHourState);
		sixHourState.addTransition(new TextValueCondition("to", false), rangeHourState);
		sixHourState.addTransition(new TextSetCondition(iv_hourlySuffixSet, false), endState);
		sixHourState.addTransition(new TextSetCondition(iv_fourTimesPerDayWordSet, false), endState);
		sixHourState.addTransition(new AnyCondition(), startState);
			
		firstDotQState.addTransition(new TextValueCondition("d", false),
				middleAbbreviateQtoDState);
		firstDotQState.addTransition(new TextValueCondition("i", false),
				middleAbbreviateQtoIState);
	    firstDotQState.addTransition(new AnyCondition(), startState);
	    
		middleAbbreviateQtoDState.addTransition(new PunctuationValueCondition('.'),
				secondDotQtoDState);
		middleAbbreviateQtoDState.addTransition(new AnyCondition(), startState);
		
		        
		secondDotQtoDState.addTransition(new TextValueCondition("s", false),
				rightAbbreviateQDSState);
		secondDotQtoDState.addTransition(new AnyCondition(), startState);
	
		secondDotQtoIState.addTransition(new TextValueCondition("d", false),
				rightAbbreviateQIDState);
		secondDotQtoIState.addTransition(new AnyCondition(), startState);
		
		middleAbbreviateQtoIState.addTransition(new PunctuationValueCondition('.'),
				secondDotQtoIState);
		middleAbbreviateQtoIState.addTransition(new AnyCondition(), startState);
	
		rightAbbreviateQDSState.addTransition(new PunctuationValueCondition('.'), endState);
		rightAbbreviateQDSState.addTransition(new AnyCondition(), startState);
		
		rightAbbreviateQIDState.addTransition(new PunctuationValueCondition('.'), endState);
		rightAbbreviateQIDState.addTransition(new AnyCondition(), startState);
		
		ntEndState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		
		return m;
	
		
	}
	/**
	 * Gets a fsm that detects four times a day
 	 * <ol>
	 * <li>four hours</li>
	 * <li>6-8 hours</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getSixTimesADayMachine() {
		State startState = new NamedState("START_6TDM");
		State endState = new NamedState("END_6TDM");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		State hyphState = new NamedState("HYPHSTATE");
		State handleRangeState = new NamedState("HANDLERANGE");
		State numState = new NamedState("NUMSTATE");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);

		
		State middleAbbreviateQtoDState = new NamedState("MID_Q2D");
		State rightAbbreviateQDSState = new NamedState("RIGHT_QDS");

		State fourHourState = new NamedState("FOURHOUR");
		State firstDotQState = new NamedState("FIRSTDOTQ");
		State secondDotQtoDState = new NamedState("SECONDDOTQ2D");

		Machine m = new Machine(startState);

		startState.addTransition(new TextSetCondition(
				iv_sixTimesPerDayWordSet, false) , endState);
		startState.addTransition(new IntegerValueCondition(4), fourHourState);
		startState.addTransition(new DisjoinCondition(new CombineCondition(new NegateCondition(new IntegerRangeCondition(1,3)), new IntegerCondition()), new IntegerValueCondition(4)), handleRangeState);
		startState.addTransition(new DisjoinCondition(new TextSetCondition(iv_postFourWordSet, false), new TextValueCondition("four", false)), fourHourState);
		
		startState.addTransition(new AnyCondition(), startState);
		
		handleRangeState.addTransition(new PunctuationValueCondition('-'), hyphState);
		handleRangeState.addTransition(new AnyCondition(), startState);
		
		fourHourState.addTransition(new PunctuationValueCondition('-'), hyphState);
		fourHourState.addTransition(new TextValueCondition("to", false), hyphState);
		//fourHourState.addTransition(new TextSetCondition(iv_hourlySuffixSet, false), endState);
		
		fourHourState.addTransition(new AnyCondition(), startState);
		
		firstDotQState.addTransition(new TextValueCondition("d", false),
				middleAbbreviateQtoDState);

        firstDotQState.addTransition(new AnyCondition(), startState);
        
        hyphState.addTransition(new IntegerRangeCondition(5,8), numState);
        hyphState.addTransition(new TextValueCondition("five", false), numState);
        hyphState.addTransition(new TextValueCondition("six", false), numState);
        hyphState.addTransition(new TextValueCondition("seven", false), numState);
        hyphState.addTransition(new TextValueCondition("eight", false), numState);
        hyphState.addTransition(new AnyCondition(), startState);
        
        numState.addTransition(new TextSetCondition(iv_hourlySuffixSet, false), endState);
        numState.addTransition(new AnyCondition(), startState);
        
		middleAbbreviateQtoDState.addTransition(new PunctuationValueCondition('.'),
				secondDotQtoDState);
		middleAbbreviateQtoDState.addTransition(new AnyCondition(), startState);
		
		        
		secondDotQtoDState.addTransition(new TextValueCondition("s", false),
				rightAbbreviateQDSState);
		secondDotQtoDState.addTransition(new AnyCondition(), startState);

		rightAbbreviateQDSState.addTransition(new PunctuationValueCondition('.'), endState);
		rightAbbreviateQDSState.addTransition(new AnyCondition(), startState);
		
		
		ntEndState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		
		return m;

		
	}

	/**
		 * Gets a finite state machine that detects the following which indicates 1 per day:
		 * <ol>
		 * <li>q.d.</li>
		 * <li>o.d.</li>
		 * <li>h.s.</li>
		 * <li>p.m.</li>
		 * </ol>
		 * 
		 * @return
		 */
		private Machine getDailyMachine() {
			State startState = new NamedState("START");
			State endState = new NamedState("END");
			
			State ntEndState = new NonTerminalEndState("NON TERMINAL END");
			ntEndState.setEndStateFlag(true);
			endState.setEndStateFlag(true);
			Machine m = new Machine(startState);

			State leftAbbreviateState = new NamedState("LEFT_FREQ");

			State clockState = new NamedState("CLOCK");
			//2 and 3 letter latin abbreviations
			State leftAbbreviateAState = new NamedState("LEFT_A");
			State leftAbbreviatePState = new NamedState("LEFT_P");
			State leftAbbreviateQState = new NamedState("LEFT_Q");
			State leftAbbreviateOState = new NamedState("LEFT_O");
			State leftAbbreviateHState = new NamedState("LEFT_H");
			
	
	//		2 letter latin abbreviations		
			State rightAbbreviateODState = new NamedState("RIGHT_OD");
			State rightAbbreviateQDState = new NamedState("RIGHT_QD");
			State rightAbbreviateHSState = new NamedState("RIGHT_HS");
			State rightAbbreviateAMState = new NamedState("RIGHT_AM");
			State rightAbbreviatePMState = new NamedState("RIGHT_PM");
	//		3 letter latin abbreviations
			State middleAbbreviateQtoHState = new NamedState("MID_Q2H");
	
			State rightAbbreviateQHSState = new NamedState("RIGHT_QHS");
			State secondDotQtoHState = new NamedState("SECONDDOTQ2H");
			
			State firstDotAState = new NamedState("FIRSTDOTA");
			State firstDotPState = new NamedState("FIRSTDOTP");
			State firstDotQState = new NamedState("FIRSTDOTQ");
			State firstDotOState = new NamedState("FIRSTDOTO");
			State firstDotHState = new NamedState("FIRSTDOTH");
			
	
			
			Condition specificWordCondition = new TextSetCondition(
					iv_dailyWordSet, false);
			Condition soloCondition = new TextSetCondition(iv_perDayWordSet, false);
			Condition containsSoloTermCondition = new ContainsSetTextValueCondition(
					iv_perDayWordSet, false);
		    
			
			startState.addTransition(new TextValueCondition("q", false),
					leftAbbreviateQState);
			startState.addTransition(new TextValueCondition("o", false),
					leftAbbreviateOState);
			startState.addTransition(new TextValueCondition("h", false),
					leftAbbreviateHState);
	
            startState.addTransition(new IntegerRangeCondition(1,12), clockState);
            startState.addTransition(new TimeCondition(), endState);
			startState.addTransition(new TextValueCondition("bed", false),
					leftAbbreviateState);
			startState.addTransition(new TextValueCondition("per", false),
					leftAbbreviateState);
	
			startState.addTransition(specificWordCondition, endState);
			startState.addTransition(soloCondition, endState);
			startState.addTransition(containsSoloTermCondition, endState);
			
			startState.addTransition(new AnyCondition(), startState);
			
			clockState.addTransition(new TextValueCondition("a", false),
			leftAbbreviateAState);
			clockState.addTransition(new TextValueCondition("p", false),
			leftAbbreviatePState);
			clockState.addTransition(new AnyCondition(), startState);
			
			leftAbbreviateState.addTransition(specificWordCondition, endState);
			leftAbbreviateState.addTransition(new TextValueCondition("time", false), endState);
			leftAbbreviateState.addTransition(new AnyCondition(), startState);
			
			leftAbbreviateQState.addTransition(new PunctuationValueCondition('.'), firstDotQState);
			leftAbbreviateQState.addTransition(new AnyCondition(), startState);
				
			leftAbbreviateOState.addTransition(new PunctuationValueCondition('.'), firstDotOState);
			leftAbbreviateOState.addTransition(new AnyCondition(), startState);
			
			leftAbbreviateHState.addTransition(new PunctuationValueCondition('.'), firstDotHState);
			leftAbbreviateHState.addTransition(new AnyCondition(), startState);
			
			firstDotQState.addTransition(new TextValueCondition("d", false),
					rightAbbreviateQDState);
			firstDotQState.addTransition(new TextValueCondition("h", false),
					middleAbbreviateQtoHState);
			firstDotQState.addTransition(new AnyCondition(), startState);
			
			firstDotOState.addTransition(new TextValueCondition("d", false),
					rightAbbreviateODState);
			firstDotOState.addTransition(new AnyCondition(), startState);
			
			middleAbbreviateQtoHState.addTransition(new PunctuationValueCondition('.'),
					secondDotQtoHState);
			middleAbbreviateQtoHState.addTransition(new AnyCondition(), startState);
			
			leftAbbreviateAState.addTransition(new PunctuationValueCondition('.'), firstDotAState);
			//leftAbbreviateAState.addTransition(soloCondition, endState);
			leftAbbreviateAState.addTransition(new AnyCondition(), startState);
		
			leftAbbreviatePState.addTransition(new PunctuationValueCondition('.'), firstDotPState);
			leftAbbreviatePState.addTransition(new AnyCondition(), startState);
		
			firstDotAState.addTransition(new TextValueCondition("m", false),
					rightAbbreviateAMState);
			firstDotAState.addTransition(new AnyCondition(), startState);
			

		    
			firstDotPState.addTransition(new TextValueCondition("m", false),
					rightAbbreviatePMState);
			firstDotPState.addTransition(new AnyCondition(), startState);
			
			secondDotQtoHState.addTransition(new TextValueCondition("s", false),
					rightAbbreviateQHSState);
			
			secondDotQtoHState.addTransition(new AnyCondition(), startState);
			

			rightAbbreviateAMState.addTransition(new PunctuationValueCondition('.'), endState);
			rightAbbreviateAMState.addTransition(new AnyCondition(), startState);
		
			rightAbbreviatePMState.addTransition(new PunctuationValueCondition('.'), endState);
			rightAbbreviatePMState.addTransition(new AnyCondition(), startState);
			
			firstDotHState.addTransition(new TextValueCondition("s", false),
					rightAbbreviateHSState);
			firstDotHState.addTransition(new AnyCondition(), startState);
			
			rightAbbreviateODState.addTransition(new PunctuationValueCondition('.'), endState);
			rightAbbreviateODState.addTransition(new AnyCondition(), startState);
			
			rightAbbreviateQDState.addTransition(new PunctuationValueCondition('.'), endState);
			rightAbbreviateQDState.addTransition(new AnyCondition(), startState);
		
			rightAbbreviateHSState.addTransition(new PunctuationValueCondition('.'), endState);
			rightAbbreviateHSState.addTransition(new AnyCondition(), startState);
			
			rightAbbreviateQHSState.addTransition(new AnyCondition(), startState);
			
			ntEndState.addTransition(new AnyCondition(), startState);
			endState.addTransition(new AnyCondition(), startState);
		
			return m;
		}
	/**
	 * Gets a finite state machine that detects the following which indicates 1 per year via the following:
	 * <ol>
	 * <li>/y</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getYearlySuffixMachine() {
	
		State endState = new NamedState("END");
		State ntStartState = new NamedState("START");
		State skipFirstState = new IndentStartState("NON TERMINAL START");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
		skipFirstState.setEndStateFlag(true);
		
		Machine m = new Machine(ntStartState);
		State forwardSlashState = new NamedState("FSLASH");
	
		
	
		Condition suffixCondition = new TextSetCondition(iv_yearlySuffixSet,
				false);
		Condition forwardSlashCondition = new PunctuationValueCondition('/');
		
	
	
		ntStartState.addTransition(forwardSlashCondition, forwardSlashState);
		ntStartState.addTransition(new AnyCondition(), ntStartState);
	
		
		forwardSlashState.addTransition(suffixCondition, skipFirstState);
		forwardSlashState.addTransition(new AnyCondition(), ntStartState);
	
		
		skipFirstState.addTransition(new AnyCondition(), ntStartState);
		endState.addTransition(new AnyCondition(), ntStartState);
	
		return m;
	}
	/**
	 * Gets a finite state machine that detects the following which indicates 1 per month via the following:
	 * <ol>
	 * <li>/m</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getMonthlySuffixMachine() {
	
		State endState = new NamedState("END");
		State ntStartState = new NamedState("START");
		State skipFirstState = new IndentStartState("NON TERMINAL START");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
		skipFirstState.setEndStateFlag(true);
		
		Machine m = new Machine(ntStartState);
		State forwardSlashState = new NamedState("FSLASH");
	
		
	
		Condition suffixCondition = new TextSetCondition(iv_monthlySuffixSet,
				false);
		Condition forwardSlashCondition = new PunctuationValueCondition('/');
		
	
	
		ntStartState.addTransition(forwardSlashCondition, forwardSlashState);
		ntStartState.addTransition(new AnyCondition(), ntStartState);
	
		
		forwardSlashState.addTransition(suffixCondition, skipFirstState);
		forwardSlashState.addTransition(new AnyCondition(), ntStartState);
	
		
		skipFirstState.addTransition(new AnyCondition(), ntStartState);
		endState.addTransition(new AnyCondition(), ntStartState);
	
		return m;
	}
	/**
	 * Gets a finite state machine that detects the following which indicates 1 per week via the following:
	 * <ol>
	 * <li>/w</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getWeeklySuffixMachine() {
	
		State endState = new NamedState("END");
		State ntStartState = new NamedState("START");
		State skipFirstState = new IndentStartState("NON TERMINAL START");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
		skipFirstState.setEndStateFlag(true);
		
		Machine m = new Machine(ntStartState);
		State forwardSlashState = new NamedState("FSLASH");
	
		
	
		Condition suffixCondition = new TextSetCondition(iv_weeklySuffixSet,
				false);
		Condition forwardSlashCondition = new PunctuationValueCondition('/');
		
	
	
		ntStartState.addTransition(forwardSlashCondition, forwardSlashState);
		ntStartState.addTransition(new AnyCondition(), ntStartState);
	
		
		forwardSlashState.addTransition(suffixCondition, skipFirstState);
		forwardSlashState.addTransition(new AnyCondition(), ntStartState);
	
		
		skipFirstState.addTransition(new AnyCondition(), ntStartState);
		endState.addTransition(new AnyCondition(), ntStartState);
	
		return m;
	}
	/**
	 * Gets a finite state machine that detects the following which indicates 1 per day via the following:
	 * <ol>
	 * <li>/d</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getDailySuffixMachine() {
	
		State endState = new NamedState("END");
		State ntStartState = new NamedState("START");
		State skipFirstState = new IndentStartState("NON TERMINAL START");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
		skipFirstState.setEndStateFlag(true);
		
		Machine m = new Machine(ntStartState);
		State forwardSlashState = new NamedState("FSLASH");
	
		
	
		Condition suffixCondition = new TextSetCondition(iv_dailySuffixSet,
				false);
		Condition forwardSlashCondition = new PunctuationValueCondition('/');
		
	
	
		ntStartState.addTransition(forwardSlashCondition, forwardSlashState);
		ntStartState.addTransition(new AnyCondition(), ntStartState);
	
		
		forwardSlashState.addTransition(suffixCondition, skipFirstState);
		forwardSlashState.addTransition(new AnyCondition(), ntStartState);
	
		
		skipFirstState.addTransition(new AnyCondition(), ntStartState);
		endState.addTransition(new AnyCondition(), ntStartState);
	
		return m;
	}
	/**
		 * Gets a finite state machine that detects the following which indicates 1 per hour via the following:
		 * <ol>
		 * <li>/h</li>
		 * </ol>
		 * 
		 * @return
		 */
		private Machine getHourlySuffixMachine() {
	
			State endState = new NamedState("END");
			State ntStartState = new NamedState("START");
			State skipFirstState = new IndentStartState("NON TERMINAL START");
			State ntEndState = new NonTerminalEndState("NON TERMINAL END");
			ntEndState.setEndStateFlag(true);
			endState.setEndStateFlag(true);
			skipFirstState.setEndStateFlag(true);
			
			Machine m = new Machine(ntStartState);
			State forwardSlashState = new NamedState("FSLASH");

			

			Condition suffixCondition = new TextSetCondition(iv_hourlySuffixSet,
					false);
			Condition forwardSlashCondition = new PunctuationValueCondition('/');
			

	
			ntStartState.addTransition(forwardSlashCondition, forwardSlashState);
			ntStartState.addTransition(new AnyCondition(), ntStartState);

			
			forwardSlashState.addTransition(suffixCondition, skipFirstState);
			forwardSlashState.addTransition(new AnyCondition(), ntStartState);

			
			skipFirstState.addTransition(new AnyCondition(), ntStartState);
			endState.addTransition(new AnyCondition(), ntStartState);
		
			return m;
		}
	/**
	 * Executes the finite state machines.
	 * 
	 * @param tokens
	 * @return Set FrequencyToken objects.
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
				BaseToken startToken = null;
			  
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
					
										
					if (currentState instanceof IndentStartState) {
                    	startToken = (BaseToken) tokens
						.get(tokenStartIndex + 1);
    					
                    }
                    else {
                    	startToken = (BaseToken) tokens
						.get(tokenStartIndex);
				
                    }
					
                    BaseToken endToken = null;
                    if (currentState instanceof NonTerminalEndState)
                    {
                        endToken = (BaseToken) tokens.get(i - 1);
                    }
                    else
                    {
                        endToken = token;
                    }
                    
					FrequencyUnitToken fractionToken = null;
					 if (fsm.equals(iv_EveryOtherHourMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_24/2);
					 else if (fsm.equals(iv_SixTimesADayMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_SIX);
					 else if (fsm.equals(iv_FiveTimesADayMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_FIVE);
					 else if (fsm.equals(iv_FourTimesADayMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_FOUR);
					 else if (fsm.equals(iv_ThreeTimesADayMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_THREE);
					else if (fsm.equals(iv_HourlyMachine) || fsm.equals(iv_HourlySuffixMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_24);
					else if (fsm.equals(iv_TwiceADayMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_TWO);
					else if (fsm.equals(iv_DailyMachine) || fsm.equals(iv_DailySuffixMachine))
						fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_ONE);
					else if (fsm.equals(iv_EveryOtherDayMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_EVERY_OTHER_DAY);
					else if (fsm.equals(iv_WeeklyMachine)|| fsm.equals(iv_WeeklySuffixMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_WEEKLY);
					else if (fsm.equals(iv_MonthlySuffixMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_MONTHLY);
					else if (fsm.equals(iv_YearlySuffixMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_YEARLY);
					else 
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_PRN);
					
					rangeSet.add(fractionToken);
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
				BaseToken startToken = null;

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
					
	               if (currentState instanceof IndentStartState){
	                    	startToken = (BaseToken) tokens
							.get(tokenStartIndex + 1);
	    					
	                    }
	               else {
	                    	startToken = (BaseToken) tokens
							.get(tokenStartIndex);
					
	                    }
                    BaseToken endToken = null;
                    if (currentState instanceof NonTerminalEndState)
                    {
                        endToken = (BaseToken) tokens.get(i - 1);
                    }
                    else
                    {
                        endToken = token;
                    }
					FrequencyUnitToken fractionToken = null;
					 if (fsm.equals(iv_EveryOtherHourMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_24/2);
					 else if (fsm.equals(iv_SixTimesADayMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_SIX);
					 else if (fsm.equals(iv_FiveTimesADayMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_FIVE);
					 else if (fsm.equals(iv_FourTimesADayMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_FOUR);
					 else if (fsm.equals(iv_ThreeTimesADayMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_THREE);
					else if (fsm.equals(iv_HourlyMachine) || fsm.equals(iv_HourlySuffixMachine))
							fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_24);
					else if (fsm.equals(iv_TwiceADayMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_TWO);
					else if (fsm.equals(iv_DailyMachine) || fsm.equals(iv_DailySuffixMachine))
						fractionToken = new FrequencyUnitToken(
								startToken.getStartOffset(), endToken
										.getEndOffset(), FrequencyUnitToken.QUANTITY_ONE);
					else if (fsm.equals(iv_EveryOtherDayMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_EVERY_OTHER_DAY);
					else if (fsm.equals(iv_WeeklyMachine)|| fsm.equals(iv_WeeklySuffixMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_WEEKLY);
					else if (fsm.equals(iv_MonthlySuffixMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_MONTHLY);
					else if (fsm.equals(iv_YearlySuffixMachine))
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_YEARLY);
					else 
						fractionToken = new FrequencyUnitToken(
							startToken.getStartOffset(), endToken
									.getEndOffset(), FrequencyUnitToken.QUANTITY_PRN);
				
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
	/**
	 * Gets a fsm that detects five times a day
	 * <ol>
	 * <li>five X</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getFiveTimesADayMachine() {
		State startState = new NamedState("START_5TDM");
		State endState = new NamedState("END_5TDM");
		State handleRangeState = new NamedState("HANDLERANGE");
		State hyphState = new NamedState("HYPHSTATE");
		State ntEndState = new NonTerminalEndState("NON TERMINAL END");
		ntEndState.setEndStateFlag(true);
		endState.setEndStateFlag(true);
	

		State fiveHourState = new NamedState("FIVEHOUR");

	
		Machine m = new Machine(startState);
	
		startState.addTransition(new TextSetCondition(
				iv_sixTimesPerDayWordSet, false) , endState);
		startState.addTransition(new DisjoinCondition(new NegateCondition(new IntegerRangeCondition(1,4)), new IntegerValueCondition(5)), handleRangeState);
		startState.addTransition(new IntegerValueCondition(5), fiveHourState);
		startState.addTransition(new TextValueCondition("five", false), fiveHourState);

		startState.addTransition(new AnyCondition(), startState);
		
		handleRangeState.addTransition(new PunctuationValueCondition('-'), hyphState);
		handleRangeState.addTransition(new AnyCondition(), startState);
		
		fiveHourState.addTransition(new TextSetCondition(iv_hourlySuffixSet, false), endState);
		fiveHourState.addTransition(new AnyCondition(), startState);
		
		hyphState.addTransition(new IntegerRangeCondition(5,10), fiveHourState);
		hyphState.addTransition(new AnyCondition(), startState);
		
		ntEndState.addTransition(new AnyCondition(), startState);
		endState.addTransition(new AnyCondition(), startState);
		
		return m;
	
		
	}
}
