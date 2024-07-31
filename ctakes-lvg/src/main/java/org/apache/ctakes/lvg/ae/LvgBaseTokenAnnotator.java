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
package org.apache.ctakes.lvg.ae;

import gov.nih.nlm.nls.lvg.Api.LvgCmdApi;
import gov.nih.nlm.nls.lvg.Api.LvgLexItemApi;
import gov.nih.nlm.nls.lvg.Lib.Category;
import gov.nih.nlm.nls.lvg.Lib.LexItem;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.lvg.resource.LvgCmdApiResource;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.Lemma;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.*;

/**
 * UIMA annotator that uses the UMLS LVG package to find the canonical form of
 * BaseTokens. The package is also used to find one or more lemmas for a given
 * BaseToken along with its associated part of speech.
 * 
 * @author Mayo Clinic
 * 
 *         to do: what effect does using the cache have on words that may be
 *         misspelled. It seems that if you automatically normalize a word from
 *         the cache, this may be bad if it is misspelled in the case where the
 *         misspelling is a word in the lexicon.
 */
@PipeBitInfo(
		name = "LVG Basetoken Annotator",
		description = "Adds canonical form of Base Tokens.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class LvgBaseTokenAnnotator extends JCasAnnotator_ImplBase {
	/**
	 * Value is "PostLemmas". This parameter determines whether the feature
	 * lemmaEntries will be populated for word annotations.
	 */
	public static final String PARAM_POST_LEMMAS = "PostLemmas";
	/**
	 * Value is "UseLemmaCache". This parameter determines whether a cache will
	 * be used to improve performance of setting lemma entries.
	 */
	public static final String PARAM_USE_LEMMA_CACHE = "UseLemmaCache";
	/**
	 * Value is "LemmaCacheFileLocation". This parameter determines where the
	 * lemma cache is located.
	 */
	public static final String PARAM_LEMMA_CACHE_FILE_LOCATION = "LemmaCacheFileLocation";
	/**
	 * Value is "LemmaCacheFrequencyCutoff". This parameter sets a threshold for
	 * the frequency of a lemma to be loaded into the cache.
	 */
	public static final String PARAM_LEMMA_CACHE_FREQUENCY_CUTOFF = "LemmaCacheFrequencyCutoff";

	// LOG4J logger based on class name
	private final Logger LOGGER = LoggerFactory.getLogger( getClass().getName() );

	private LvgCmdApi lvgCmd;

	private LvgLexItemApi lvgLexItem;

	private UimaContext context;

	private boolean useSegments;

	private Set<String> skipSegmentsSet;

	private boolean useCmdCache;
	private String cmdCacheFileLocation;
	private int cmdCacheFreqCutoff;

	private Map<String,String> xeroxTreebankMap;

	private boolean postLemmas;
	private boolean useLemmaCache;
	private String lemmaCacheFileLocation;
	private int lemmaCacheFreqCutoff;

	// key = word, value = canonical word
	private Map<String,String> normCacheMap;

	// key = word, value = Set of Lemma objects
	private Map<String,Collection<LemmaLocalClass>> lemmaCacheMap;

	private Set<String> exclusionSet;

	/**
	 * Performs initialization logic. This implementation just reads values for
	 * the configuration parameters.
	 * 
//	 * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(AnnotatorContext)
	 */
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		context = aContext;
		configInit();

		try {
			String LVGCMDAPI_RESRC_KEY = "LvgCmdApi";
			LvgCmdApiResource lvgResource = (LvgCmdApiResource) context
					.getResourceObject( LVGCMDAPI_RESRC_KEY );

			if (lvgResource == null)
				throw new AnnotatorInitializationException(new Exception(
						"Unable to locate resource with key="
						+ LVGCMDAPI_RESRC_KEY + "."));

			lvgCmd = lvgResource.getLvg();

			if (useCmdCache) {
				LOGGER.info("Loading Cmd cache=" + cmdCacheFileLocation);
				loadCmdCacheFile(cmdCacheFileLocation);
				LOGGER.info("Loaded " + normCacheMap.size() + " entries");
			}

			if (postLemmas) {
				lvgLexItem = lvgResource.getLvgLex();
				if (useLemmaCache) {
					LOGGER.info("Loading Lemma cache=" + lemmaCacheFileLocation);
					loadLemmaCacheFile(lemmaCacheFileLocation);
					LOGGER.info("Loaded " + lemmaCacheMap.size() + " entries");
				}
			}

		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	/**
	 * Sets configuration parameters with values from the descriptor.
	 */
	private void configInit() throws ResourceInitializationException {
		useSegments = (Boolean) context.getConfigParameterValue( "UseSegments" );
		String[] skipSegmentIDs = (String[]) context
				.getConfigParameterValue("SegmentsToSkip");
		skipSegmentsSet = new HashSet<>();
		skipSegmentsSet.addAll( Arrays.asList( skipSegmentIDs ) );

		// Load Xerox Treebank tagset map
		String[] xtMaps = (String[]) context.getConfigParameterValue("XeroxTreebankMap");
		xeroxTreebankMap = new HashMap<>();
		for ( final String map : xtMaps ) {
			StringTokenizer tokenizer = new StringTokenizer( map, "|" );
			if ( tokenizer.countTokens() == 2 ) {
				String xTag = tokenizer.nextToken();
				String tTag = tokenizer.nextToken();
				xeroxTreebankMap.put( xTag, tTag );
			}
		}

		useCmdCache = (Boolean) context.getConfigParameterValue( "UseCmdCache" );

		cmdCacheFileLocation = (String) context.getConfigParameterValue("CmdCacheFileLocation");

		cmdCacheFreqCutoff = (Integer) context.getConfigParameterValue( "CmdCacheFrequencyCutoff" );

		String[] wordsToExclude = (String[]) context
				.getConfigParameterValue("ExclusionSet");
		exclusionSet = new HashSet<>();
		exclusionSet.addAll( Arrays.asList( wordsToExclude ) );

		Boolean bPostLemmas = (Boolean) context
				.getConfigParameterValue(PARAM_POST_LEMMAS);
		postLemmas = bPostLemmas != null && bPostLemmas;
		if (postLemmas) {
			Boolean useLemmaCache = (Boolean) context
					.getConfigParameterValue(PARAM_USE_LEMMA_CACHE);
			useLemmaCache = useLemmaCache != null && useLemmaCache;
			if (useLemmaCache) {
				lemmaCacheFileLocation = (String) context
						.getConfigParameterValue(PARAM_LEMMA_CACHE_FILE_LOCATION);
				if (lemmaCacheFileLocation == null)
					throw new ResourceInitializationException(new Exception(
							"Parameter for " + PARAM_LEMMA_CACHE_FILE_LOCATION
									+ " was not set."));
//				Integer lemmaCacheFreqCutoff = (Integer) context
//						.getConfigParameterValue(PARAM_LEMMA_CACHE_FREQUENCY_CUTOFF);
//				if (lemmaCacheFreqCutoff == null)
//					lemmaCacheFreqCutoff = 20;
			}
		}
	}

	/**
	 * Invokes this annotator's analysis logic.
	 */
	public void process(JCas jcas)
			throws AnalysisEngineProcessException {

		LOGGER.info(" process(JCas, ResultSpecification)");

		String text = jcas.getDocumentText();

		try {
			if (useSegments) {
				JFSIndexRepository indexes = jcas.getJFSIndexRepository();
				for ( final Annotation annotation : indexes.getAnnotationIndex( Segment.type ) ) {
					Segment segmentAnnotation = (Segment) annotation;
					String segmentID = segmentAnnotation.getId();

					if ( !skipSegmentsSet.contains( segmentID ) ) {
						int start = segmentAnnotation.getBegin();
						int end = segmentAnnotation.getEnd();
						annotateRange( jcas, text, start, end );
					}
				}
			} else {
				// annotate over full doc text
				annotateRange(jcas, text, 0, text.length());
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

	}

	/**
	 * A utility method that annotates a given range.
	 */
	protected void annotateRange(JCas jcas, String text, int rangeBegin,
			int rangeEnd) throws AnalysisEngineProcessException {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		for ( final Annotation annotation : indexes.getAnnotationIndex( BaseToken.type ) ) {
			BaseToken tokenAnnotation = (BaseToken) annotation;
			if ( tokenAnnotation.getBegin() >= rangeBegin
				  && tokenAnnotation.getEnd() <= rangeEnd ) {
				String token = text.substring( tokenAnnotation.getBegin(),
														 tokenAnnotation.getEnd() );

				// skip past words that are part of the exclusion set
				if ( exclusionSet.contains( token ) )
					continue;

				setNormalizedForm( tokenAnnotation, token );
				if ( postLemmas )
					setLemma( tokenAnnotation, token, jcas );
			}
		}
	}

	private void setNormalizedForm(BaseToken tokenAnnotation, String token)
			throws AnalysisEngineProcessException {
		// apply LVG processing to get canonical form
		String normalizedForm = null;
		if (useCmdCache) {
			normalizedForm = normCacheMap.get(token);
//			if (normalizedForm == null) {
				// LOGGER.info("["+ word+ "] was not found in LVG norm cache.");
//			}
		}

		// only apply LVG processing if not found in cache first
		if (normalizedForm == null) {
			try {
				String out = lvgCmd.MutateToString(token);

				String[] output = out.split("\\|");

				if ((output != null) && (output.length >= 2)
						&& (!output[1].matches("No Output"))) {
					normalizedForm = output[1];
				}
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}

		}

		if (normalizedForm != null) {
			tokenAnnotation.setNormalizedForm(normalizedForm);
		}
	}

	private void setLemma(BaseToken wordAnnotation, String word, JCas jcas)
			throws AnalysisEngineProcessException {
		// apply LVG processing to get lemmas
		// key = lemma string, value = Set of POS tags
		Map<String,Collection<String>> lemmaMap = null;

		if (useLemmaCache) {
			Collection<LemmaLocalClass> lemmaSet = lemmaCacheMap.get(word);
			if (lemmaSet == null) {
				// LOGGER.info("["+ word+
				// "] was not found in LVG lemma cache.");
			} else {
				lemmaMap = new HashMap<>();
				for ( final LemmaLocalClass l : lemmaSet ) {
					lemmaMap.put( l.word, l.posSet );
				}
			}
		}

		if (lemmaMap == null) {
			lemmaMap = new HashMap<>();
			try {
				Vector<LexItem> lexItems = lvgLexItem.MutateLexItem( word );
				for ( final LexItem item : lexItems ) {
					Category c = item.GetTargetCategory();
					String lemmaStr = item.GetTargetTerm();
					long[] bitValues = Category.ToValuesArray( c.GetValue() );
					for ( final long value : bitValues ) {
						// note that POS is Xerox tagset
						String lemmaPos = Category.ToName( value );
						// convert Xerox tagset to PennTreebank tagset
						String treebankTag = xeroxTreebankMap
								.get( lemmaPos );
						if ( treebankTag != null ) {
							Collection<String> posSet = null;
							if ( lemmaMap.containsKey( lemmaStr ) ) {
								posSet = lemmaMap.get( lemmaStr );
							} else {
								posSet = new HashSet<>();
							}
							posSet.add( treebankTag );
							lemmaMap.put( lemmaStr, posSet );
						}
					}
				}
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}

		// add lemma information to CAS
		// FSArray lemmas = new FSArray(jcas, lemmaMap.keySet().size());
		Collection<Lemma> lemmas = new ArrayList<>(lemmaMap.keySet().size());

		for ( final String form : lemmaMap.keySet() ) {
			Collection<String> posTagSet = lemmaMap.get( form );
			for ( final String pos : posTagSet ) {
				Lemma lemma = new Lemma( jcas );
				lemma.setKey( form );
				lemma.setPosTag( pos );
				lemmas.add( lemma );
			}
		}
		Lemma[] lemmaArray = lemmas.toArray( new Lemma[ 0 ] );
		FSList fsList = ListFactory.buildList(jcas, lemmaArray);
		wordAnnotation.setLemmaEntries(fsList);
	}

	/**
	 * Helper method that loads a Norm cache file.
	 * 
	 * @param cpLocation -
	 */
	private void loadCmdCacheFile(String cpLocation)
			throws FileNotFoundException, IOException {
		InputStream inStream = getClass().getResourceAsStream(cpLocation);
		if (inStream == null) {
			throw new FileNotFoundException("Unable to find: " + cpLocation);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));

		// initialize map
		normCacheMap = new HashMap<>();

		String line = br.readLine();
		while (line != null) {
			StringTokenizer st = new StringTokenizer(line, "|");
			if (st.countTokens() == 7) {
				int freq = Integer.parseInt(st.nextToken());
				if (freq > cmdCacheFreqCutoff) {
					String origWord = st.nextToken();
					String normWord = st.nextToken();
					if (!normCacheMap.containsKey(origWord)) {
						// if there are duplicates, then only have the first
						// occurrence in the map
						normCacheMap.put(origWord, normWord);
					}
				} else {
					LOGGER.debug("Discarding norm cache line due to frequency cutoff: "
							+ line);
				}
			} else {
				LOGGER.warn("Invalid LVG norm cache " + "line: " + line);
			}
			line = br.readLine();
		}
	}

	/**
	 * Helper method that loads a Lemma cache file.
	 * 
	 * @param cpLocation -
	 */
	private void loadLemmaCacheFile(String cpLocation)
			throws FileNotFoundException, IOException {
		InputStream inStream = getClass().getResourceAsStream(cpLocation);
		if (inStream == null) {
			throw new FileNotFoundException("Unable to find: " + cpLocation);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));

		// initialize map
		lemmaCacheMap = new HashMap<>();

		String line = br.readLine();
		while (line != null) {
			StringTokenizer st = new StringTokenizer(line, "|");
			if (st.countTokens() == 4) // JZ: changed from 7 to 4 as used a new
										// dictionary
			{
				int freq = Integer.parseInt(st.nextToken());
				if (freq > lemmaCacheFreqCutoff) {
					String origWord = st.nextToken();
					String lemmaWord = st.nextToken();
					String combinedCategories = st.nextToken();

					// strip < and > chars
					combinedCategories = combinedCategories.substring(1,
							combinedCategories.length() - 1);

					// construct Lemma object
					LemmaLocalClass l = new LemmaLocalClass();
					l.word = lemmaWord;
					l.posSet = new HashSet<>();
					long bitVector = Category.ToValue(combinedCategories);
					long[] bitValues = Category.ToValuesArray(bitVector);
					for ( final long value : bitValues ) {
						String pos = Category.ToName( value );
						// convert Xerox tag into Treebank
						String treebankTag = xeroxTreebankMap.get( pos );
						if ( treebankTag != null ) {
							l.posSet.add( treebankTag );
						}
					}

					// add Lemma to cache map
					Collection<LemmaLocalClass> lemmaSet = null;
					if (!lemmaCacheMap.containsKey(origWord)) {
						lemmaSet = new HashSet<>();
					} else {
						lemmaSet = lemmaCacheMap.get(origWord);
					}
					lemmaSet.add(l);
					lemmaCacheMap.put(origWord, lemmaSet);
				} else {
					LOGGER.debug("Discarding lemma cache line due to frequency cutoff: "
							+ line);
				}
			} else {
				LOGGER.warn("Invalid LVG lemma cache " + "line: " + line);
			}
			line = br.readLine();
		}
	}

	/**
	 * Basic class to group a lemma word with its various parts of speech.
	 * 
	 * @author Mayo Clinic
	 */
	static class LemmaLocalClass {
		public String word;

		public Collection<String> posSet;
	}

}