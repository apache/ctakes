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
package org.apache.ctakes.drugner.ae.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.core.fsm.adapters.TextTokenAdapter;
import org.apache.ctakes.core.fsm.machine.FSM;
import org.apache.ctakes.core.fsm.output.NegationIndicator;
import org.apache.ctakes.core.fsm.token.TextToken;
import org.apache.ctakes.core.util.JCasUtil;
import org.apache.ctakes.drugner.fsm.machines.util.NegIndicatorFSM;
import org.apache.ctakes.necontexts.ContextAnalyzerAdapter;
import org.apache.ctakes.necontexts.ContextHit;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;



public class NegContextInitializerImpl extends ContextAnalyzerAdapter
//        implements ContextAnnotator
{
	  /**
	   * Specifies the parameter AnnotationType to iterate on, specified in the descriptor file.
	   * example values would be NamedEntityAnnotation, WordTokenAnnotation, etc.
	   * @see #getFocusAnnotationIterator(JCas)
	   */
	  
	public static final String PARAM_ANNOTATION_TYPE = "AnnotationType";
	    
	  /**
	   * Specifies the NegationIndicatorFSMClass that needs to be used
	   */
    public static final String NEGATION_IND_FSM_CLASS = "NegationIndicatorFSMClass";
    
    protected List wrapAsFsmTokens(List tokenList)
    {
        List fsmTokenList = new ArrayList();

        Iterator tokenItr = tokenList.iterator();
        while (tokenItr.hasNext())
        {
            Annotation tokenAnnot = (Annotation) tokenItr.next();
            fsmTokenList.add(new TextTokenAdapter(tokenAnnot));
        }

        // Add dummy token to end of the list
        // This is a workaround for cases where a meaningful token occurs at the
        // end of the list. Since there are no more tokens, the FSM cannot push
        // itself into the next state. The dummy token's intent is to provide
        // that extra token.
        fsmTokenList.add(new TextToken()
        {

            public String getText()
            {
                return "+DUMMY_TOKEN+";
            }

            public int getEndOffset()
            {
                return 0;
            }

            public int getStartOffset()
            {
                return 0;
            }
        });

        return fsmTokenList;
    }  

    public ContextHit getContextHit(List tokenList, int scope)
            throws AnnotatorProcessException
    {
        List fsmTokenList = wrapAsFsmTokens(tokenList);

        try
        {
            Set s = iv_negIndicatorFSM.execute(fsmTokenList);

            if (s.size() > 0)
            {
                NegationIndicator neg = (NegationIndicator) s.iterator().next();
                return new ContextHit(neg.getStartOffset(), neg.getEndOffset());
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            throw new AnnotatorProcessException(e);
        }
    }

	public Iterator getAnnotationIterator(JCas jcas)
			throws AnnotatorProcessException {
		// TODO Auto-generated method stub
	      JFSIndexRepository indexes = jcas.getJFSIndexRepository();
	      return indexes.getAnnotationIndex(iAnnotationType).iterator();
	}

	public void initialize(UimaContext uimaContext)
	throws ResourceInitializationException 
	{
	  super.initialize(uimaContext);
	  try
	  {
	    Object val = uimaContext.getConfigParameterValue(PARAM_ANNOTATION_TYPE);
	    iAnnotationType = JCasUtil.getType((String)val);
	    
	    String statusIndicatorFSMClass = (String) uimaContext.getConfigParameterValue(NEGATION_IND_FSM_CLASS);
	    iv_negIndicatorFSM = (FSM) Class.forName(statusIndicatorFSMClass).newInstance();
	  }
	  catch (Exception ace)
	  {
	    throw new ResourceInitializationException(ace);
	  }
	}
	 protected int iAnnotationType;
	 private FSM iv_negIndicatorFSM = new NegIndicatorFSM();
}