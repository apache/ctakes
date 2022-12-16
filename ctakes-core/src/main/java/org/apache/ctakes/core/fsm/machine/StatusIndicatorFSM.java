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

import org.apache.ctakes.core.fsm.condition.TextSetCondition;
import org.apache.ctakes.core.fsm.condition.TextValueCondition;
import org.apache.ctakes.core.fsm.output.StatusIndicator;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.state.NonTerminalEndState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.core.fsm.token.TextToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect indications of 
 * <br>family history of
 * <br>history of
 * <br>possible/probable/likely
 * 
 * @author Mayo Clinic
 */
public class StatusIndicatorFSM
{
    private Set<String> iv_probableSet = new HashSet<String>();
    private Set<String> iv_historySet = new HashSet<String>();
    private Set<String> iv_familyHistorySet = new HashSet<String>();

    // contains the finite state machines
    private Machine iv_probableMachine;
    private Machine iv_historyMachine;
    private Machine iv_familyHistoryMachine;
    private Set<Machine> iv_machineSet = new HashSet<Machine>();

    /**
     * 
     * Constructor
     *  
     */
    public StatusIndicatorFSM()
    {
        iv_probableSet.add("possible");
        iv_probableSet.add("possibly");
        iv_probableSet.add("probable");
        iv_probableSet.add("likely");

        iv_historySet.add("hx");
        iv_historySet.add("history");

        iv_familyHistorySet.add("fx");
        iv_familyHistorySet.add("fh");

        iv_historyMachine = getHistoryMachine();
        iv_probableMachine = getProbableMachine();
        iv_familyHistoryMachine = getFamilyHistoryMachine();
        iv_machineSet.add(iv_historyMachine);
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
    private Machine getProbableMachine()
    {
        State startState = new NamedState("START");
        State endState = new NamedState("END");

        endState.setEndStateFlag(true);

        Machine m = new Machine(startState);

        Condition probableC = new TextSetCondition(iv_probableSet, false);

        startState.addTransition(probableC, endState);
        startState.addTransition(new AnyCondition(), startState);

        endState.addTransition(new AnyCondition(), startState);

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

        // for case h/o
        State hAbbrState = new NamedState("HISTORY_ABBR");
        State fslashState = new NamedState("FORWARD_SLASH");

        endState.setEndStateFlag(true);

        Machine m = new Machine(startState);

        Condition historyC = new TextSetCondition(iv_historySet, false);

        startState.addTransition(historyC, endState);
        startState
                .addTransition(new TextValueCondition("h", false), hAbbrState);
        startState.addTransition(new AnyCondition(), startState);

        hAbbrState.addTransition(
                new TextValueCondition("/", false),
                fslashState);
        hAbbrState.addTransition(new AnyCondition(), startState);

        fslashState.addTransition(new TextValueCondition("o", false), endState);
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
     * @return Set of DateToken objects.
     */
    public Set<StatusIndicator> execute(List<TextToken> tokens) throws Exception
    {
        Set<StatusIndicator> outSet = new HashSet<StatusIndicator>();

        // maps a fsm to a token start index
        // key = Machine , value = token start index
        Map<Machine, Integer> tokenStartMap = new HashMap<Machine, Integer>();

        for (int i = 0; i < tokens.size(); i++)
        {
            BaseToken token = tokens.get(i);

            Iterator<Machine> machineItr = iv_machineSet.iterator();
            while (machineItr.hasNext())
            {
                Machine m = machineItr.next();

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
                        // By default, all machines start with token zero.
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
                        endToken = tokens.get(i - 1);
                    }
                    else
                    {
                        endToken = token;
                    }

                    BaseToken startToken = tokens.get(tokenStartIndex);
                    StatusIndicator si = null;
                    if (m.equals(iv_historyMachine))
                    {
                        si = new StatusIndicator(
                                startToken.getStartOffset(),
                                endToken.getEndOffset(),
                                StatusIndicator.HISTORY_STATUS);
                    }
                    else if (m.equals(iv_probableMachine))
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

        // cleanup
        tokenStartMap.clear();

        // reset machines
        Iterator<Machine> itr = iv_machineSet.iterator();
        while (itr.hasNext())
        {
            Machine m = itr.next();
            m.reset();
        }

        return outSet;
    }
}