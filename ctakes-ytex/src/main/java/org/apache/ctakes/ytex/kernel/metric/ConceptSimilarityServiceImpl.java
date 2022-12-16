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
package org.apache.ctakes.ytex.kernel.metric;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.ImputedFeatureEvaluator;
import org.apache.ctakes.ytex.kernel.InfoContentEvaluator;
import org.apache.ctakes.ytex.kernel.IntrinsicInfoContentEvaluator;
import org.apache.ctakes.ytex.kernel.OrderedPair;
import org.apache.ctakes.ytex.kernel.SimSvcContextHolder;
import org.apache.ctakes.ytex.kernel.dao.ClassifierEvaluationDao;
import org.apache.ctakes.ytex.kernel.dao.ConceptDao;
import org.apache.ctakes.ytex.kernel.model.ConcRel;
import org.apache.ctakes.ytex.kernel.model.ConceptGraph;
import org.apache.ctakes.ytex.kernel.model.FeatureRank;
import org.apache.ctakes.ytex.kernel.pagerank.PageRankService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.ImmutableMap;

/**
 * compute concept similarity
 * 
 * @author vijay
 * 
 */
public class ConceptSimilarityServiceImpl implements ConceptSimilarityService {
	private static final Log log = LogFactory
			.getLog(ConceptSimilarityServiceImpl.class);

	private static String formatPaths(List<LCSPath> lcsPaths) {
		StringBuilder b = new StringBuilder();
		Iterator<LCSPath> lcsPathIter = lcsPaths.iterator();
		while (lcsPathIter.hasNext()) {
			LCSPath lcsPath = lcsPathIter.next();
			String lcs = lcsPath.getLcs();
			b.append(lcs);
			b.append("=");
			b.append(lcsPath.toString());
			if (lcsPathIter.hasNext())
				b.append("|");
		}
		return b.toString();
	}

