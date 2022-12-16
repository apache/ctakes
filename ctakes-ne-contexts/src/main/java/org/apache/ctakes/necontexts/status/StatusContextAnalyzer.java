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
package org.apache.ctakes.necontexts.status;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.core.fsm.machine.StatusIndicatorFSM;
import org.apache.ctakes.core.fsm.output.StatusIndicator;
import org.apache.ctakes.core.fsm.token.TextToken;
import org.apache.ctakes.necontexts.ContextAnalyzer;
import org.apache.ctakes.necontexts.ContextAnnotator;
import org.apache.ctakes.necontexts.ContextHit;
import org.apache.ctakes.necontexts.NamedEntityContextAnalyzer;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.tcas.Annotation;


public class StatusContextAnalyzer extends NamedEntityContextAnalyzer implements ContextAnalyzer {
	public static final String CTX_HIT_KEY_STATUS_TYPE = "STATUS_TYPE";

	private StatusIndicatorFSM _statusIndicatorFSM = new StatusIndicatorFSM();

	/**
	 * Analyze a list of tokens looking for a status pattern as
	 * specified by the class StatusIndicatorFSM.
	 * An indication of family history found within the scope takes precedence over 
	 * an indication of history of, even if evidence of history of is closer.
	 * Otherwise, the closest indicator of status is used.
	 * 
	 * @see StatusIndicatorFSM
	 */

	public ContextHit analyzeContext(List<? extends Annotation> tokenList, int scope) throws AnalysisEngineProcessException{
		List<TextToken> fsmTokenList = wrapAsFsmTokens(tokenList);

		try {
			Set<StatusIndicator> s = _statusIndicatorFSM.execute(fsmTokenList);

			if (s.size() > 0) {
				StatusIndicator finalSi = null;
				Iterator<StatusIndicator> siItr = s.iterator();
				while (siItr.hasNext()) {
					StatusIndicator si = siItr.next();
					if (finalSi == null) {
						finalSi = si;
					} else if ((si.getStatus() == StatusIndicator.FAMILY_HISTORY_STATUS)
							&& (finalSi.getStatus() == StatusIndicator.HISTORY_STATUS)) {
						// family history always overrides history
						finalSi = si;
					} else if ((scope == ContextAnnotator.LEFT_SCOPE) && (si.getEndOffset() > finalSi.getEndOffset())) {
						// pick one with closest proximity to focus
						finalSi = si;
					} else if ((scope == ContextAnnotator.RIGHT_SCOPE)
							&& (si.getStartOffset() < finalSi.getStartOffset())) {
						// pick one w/ closest proximity to focus
						finalSi = si;
					}
				}
				ContextHit ctxHit = new ContextHit(finalSi.getStartOffset(), finalSi.getEndOffset());

				ctxHit.addMetaData(CTX_HIT_KEY_STATUS_TYPE, new Integer(finalSi.getStatus()));

				return ctxHit;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
}