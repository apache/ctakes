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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.lvg.resource.LvgCmdApiResource;
import org.apache.ctakes.lvg.resource.LvgCmdApiResourceImpl;
import org.apache.ctakes.typesystem.type.syntax.Lemma;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;

/**
 * UIMA annotator that uses the UMLS LVG package to find the canonical form of
 * WordTokens. The package is also used to find one or more lemmas for a given
 * WordToken along with its associated part of speech.
 * 
 * @author Mayo Clinic
 * 
 *         to do: what effect does using the cache have on words that may be
 *         misspelled. It seems that if you automatically normalize a word from
 *         the cache, this may be bad if it is misspelled in the case where the
 *         misspelling is a word in the lexicon.
 */
@PipeBitInfo(
		name = "LVG Annotator",
		description = "Adds cononical form of words.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class LvgAnnotator extends JCasAnnotator_ImplBase {
  public static final String[] defaultExclusionWords = {"And", "and", "By", "by", "For", "for", "In", "in", "Of", "of", "On", "on", "The", "the", "To", "to", "With", "with"};
  public static final String[] defaultTreebankMap = {"adj|JJ", "adv|RB", "aux|AUX", "compl|CS", "conj|CC", "det|DET", "modal|MD", "noun|NN", "prep|IN", "pron|PRP", "verb|VB"};

  /**
	 * Value is "PostLemmas". This parameter determines whether the feature
	 * lemmaEntries will be populated for word annotations.
	 */
	public static final String PARAM_POST_LEMMAS = "PostLemmas";
	@ConfigurationParameter(
	    name = PARAM_POST_LEMMAS,
	    mandatory = false,
	    defaultValue =  "false",
	    description = "Whether to extract the lexical variants and write to cas (creates large files)"
	    )
  private boolean postLemmas;

	/**
	 * Value is "UseLemmaCache". This parameter determines whether a cache will
	 * be used to improve performance of setting lemma entries.
	 */
	public static final String PARAM_USE_LEMMA_CACHE = "UseLemmaCache";
	@ConfigurationParameter(
	    name = PARAM_USE_LEMMA_CACHE,
	    mandatory = false,
	    defaultValue = "false",
	    description = "Whether to use a cache for lemmas"
	    )
  private boolean useLemmaCache;

	/**
	 * Value is "LemmaCacheFileLocation". This parameter determines where the
	 * lemma cache is located.
	 */
	public static final String PARAM_LEMMA_CACHE_FILE_LOCATION = "LemmaCacheFileLocation";
	@ConfigurationParameter(
	    name = PARAM_LEMMA_CACHE_FILE_LOCATION,
	    mandatory = false,
	    defaultValue = "org/apache/ctakes/lvg/2005_lemma.voc",
	    description = "Path to lemma cache file -- if useLemmaCache and postLemmas are true"
	    )
  private String lemmaCacheFileLocation=null;
	
	/**
	 * Value is "LemmaCacheFrequencyCutoff". This parameter sets a threshold for
	 * the frequency of a lemma to be loaded into the cache.
	 */
	public static final String PARAM_LEMMA_CACHE_FREQUENCY_CUTOFF = "LemmaCacheFrequencyCutoff";
	@ConfigurationParameter(
	    name = PARAM_LEMMA_CACHE_FREQUENCY_CUTOFF,
	    mandatory = false,
	    description = "Threshold for the frequency of a lemma to be loaded into the cache",
	    defaultValue = "20"
	    )
  private int cmdCacheFreqCutoff;

	public static final String PARAM_USE_SEGMENTS = "UseSegments";
	@ConfigurationParameter(
	    name = PARAM_USE_SEGMENTS,
	    mandatory = false,
	    defaultValue = "false",
	    description = "Whether to use segments found in upstream cTAKES components"
	    )
	private boolean useSegments;

	public static final String PARAM_SKIP_SEGMENTS = "SegmentsToSkip";
	@ConfigurationParameter(
	    name = PARAM_SKIP_SEGMENTS,
	    mandatory = false,
	    defaultValue = {},
	    description = "Segment IDs to skip during processing"
	    )
  private String[] skipSegmentIDs;
  private Set<String> skipSegmentsSet;
	
	public static final String PARAM_XT_MAP = "XeroxTreebankMap";
	@ConfigurationParameter(
	    name = PARAM_XT_MAP,
	    mandatory = false,
	    description = "Mapping from Xerox parts of speech to Treebank equivalents"
	    )
	private String[] xtMaps = defaultTreebankMap;
  private Map<String, String> xeroxTreebankMap;
	
	public static final String PARAM_USE_CMD_CACHE = "UseCmdCache";
	@ConfigurationParameter(
	    name = PARAM_USE_CMD_CACHE,
	    mandatory = false,
	    defaultValue = "false",
	    description = "Use cache to track canonical forms"
	    )
  private boolean useCmdCache;

	public static final String PARAM_CMD_CACHE_FILE = "CmdCacheFileLocation";
	@ConfigurationParameter(
	    name = PARAM_CMD_CACHE_FILE,
	    mandatory = false,
	    defaultValue = "org/apache/ctakes/lvg/2005_norm.voc",
	    description = "File with stored cache of canonical forms"
	    )
  private String cmdCacheFileLocation;

	public static final String PARAM_LEMMA_FREQ_CUTOFF = "CmdCacheFrequencyCutoff";
	@ConfigurationParameter(
	    name = PARAM_LEMMA_FREQ_CUTOFF,
	    mandatory = false,
	    description = "Minimum frequency required for loading from cache",
	    defaultValue = "20"
	    )
  private int lemmaCacheFreqCutoff;

	public static final String PARAM_EXCLUSION_WORDS = "ExclusionSet";
	@ConfigurationParameter(
	    name = PARAM_EXCLUSION_WORDS,
	    mandatory = false,
	    description = "Words to exclude when doing LVG normalization"
	    )
	private String[] wordsToExclude = defaultExclusionWords;
  private Set<String> exclusionSet;
  
	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	public static final String PARAM_LVGCMDAPI_RESRC_KEY = "LvgCmdApi";
  @ExternalResource(
      key = PARAM_LVGCMDAPI_RESRC_KEY,
      mandatory = true
      )
  private LvgCmdApiResource lvgResource;
      
	private LvgCmdApi lvgCmd;

	private LvgLexItemApi lvgLexItem;

	// key = word, value = canonical word
	private Map<String, String> normCacheMap;

	// key = word, value = Set of Lemma objects
	private Map<String, Set<LemmaLocalClass>> lemmaCacheMap;


	/**
	 * Performs initialization logic. This implementation just reads values for
	 * the configuration parameters.
	 * 
	 * @see JCasAnnotator_ImplBase#initialize(UimaContext)
	 */
	@Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		configInit();
		try {
			lvgCmd = lvgResource.getLvg();

			if (useCmdCache) {
				logger.info("Loading Cmd cache=" + cmdCacheFileLocation);
				loadCmdCacheFile(cmdCacheFileLocation);
				logger.info("Loaded " + normCacheMap.size() + " entries");
			}

			if (postLemmas) {
				lvgLexItem = lvgResource.getLvgLex();
				if (useLemmaCache) {
					logger.info("Loading Lemma cache=" + lemmaCacheFileLocation);
					loadLemmaCacheFile(lemmaCacheFileLocation);
					logger.info("Loaded " + lemmaCacheMap.size() + " entries");
				}
			}
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
	}

	/**
	 * Sets configuration parameters with values from the descriptor.
	 */
	private void configInit() {
		skipSegmentsSet = new HashSet<>();
		for (int i = 0; i < skipSegmentIDs.length; i++) {
			skipSegmentsSet.add(skipSegmentIDs[i]);
		}

		// Load Xerox Treebank tagset map
		xeroxTreebankMap = new HashMap<>();
		for (int i = 0; i < xtMaps.length; i++) {
			StringTokenizer tokenizer = new StringTokenizer(xtMaps[i], "|");
			if (tokenizer.countTokens() == 2) {
				String xTag = tokenizer.nextToken();
				String tTag = tokenizer.nextToken();
				xeroxTreebankMap.put(xTag, tTag);
			}
		}

		exclusionSet = new HashSet<>();
		for (int i = 0; i < wordsToExclude.length; i++) {
			exclusionSet.add(wordsToExclude[i]);
		}
	}

	/**
	 * Invokes this annotator's analysis logic.
	 */
	@Override
  public void process(JCas jcas)
			throws AnalysisEngineProcessException {

		logger.info("process(JCas)");

		String text = jcas.getDocumentText();

		try {
			if (useSegments) {
				JFSIndexRepository indexes = jcas.getJFSIndexRepository();
				Iterator<?> segmentItr = indexes.getAnnotationIndex(Segment.type)
						.iterator();
				while (segmentItr.hasNext()) {
					Segment segmentAnnotation = (Segment) segmentItr.next();
					String segmentID = segmentAnnotation.getId();

					if (!skipSegmentsSet.contains(segmentID)) {
						int start = segmentAnnotation.getBegin();
						int end = segmentAnnotation.getEnd();
						annotateRange(jcas, text, start, end);
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
			int rangeEnd)
			throws AnalysisEngineProcessException {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> wordItr = indexes.getAnnotationIndex(WordToken.type)
				.iterator();
		while (wordItr.hasNext()) {
			WordToken wordAnnotation = (WordToken) wordItr.next();
			if (wordAnnotation.getBegin() >= rangeBegin
					&& wordAnnotation.getEnd() <= rangeEnd) {
				String word = text.substring(wordAnnotation.getBegin(),
						wordAnnotation.getEnd());

				// if the original word was misspelled, use the spell correction
				String suggestion = wordAnnotation.getSuggestion();

				if ((suggestion != null) && (suggestion.length() > 0)) {
					word = suggestion;
				}

				// skip past words that are part of the exclusion set
				if (exclusionSet.contains(word))
					continue;

				setCanonicalForm(wordAnnotation, word);
				if (postLemmas)
					setLemma(wordAnnotation, word, jcas);
			}
		}
	}

	private void setCanonicalForm(WordToken wordAnnotation, String word)
			throws AnalysisEngineProcessException {
		// apply LVG processing to get canonical form
		String canonicalForm = null;
		if (useCmdCache) {
			canonicalForm = normCacheMap.get(word);
			if (canonicalForm == null) {
				// logger.info("["+ word+ "] was not found in LVG norm cache.");
			}
		}

		// only apply LVG processing if not found in cache first
		if (canonicalForm == null) {
			try {
				String out = lvgCmd.MutateToString(word);

				String[] output = out.split("\\|");

				if ((output != null) && (output.length >= 2)
						&& (!output[1].matches("No Output"))) {
					canonicalForm = output[1];
				}
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}

		}

		if (canonicalForm != null) {
			wordAnnotation.setCanonicalForm(canonicalForm);
		}
	}

	private void setLemma(WordToken wordAnnotation, String word, JCas jcas)
			throws AnalysisEngineProcessException {
		// apply LVG processing to get lemmas
		// key = lemma string, value = Set of POS tags
		Map<String, Set<String>> lemmaMap = null;

		if (useLemmaCache) {
			Set<?> lemmaSet = lemmaCacheMap.get(word);
			if (lemmaSet == null) {
				// logger.info("["+ word+
				// "] was not found in LVG lemma cache.");
			} else {
				lemmaMap = new HashMap<>();
				Iterator<?> lemmaItr = lemmaSet.iterator();
				while (lemmaItr.hasNext()) {
					LemmaLocalClass l = (LemmaLocalClass) lemmaItr.next();
					lemmaMap.put(l.word, l.posSet);
				}
			}
		}

		if (lemmaMap == null) {
			lemmaMap = new HashMap<>();
			try {
				Vector<?> lexItems = lvgLexItem.MutateLexItem(word);
				Iterator<?> lexItemItr = lexItems.iterator();
				while (lexItemItr.hasNext()) {
					LexItem li = (LexItem) lexItemItr.next();

					Category c = li.GetTargetCategory();
					String lemmaStr = li.GetTargetTerm();
					long[] bitValues = Category.ToValuesArray(c.GetValue());
					for (int i = 0; i < bitValues.length; i++) {
						// note that POS is Xerox tagset
						String lemmaPos = Category.ToName(bitValues[i]);
						// convert Xerox tagset to PennTreebank tagset
						String treebankTag = xeroxTreebankMap
								.get(lemmaPos);
						if (treebankTag != null) {
							Set<String> posSet = null;
							if (lemmaMap.containsKey(lemmaStr)) {
								posSet = lemmaMap.get(lemmaStr);
							} else {
								posSet = new HashSet<>();
							}
							posSet.add(treebankTag);
							lemmaMap.put(lemmaStr, posSet);
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

		Iterator<String> lemmaStrItr = lemmaMap.keySet().iterator();
		while (lemmaStrItr.hasNext()) {
			String form = lemmaStrItr.next();
			Set<?> posTagSet = lemmaMap.get(form);
			Iterator<?> posTagItr = posTagSet.iterator();
			while (posTagItr.hasNext()) {
				String pos = (String) posTagItr.next(); // part of speech
				Lemma lemma = new Lemma(jcas);
				lemma.setKey(form);
				lemma.setPosTag(pos);
				lemmas.add(lemma);
			}
		}
		Lemma[] lemmaArray = lemmas.toArray(new Lemma[lemmas.size()]);
		FSList fsList = ListFactory.buildList(jcas, lemmaArray);
		wordAnnotation.setLemmaEntries(fsList);
	}

	/**
	 * Helper method that loads a Norm cache file.
	 * 
	 * @param location
	 */
	private void loadCmdCacheFile(String cpLocation)
			throws FileNotFoundException, IOException {
	  try(
	    InputStream inStream = getClass().getResourceAsStream(cpLocation);
	    BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
	  ){
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
	          logger.debug("Discarding norm cache line due to frequency cutoff: "
	              + line);
	        }
	      } else {
	        logger.warn("Invalid LVG norm cache " + "line: " + line);
	      }
	      line = br.readLine();
	    }
	  }
	}

	/**
	 * Helper method that loads a Lemma cache file.
	 * 
	 * @param location
	 */
	private void loadLemmaCacheFile(String cpLocation)
			throws FileNotFoundException, IOException {
		try(
	    InputStream inStream = getClass().getResourceAsStream(cpLocation);
	    BufferedReader br = new BufferedReader(new InputStreamReader(inStream)); 
		){
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
		        for (int i = 0; i < bitValues.length; i++) {
		          String pos = Category.ToName(bitValues[i]);
		          // convert Xerox tag into Treebank
		          String treebankTag = xeroxTreebankMap.get(pos);
		          if (treebankTag != null) {
		            l.posSet.add(treebankTag);
		          }
		        }

		        // add Lemma to cache map
		        Set<LemmaLocalClass> lemmaSet = null;
		        if (!lemmaCacheMap.containsKey(origWord)) {
		          lemmaSet = new HashSet<>();
		        } else {
		          lemmaSet = lemmaCacheMap.get(origWord);
		        }
		        lemmaSet.add(l);
		        lemmaCacheMap.put(origWord, lemmaSet);
		      } else {
		        logger.debug("Discarding lemma cache line due to frequency cutoff: "
		            + line);
		      }
		    } else {
		      logger.warn("Invalid LVG lemma cache " + "line: " + line);
		    }
		    line = br.readLine();
		  }
		}
	}


  /**
   * Copy to under /tmp/ (or some other specified directory) the files needed for EventAnnotatorTest and anyone else 
   * using UIMAfit to create a pipeline.
   * Localize to this method all hardcoded file names and subdirs related to copying files to under /tmp (or C:\tmp).
   * @param absolutePath - Where to copy the LVG data/config subtree. Typically "/tmp/".
   * @return The full path to the copy of lvg.properties file
   */
  public static String copyLvgFiles(String absolutePath) {

	final String returnValue = "/tmp/data/config/lvg.properties";
	final String prefix = "org/apache/ctakes/lvg/";
	final String [] filesToCopy = { 
			"data/config/lvg.properties",
			"data/HSqlDb/lvg2008.backup",
			"data/HSqlDb/lvg2008.data",
			"data/HSqlDb/lvg2008.properties",
			"data/HSqlDb/lvg2008.script",
            "data/misc/conjunctionWord.data",
            "data/misc/nonInfoWords.data",
            "data/misc/removeS.data",
            "data/misc/stopWords.data",
            "data/misc/symbolSynonyms.data",
            "data/rules/dm.rul",
            "data/rules/im.rul",
            "data/rules/plural.rul",
            "data/rules/verbinfl.rul",
            "data/rules/exceptionD.data",
            "data/rules/exceptionI.data",
            "data/rules/ruleD.data",
            "data/rules/ruleI.data",
            "data/rules/trieD.data",
            "data/rules/trieI.data",
            "data/Unicode/diacriticMap.data",
            "data/Unicode/ligatureMap.data",
            "data/Unicode/nonStripMap.data",
            "data/Unicode/synonymMap.data",
            "data/Unicode/symbolMap.data",
            "data/Unicode/unicodeMap.data",
            
	};
	
	for (String path:filesToCopy) {
		InputStream stream =  LvgAnnotator.class.getClassLoader().getResourceAsStream(prefix+path);
		
		File file = new File(absolutePath, path);
		Logger logger = Logger.getLogger(LvgAnnotator.class.getName());
		logger.info("Copying lvg-related file to " + file.getAbsolutePath());

		try {
	        FileUtils.copyInputStreamToFile(stream, file);
		} catch (IOException e) {
	        throw new RuntimeException("Error copying temporary InpuStream " + stream.toString() + " to " + file.getAbsolutePath() + ".", e);
		}
		
	}
	
    return returnValue;
    
  }
  
  
  public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException, MalformedURLException {
	
    // Here if a pipeline is run from source, for example in Eclipse using a Run Configuration for project ctakes-clinical-pipeline,  
	// the cwd might be, for example, C:\workspaces\cTAKES\ctakes\ctakes-clinical-pipeline
	// Therefore we can no longer let LvgCmdApiResourceImpl use the current working directory to look for 
	// the lvg properties file or the lvg resources (plural.rul etc.)
	// Instead we use getResource to find the URL for the lvg.properties file. 
	final String lvgProperties = "org/apache/ctakes/lvg/data/config/lvg.properties";
	Logger logger = Logger.getLogger(LvgAnnotator.class.getName());
	java.net.URL url = LvgAnnotator.class.getClassLoader().getResource(lvgProperties);
	if (url!=null) {
		logger.info("URL for lvg.properties =" + url.getFile());
	} else {
		String absolutePath = "/tmp/";
		logger.info("URL==null");
		logger.info("Unable to find " + lvgProperties + ".");
		logger.info("Copying files and directories to under " + absolutePath);
	    File lvgFile = new File(copyLvgFiles(absolutePath));
	    url = lvgFile.toURI().toURL();
	}
	

    return AnalysisEngineFactory.createEngineDescription(LvgAnnotator.class,
        LvgAnnotator.PARAM_USE_CMD_CACHE,
        false,
        LvgAnnotator.PARAM_USE_LEMMA_CACHE,
        false,
        LvgAnnotator.PARAM_USE_SEGMENTS,
        false,
        LvgAnnotator.PARAM_LEMMA_CACHE_FREQUENCY_CUTOFF,
        20,
        LvgAnnotator.PARAM_LEMMA_FREQ_CUTOFF,
        20,
        LvgAnnotator.PARAM_POST_LEMMAS,
        false,
        LvgAnnotator.PARAM_LVGCMDAPI_RESRC_KEY,
        ExternalResourceFactory.createExternalResourceDescription(
            LvgCmdApiResourceImpl.class, url));
	}
	
	/**
	 * Basic class to group a lemma word with its various parts of speech.
	 * 
	 * @author Mayo Clinic
	 */
	class LemmaLocalClass {
		public String word;

		public Set<String> posSet;
	}

}