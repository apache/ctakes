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
package org.apache.ctakes.necontexts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.core.fsm.adapters.TextTokenAdapter;
import org.apache.ctakes.core.fsm.token.TextToken;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * This context analyzer provides code that is shared by the
 * StatusContextAnalyzer and NegationContextAnalyzer which are both analyzers
 * that examine the contexts surrounding named entity annotations.
 */
public abstract class NamedEntityContextAnalyzer implements ContextAnalyzer {

	private static final Logger logger = Logger.getLogger(NamedEntityContextAnalyzer.class);

	private Set<String> _boundaryWordSet;

	public void initialize(UimaContext annotatorContext) throws ResourceInitializationException {
		initBoundaryData();
	}

	private void initBoundaryData() {
		logger.info("initBoundaryData() called for ContextInitializer");
		_boundaryWordSet = new HashSet<String>();
		_boundaryWordSet.add("but");
		_boundaryWordSet.add("however");
		_boundaryWordSet.add("nevertheless");
		_boundaryWordSet.add("notwithstanding");
		_boundaryWordSet.add("though");
		_boundaryWordSet.add("although");
		_boundaryWordSet.add("if");
		_boundaryWordSet.add("when");
		_boundaryWordSet.add("how");
		_boundaryWordSet.add("what");
		_boundaryWordSet.add("which");
		_boundaryWordSet.add("while");
		_boundaryWordSet.add("since");
		_boundaryWordSet.add("then");
		_boundaryWordSet.add("i");
		_boundaryWordSet.add("he");
		_boundaryWordSet.add("she");
		_boundaryWordSet.add("they");
		_boundaryWordSet.add("we");

		_boundaryWordSet.add(";");
		_boundaryWordSet.add(":");
		_boundaryWordSet.add(".");
		_boundaryWordSet.add(")");
	}

	public boolean isBoundary(Annotation contextAnnotation, int scopeOrientation) throws AnalysisEngineProcessException {
		String lcText = contextAnnotation.getCoveredText().toLowerCase();
		return _boundaryWordSet.contains(lcText);
	}

	/**
	 * This method converts Token annotations to TextTokens required by the fsm library used by both subclasses of this class.
	 * @param tokenList a list of token annotations
	 * @return a conversion of the token annotations as a list of TextTokens
	 */
	protected List<TextToken> wrapAsFsmTokens(List<? extends Annotation> tokenList) {
		List<TextToken> fsmTokenList = new ArrayList<TextToken>();

		Iterator<? extends Annotation> tokenItr = tokenList.iterator();
		while (tokenItr.hasNext()) {
			Annotation tokenAnnot = tokenItr.next();
			fsmTokenList.add(new TextTokenAdapter(tokenAnnot));
		}

		// Add dummy token to end of the list
		// This is a workaround for cases where a meaningful token occurs at the
		// end of the list. Since there are no more tokens, the FSM cannot push
		// itself into the next state. The dummy token's intent is to provide
		// that extra token.
		fsmTokenList.add(new TextToken() {

			public String getText() {
				return "+DUMMY_TOKEN+";
			}

			public int getEndOffset() {
				return 0;
			}

			public int getStartOffset() {
				return 0;
			}
		});

		return fsmTokenList;
	}
}
