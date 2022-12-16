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

import org.apache.ctakes.core.fsm.condition.DisjoinCondition;
import org.apache.ctakes.core.fsm.condition.NegateCondition;
import org.apache.ctakes.core.fsm.condition.TextSetCondition;
import org.apache.ctakes.core.fsm.condition.TextValueCondition;
import org.apache.ctakes.core.fsm.machine.FSM;
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
 * @author DUFFP
 */
public class NegIndicatorFSM implements FSM
{

    // regular modal verb
    private Set iv_modalVerbsSet = new HashSet();
    // negative particle
    private Set iv_negParticlesSet = new HashSet();
    // regular verbs requiring negation particle
    private Set iv_regVerbsSet = new HashSet();
    // neagive verbs that contain negation in them
    private Set iv_negVerbsSet = new HashSet();
     // negation preposition
    private Set iv_negPrepositionsSet = new HashSet();
    // negatively charged determiners
    private Set iv_negDeterminersSet = new HashSet();
    // regular nouns - indicators
    private Set iv_regNounsSet = new HashSet();
    // regular prepositions
    private Set iv_regPrepositionsSet = new HashSet();
  
    // negative adjectives
    private Set iv_negAdjectivesSet = new HashSet();

    //	negative collocations
    private Set iv_negCollocSet = new HashSet();
    // NEGATIVE COLLOCATION PARTICLE
    private Set iv_negColPartSet = new HashSet();

    private Set iv_negCol1of3PartSet = new HashSet();
    
    private Set iv_negCol2of3PartSet = new HashSet();
    
    private Set iv_negColAnyOf3PartSet = new HashSet();

    // contains the finite state machines
    private Set iv_machineSet = new HashSet();
    // beginning with capitalize. E.g 'No'
    private Set iv_negInitialDeterminersSet = new HashSet();
    
    private Machine iv_negInitialDetermineMachine = new Machine();
    /**
     * 
     * Constructor
     *  
     */
    public NegIndicatorFSM()
    {
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
        iv_negParticlesSet.add("n't");
        iv_negParticlesSet.add("'t");

        iv_negColPartSet.add("out");
        
        iv_negCol2of3PartSet.add("the");
        iv_negCol2of3PartSet.add("this");
        iv_negCol2of3PartSet.add("about");
        iv_negCol2of3PartSet.add("her");
        iv_negCol2of3PartSet.add("his");
        iv_negCol2of3PartSet.add("their");
        
        iv_negColAnyOf3PartSet.add("the");
        iv_negColAnyOf3PartSet.add("is");
               
        iv_negCollocSet.add("rule");
        iv_negCollocSet.add("rules");
        iv_negCollocSet.add("ruled");
        iv_negCollocSet.add("ruling");
        iv_negCollocSet.add("rule-out");
        iv_negCollocSet.add("r/o");
        iv_negCollocSet.add("w/o");
     
        
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


       	// end special list of terms
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
        iv_negVerbsSet.add("rule-out");
        // special negation terms for PGRN study
        iv_negVerbsSet.add("doubt");
        iv_negVerbsSet.add("doubted");
        iv_negVerbsSet.add("discuss");
        iv_negVerbsSet.add("discussed");
        iv_negVerbsSet.add("discussing");
        iv_negVerbsSet.add("discussion");
        iv_negVerbsSet.add("decide");
        iv_negVerbsSet.add("decided");
        iv_negVerbsSet.add("deciding");
        iv_negVerbsSet.add("recommend");
        iv_negVerbsSet.add("recommends");
       	iv_negVerbsSet.add("recommending");
       	iv_negVerbsSet.add("recommended"); 
       	iv_negVerbsSet.add("plan");
       	iv_negVerbsSet.add("plans");
       	iv_negVerbsSet.add("planned");
       	iv_negVerbsSet.add("planning");
       	iv_negVerbsSet.add("think");
       	iv_negVerbsSet.add("thinks");
       	iv_negVerbsSet.add("talk"); 
       	iv_negVerbsSet.add("talked");
       	iv_negVerbsSet.add("talking");
       	iv_negVerbsSet.add("consider");
       	iv_negVerbsSet.add("considers");
       	iv_negVerbsSet.add("considered");
       	iv_negVerbsSet.add("considering");
       	iv_negVerbsSet.add("suggest");
       	iv_negVerbsSet.add("suggests");
       	iv_negVerbsSet.add("suggested");
       	iv_negVerbsSet.add("suggesting");
  
        
        iv_negPrepositionsSet.add("without");
        iv_negPrepositionsSet.add("absent");
        iv_negPrepositionsSet.add("none");

        iv_negDeterminersSet.add("no");
        iv_negDeterminersSet.add("any");
        iv_negDeterminersSet.add("neither");
        iv_negDeterminersSet.add("nor");
        iv_negDeterminersSet.add("never");
        iv_negDeterminersSet.add("nothing");
        iv_negDeterminersSet.add("unlikely");//added for phrase "ne is very unlikely" situations
        
        iv_negInitialDeterminersSet.add("No");
        
         
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

        iv_negInitialDetermineMachine = getInitialNegIndicatorMachine();
        iv_machineSet.add(getAspectualNegIndicatorMachine());
        iv_machineSet.add(getNominalNegIndicatorMachine());
        iv_machineSet.add(getAdjNegIndicatorMachine());
        iv_machineSet.add(iv_negInitialDetermineMachine);

    }