	@SuppressWarnings("static-access")
	public static void main(String args[]) throws IOException {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("concepts")
				.hasArg()
				.withDescription(
						"concept pairs or a file containing concept pairs.  To specify pairs on command line, separate concepts by comma, concept pairs by semicolon.  For file, separate concepts by comma or tab, each concept pair on a new line.")
				.isRequired(true).create("concepts"));
		options.addOption(OptionBuilder
				.withArgName("metrics")
				.hasArg()
				.withDescription(
						"comma-separated list of metrics.  Valid metrics: "
								+ Arrays.asList(SimilarityMetricEnum.values()))
				.isRequired(true).create("metrics"));
		options.addOption(OptionBuilder
				.withArgName("out")
				.hasArg()
				.withDescription(
						"file to write oputput to.  if not specified, output sent to stdout.")
				.create("out"));
		options.addOption(OptionBuilder.withArgName("lcs")
				.withDescription("output lcs and path for each concept pair")
				.create("lcs"));
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			String concepts = line.getOptionValue("concepts");
			String metrics = line.getOptionValue("metrics");
			String out = line.getOptionValue("out");
			boolean lcs = line.hasOption("lcs");
			PrintStream os = null;
			try {
				if (out != null) {
					os = new PrintStream(new BufferedOutputStream(
							new FileOutputStream(out)));
				} else {
					os = System.out;
				}
				List<ConceptPair> conceptPairs = parseConcepts(concepts);
				List<SimilarityMetricEnum> metricList = parseMetrics(metrics);
				ConceptSimilarityService simSvc = SimSvcContextHolder
						.getApplicationContext().getBean(
								ConceptSimilarityService.class);
				List<SimilarityInfo> simInfos = lcs ? new ArrayList<SimilarityInfo>(
						conceptPairs.size()) : null;
				List<ConceptPairSimilarity> conceptSimMap = simSvc.similarity(
						conceptPairs, metricList, null, lcs);
				printSimilarities(conceptPairs, conceptSimMap, metricList,
						simInfos, lcs, os);
				// try {
				// Thread.sleep(60*1000);
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
			} finally {
				if (out != null) {
					try {
						os.close();
					} catch (Exception e) {
					}
				}
			}
		} catch (ParseException pe) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"java " + ConceptSimilarityServiceImpl.class.getName()
							+ " get concept similiarity", options);
		}
	}

	private static List<ConceptPair> parseConcepts(String concepts)
			throws IOException {
		BufferedReader r = null;
		try {
			List<ConceptPair> conceptPairs = new ArrayList<ConceptPair>();
			File f = new File(concepts);
			if (f.exists()) {
				r = new BufferedReader(new FileReader(f));
			} else {
				r = new BufferedReader(new StringReader(concepts));
			}
			String line = null;
			while ((line = r.readLine()) != null) {
				// for command line, split pairs by semicolon
				String lines[] = line.split(";");
				for (String subline : lines) {
					String pair[] = subline.split(",|\\t");
					if (pair.length != 2) {
						System.err.println("cannot parse concept pair: "
								+ subline);
					} else {
						conceptPairs.add(new ConceptPair(pair[0], pair[1]));
					}
				}
			}
			return conceptPairs;
		} finally {
			if (r != null)
				r.close();
		}
	}

	private static List<SimilarityMetricEnum> parseMetrics(String metrics) {
		String ms[] = metrics.split(",");
		List<SimilarityMetricEnum> metricSet = new ArrayList<SimilarityMetricEnum>();
		for (String metric : ms) {
			SimilarityMetricEnum m = SimilarityMetricEnum.valueOf(metric);
			if (m == null)
				System.err.println("invalid metric: " + ms);
			else
				metricSet.add(m);
		}
		return metricSet;
	}

	private static void printSimilarities(List<ConceptPair> conceptPairs,
			List<ConceptPairSimilarity> conceptSimList,
			List<SimilarityMetricEnum> metricList,
			List<SimilarityInfo> simInfos, boolean lcs, PrintStream os) {
		// print header
		os.print("Concept 1\tConcept 2");
		for (SimilarityMetricEnum metric : metricList) {
			os.print("\t");
			os.print(metric);
		}
		if (lcs) {
			os.print("\tlcs(s)\tcorpus lcs\tintrinsic lcs\tpaths");
		}
		os.println();
		// print content
		for (ConceptPairSimilarity csim : conceptSimList) {
			ConceptPair p = csim.getConceptPair();
			os.print(p.getConcept1());
			os.print("\t");
			os.print(p.getConcept2());
			for (Double sim : csim.getSimilarities()) {
				os.print("\t");
				if (sim != null)
					os.print(String.format("%6f", sim));
				else
					os.print(0d);
			}
			if (lcs) {
				SimilarityInfo simInfo = csim.getSimilarityInfo();
				os.print("\t");
				Iterator<String> lcsIter = simInfo.getLcses().iterator();
				while (lcsIter.hasNext()) {
					os.print(lcsIter.next());
					if (lcsIter.hasNext())
						os.print('|');
				}
				os.print("\t");
				os.print(simInfo.getCorpusLcs() == null ? "" : simInfo
						.getCorpusLcs());
				os.print("\t");
				os.print(simInfo.getIntrinsicLcs() == null ? "" : simInfo
						.getIntrinsicLcs());
				os.print("\t");
				os.print(formatPaths(simInfo.getLcsPaths()));
			}
			os.println();
		}
	}

	private CacheManager cacheManager;

	private ConceptGraph cg = null;

	private ClassifierEvaluationDao classifierEvaluationDao;

	private ConceptDao conceptDao;
	private String conceptGraphName;

	private String conceptSetName;

	// /**
	// * information concept cache
	// */
	// private Map<String, Double> corpusICMap = null;

	private String corpusName;

	private Map<String, BitSet> cuiTuiMap;

	// private Map<String, ConceptInfo> conceptInfoMap = null;
	// private ConceptInfo[] conceptInfoCache;

	/**
	 * cache to hold lcs's
	 */
	private Cache lcsCache;
	private String lcsImputedType = ImputedFeatureEvaluator.MeasureType.INFOGAIN
			.getName();

	private PageRankService pageRankService;

	private boolean preload = true;
	private Map<String, Double> corpusICMap;

	private Map<SimilarityMetricEnum, SimilarityMetric> similarityMetricMap = null;
	private PlatformTransactionManager transactionManager;

	private List<String> tuiList;

	private void addCuiTuiToMap(Map<String, Set<String>> cuiTuiMap,
			Map<String, String> tuiMap, String cui, String tui) {
		// get 'the' tui string
		if (tuiMap.containsKey(tui))
			tui = tuiMap.get(tui);
		else
			tuiMap.put(tui, tui);
		Set<String> tuis = cuiTuiMap.get(cui);
		if (tuis == null) {
			tuis = new HashSet<String>();
			cuiTuiMap.put(cui, tuis);
		}
		tuis.add(tui);
	}

	@Override
	public Object[] getBestLCS(Set<String> lcses, boolean intrinsicIC,
			Map<String, Double> conceptFilter) {
		Map<String, Double> lcsICMap = new HashMap<String, Double>(lcses.size());
		// if (isPreload()) {
		// look in conceptInfoMap for info content
		for (String lcs : lcses) {
			lcsICMap.put(lcs, getIC(lcs, intrinsicIC));
			// }
			// } else {
			// // load info content on demand
			// Map<String, FeatureRank> frMap = getICOnDemand(lcses,
			// intrinsicIC);
			// for (Map.Entry<String, FeatureRank> frMapEntry :
			// frMap.entrySet()) {
			// lcsICMap.put(frMapEntry.getKey(), frMapEntry.getValue()
			// .getEvaluation());
			// }
		}
		if (conceptFilter != null) {
			double currentBest = -1;
			Set<String> bestLcses = new HashSet<String>();
			for (String lcs : lcses) {
				if (conceptFilter.containsKey(lcs)) {
					double lcsEval = conceptFilter.get(lcs);
					if (currentBest == -1 || lcsEval > currentBest) {
						bestLcses.clear();
						bestLcses.add(lcs);
						currentBest = lcsEval;
					} else if (currentBest == lcsEval) {
						bestLcses.add(lcs);
					}
				}
			}
			if (currentBest < 0)
				currentBest = 0d;
			if (bestLcses.size() > 0) {
				return this.getBestLCS(bestLcses, lcsICMap);
			} else {
				// no lcses made the cut
				return null;
			}
		} else {
			// unfiltered - get the lowest ic
			return this.getBestLCS(lcses, lcsICMap);
		}
	}

	public Object[] getBestLCS(Set<String> lcses, Map<String, Double> icMap) {
		double ic = -1;
		String bestLCS = null;
		for (String lcs : lcses) {
			Double ictmp = icMap.get(lcs);
			if (ictmp != null && ic < ictmp.doubleValue()) {
				ic = ictmp;
				bestLCS = lcs;
			}
		}
		if (ic < 0)
			ic = 0d;
		return new Object[] { bestLCS, ic };
	}

	// /**
	// * return lin measure. optionally filter lin measure so that only concepts
	// * that have an lcs that is relevant to the classification task have a
	// * non-zero lin measure.
	// *
	// * relevant concepts are those whose evaluation wrt the label exceeds a
	// * threshold.
	// *
	// * @param concept1
	// * @param concept2
	// * @param label
	// * if not null, then filter lcses.
	// * @param lcsMinEvaluation
	// * if gt; 0, then filter lcses. this is the threshold.
	// * @return 0 - no lcs, or no lcs that meets the threshold.
	// */
	// @Override
	// public double filteredLin(String concept1, String concept2,
	// Map<String, Double> conceptFilter) {
	// double ic1 = getIC(concept1);
	// double ic2 = getIC(concept2);
	// // lin not defined if one of the concepts doesn't exist in the corpus
	// if (ic1 == 0 || ic2 == 0)
	// return 0;
	// double denom = getIC(concept1) + getIC(concept2);
	// if (denom != 0) {
	// ConcRel cr1 = cg.getConceptMap().get(concept1);
	// ConcRel cr2 = cg.getConceptMap().get(concept2);
	// if (cr1 != null && cr2 != null) {
	// Set<String> lcses = new HashSet<String>();
	// int dist = getLCSFromCache(cr1, cr2, lcses);
	// if (dist > 0) {
	// double ic = getBestIC(lcses, conceptFilter);
	// return 2 * ic / denom;
	// }
	// }
	// }
	// return 0;
	// }

	// /**
	// * get the information content for the concept with the highest evaluation
	// * greater than a specified threshold.
	// *
	// * If threshold 0, get the lowest IC of all the lcs's.
	// *
	// * @param lcses
	// * the least common subsumers of a pair of concepts
	// * @param label
	// * label against which feature was evaluated
	// * @param lcsMinEvaluation
	// * threshold that the feature has to exceed. 0 for no filtering.
	// * @return 0 if no lcs that makes the cut. else find the lcs(es) with the
	// * maximal evaluation, and return getIC on these lcses.
	// *
	// * @see #getIC(Iterable)
	// */
	// private double getBestIC(Set<String> lcses,
	// Map<String, Double> conceptFilter) {
	// if (conceptFilter != null) {
	// double currentBest = -1;
	// Set<String> bestLcses = new HashSet<String>();
	// for (String lcs : lcses) {
	// if (conceptFilter.containsKey(lcs)) {
	// double lcsEval = conceptFilter.get(lcs);
	// if (currentBest == -1 || lcsEval > currentBest) {
	// bestLcses.clear();
	// bestLcses.add(lcs);
	// currentBest = lcsEval;
	// } else if (currentBest == lcsEval) {
	// bestLcses.add(lcs);
	// }
	// }
	// }
	// if (bestLcses.size() > 0) {
	// return this.getIC(bestLcses);
	// }
	// } else {
	// // unfiltered - get the lowest ic
	// return this.getIC(lcses);
	// }
	// return 0;
	// }

	// private ConceptInfo getPreloadedConceptInfo(String conceptId) {
	// ConcRel cr = cg.getConceptMap().get(conceptId);
	// if (cr != null) {
	// return this.conceptInfoCache[cr.getNodeIndex()];
	// }
	// return null;
	// }

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public ClassifierEvaluationDao getClassifierEvaluationDao() {
		return classifierEvaluationDao;
	}

	public ConceptDao getConceptDao() {
		return conceptDao;
	}

	// private String createKey(String c1, String c2) {
	// if (c1.compareTo(c2) < 0) {
	// return new StringBuilder(c1).append("-").append(c2).toString();
	// } else {
	// return new StringBuilder(c2).append("-").append(c1).toString();
	// }
	// }

	@Override
	public ConceptGraph getConceptGraph() {
		return cg;
	}

	public String getConceptGraphName() {
		return conceptGraphName;
	}

	public String getConceptSetName() {
		return conceptSetName;
	}

	public String getCorpusName() {
		return corpusName;
	}

	@Override
	public Map<String, BitSet> getCuiTuiMap() {
		return cuiTuiMap;
	}

	@Override
	public int getDepth(String concept) {
		// if (isPreload()) {
		// // preloaded all concept info - depth should be there
		// ConceptInfo ci = this.getPreloadedConceptInfo(concept);
		// if (ci != null)
		// return (int) ci.getDepth();
		// } else {
		// // get the feature ranks for the intrinsic infocontent -
		// // rank = depth
		// Map<String, FeatureRank> frMap = getICOnDemand(new HashSet<String>(
		// Arrays.asList(concept)), true);
		// if (frMap.containsKey(concept))
		// return frMap.get(concept).getRank();
		// }
		ConcRel cr = this.cg.getConceptMap().get(concept);
		if (cr != null)
			return cr.getDepth();
		return 0;
	}

	@Override
	public double getIC(String concept, boolean intrinsicICMap) {
		double ic = 0d;
		if (intrinsicICMap) {
			ConcRel cr = this.cg.getConceptMap().get(concept);
			if (cr != null)
				ic = cr.getIntrinsicInfoContent();
		} else {
			Double icC = null;
			if (isPreload()) {
				// we preloaded all ic - just look in the cache
				icC = this.corpusICMap.get(concept);
			} else {
				// we need to load the ic from the database on demand
				Map<String, FeatureRank> frMap = getICOnDemand(
						new HashSet<String>(Arrays.asList(concept)), false);
				if (frMap.containsKey(concept))
					return frMap.get(concept).getEvaluation();
			}
			if (icC != null)
				ic = icC;
		}
		return ic;
		// if (isPreload()) {
		// ConceptInfo ci = this.getPreloadedConceptInfo(concept);
		// if (ci != null)
		// return intrinsicICMap ? ci.getIntrinsicIC() : ci.getCorpusIC();
		// } else {
		// Map<String, FeatureRank> frMap = getICOnDemand(new HashSet<String>(
		// Arrays.asList(concept)), intrinsicICMap);
		// if (frMap.containsKey(concept))
		// return frMap.get(concept).getEvaluation();
		// }
		// return 0d;
	}

	private Map<String, FeatureRank> getICOnDemand(Set<String> lcses,
			boolean intrinsicIC) {
		if (lcses == null || lcses.isEmpty())
			return new HashMap<String, FeatureRank>(0);
		Map<String, FeatureRank> lcsICMap;
		lcsICMap = this.classifierEvaluationDao
				.getFeatureRanks(
						lcses,
						intrinsicIC ? null : this.corpusName,
						intrinsicIC ? null : this.conceptSetName,
						null,
						intrinsicIC ? IntrinsicInfoContentEvaluator.INTRINSIC_INFOCONTENT
								: InfoContentEvaluator.INFOCONTENT, null, 0d,
						this.getConceptGraphName());
		return lcsICMap;
	}

	// /**
	// * get the concept with the lowest Information Content of all the LCSs.
	// * Functionality copied from umls interface.
	// *
	// * @todo make this configurable/add a parameter - avg/min/max/median?
	// * @param lcses
	// * @return
	// */
	// public double getIC(Iterable<String> lcses) {
	// double ic = 0;
	// for (String lcs : lcses) {
	// double ictmp = getIC(lcs);
	// if (ic < ictmp)
	// ic = ictmp;
	// }
	// return ic;
	// }
	//
	// public double getIC(String concept1) {
	// Double dRetVal = corpusICMap.get(concept1);
	// if (dRetVal != null)
	// return (double) dRetVal;
	// else
	// return 0;
	// }

	public int getLCS(String concept1, String concept2, Set<String> lcses,
			List<LCSPath> lcsPaths) {
		int lcsDist = 0;
		ConcRel cr1 = getConceptGraph().getConceptMap().get(concept1);
		ConcRel cr2 = getConceptGraph().getConceptMap().get(concept2);
		if (cr1 != null && cr2 != null) {
			lcses.clear();
			if (lcsPaths == null) {
				// no need to get paths which we don't cache - look in the cache
				lcsDist = getLCSFromCache(cr1, cr2, lcses);
			} else {
				lcsPaths.clear();
				// need to get paths - compute the lcses and their paths
				lcsDist = lcs(concept1, concept2, lcsPaths);
				for (LCSPath lcsPath : lcsPaths) {
					lcses.add(lcsPath.getLcs());
				}
			}
		} else {
			if (log.isDebugEnabled()) {
				if (cr1 == null)
					log.debug("could not find concept:" + concept1);
				if (cr2 == null)
					log.debug("could not find concept:" + concept2);
			}
		}
		return lcsDist;
	}

	public Cache getLcsCache() {
		return lcsCache;
	}

	@SuppressWarnings("unchecked")
	private int getLCSFromCache(ConcRel cr1, ConcRel cr2, Set<String> lcses) {
		StringBuilder cacheKeyBuilder = new StringBuilder(this.conceptGraphName);
		cacheKeyBuilder
				.append(cr1.getConceptID().compareTo(cr2.getConceptID()) < 0 ? cr1
						.getConceptID() : cr2.getConceptID());
		cacheKeyBuilder
				.append(cr1.getConceptID().compareTo(cr2.getConceptID()) >= 0 ? cr2
						.getConceptID() : cr1.getConceptID());
		String cacheKey = cacheKeyBuilder.toString();
		Element e = this.lcsCache != null ? this.lcsCache.get(cacheKey) : null;
		if (e != null) {
			// hit the cache - unpack the lcs
			if (e.getObjectValue() != null) {
				Object[] val = (Object[]) e.getObjectValue();
				lcses.addAll((Set<String>) val[1]);
				return (Integer) val[0];
			} else {
				return -1;
			}
		} else {
			// missed the cache - save the lcs
			Object[] val = null;
			Set<ConcRel> lcsCRSet = new HashSet<ConcRel>(2);
			int dist = ConcRel.getLeastCommonConcept(cr1, cr2, lcsCRSet, null);
			if (dist >= 0) {
				val = new Object[2];
				val[0] = dist;
				for (ConcRel cr : lcsCRSet) {
					lcses.add(cr.getConceptID());
				}
				val[1] = lcses;
			}
			if (this.lcsCache != null) {
				e = new Element(cacheKey, val);
				this.lcsCache.put(e);
			}
			return dist;
		}
	}

	public String getLcsImputedType() {
		return lcsImputedType;
	}

	public PageRankService getPageRankService() {
		return pageRankService;
	}

	public Map<SimilarityMetricEnum, SimilarityMetric> getSimilarityMetricMap() {
		return similarityMetricMap;
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	@Override
	public List<String> getTuiList() {
		return this.tuiList;
	}

	public void init() {
		log.info("begin initialization for concept graph: " + conceptGraphName);
		cg = conceptDao.getConceptGraph(conceptGraphName);
		if (cg == null) {
			log.warn("concept graph null, name: " + conceptGraphName);
		} else {
			initSimilarityMetricMap();
			if (isPreload()) {
				try {
					TransactionTemplate t = new TransactionTemplate(
							this.transactionManager);
					t.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
					t.execute(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus arg0) {
							initInfoContent();
							initCuiTuiMapFromCorpus();
							return null;
						}
					});
				} catch (Exception e) {
					log.info("could not initialize cui-tui map: "
							+ e.getMessage()
							+ ".  This is expected if you do not have umls installed in your db.");
				}
			}
		}
		log.info("end initialization for concept graph: " + conceptGraphName);
	}

	/**
	 * load cui-tui for the specified corpus from the MRSTY table
	 */
	public void initCuiTuiMapFromCorpus() {
		// don't duplicate tui strings to save memory
		SortedMap<String, String> tuiMap = new TreeMap<String, String>();
		Map<String, Set<String>> tmpTuiCuiMap = new HashMap<String, Set<String>>();
		List<Object[]> listCuiTui = this.classifierEvaluationDao
				.getCorpusCuiTuis(this.getCorpusName(),
						this.getConceptGraphName(), this.getConceptSetName());
		for (Object[] cuiTui : listCuiTui) {
			String cui = (String) cuiTui[0];
			String tui = (String) cuiTui[1];
			addCuiTuiToMap(tmpTuiCuiMap, tuiMap, cui, tui);
		}
		// map of tui - bitset index
		SortedMap<String, Integer> mapTuiIndex = new TreeMap<String, Integer>();
		// list of tuis corresponding to bitset indices
		List<String> tmpTuiList = new ArrayList<String>(tuiMap.size());
		int index = 0;
		for (String tui : tuiMap.keySet()) {
			mapTuiIndex.put(tui, index++);
			tmpTuiList.add(tui);
		}
		this.tuiList = Collections.unmodifiableList(tmpTuiList);
		// convert list of cuis into bitsets
		// Map<String, BitSet> tmpCuiTuiBitsetMap = new HashMap<String,
		// BitSet>();
		ImmutableMap.Builder<String, BitSet> cuiTuiBitsetMapBuilder = new ImmutableMap.Builder<String, BitSet>();
		for (Map.Entry<String, Set<String>> cuiTuiMapEntry : tmpTuiCuiMap
				.entrySet()) {
			// tmpCuiTuiBitsetMap.put(cuiTuiMapEntry.getKey(),
			// tuiListToBitset(cuiTuiMapEntry.getValue(), mapTuiIndex));
			cuiTuiBitsetMapBuilder.put(cuiTuiMapEntry.getKey(),
					tuiListToBitset(cuiTuiMapEntry.getValue(), mapTuiIndex));
		}
		// this.cuiTuiMap = Collections.unmodifiableMap(tmpCuiTuiBitsetMap);
		this.cuiTuiMap = cuiTuiBitsetMapBuilder.build();
	}

	/**
	 * initialize information content caches TODO replace strings with concept
	 * ids from conceptGraph to save memory
	 */
	private void initInfoContent() {
		// log.info("loading intrinsic infocontent for concept graph: "
		// + conceptGraphName);
		// List<ConceptInfo> listConceptInfo = classifierEvaluationDao
		// .getIntrinsicInfoContent(conceptGraphName);
		// if (listConceptInfo.isEmpty()) {
		// log.warn("intrinsic info content not available! most similarity measures will not work");
		// }
		// this.conceptInfoCache = new ConceptInfo[cg.getConceptMap().size()];
		// for (ConceptInfo ci : listConceptInfo) {
		// ConcRel cr = cg.getConceptMap().get(ci.getConceptId());
		// if (cr != null) {
		// // save a little memory by reusing the string
		// ci.setConceptId(cr.getConceptID());
		// conceptInfoCache[cr.getNodeIndex()] = ci;
		// }
		// }
		// fill intrinsicIC
		// Map<String, FeatureRank> intrinsicICMap = classifierEvaluationDao
		// .getIntrinsicInfoContent(conceptGraphName);
		// for (Map.Entry<String, FeatureRank> icMapEntry : intrinsicICMap
		// .entrySet()) {
		// FeatureRank r = icMapEntry.getValue();
		// ConcRel cr = cg.getConceptMap().get(r.getFeatureName());
		// if (cr != null) {
		// ConceptInfo ci = new ConceptInfo();
		// ci.setConceptId(cr.getConceptID());
		// ci.setDepth(r.getRank());
		// ci.setIntrinsicIC(r.getEvaluation());
		// conceptInfoMap.put(ci.getConceptId(), ci);
		// }
		// }
		// fill corpusIC
		log.info("loading corpus infocontent for corpusName=" + corpusName
				+ ", conceptGraphName=" + conceptGraphName
				+ ", conceptSetName=" + conceptSetName);
		Map<String, Double> corpusICMap = classifierEvaluationDao
				.getInfoContent(corpusName, conceptGraphName,
						this.conceptSetName);
		if (corpusICMap == null || corpusICMap.isEmpty()) {
			log.warn("IC not found");
		}
		ImmutableMap.Builder<String, Double> mb = new ImmutableMap.Builder<String, Double>();
		for (Map.Entry<String, Double> corpusICEntry : corpusICMap.entrySet()) {
			ConcRel cr = cg.getConceptMap().get(corpusICEntry.getKey());
			if (cr != null) {
				mb.put(cr.getConceptID(), corpusICEntry.getValue());
			}
		}
		this.corpusICMap = mb.build();
		// ConceptInfo ci = this.conceptInfoCache[cr.getNodeIndex()];
		// if (ci == null) {
		// // this shouldn't happen! there should be intrinsic ic for
		// // this concept
		// ci = new ConceptInfo();
		// ci.setConceptId(cr.getConceptID());
		// this.conceptInfoCache[cr.getNodeIndex()] = ci;
		// }
		// ci.setCorpusIC(corpusICEntry.getValue());
		// }
		// }
	}

	/**
	 * initialize the metrics
	 */
	private void initSimilarityMetricMap() {
		log.info("initializing similarity measures");
		// Double maxIC = this.classifierEvaluationDao.getMaxFeatureEvaluation(
		// null, null, null,
		// IntrinsicInfoContentEvaluator.INTRINSIC_INFOCONTENT, 0, 0,
		// conceptGraphName);
		// Integer maxDepth = this.classifierEvaluationDao
		// .getMaxDepth(conceptGraphName);
		double maxIC = this.cg.getIntrinsicICMax();
		int maxDepth = this.cg.getDepthMax();
		this.similarityMetricMap = new HashMap<SimilarityMetricEnum, SimilarityMetric>(
				SimilarityMetricEnum.values().length);
		if (maxDepth > 0) {
			this.similarityMetricMap.put(SimilarityMetricEnum.LCH,
					new LCHMetric(this, maxDepth));
			this.similarityMetricMap.put(SimilarityMetricEnum.LIN,
					new LinMetric(this, false));
			this.similarityMetricMap.put(SimilarityMetricEnum.INTRINSIC_LIN,
					new LinMetric(this, true));
			this.similarityMetricMap.put(SimilarityMetricEnum.INTRINSIC_LCH,
					new IntrinsicLCHMetric(this, maxIC));
			this.similarityMetricMap.put(SimilarityMetricEnum.PATH,
					new PathMetric(this));
			this.similarityMetricMap.put(SimilarityMetricEnum.INTRINSIC_PATH,
					new IntrinsicPathMetric(this, maxIC));
			this.similarityMetricMap.put(SimilarityMetricEnum.RADA,
					new RadaMetric(this, maxDepth));
			this.similarityMetricMap.put(SimilarityMetricEnum.INTRINSIC_RADA,
					new IntrinsicRadaMetric(this, maxIC));
			this.similarityMetricMap.put(SimilarityMetricEnum.SOKAL,
					new SokalSneathMetric(this));
			this.similarityMetricMap.put(SimilarityMetricEnum.JACCARD,
					new JaccardMetric(this));
			this.similarityMetricMap.put(SimilarityMetricEnum.WUPALMER,
					new WuPalmerMetric(this));
		} else {
			this.similarityMetricMap.put(SimilarityMetricEnum.PAGERANK,
					new PageRankMetric(this, this.getPageRankService()));
		}
	}

	public boolean isPreload() {
		return preload;
	}

	public int lcs(String concept1, String concept2, List<LCSPath> lcsPaths) {
		ConcRel cr1 = cg.getConceptMap().get(concept1);
		ConcRel cr2 = cg.getConceptMap().get(concept2);
		int dist = -1;
		if (cr1 != null && cr2 != null) {
			Set<ConcRel> crlcses = new HashSet<ConcRel>();
			Map<ConcRel, LCSPath> crpaths = new HashMap<ConcRel, LCSPath>();
			dist = ConcRel.getLeastCommonConcept(cr1, cr2, crlcses, crpaths);
			lcsPaths.addAll(crpaths.values());
		}
		return dist;
	}

	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// org.apache.ctakes.ytex.kernel.ConceptSimilarity#lch(java.lang.String,
	// * java.lang.String)
	// */
	// public double lch(String concept1, String concept2) {
	// double dm = 2 * cg.getDepthMax() + 1.0;
	// ConcRel cr1 = cg.getConceptMap().get(concept1);
	// ConcRel cr2 = cg.getConceptMap().get(concept2);
	// if (cr1 != null && cr2 != null) {
	// Set<String> lcses = new HashSet<String>();
	// int lcsDist = getLCSFromCache(cr1, cr2, lcses);
	// // leacock is defined as -log([path length]/(2*[depth])
	// double lch = -Math.log(((double) lcsDist + 1.0) / dm);
	// // scale to depth
	// return lch / Math.log(dm);
	// } else {
	// if (log.isDebugEnabled()) {
	// if (cr1 == null)
	// log.debug("could not find concept:" + concept1);
	// if (cr2 == null)
	// log.debug("could not find concept:" + concept2);
	// }
	// return 0;
	// }
	// }

	/**
	 * For the given label and cutoff, get the corresponding concepts whose
	 * propagated ig meets the threshold. Used by lin kernel to find concepts
	 * that actually have a non-trivial similarity
	 * 
	 * @param label
	 *            label
	 * @param rankCutoff
	 *            cutoff
	 * @param conceptFilter
	 *            set to fill with concepts
	 * @return double minimum evaluation
	 */
	@Override
	public double loadConceptFilter(String label, int rankCutoff,
			Map<String, Double> conceptFilter) {
		List<FeatureRank> imputedConcepts = this.classifierEvaluationDao
				.getImputedFeaturesByPropagatedCutoff(corpusName,
						conceptSetName, label, lcsImputedType
								+ ImputedFeatureEvaluator.SUFFIX_IMPUTED,
						conceptGraphName, lcsImputedType
								+ ImputedFeatureEvaluator.SUFFIX_PROP,
						rankCutoff);
		double minEval = 1d;
		for (FeatureRank r : imputedConcepts) {
			conceptFilter.put(r.getFeatureName(), r.getEvaluation());
			if (minEval >= r.getEvaluation())
				minEval = r.getEvaluation();
		}
		return minEval;
	}

	// public double lin(String concept1, String concept2) {
	// return filteredLin(concept1, concept2, null);
	// }

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void setClassifierEvaluationDao(
			ClassifierEvaluationDao classifierEvaluationDao) {
		this.classifierEvaluationDao = classifierEvaluationDao;
	}

	public void setConceptDao(ConceptDao conceptDao) {
		this.conceptDao = conceptDao;
	}

	public void setConceptGraphName(String conceptGraphName) {
		this.conceptGraphName = conceptGraphName;
	}

	public void setConceptSetName(String conceptSetName) {
		this.conceptSetName = conceptSetName;
	}

	public void setCorpusName(String corpusName) {
		this.corpusName = corpusName;
	}

	public void setLcsCache(Cache lcsCache) {
		this.lcsCache = lcsCache;
	}

	public void setLcsImputedType(String lcsImputedType) {
		this.lcsImputedType = lcsImputedType;
	}

	// double minEval = 1d;
	// List<FeatureRank> listPropagatedConcepts = classifierEvaluationDao
	// .getTopFeatures(corpusName, conceptSetName, label,
	// ImputedFeatureEvaluator.MeasureType.INFOGAIN.toString()
	// + ImputedFeatureEvaluator.SUFFIX_PROP, 0, 0,
	// conceptGraphName, rankCutoff);
	// for (FeatureRank r : listPropagatedConcepts) {
	// ConcRel cr = cg.getConceptMap().get(r.getFeatureName());
	// if (cr != null) {
	// addSubtree(conceptFilterSet, cr);
	// }
	// if (r.getEvaluation() < minEval)
	// minEval = r.getEvaluation();
	// }
	// return minEval;
	// }
	//
	// /**
	// * add all children of parent to conceptSet. Limit only to children that
	// * actually appear in the corpus
	// *
	// * @param conceptSet
	// * set of concepts to add ids to
	// * @param parent
	// * parent which will be added to the conceptSet
	// * @param corpusICSet
	// * set of concepts and hypernyms contained in corpus
	// */
	// private void addSubtree(Map<String, Double> conceptSet, ConcRel parent) {
	// if (!conceptSet.containsKey(parent.getConceptID())
	// && conceptFreq.containsKey(parent.getConceptID())) {
	// conceptSet.put(parent.getConceptID(), 0d);
	// for (ConcRel child : parent.getChildren()) {
	// addSubtree(conceptSet, child);
	// }
	// }
	// }

	public void setPageRankService(PageRankService pageRankService) {
		this.pageRankService = pageRankService;
	}

	public void setPreload(boolean preload) {
		this.preload = preload;
	}

	public void setSimilarityMetricMap(
			Map<SimilarityMetricEnum, SimilarityMetric> similarityMetricMap) {
		this.similarityMetricMap = similarityMetricMap;
	}

	public void setTransactionManager(
			PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public List<ConceptPairSimilarity> similarity(
			List<ConceptPair> conceptPairs, List<SimilarityMetricEnum> metrics,
			Map<String, Double> conceptFilter, boolean lcs) {
		List<ConceptPairSimilarity> conceptSimMap = new ArrayList<ConceptPairSimilarity>(
				conceptPairs.size());
		for (ConceptPair conceptPair : conceptPairs) {
			conceptSimMap.add(similarity(metrics, conceptPair.getConcept1(),
					conceptPair.getConcept2(), conceptFilter, lcs));
		}
		return conceptSimMap;
	}

	/**
	 * 
	 */
	@Override
	public ConceptPairSimilarity similarity(List<SimilarityMetricEnum> metrics,
			String concept1, String concept2,
			Map<String, Double> conceptFilter, boolean lcs) {
		// allocate simInfo if this isn't provided
		SimilarityInfo simInfo = new SimilarityInfo();
		if (lcs)
			simInfo.setLcsPaths(new ArrayList<LCSPath>(1));
		// allocate result map
		List<Double> similarities = new ArrayList<Double>(metrics.size());
		if (cg != null) {
			// iterate over metrics, compute, stuff in map
			for (SimilarityMetricEnum metric : metrics) {
				double sim = this.similarityMetricMap.get(metric).similarity(
						concept1, concept2, conceptFilter, simInfo);
				similarities.add(sim);
			}
		}
		ConceptPairSimilarity csim = new ConceptPairSimilarity();
		csim.setConceptPair(new ConceptPair(concept1, concept2));
		csim.setSimilarities(similarities);
		csim.setSimilarityInfo(simInfo);
		return csim;
	}

	/**
	 * convert the list of tuis into a bitset
	 * 
	 * @param tuis
	 * @param mapTuiIndex
	 * @return
	 */
	private BitSet tuiListToBitset(Set<String> tuis,
			SortedMap<String, Integer> mapTuiIndex) {
		BitSet bs = new BitSet(mapTuiIndex.size());
		for (String tui : tuis) {
			bs.set(mapTuiIndex.get(tui));
		}
		return bs;
	}
}
