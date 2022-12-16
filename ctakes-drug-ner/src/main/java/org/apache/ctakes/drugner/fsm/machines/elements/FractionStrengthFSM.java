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

import org.apache.ctakes.core.fsm.condition.DecimalCondition;
import org.apache.ctakes.core.fsm.condition.NumberCondition;
import org.apache.ctakes.core.fsm.condition.PunctuationValueCondition;
import org.apache.ctakes.core.fsm.condition.WordSetCondition;
import org.apache.ctakes.core.fsm.state.NamedState;
import org.apache.ctakes.core.fsm.token.BaseToken;
import org.apache.ctakes.drugner.fsm.elements.conditions.ContainsSetTextValueCondition;
import org.apache.ctakes.drugner.fsm.output.util.FractionStrengthToken;

import net.openai.util.fsm.AnyCondition;
import net.openai.util.fsm.Condition;
import net.openai.util.fsm.Machine;
import net.openai.util.fsm.State;

/**
 * Uses one or more finite state machines to detect fraction strength tokens in the given
 * input of tokens.
 * 
 * @author Mayo clinic
 */
public class FractionStrengthFSM
{
    // text fractions
    Set<String> iv_textNumeratorSet = new HashSet<String>();
    Set<String> iv_textDenominatorSet = new HashSet<String>();

    // contains the finite state machines
    private Set<Machine> iv_machineSet = new HashSet<Machine>();

    /**
     * Constructor
     */
    public FractionStrengthFSM()
    {
        iv_textNumeratorSet.add("one");
        iv_textNumeratorSet.add("two");
        iv_textNumeratorSet.add("three");
        iv_textNumeratorSet.add("four");
        iv_textNumeratorSet.add("five");
        iv_textNumeratorSet.add("six");
        iv_textNumeratorSet.add("seven");
        iv_textNumeratorSet.add("eight");
        iv_textNumeratorSet.add("nine");
        iv_textNumeratorSet.add("ten");

        iv_textDenominatorSet.add("half");
        iv_textDenominatorSet.add("halfs");
        iv_textDenominatorSet.add("third");
        iv_textDenominatorSet.add("thirds");
        iv_textDenominatorSet.add("fourth");
        iv_textDenominatorSet.add("fourths");
        iv_textDenominatorSet.add("fifth");
        iv_textDenominatorSet.add("fifths");
        iv_textDenominatorSet.add("sixth");
        iv_textDenominatorSet.add("sixths");
        iv_textDenominatorSet.add("seventh");
        iv_textDenominatorSet.add("sevenths");
        iv_textDenominatorSet.add("eighth");
        iv_textDenominatorSet.add("eighths");
        iv_textDenominatorSet.add("nineths");
        iv_textDenominatorSet.add("nineth");
        iv_textDenominatorSet.add("tenth");
        iv_textDenominatorSet.add("tenths");

        iv_machineSet.add(getStrengthSlashMachine());
        iv_machineSet.add(getStandardMachine());
       
    }

    /**
	 * Gets a finite state machine that detects the following:
	 * <ol>
	 * 		<li>0.5/12.2</li>
	 * </ol>
	 * @return
	 */
	private Machine getStrengthSlashMachine()
	{
	    State startState = new NamedState("START");
	    State endState = new NamedState("END");
	    endState.setEndStateFlag(true);
	
	    Machine m = new Machine(startState);
	    State numeratorLeftState = new NamedState("NUMERATOR_LEFT");
	    State remainderRightState = new NamedState("REMAINDER_RIGHT");
	    State numeratorRightState = new NamedState("NUMERATOR_RIGHT");
	    State decPartNumState = new NamedState("DECIMAL_NUM");
	    State fslashState = new NamedState("FORWARD-SLASH");
	    State hypenState = new NamedState("HYPHEN");
	    State dotLeftState = new NamedState("DOT_LEFT");
	    State dotRightState = new NamedState("DOT_RIGHT");
	    Condition intNumeratorCondition = new NumberCondition();
	    Condition decPartInt = new NumberCondition();
	    Condition fslashCondition = new PunctuationValueCondition('/');
	    Condition intDenominatorCondition = new NumberCondition();
	    Condition decDenominatorCondition = new DecimalCondition();
	    Condition decimalCondition = new DecimalCondition();
	    Condition containsdotCondition = new PunctuationValueCondition('.');
	    Condition leftContainsShortDose = new ContainsSetTextValueCondition(
				iv_textNumeratorSet, false);
		
	    startState.addTransition(intNumeratorCondition, numeratorLeftState);
	    startState.addTransition(leftContainsShortDose, numeratorLeftState);
	    startState.addTransition(new DecimalCondition(), numeratorLeftState);
	    startState.addTransition(new AnyCondition(), startState);
	    
	    numeratorLeftState.addTransition(containsdotCondition, dotLeftState);
	    numeratorLeftState.addTransition(fslashCondition, fslashState);
	    numeratorLeftState.addTransition(new PunctuationValueCondition('-'), numeratorRightState);
	    numeratorLeftState.addTransition(new AnyCondition(), startState);
	    
	    dotLeftState.addTransition(decPartInt, hypenState);
	    dotLeftState.addTransition(decDenominatorCondition, hypenState);
	    dotLeftState.addTransition(new AnyCondition(), startState);
		
	    hypenState.addTransition(new PunctuationValueCondition('-'), numeratorRightState);
	    hypenState.addTransition(new AnyCondition(), startState);
	    
	    numeratorRightState.addTransition(new NumberCondition(), endState);
	    numeratorRightState.addTransition(new DecimalCondition(), endState);
	    numeratorRightState.addTransition(new AnyCondition(), startState);
	    
	    dotRightState.addTransition(new PunctuationValueCondition('.'), fslashState);
	    dotRightState.addTransition(new AnyCondition(), startState);
	    
	    decPartNumState.addTransition(fslashCondition, fslashState);
	    decPartNumState.addTransition(new AnyCondition(), startState);
	    
	    fslashState.addTransition(intDenominatorCondition, endState);
	    fslashState.addTransition(decimalCondition, endState);
	    fslashState.addTransition(new AnyCondition(), startState);
		
	    endState.addTransition(new AnyCondition(), startState);
	
	    return m;
	}

