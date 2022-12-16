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
package org.apache.ctakes.ytex.uima.annotators;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService;
import org.apache.ctakes.ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import org.apache.ctakes.ytex.kernel.wsd.WordSenseDisambiguator;
import org.apache.ctakes.ytex.uima.ApplicationContextHolder;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.common.base.Strings;

/**
 * Disambiguate named entities via adapated Lesk algorithm with semantic
 * similarity. Configuration parameters set in ytex.properties / via -D option /
 * in config for SenseDisambiguatorAnnotator (minus the 'ytex.' prefix)
 * <ul>
 * <li>ytex.sense.windowSize - context window size. concepts from named entities
 * +- windowSize around the target named entity are used for disambiguation.
 * defaults to 10
 * <li>ytex.sense.metric - measure to use. defaults to INTRINSIC_PATH
 * <li>ytex.conceptGraph - concept graph to use.
 * <li>ytex.conceptProperty - field of ontology concept to use. Use cui for
 * UmlsConcept, code for OntologyConcept.
 * </ul>
 * 
 * @author vijay
 * 
 */
public class SenseDisambiguatorAnnotator extends JCasAnnotator_ImplBase {
	int windowSize = 50;
	SimilarityMetricEnum metric = SimilarityMetricEnum.INTRINSIC_PATH;
	WordSenseDisambiguator wsd;
	boolean disabled = false;
	String conceptProperty = null;
	private static final Log log = LogFactory
			.getLog(SenseDisambiguatorAnnotator.class);

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		Properties props = ApplicationContextHolder.getYtexProperties();
		String conceptProperty = (String) aContext
				.getConfigParameterValue("conceptProperty");
		if (!Strings.isNullOrEmpty(conceptProperty))
			this.conceptProperty = conceptProperty;
		else
			this.conceptProperty = props.getProperty("ytex.conceptProperty");
		Integer nWindowSize = (Integer) aContext
				.getConfigParameterValue("windowSize");
		if (nWindowSize != null && nWindowSize.intValue() > 0)
			windowSize = nWindowSize.intValue();
		else
			windowSize = Integer.parseInt(props.getProperty(
					"ytex.sense.windowSize", "50"));
		String uMetric = (String) aContext.getConfigParameterValue("metric");
		if (!Strings.isNullOrEmpty(uMetric))
			metric = SimilarityMetricEnum.valueOf(uMetric);
		else
			metric = SimilarityMetricEnum.valueOf(props.getProperty(
					"ytex.sense.metric", "INTRINSIC_PATH"));
		wsd = ApplicationContextHolder.getApplicationContext().getBean(
				WordSenseDisambiguator.class);
		ConceptSimilarityService simSvc = ApplicationContextHolder
				.getApplicationContext().getBean(
						ConceptSimilarityService.class);
		if (simSvc.getConceptGraph() == null) {
			log.warn("Concept Graph was not loaded - word sense disambiguation disabled");
			disabled = true;
		}
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		if (disabled)
			return;
		// iterate through sentences
		FSIterator<Annotation> neIter = jcas.getAnnotationIndex(
				IdentifiedAnnotation.type).iterator();
		List<IdentifiedAnnotation> listNE = new ArrayList<IdentifiedAnnotation>();
		while (neIter.hasNext()) {
			listNE.add((IdentifiedAnnotation) neIter.next());
		}
		// disambiguate the named entities
		disambiguate(jcas, listNE);
	}

	/**
	 * get the concept id for the specified concept. if conceptProperty is
	 * defined, get that property. for UmlsConcept, return cui for
	 * OntologyConcept, return code
	 * 
	 * @param oc
	 * @return
	 * @throws IllegalAccessException
	 * @throws ReflectiveOperationException
	 * @throws NoSuchMethodException
	 */
	protected String getConceptId(FeatureStructure oc)
			throws AnalysisEngineProcessException {
		try {

			if (!Strings.isNullOrEmpty(this.conceptProperty))
				return BeanUtils.getProperty(oc, conceptProperty);
			if (oc instanceof UmlsConcept) {
				return ((UmlsConcept) oc).getCui();
			} else if (oc instanceof OntologyConcept) {
				return ((OntologyConcept) oc).getCode();
			} else {
				throw new IllegalArgumentException(
						"don't know how to get concept id for: "
								+ oc.getClass().getName());
			}
		} catch (IllegalAccessException e) {
			throw new AnalysisEngineProcessException(e);
		} catch (InvocationTargetException e) {
			throw new AnalysisEngineProcessException(e);
		} catch (NoSuchMethodException e) {
			throw new AnalysisEngineProcessException(e);
		}

	}

	/**
	 * 
	 * @param jcas
	 * @param listNE
	 *            list of named entities to disambiguate
	 */
	protected void disambiguate(JCas jcas, List<IdentifiedAnnotation> listNE)
			throws AnalysisEngineProcessException {
		// allocate list to hold IdentifiedAnnotations with concepts
		List<IdentifiedAnnotation> listNonTrivialNE = new ArrayList<IdentifiedAnnotation>();
		// allocate list to hold concepts in each named entity
		List<Set<String>> listConcept = new ArrayList<Set<String>>();
		for (IdentifiedAnnotation ne : listNE) {
			FSArray concepts = ne.getOntologyConceptArr();
			// add the concept senses from each named entity
			if (concepts != null && concepts.size() > 0) {
				listNonTrivialNE.add(ne);
				Set<String> conceptSenses = new HashSet<String>();
				listConcept.add(conceptSenses);
				for (int i = 0; i < concepts.size(); i++) {

					if (concepts.get(i) != null) {
						String conceptId = this.getConceptId(concepts.get(i));
						if (!Strings.isNullOrEmpty(conceptId))
							conceptSenses.add(conceptId);
					}
				}
			}
		}
		if(log.isTraceEnabled())
			log.trace("listConcept: " + listConcept);
		// iterate through named entities and disambiguate
		for (int i = 0; i < listConcept.size(); i++) {
			Set<String> conceptSenses = listConcept.get(i);
			// only bother with wsd if there is more than one sense
			if (conceptSenses.size() > 1) {
				if(log.isTraceEnabled())
					log.trace("i: " + i + ", conceptSenses: " + conceptSenses);
				Map<String, Double> scores = new HashMap<String, Double>();
				String concept = this.wsd.disambiguate(listConcept, i, null,
						windowSize, metric, scores, true);
				if(log.isTraceEnabled())
					log.trace("i: " + i + ", concept: " + concept);
				IdentifiedAnnotation ne = listNonTrivialNE.get(i);
				FSArray concepts = ne.getOntologyConceptArr();
				for (int j = 0; j < concepts.size(); j++) {
					OntologyConcept yoc = (OntologyConcept) concepts.get(j);
					String conceptId = this.getConceptId(yoc);
					// update the score and set the predicted concept field
					if (concept == null || concept.equals(conceptId))
						yoc.setDisambiguated(true);
					if (scores.containsKey(conceptId))
						yoc.setScore(scores.get(conceptId));

				}
			} else if (conceptSenses.size() == 1) {
				// only one concept - for ytex concept set the predicted concept
				IdentifiedAnnotation ne = listNonTrivialNE.get(i);
				FSArray concepts = ne.getOntologyConceptArr();
				OntologyConcept oc = (OntologyConcept) concepts.get(0);
				oc.setDisambiguated(true);
			}
		}
	}
}
