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

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.core.fsm.output.StatusIndicator;
import org.apache.ctakes.necontexts.ContextHit;
import org.apache.ctakes.necontexts.ContextHitConsumer;
import org.apache.ctakes.necontexts.NamedEntityContextHitConsumer;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

/**
 * @author Mayo Clinic
 */
public class StatusContextHitConsumer extends NamedEntityContextHitConsumer implements ContextHitConsumer {
	public void consumeHit(JCas jcas, Annotation focusAnnot, int scope, ContextHit ctxHit)
			throws AnalysisEngineProcessException {
		
		Integer status = (Integer) ctxHit.getMetaData(StatusContextAnalyzer.CTX_HIT_KEY_STATUS_TYPE);
		if (focusAnnot instanceof IdentifiedAnnotation) {
			IdentifiedAnnotation neAnnot = (IdentifiedAnnotation) focusAnnot;
			//TODO: currently status is an int in the old system.  Let's update this to a constant string?
			neAnnot.setUncertainty(status);
			if(StatusIndicator.HISTORY_STATUS == status 
					|| StatusIndicator.FAMILY_HISTORY_STATUS == status ) {
				neAnnot.setHistoryOf(1);
			}
		}

		createContextAnnot(jcas, focusAnnot, scope, ctxHit).addToIndexes();
	}
}