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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationEventMention;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Convert metamap concepts to ctakes named entities.
 * <p/>
 * Create MedicationEventMention/EntityMention annotations for each set of
 * CandidateConcept annotations that span the same text.
 * <p/>
 * if checkMedications is set to true, see if a named entity has a medication
 * semantic type. if so, create a MedicationEventMention. else create
 * EntityMention.
 * 
 * @author vijay
 * 
 */
@PipeBitInfo(
		name = "Metamap Annotation xlater",
		description = "Create MedicationEventMention/EntityMention annotations for each set of" +
				" CandidateConcept annotations that span the same text.",
		role = PipeBitInfo.Role.SPECIAL,
		products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class MetaMapToCTakesAnnotator extends JCasAnnotator_ImplBase {
	private static final Log log = LogFactory
			.getLog(MetaMapToCTakesAnnotator.class);
	private boolean checkMedications = false;

	private static final String[] medicationAbrs = { "aapp", "antb", "bacs",
			"bodm", "carb", "chem", "chvf", "chvs", "clnd", "eico", "elii",
			"enzy", "hops", "horm", "imft", "inch", "irda", "lipd", "nnon",
			"nsba", "opco", "orch", "phsu", "rcpt", "strd", "vita" };

	private static Set<String> setMedicationAbrs;
	static {
		setMedicationAbrs = new HashSet<String>(Arrays.asList(medicationAbrs));
	}

	public static class NegSpan {
		int begin;

		public int getBegin() {
			return begin;
		}

		public void setBegin(int begin) {
			this.begin = begin;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + begin;
			result = prime * result + end;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NegSpan other = (NegSpan) obj;
			if (begin != other.begin)
				return false;
			if (end != other.end)
				return false;
			return true;
		}

		public NegSpan(int begin, int end) {
			super();
			this.begin = begin;
			this.end = end;
		}

		public NegSpan(Annotation anno) {
			super();
			this.begin = anno.getBegin();
			this.end = anno.getEnd();
		}

		int end;
	}

	/**
	 * get all negated spans
	 * 
	 * @param jcas
	 * @return
	 */
	private Set<NegSpan> getNegatedSpans(JCas jcas) {
		Set<NegSpan> negSet = new HashSet<NegSpan>();
		// get the Metamap type
		Type negType = jcas.getTypeSystem().getType(
				"org.metamap.uima.ts.Negation");
		// abort if the type is not found
		if (negType == null) {
			log.debug("no negated concepts");
		} else {
			Feature spanFeature = negType.getFeatureByBaseName("ncSpans");
			if (spanFeature == null) {
				log.warn("no ncSpans feature!");
			} else {
				FSIterator<Annotation> negIter = jcas.getAnnotationIndex(
						negType).iterator();
				while (negIter.hasNext()) {
					Annotation negAnno = negIter.next();
					FSArray spanArr = (FSArray) negAnno
							.getFeatureValue(spanFeature);
					if (spanArr != null) {
						for (int i = 0; i < spanArr.size(); i++) {
							negSet.add(new NegSpan((Annotation) spanArr.get(i)));
						}
					}
				}
			}
		}
		return negSet;
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// get the negated spans
		Set<NegSpan> negSet = getNegatedSpans(jcas);
		// get the Metamap type
		Type candidateType = jcas.getTypeSystem().getType(
				"org.metamap.uima.ts.Candidate");
		// abort if the type is not found
		if (candidateType == null) {
			log.debug("no candidate concepts");
			return;
		}
		// get the cui feature
		Feature cuiFeature = candidateType.getFeatureByBaseName("cui");
		if (cuiFeature == null) {
			log.warn("no cui feature!");
			return;
		}
		Feature tuiFeature = candidateType
				.getFeatureByBaseName("semanticTypes");
		if (tuiFeature == null) {
			log.warn("no semanticTypes feature!");
			return;
		}

		// iterate through candidates
		FSIterator<Annotation> candidateIter = jcas.getAnnotationIndex(
				candidateType).iterator();
		// span we are working on in loop
		int begin = -1;
		int end = -1;
		// concepts for a given span
		Map<String, OntologyConcept> concepts = new HashMap<String, OntologyConcept>();
		// is one of the concepts for the span a medication?
		boolean bMedication = false;
		while (candidateIter.hasNext()) {
			Annotation annoCandidate = candidateIter.next();
			if (begin >= 0 && begin == annoCandidate.getBegin()
					&& end == annoCandidate.getEnd()) {
				// this candidate spans the same text as the last named entity
				// add it as one of the concepts
				bMedication = addConcept(jcas, concepts, annoCandidate,
						cuiFeature, tuiFeature, bMedication);

			} else {
				// moving on to a new named entity, finalize the old one
				addNamedEntity(jcas, begin, end, concepts, bMedication, negSet);
				// reset span
				begin = annoCandidate.getBegin();
				end = annoCandidate.getEnd();
				bMedication = addConcept(jcas, concepts, annoCandidate,
						cuiFeature, tuiFeature, bMedication);
			}
		}
		addNamedEntity(jcas, begin, end, concepts, bMedication, negSet);
	}

	private void addNamedEntity(JCas jcas, int begin, int end,
			Map<String, OntologyConcept> concepts, boolean bMedication,
			Set<NegSpan> negSet) {
		if (concepts.isEmpty())
			return;
		IdentifiedAnnotation neLast = bMedication ? new MedicationEventMention(
				jcas) : new EntityMention(jcas);
		neLast.setPolarity(negSet.contains(new NegSpan(begin, end)) ? CONST.NE_POLARITY_NEGATION_PRESENT
				: CONST.NE_POLARITY_NEGATION_ABSENT);
		neLast.setBegin(begin);
		neLast.setEnd(end);
		FSArray ocArr = new FSArray(jcas, concepts.size());
		int ocArrIdx = 0;
		for (OntologyConcept oc : concepts.values()) {
			// set the cui field if this is in fact a cui
			ocArr.set(ocArrIdx, oc);
			ocArrIdx++;
		}
		neLast.setOntologyConceptArr(ocArr);
		concepts.clear();
		neLast.addToIndexes();
	}

	/**
	 * add a concept to the map of concepts for the current named entity.
	 * 
	 * @param jcas
	 * @param concepts
	 * @param annoCandidate
	 * @param cuiFeature
	 * @param tuiFeature
	 * @param bMedication
	 * @return is this concept a medication concept? only check if
	 *         checkMedications is true
	 */
	private boolean addConcept(JCas jcas,
			Map<String, OntologyConcept> concepts, Annotation annoCandidate,
			Feature cuiFeature, Feature tuiFeature, boolean bMedication) {
		String cui = annoCandidate.getStringValue(cuiFeature);
		if (concepts.containsKey(cui))
			return bMedication;
		OntologyConcept oc = new OntologyConcept(jcas);
		oc.setCode(cui);
		oc.setCodingScheme("METAMAP");
		StringArray tuiArr = (StringArray) annoCandidate
				.getFeatureValue(tuiFeature);
		List<String> tuis = null;
		if (tuiArr != null)
			tuis = Arrays.asList(tuiArr.toStringArray());
		concepts.put(cui, oc);
		return checkMedications && tuis != null ? !Collections.disjoint(
				setMedicationAbrs, tuis) : false;
	}

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		checkMedications = (Boolean) aContext
				.getConfigParameterValue("checkMedications");
	}

}
