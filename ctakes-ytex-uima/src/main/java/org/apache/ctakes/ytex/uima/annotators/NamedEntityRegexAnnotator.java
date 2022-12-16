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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.ytex.uima.ApplicationContextHolder;
import org.apache.ctakes.ytex.uima.dao.NamedEntityRegexDao;
import org.apache.ctakes.ytex.uima.model.NamedEntityRegex;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Create NamedEntity annotations. Use regex to identify the Named Entities.
 * Read the named entity regex - concept id map from the db.
 * 
 * @author vijay
 * 
 */
@PipeBitInfo(
		name = "Named Entity Annotator (RegEx)",
		description = "Use regex to identify the Named Entities. " +
				" Read the named entity regex - concept id map from the db.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION },
		products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class NamedEntityRegexAnnotator extends JCasAnnotator_ImplBase {
	private static final Log log = LogFactory
			.getLog(NamedEntityRegexAnnotator.class);

	private NamedEntityRegexDao neRegexDao;
	private Map<NamedEntityRegex, Pattern> regexMap = new HashMap<NamedEntityRegex, Pattern>();

	// private Integer getTypeIdForClassName(String strClassName) {
	// try {
	// Class<?> clazz = Class.forName(strClassName);
	// Field field = clazz.getDeclaredField("typeIndexID");
	// return field.getInt(clazz);
	// } catch (Exception e) {
	// log.error("config error, could not get type id for class: "
	// + strClassName, e);
	// return null;
	// }
	// }

	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		neRegexDao = (NamedEntityRegexDao) ApplicationContextHolder
				.getApplicationContext().getBean("namedEntityRegexDao");
		List<NamedEntityRegex> regexList = neRegexDao.getNamedEntityRegexs();
		initRegexMap(regexList);
	}

	protected void initRegexMap(List<NamedEntityRegex> regexList) {
		for (NamedEntityRegex regex : regexList) {
			if (log.isDebugEnabled())
				log.debug(regex);
			Pattern pat = Pattern.compile(regex.getRegex());
			regexMap.put(regex, pat);
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		for (Map.Entry<NamedEntityRegex, Pattern> entry : regexMap.entrySet()) {
			if (entry.getKey().getContext() != null) {
				AnnotationIndex<Annotation> idx = aJCas
						.getAnnotationIndex(Segment.typeIndexID);
				FSIterator<Annotation> iter = idx.iterator();
				while (iter.hasNext()) {
					Segment segment = (Segment) iter.next();
					if (entry.getKey().getContext().equals(segment.getId())) {
						processRegex(aJCas, segment, entry.getKey(),
								entry.getValue());
					}
				}
			} else {
				// no context specified - search entire document
				processRegex(aJCas, null, entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Search the document / annotation span for with the supplied pattern. If
	 * we get a hit, create a named entity annotation.
	 * 
	 * @param aJCas
	 * @param anno
	 * @param neRegex
	 * @param pattern
	 */
	private void processRegex(JCas aJCas, Annotation anno,
			NamedEntityRegex neRegex, Pattern pattern) {
		String docText = aJCas.getDocumentText();
		String annoText = anno != null ? docText.substring(anno.getBegin(),
				anno.getEnd()) : docText;
		int nOffset = anno != null ? anno.getBegin() : 0;
		Matcher matcher = pattern.matcher(annoText);
		while (matcher.find()) {
			EntityMention ne = new EntityMention(aJCas);
			ne.setBegin(nOffset + matcher.start());
			ne.setEnd(nOffset + matcher.end());
			FSArray ocArr = new FSArray(aJCas, 1);
			OntologyConcept oc = new OntologyConcept(aJCas);
			oc.setCode(neRegex.getCode());
			oc.setCodingScheme(neRegex.getCodingScheme());
			oc.setOid(neRegex.getOid());
			ocArr.set(0, oc);
			ne.setOntologyConceptArr(ocArr);
			ne.addToIndexes();
		}
	}
}
