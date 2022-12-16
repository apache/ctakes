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

import org.apache.ctakes.core.fsm.condition.TextSetCondition;
import org.apache.ctakes.core.fsm.condition.TextValueCondition;
import org.apache.ctakes.core.fsm.output.StatusIndicator;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.state.NonTerminalEndState;
import org.apache.ctakes.core.fsm.token.BaseToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect ???
 * 
 * @author Mayo Clinic
 */
public class StatusIndicatorFSM
{
    private Set iv_probableSet = new HashSet();
    private Set iv_historySet = new HashSet();
    private Set iv_histColPartSet = new HashSet();
    private Set iv_histCollocSet = new HashSet();
    private Set iv_probableColPartSet = new HashSet();
    private Set iv_probableCollocSet = new HashSet();
    private Set iv_familyHistorySet = new HashSet();

    // contains the finite state machines
    private Machine iv_probableMachine;
    private Machine iv_probableMachineNoTerm;
    private Machine iv_historyMachine;
    private Machine iv_familyHistoryMachine;
    /**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * <li>probable</li>
	 * <li>probably</li>
	 * <li>likely</li>
	 * </ol>
	 * 
	 * @return
	 */
	private Machine getProbableMachine()
	{
	    State startState = new NamedState("START");
	    State endState = new NamedState("END");
	    State dualState = new NamedState("DUALSTATE");

	    endState.setEndStateFlag(true);
	
	    Machine m = new Machine(startState);
	
	    Condition probableC = new TextSetCondition(iv_probableSet, false);
	   
	
	    startState.addTransition(probableC, endState);
	    startState.addTransition(new AnyCondition(), startState);

	
	    dualState.addTransition(new AnyCondition(), startState);
	    
	    endState.addTransition(new AnyCondition(), startState);
	
	    return m;
	}

	private Set iv_machineSet = new HashSet();

