/*
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
package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.nlp.tokenizer.Token;
import org.apache.ctakes.core.nlp.tokenizer.Tokenizer;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.StringIntegerMapResource;
import org.apache.ctakes.core.util.ParamUtil;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UIMA annotator that uses the Tokenizer module.
 * 
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "Tokenizer",
      description = "Annotates Document Tokens.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class TokenizerAnnotator extends JCasAnnotator_ImplBase {
	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	public static final int TOKEN_CAP_NONE = 0;
	public static final int TOKEN_CAP_FIRST_ONLY = 1;
	public static final int TOKEN_CAP_MIXED = 2;
	public static final int TOKEN_CAP_ALL = 3;

	public static final int TOKEN_NUM_POS_NONE = 0;
	public static final int TOKEN_NUM_POS_FIRST = 1;
	public static final int TOKEN_NUM_POS_MIDDLE = 2;
	public static final int TOKEN_NUM_POS_LAST = 3;

	public static final int TOKEN_NUM_TYPE_UNKNOWN = 0;
	public static final int TOKEN_NUM_TYPE_INTEGER = 1;
	public static final int TOKEN_NUM_TYPE_DECIMAL = 2;

	/**
	 * Value is "SegmentsToSkip". This parameter specifies which segments to
	 * skip. The parameter should be of type String, should be multi-valued and
	 * optional.
	 */
	public static final String PARAM_SEGMENTS_TO_SKIP = "SegmentsToSkip";

	private final String HYPH_FREQ_TABLE_RESRC_KEY = "HyphFreqTable";

	private UimaContext context;
	private Set<String> skipSegmentsSet;

	private Tokenizer tokenizer;

	private int tokenCount = 0;

   @Override
   public void initialize( UimaContext aContext )
         throws ResourceInitializationException {
      super.initialize(aContext);

		context = aContext;
		try {
			configInit();
		} catch (ResourceAccessException ace) {
			throw new ResourceInitializationException(ace);
		}
	}

	/**
	 * Reads configuration parameters.
	 */
	private void configInit() throws ResourceAccessException {
		skipSegmentsSet = ParamUtil.getStringParameterValuesSet(
				PARAM_SEGMENTS_TO_SKIP, context);

		int freqCutoff = ((Integer) context
				.getConfigParameterValue("FreqCutoff")).intValue();

		StringIntegerMapResource strIntMapResrc = (StringIntegerMapResource) context
				.getResourceObject(HYPH_FREQ_TABLE_RESRC_KEY);
		if (strIntMapResrc == null) {
			logger.warn("Unable to locate resource with key="
					+ HYPH_FREQ_TABLE_RESRC_KEY
					+ ".  Proceeding without hyphenation support.");
			tokenizer = new Tokenizer();
		} else {
			logger.info("Hyphen dictionary: " + strIntMapResrc.toString());
			Map<String, Integer> hyphMap = strIntMapResrc.getMap();
			tokenizer = new Tokenizer(hyphMap, freqCutoff);
		}

	}

	/**
	 * Entry point for processing.
	 */
   @Override
   public void process( JCas jcas ) throws AnalysisEngineProcessException {

		logger.info("process(JCas)");

		tokenCount = 0;

		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		FSIterator<Annotation> segmentItr = indexes.getAnnotationIndex(
				Segment.type).iterator();
		while (segmentItr.hasNext()) {
			Segment sa = (Segment) segmentItr.next();
			String segmentID = sa.getId();
			if (!skipSegmentsSet.contains(segmentID)) {
				annotateRange(jcas, sa.getBegin(), sa.getEnd());
			}
		}
	}

	/**
	 * A utility method that tokenizes a range of text.
	 */
	protected void annotateRange(JCas jcas, int beginPos, int endPos)
			throws AnalysisEngineProcessException {
		String text = jcas.getDocumentText().substring(beginPos, endPos);

		List<Token> tokens = null;
		try {
			tokens = tokenizer.tokenizeAndSort(text);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		Iterator<Token> tokenItr = tokens.iterator();
		while (tokenItr.hasNext()) {
         Token token = tokenItr.next();

			// convert token into JCas object
			BaseToken bta = TokenConverter.convert(token, jcas, beginPos);

			bta.setTokenNumber(tokenCount);

			// add JCas object to CAS index
			bta.addToIndexes();

			tokenCount++;
		}
	}
}