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

import java.util.List;

import org.apache.ctakes.necontexts.negation.NegationContextAnalyzer;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * A context analyzer analyzes a set of annotations that make up a context and
 * produces context hits which are consumed by context hit consumers.
 * 
 * @see NegationContextAnalyzer for an example
 * @see ContextHitConsumer
 */
public interface ContextAnalyzer {
	/**
	 * This will be called by the ContextAnnotator during its initialize method
	 * 
	 * @param context
	 * @throws AnnotatorConfigurationException
	 * @throws AnnotatorInitializationException
	 */
	public void initialize(UimaContext context) throws ResourceInitializationException;

	/**
	 * This method is called by the ContextAnnotator.
	 * <p>
	 * For the left and right scopes, the context annotator will collect a
	 * number of annotations up to the maximum specified by the appropriate
	 * parameter unless it comes to the edge of the window annotation or a
	 * boundary condition is met as specified by this method. For example, you
	 * may specify that the scope is LEFT, the maximum left scope size is 10,
	 * the context annotation is a token of some sort, and the window annotation
	 * is a sentence. The context annotator will collect the annotations to the
	 * left of the focus annotation such that there will be no more than 10
	 * context annotations, they will all be inside the sentence, and no context
	 * annotations will be collected to the left of a boundary condition as
	 * specified by this method.
	 * 
	 * @param contextAnnotation
	 *            the context annotation that may or may not satisfy a boundary
	 *            condition
	 * @param scopeOrientation
	 *            the scope of the context annotator. The value will be the left
	 *            or right scope.
	 * @return true if the context annotation satisfies a boundary condition.
	 * @throws AnnotatorProcessException
	 * 
	 * @see ContextAnnotator#LEFT_SCOPE
	 * @see ContextAnnotator#RIGHT_SCOPE
	 * @see NegationContextAnalyzer#isBoundary(Annotation, int)
	 */
	public boolean isBoundary(Annotation contextAnnotation, int scopeOrientation) throws AnalysisEngineProcessException;

	/**
	 * This method is called by the ContextAnnotator.
	 * <p>
	 * This method analyzes a list of context annotations to determine if there
	 * is a "context hit" - some event that we are looking for in the context.
	 * 
	 * @param contextAnnotations
	 *            the annotations to be analyzed
	 * @param scopeOrientation
	 *            the scope of the context annotator. The value will be the
	 *            left, right, middle, or "all" context.
	 * @return a context hit if one exists, else null is returned.
	 * @throws AnnotatorProcessException
	 * 
	 * @see NegationContextAnalyzer#analyzeContext(List, int)
	 * @see ContextAnnotator#LEFT_SCOPE
	 * @see ContextAnnotator#RIGHT_SCOPE
	 * @see ContextAnnotator#MIDDLE_SCOPE
	 * @see ContextAnnotator#ALL_SCOPE
	 */
	public ContextHit analyzeContext(List<? extends Annotation> contextAnnotations, int scopeOrientation)
			throws AnalysisEngineProcessException;
}