    /**
     * 
     * Constructor
     *  
     */
    public StatusIndicatorFSM()
    {
    
    	
        iv_probableSet.add("possible");
        iv_probableSet.add("possibly");
        iv_probableSet.add("possibility");
        iv_probableSet.add("probable");
        iv_probableSet.add("likely");
        iv_probableSet.add("questionable");
        iv_probableSet.add("question");
        iv_probableSet.add("probability");
        iv_probableSet.add("differential");
    
        iv_probableSet.add("may");
        iv_probableSet.add("consider");
        iv_probableSet.add("evaluate");
        iv_probableSet.add("evaluation");
        iv_probableSet.add("might");
        iv_probableSet.add("?");
        iv_probableSet.add("vs");
        iv_probableSet.add("considered");
        
        iv_probableCollocSet.add("questionable");
        iv_probableColPartSet.add("history");
        iv_probableColPartSet.add("hx");
       // iv_probableColPartSet.add("h");
        
        iv_histColPartSet.add("status");
        
        iv_histCollocSet.add("post");
        
        iv_historySet.add("hx");
        iv_historySet.add("h/o");
        iv_historySet.add("s/p");
        iv_historySet.add("history");

        iv_familyHistorySet.add("fx");
        iv_familyHistorySet.add("fh");
        iv_familyHistorySet.add("mother");
        iv_familyHistorySet.add("father");
        iv_familyHistorySet.add("sister");
        iv_familyHistorySet.add("brother");
        iv_familyHistorySet.add("son");
        iv_familyHistorySet.add("daugher");
        iv_familyHistorySet.add("grandfather");
        iv_familyHistorySet.add("grandmother");

        iv_historyMachine = getHistoryMachine();
        iv_probableMachine = getProbableMachine();
        iv_probableMachineNoTerm = getProbableMachineNonTerm();
        iv_familyHistoryMachine = getFamilyHistoryMachine();
        iv_machineSet.add(iv_historyMachine);
        iv_machineSet.add(iv_probableMachineNoTerm);
        iv_machineSet.add(iv_probableMachine);
        iv_machineSet.add(iv_familyHistoryMachine);
    }
    /**
     * Gets a finite state machine that detects the following:
     * <ol>
     * <li>probable</li>
     * <li>probably</li>
     * <li>likely</li>
     * </ol>
     * 
     * @return
     */
    private Machine getProbableMachineNonTerm()
    {
        State startState = new NamedState("START");
        State endState = new NamedState("END");
        State anyState = new NamedState("ANY");

        State ntEndState = new NonTerminalEndState("NON TERMINAL END");
        endState.setEndStateFlag(true);
        ntEndState.setEndStateFlag(true);
        // for case h/o
        State hAbbrState = new NamedState("HISTORY_ABBR");
        State oAbbrState = new NamedState("OF_ABBR");
        
        Machine m = new Machine(startState);
        State fslashState = new NamedState("FORWARD_SLASH");
        State probCollocState = new NamedState("PROB_COLLOC");
        State probFamilyPartState = new NamedState("PROB_FAMC");
        State probColPartState = new NamedState("PROB_COLPART");
       // Condition probableFHC = new TextValueCondition("family", false);
        Condition probCollecC = new TextSetCondition(iv_probableCollocSet, false);
        Condition probColPartC = new TextSetCondition(iv_probableColPartSet, false);
	       
  
        startState.addTransition(probCollecC, probCollocState); //questionable
        startState.addTransition(new AnyCondition(), startState);

        probCollocState.addTransition(probColPartC, probColPartState); //history
        probCollocState.addTransition(new TextValueCondition("h", false), hAbbrState); //h   
       // probCollocState.addTransition(probableFHC, probFamilyPartState); 
        probCollocState.addTransition(new AnyCondition(), anyState);
        
        hAbbrState.addTransition(
                new TextValueCondition("/", false),
                fslashState);
	    hAbbrState.addTransition(new AnyCondition(), startState);
	    	    		

	    
	    probFamilyPartState.addTransition(new AnyCondition(), ntEndState);
	    
	   // anyState.addTransition(probableFHC, probFamilyPartState);  
	   // anyState.addTransition(probColPartC, probColPartState);
        anyState.addTransition(new TextValueCondition("h", false), hAbbrState);
       // anyState.addTransition(new TextValueCondition("/", false), fslashState);
        anyState.addTransition(new AnyCondition(), anyState);
        
        fslashState.addTransition(new TextValueCondition("o", false), oAbbrState);
	    fslashState.addTransition(new AnyCondition(), startState);
	    
        oAbbrState.addTransition(new AnyCondition(), ntEndState);
        oAbbrState.addTransition(new AnyCondition(), startState);
        
	    probColPartState.addTransition(new AnyCondition(), ntEndState);
	    //probColPartState.addTransition(new AnyCondition(), startState);
	    ntEndState.addTransition(new AnyCondition(), startState);
        return m;
    }

    /**
     * Gets a finite state machine that detects the following:
     * <ol>
     * <li>history</li>
     * <li>hx</li>
     * <li>h/o</li>
     * </ol>
     * 
     * @return
     */
    private Machine getHistoryMachine()
    {
        State startState = new NamedState("START");
        State endState = new NamedState("END");
      
        //Condition negCollocC = new TextSetCondition(iv_negCollocSet, false);
        //Condition negColPartC = new TextSetCondition(iv_negColPartSet, false);
        // for case h/o
        State hAbbrState = new NamedState("HISTORY_ABBR");
        // for case s/p
        State sAbbrState = new NamedState("STATUS_ABBR");
        
        State fslashState = new NamedState("FORWARD_SLASH");

        endState.setEndStateFlag(true);

        Machine m = new Machine(startState);

        Condition historyC = new TextSetCondition(iv_historySet, false);
  
        startState.addTransition(historyC, endState);
        startState
                .addTransition(new TextValueCondition("h", false), hAbbrState);
        startState
        .addTransition(new TextValueCondition("s", false), hAbbrState);
        startState.addTransition(new AnyCondition(), startState);

        hAbbrState.addTransition(
                new TextValueCondition("/", false),
                fslashState);
        hAbbrState.addTransition(new AnyCondition(), startState);
	   
        sAbbrState.addTransition(
                new TextValueCondition("/", false),
                fslashState);
        sAbbrState.addTransition(new AnyCondition(), startState);

        fslashState.addTransition(new TextValueCondition("o", false), endState);
        fslashState.addTransition(new TextValueCondition("p", false), endState);
        fslashState.addTransition(new AnyCondition(), startState);

        endState.addTransition(new AnyCondition(), startState);

        return m;
    }

