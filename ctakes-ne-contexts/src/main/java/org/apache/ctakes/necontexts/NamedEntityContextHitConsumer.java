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

import org.apache.ctakes.typesystem.type.textsem.ContextAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;


public abstract class NamedEntityContextHitConsumer implements ContextHitConsumer {
	public abstract void consumeHit(JCas jcas, Annotation focusAnnot, int scope, ContextHit ctxHit)
			throws AnalysisEngineProcessException;

	protected ContextAnnotation createContextAnnot(JCas jcas, Annotation focusAnnot, int scope, ContextHit ctxHit) {
		ContextAnnotation ctxAnnot = new ContextAnnotation(jcas);
		ctxAnnot.setBegin(ctxHit.getStartOffset());
		ctxAnnot.setEnd(ctxHit.getEndOffset());
		if (scope == ContextAnnotator.LEFT_SCOPE) {
			ctxAnnot.setScope("LEFT");
		} else if (scope == ContextAnnotator.MIDDLE_SCOPE) {
			ctxAnnot.setScope("MIDDLE");
		} else if (scope == ContextAnnotator.RIGHT_SCOPE) {
			ctxAnnot.setScope("RIGHT");
		}
		ctxAnnot.setFocusText(focusAnnot.getCoveredText());
		return ctxAnnot;
	}

}