    /**
     * Gets a finite state machine that detects the following:
     * <ol>
     * <li>10/15/2002</li>
     * <li>10/15</li>
     * <li>10-15-2002</li>
     * <li>10-15</li>
     * </ol>
     * 
     * @return
     */
    private Machine getAspectualNegIndicatorMachine()
    {
        State startState = new NamedState("START");
        State endState = new NamedState("END");
        State anyState = new NamedState("ANY");
        // for case h/o
        State rAbbrState = new NamedState("RULEOUT_ABBR");
        State wAbbrState = new NamedState("WITHOUT_ABBR");
        State fslashState = new NamedState("FORWARD_SLASH");
        
        State ntEndState = new NonTerminalEndState("NON TERMINAL END");
        endState.setEndStateFlag(true);
        ntEndState.setEndStateFlag(true);

        Machine m = new Machine(startState);
        State regModalState = new NamedState("REG_MODAL");
        State negPartState = new NamedState("NEG_PART");
        State negVerbState = new NamedState("NEG_VERB");
        State negCollocState = new NamedState("NEG_COLLOC");
        State negColPartState = new NamedState("NEG_COLPART");
        State negColMultiPartState = new NamedState("NEG_COLMULTIPART");
        State negColSecondPartState = new NamedState("NEG_COL2NDPART");

        Condition regModalC = new TextSetCondition(iv_modalVerbsSet, false);
        Condition negPartC = new TextSetCondition(iv_negParticlesSet, false);
        Condition regVerbC = new TextSetCondition(iv_regVerbsSet, false);
 
        Condition negVerbC = new TextSetCondition(iv_negVerbsSet, false);
        Condition negDetC = new TextSetCondition(iv_negDeterminersSet, false);
        Condition negCollocC = new TextSetCondition(iv_negCollocSet, false);
        Condition negColPartC = new TextSetCondition(iv_negColPartSet, false);
        Condition neg1of3PartC = new TextSetCondition(iv_negCol1of3PartSet, false);
        Condition neg2of3PartC = new TextSetCondition(iv_negCol2of3PartSet, false);
        Condition notCollocC = new NegateCondition(negCollocC);

        startState.addTransition(negVerbC, negVerbState);
        startState.addTransition(neg1of3PartC, negColMultiPartState);
        startState.addTransition(negCollocC, negCollocState); //rule
      
        startState.addTransition(
                new DisjoinCondition(regModalC, regVerbC),
                regModalState); // start with a modal
        startState.addTransition(
                new DisjoinCondition(negPartC, negDetC),
                negPartState);
        
        startState
        .addTransition(new TextValueCondition("r", false), rAbbrState);
        startState
        .addTransition(new TextValueCondition("w", false), wAbbrState);
        startState.addTransition(new AnyCondition(), startState);
        

        negColMultiPartState.addTransition(neg2of3PartC, negColSecondPartState);
        negColMultiPartState.addTransition(negColPartC, negCollocState);
        negColMultiPartState.addTransition(new AnyCondition(), startState);
        
        negColSecondPartState.addTransition(new TextSetCondition(iv_negColAnyOf3PartSet, false), negCollocState);
        negColSecondPartState.addTransition(negColPartC, negCollocState);
        negColSecondPartState.addTransition(new AnyCondition(), startState);
        
        regModalState.addTransition(negCollocC, negCollocState);
        negCollocState.addTransition(negColPartC, negColPartState); //out
        
        negColPartState.addTransition(new AnyCondition(), ntEndState);
        
        negCollocState.addTransition(new AnyCondition(), startState);

        regModalState.addTransition(
                new DisjoinCondition(negPartC, negDetC),
                negPartState);
        regModalState.addTransition(new AnyCondition(), anyState);

        anyState.addTransition(
                new DisjoinCondition(negPartC, negDetC),
                negPartState);
        anyState.addTransition(new AnyCondition(), startState);
        rAbbrState.addTransition(
                new TextValueCondition("/", false),
                fslashState);
        rAbbrState.addTransition(new AnyCondition(), startState);
        
        wAbbrState.addTransition(
                new TextValueCondition("/", false),
                fslashState);
        wAbbrState.addTransition(new AnyCondition(), startState);

        fslashState.addTransition(new TextValueCondition("o", false), endState);
        fslashState.addTransition(new AnyCondition(), startState);
        
        negPartState.addTransition(notCollocC, ntEndState);
        negVerbState.addTransition(notCollocC, ntEndState);
        negPartState.addTransition(new AnyCondition(), startState);
      //  negVerbState.addTransition(new AnyCondition(), startState);

        negPartState.addTransition(new AnyCondition(), ntEndState);
        negVerbState.addTransition(new AnyCondition(), ntEndState);

        ntEndState.addTransition(new AnyCondition(), startState);

        return m;
    }

