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
package org.apache.ctakes.sideeffect.ae;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;

import org.apache.ctakes.core.util.FSUtil;
import org.apache.ctakes.sideeffect.util.SEUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.sideeffect.type.PSESentence;

/**
 * Identify the sentence(s) that contains PSE (signs/symptoms or
 * disease/disorder) and drugs and add them to PSESentence. - If the sentence
 * containing PSE doesn't have drug, include the previous sentence if it
 * contains drug and it is in the same line (paragraph). - Disregard the
 * particular section(s) (eg, allergy section (20105)) This annotation will be
 * used to extract features for side-effect sentence classification.
 * 
 * @author Mayo Clinic
 * 
 */
public class PSESentenceAnnotator extends JCasAnnotator_ImplBase {
	public static final String PARAM_IGNORE_SECTIONS = "SectionsToIgnore";
	private Set setionsToIgnore;

	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		// get sections to ignore
		String[] sections;
		try {
			sections = (String[]) getContext().getConfigParameterValue(
					PARAM_IGNORE_SECTIONS);
			setionsToIgnore = new HashSet();
			for (int i = 0; i < sections.length; i++)
				setionsToIgnore.add(sections[i]);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}

	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator senIter = indexes.getAnnotationIndex(Sentence.type).iterator();

		while (senIter.hasNext()) {
			Sentence sen = (Sentence) senIter.next();
			boolean foundDrug = false;
			boolean foundPSE = false;

			if (setionsToIgnore.contains(sen.getSegmentId()))
				continue;

			// if drug is not found in the same sentence and the previous
			// sentence contains
			// drug and they are in the same line, then sentence will be
			// previous + current sentence
			Iterator neIter = FSUtil.getAnnotationsIteratorInSpan(jcas, IdentifiedAnnotation.type, sen.getBegin(), sen.getEnd() + 1);
			while (neIter.hasNext()) {
				IdentifiedAnnotation n = (IdentifiedAnnotation) neIter.next();
				if (n.getTypeID() == 2 || n.getTypeID() == 3)
					foundPSE = true;

				if (n.getTypeID() == 1)
					foundDrug = true;
			}

			if (!foundPSE)
				continue;

			if (foundPSE && foundDrug) {
				PSESentence ps = new PSESentence(jcas);
				ps.setBegin(sen.getBegin());
				ps.setEnd(sen.getEnd());
				ps.addToIndexes();
			} else if (foundPSE && !foundDrug) {
				int num = sen.getSentenceNumber();
				num = (num > 0) ? num - 1 : num;
				int[] previousSenSpan = SEUtil
						.getSentenceSpanOfGivenSentenceNum(jcas, num);

				// only if they are in the same line
				if (SEUtil.isSpanInSameLine(jcas, previousSenSpan[0],
						sen.getEnd())) {
					neIter = FSUtil.getAnnotationsIteratorInSpan(jcas, IdentifiedAnnotation.type, previousSenSpan[0],previousSenSpan[1] + 1);
					while (neIter.hasNext()) {
						IdentifiedAnnotation n = (IdentifiedAnnotation) neIter
								.next();
						if (n.getTypeID() == 1) {
							PSESentence ps = new PSESentence(jcas);
							ps.setBegin(previousSenSpan[0]);
							ps.setEnd(sen.getEnd());
							ps.addToIndexes();
							break;
						}
					}
				}
			}
		}
	}
}
