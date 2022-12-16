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
package org.apache.ctakes.assertion.attributes;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;

import org.apache.ctakes.assertion.attributes.subject.SubjectAttributeClassifier;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

/**
 * <br>
* This class provides a basic subject attribute extraction technique based on dependency parses.
 * <p>
 * 
 * 
 */

public class SubjectAttributeAnalysisEngine extends JCasAnnotator_ImplBase {

	public Logger logger = Logger.getLogger(getClass().getName());
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (IdentifiedAnnotation mention : JCasUtil.select(jCas, IdentifiedAnnotation.class)) {

			// For each mention, set the subject attribute
//			if (mention.getGeneric() == true ) {
//				mention.setSubject (null);
//			} else {
				String oldsubj = mention.getSubject();
				mention.setSubject( SubjectAttributeClassifier.getSubject(jCas, mention) );
//				System.err.println("Word:"+mention.getCoveredText()+", overwrote subject "+oldsubj+" with "+mention.getSubject());
//			}
		}
		
		
	}
}