    /**
     * Gets a finite state machine that detects the following:
     * <ol>
     * <li>fh</li>
     * <li>fx</li>
     * <li>family history</li>
     * </ol>
     * 
     * @return
     */
    private Machine getFamilyHistoryMachine()
    {
        State startState = new NamedState("START");
        State endState = new NamedState("END");

        // for case h/o
        State familyState = new NamedState("FAMILY");

        endState.setEndStateFlag(true);

        Machine m = new Machine(startState);

        Condition familyHistoryC = new TextSetCondition(
                iv_familyHistorySet,
                false);

        startState.addTransition(familyHistoryC, endState);
        startState.addTransition(
                new TextValueCondition("family", false),
                familyState);
        startState.addTransition(new AnyCondition(), startState);

        familyState.addTransition(
                new TextValueCondition("history", false),
                endState);
        familyState.addTransition(new AnyCondition(), startState);

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
    public Set execute(List tokens) throws Exception
    {
        Set outSet = new HashSet();

        // maps a fsm to a token start index
        // key = Machine , value = token start index
        Map tokenStartMap = new HashMap();

        for (int i = 0; i < tokens.size(); i++)
        {
            BaseToken token = (BaseToken) tokens.get(i);

            Iterator machineItr = iv_machineSet.iterator();
            while (machineItr.hasNext())
            {
                Machine m = (Machine) machineItr.next();
                m.input(token);

                State currentState = m.getCurrentState();
                if (currentState.getStartStateFlag())
                {
                    tokenStartMap.put(m, new Integer(i));
                }
                if (currentState.getEndStateFlag())
                {
                    Object o = tokenStartMap.get(m);
                    int tokenStartIndex;
                    if (o == null)
                    {
                        // By default, all machines start with
                        // token zero.
                        tokenStartIndex = 0;
                    }
                    else
                    {
                        tokenStartIndex = ((Integer) o).intValue();
                        // skip ahead over single token we don't want
                        tokenStartIndex++;
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

                    BaseToken startToken = (BaseToken) tokens
                            .get(tokenStartIndex);
                    StatusIndicator si = null;
                    if (m.equals(iv_historyMachine))
                    {
                        si = new StatusIndicator(
                                startToken.getStartOffset(),
                                endToken.getEndOffset(),
                                StatusIndicator.HISTORY_STATUS);
                    }
                    else if (m.equals(iv_probableMachine) || (m.equals(iv_probableMachineNoTerm)))
                    {

                        si = new StatusIndicator(
                                startToken.getStartOffset(),
                                endToken.getEndOffset(),
                                StatusIndicator.PROBABLE_STATUS);
                    }
                    else if (m.equals(iv_familyHistoryMachine))
                    {
                        si = new StatusIndicator(
                                startToken.getStartOffset(),
                                endToken.getEndOffset(),
                                StatusIndicator.FAMILY_HISTORY_STATUS);
                    }
                    outSet.add(si);
                    m.reset();
                }
            }
        }

        // TODO resolve conflict between history & family history

        // cleanup
        tokenStartMap.clear();

        // reset machines
        Iterator itr = iv_machineSet.iterator();
        while (itr.hasNext())
        {
            Machine m = (Machine) itr.next();
            m.reset();
        }

        return outSet;
    }
}