	/**
     * Gets a finite state machine that detects the following:
     * <ol>
     * 		<li>1/2</li>
     * 		<li>0.5</li>
     * 		<li>half</li>
     * 		<li>one half</li>
     * 		<li>1 half</li>
     * </ol>
     * @return
     */
    private Machine getStandardMachine()
    {
        State startState = new NamedState("START");
        State endState = new NamedState("END");
        endState.setEndStateFlag(true);

        Machine m = new Machine(startState);
        State numeratorNumState = new NamedState("NUMERATOR_NUM");
        State fslashState = new NamedState("FORWARD_SLASH");
        State numeratorTextState = new NamedState("NUMERATOR_TEXT");

        Condition intNumeratorCondition = new NumberCondition();
        Condition fslashCondition = new PunctuationValueCondition('/');
        Condition intDenominatorCondition = new NumberCondition();
        Condition decimalCondition = new DecimalCondition();
        Condition textNumeratorCondition =
            new WordSetCondition(iv_textNumeratorSet, false);
        Condition textDenominatorCondition =
            new WordSetCondition(iv_textDenominatorSet, false);

        startState.addTransition(intNumeratorCondition, numeratorNumState);
        startState.addTransition(decimalCondition, numeratorNumState);
		startState.addTransition(textNumeratorCondition, numeratorTextState);
		startState.addTransition(textDenominatorCondition, endState);
        startState.addTransition(new AnyCondition(), startState);

        numeratorNumState.addTransition(fslashCondition, fslashState);
		numeratorNumState.addTransition(textDenominatorCondition, endState);
        numeratorNumState.addTransition(new AnyCondition(), startState);

        fslashState.addTransition(intDenominatorCondition, endState);
        fslashState.addTransition(new AnyCondition(), startState);

		numeratorTextState.addTransition(textDenominatorCondition, endState);
		numeratorTextState.addTransition(new AnyCondition(), startState);

        endState.addTransition(new AnyCondition(), startState);

        return m;
    }

    /**
     * Executes the finite state machines.
     * @param tokens
     * @return Set of FractionToken objects.
     * @throws Exception
     */
    public Set execute(List tokens) throws Exception
    {
        Set<FractionStrengthToken> fractionSet = new HashSet<FractionStrengthToken>();

        // maps a fsm to a token start index
        // key = fsm , value = token start index
        Map tokenStartMap = new HashMap();

        for (int i = 0; i < tokens.size(); i++)
        {
            BaseToken token = (BaseToken) tokens.get(i);

            Iterator machineItr = iv_machineSet.iterator();
            while (machineItr.hasNext())
            {
            	Machine fsm = (Machine) machineItr.next();

            	fsm.input(token);

            	State currentState = fsm.getCurrentState();
            	if (currentState.getStartStateFlag())
            	{
            		tokenStartMap.put(fsm, Integer.valueOf(i));
            	}
            	if (currentState.getEndStateFlag())
            	{
            		Object o = tokenStartMap.get(fsm);
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
            		BaseToken startToken =
            			(BaseToken) tokens.get(tokenStartIndex);
            		BaseToken endToken = token;
            		FractionStrengthToken fractionToken =
            			new FractionStrengthToken(
            					startToken.getStartOffset(),
            					endToken.getEndOffset());
            		fractionSet.add(fractionToken);
            		fsm.reset();
            	}
            }
        }

        // cleanup
        tokenStartMap.clear();

        // reset machines
        Iterator<Machine> itr = iv_machineSet.iterator();
        while (itr.hasNext())
        {
            Machine fsm = itr.next();
            fsm.reset();
        }

        return fractionSet;
    }
}