    private Machine getNominalNegIndicatorMachine()
	{
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
	
	    ntEndState.addTransition(new AnyCondition(), startState);
	
	    return m;
	}

	private Machine getInitialNegIndicatorMachine()
    {
        State startState = new NamedState("START");
        State endState = new NamedState("END");

        State ntEndState = new NonTerminalEndState("NON TERMINAL END");
        endState.setEndStateFlag(true);
        ntEndState.setEndStateFlag(true);

        Machine m = new Machine(startState);
       
        State negDetState = new NamedState("NEG_DET");

        Condition negInitDetC = new TextSetCondition(iv_negInitialDeterminersSet, true);

        startState.addTransition(negInitDetC, negDetState); // start with a modal
        startState.addTransition(new AnyCondition(), startState);

        negDetState.addTransition(new AnyCondition(), endState);
        endState.addTransition(new AnyCondition(), startState);
        return m;
    }

    private Machine getAdjNegIndicatorMachine()
    {
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

        ntEndState.addTransition(new AnyCondition(), startState);

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
                    tokenStartMap.put(fsm, new Integer(i));
                }
                if (currentState.getEndStateFlag())
                {
                    Object o = tokenStartMap.get(fsm);
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
                    NegationIndicator neg = null;
                    if (fsm.equals(iv_negInitialDetermineMachine))
                    {
                        neg =  new NegationIndicator(
                                startToken.getStartOffset(),
                                endToken.getEndOffset());
                    }
                    else neg = new NegationIndicator(
                            startToken.getStartOffset(),
                            endToken.getEndOffset());
                    outSet.add(neg);
                    fsm.reset();
                }
            }
        }

        // cleanup
        tokenStartMap.clear();

        // reset machines
        Iterator itr = iv_machineSet.iterator();
        while (itr.hasNext())
        {
            Machine fsm = (Machine) itr.next();
            fsm.reset();
        }

        return outSet;
    }
}