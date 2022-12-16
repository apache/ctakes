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
package org.apache.ctakes.ytex.kernel.pagerank;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;
import org.apache.ctakes.ytex.kernel.dao.ConceptDao;
import org.apache.ctakes.ytex.kernel.model.ConcRel;
import org.apache.ctakes.ytex.kernel.model.ConceptGraph;


public class PageRankServiceImpl implements PageRankService {
	private static final Log log = LogFactory.getLog(PageRankServiceImpl.class);

	private double[] rankInternal(Map<Integer, Double> dampingVector,
			ConceptGraph cg, int iter, double threshold, double dampingFactor) {
		Map<Integer, Double> scoreMapCurrent = dampingVector;
		double N = (double) cg.getConceptList().size();
		double scoresCurrent[] = new double[cg.getConceptList().size()];
		double diff = 1d;
		for (int i = 0; i < iter; i++) {
			double[] scoresOld = scoresCurrent;
			long timeBegin = 0;
			if (log.isDebugEnabled()) {
				timeBegin = System.currentTimeMillis();
			}
			scoresCurrent = pagerankIter(scoresOld, dampingVector, cg,
					dampingFactor, N);
			if (log.isDebugEnabled()) {
				log.debug("iter " + i + " "
						+ Long.toString(System.currentTimeMillis() - timeBegin));
			}
			if ((diff = difference(scoresOld, scoresCurrent)) <= threshold)
				break;
		}
		if (log.isDebugEnabled() && diff > threshold) {
			log.debug("did not converge, diff = " + diff + ", dampingVector = "
					+ dampingVector);
		}
		return scoresCurrent;
	}

	/**
	 * difference between 2 vectors
	 * 
	 * @param a
	 * @param b
	 * @return a-b
	 */
	private <T> double difference(Map<T, Double> a, Map<T, Double> b) {
		double diff = 0d;
		for (Map.Entry<T, Double> aiEntry : a.entrySet()) {
			Double bi = b.get(aiEntry.getKey());
			diff += Math.pow(
					aiEntry.getValue() - (bi != null ? bi.doubleValue() : 0d),
					2);
		}
		for (Map.Entry<T, Double> biEntry : b.entrySet()) {
			if (!a.containsKey(biEntry.getKey())) {
				diff += Math.pow(biEntry.getValue(), 2);
			}
		}
		return diff;
	}

	/**
	 * 
	 * @param u
	 * @param v
	 * @return norm(u-v)
	 */
	private double difference(double[] u, double[] v) {
		double diff = 0d;
		for (int i = 0; i < u.length; i++) {
			double d = (u[i] - v[i]);
			diff += d * d;
		}
		return Math.sqrt(diff);
	}

	private double cosine(double[] u, double[] v) {
		double uu = 0;
		double vv = 0;
		double uv = 0;
		for (int i = 0; i < u.length; i++) {
			uu += u[i] * u[i];
			vv += v[i] * v[i];
			uv += u[i] * v[i];
		}
		return uv / Math.sqrt(uu * vv);
	}

	public double[] pagerankIter(double[] currentScores,
			Map<Integer, Double> dampingVector, ConceptGraph cg,
			double dampingFactor, double N, Set<Integer> activeNodes) {
		double newScores[] = new double[(int) N];
		Arrays.fill(newScores, 0d);
		Integer[] activeNodeArr = new Integer[activeNodes.size()];
		activeNodes.toArray(activeNodeArr);
		for (int index : activeNodeArr) {
			// pagerank with non-uniform damping vector (topic vector).
			// because of the non-uniform damping vector, few nodes will have a
			// non-zero pagerank.
			// optimized so that we only iterate over nodes with non-zero
			// pagerank.
			// propagate from non-zero nodes to linked nodes
			// we assume currentScores is non-null - it is initialized to the
			// damping vector.
			// iterate over nodes that have a pagerank, and propagate the
			// pagerank to out-links.
			// pagerank
			double score = currentScores[index];
			// get concept id
			ConcRel cr = cg.getConceptList().get(index);
			// get number of out-links
			double nOutlinks = (double) cr.getChildren().size();
			if (nOutlinks > 0) {
				// propagate pagerank to out-links (children)
				for (ConcRel crOut : cr.getChildren()) {
					int targetIndex = crOut.getNodeIndex();
					// get current pagerank value for target page
					double childScore = newScores[targetIndex];
					// add the pagerank/|links|
					childScore += (score / nOutlinks);
					newScores[targetIndex] = childScore;
					activeNodes.add(targetIndex);
				}
			}
		}
		// we just added the contribution of pages to newScores sum(score).
		// adjust: convert to (d)*sum(score) + (1-d)*v_i
		for (int index : activeNodes) {
			// personalized pagerank
			double adjusted = (newScores[index] * dampingFactor);
			// v_i
			Double v_i = dampingVector.get(index);
			// 1-c * v_i
			if (v_i != null)
				adjusted += v_i;
			newScores[index] = adjusted;
		}
		return newScores;
	}

	public double[] pagerankIter(double[] currentScores,
			Map<Integer, Double> dampingVector, ConceptGraph cg,
			double dampingFactor, double N) {
		double newScores[] = new double[(int) N];
		double jump = ((1 - dampingFactor) / N);
		for (int i = 0; i < currentScores.length; i++) {
			double score = 0d;
			ConcRel c = cg.getConceptList().get(i);
			// get nodes pointing at node c
			for (int parentIndex : c.getParentsArray()) {
				ConcRel p = cg.getConceptList().get(parentIndex);
				// get the pagerank for node p which is pointing at c
				// if this is the first iteration, currentScores is null so
				// use the initial pagerank
				double prIn = currentScores[parentIndex];
				// add the pagerank divided by the number of nodes p is
				// pointing at
				score += (prIn / (double) p.getChildrenArray().length);
			}
			if (dampingVector == null) {
				// uniform damping
				newScores[i] = (score * dampingFactor) + jump;
			} else {
				// personalized pagerank
				double adjusted = (score * dampingFactor);
				// get the random jump for this node
				Double v_i = dampingVector.get(i);
				// if not null, add it
				if (v_i != null)
					adjusted += v_i;
				newScores[i] = adjusted;
			}
		}
		return newScores;
	}

	@Override
	public double[] rank2(Map<Integer, Double> dampingVector, ConceptGraph cg,
			int iter, double threshold, double dampingFactor) {
		double N = (double) cg.getConceptMap().size();
		double scoresCurrent[] = new double[cg.getConceptMap().size()];
		Map<Integer, Double> dampingVectorAdj = null;
		// Set<Integer> activeNodes = null;
		if (dampingVector != null) {
			// for personalized page rank, put together a map of possibilities
			// of randomly jumping to a specific node
			dampingVectorAdj = new HashMap<Integer, Double>(
					dampingVector.size());
			// // initialize set of active nodes
			// activeNodes = new HashSet<Integer>(dampingVector.keySet());
			Arrays.fill(scoresCurrent, 0d);
			for (Map.Entry<Integer, Double> dvEntry : dampingVector.entrySet()) {
				// set the random jump for the node
				dampingVectorAdj.put(dvEntry.getKey(), dvEntry.getValue()
						* (1 - dampingFactor));
				// set the initial weight for the node
				scoresCurrent[dvEntry.getKey()] = dvEntry.getValue();
			}
		} else {
			// for static page rank, all nodes have same weight initially
			Arrays.fill(scoresCurrent, 1d / N);
		}
		double diff = 1d;
		for (int i = 0; i < iter; i++) {
			double scoresOld[] = scoresCurrent;
			long timeBegin = 0;
			if (log.isDebugEnabled()) {
				timeBegin = System.currentTimeMillis();
			}
			// if (activeNodes == null) {
			scoresCurrent = pagerankIter(scoresCurrent, dampingVectorAdj, cg,
					dampingFactor, N);
			// } else {
			// scoresCurrent = pagerankIter(scoresCurrent, dampingVectorAdj,
			// cg, dampingFactor, N, activeNodes);
			// }
			if (log.isDebugEnabled()) {
				log.debug("iter " + i + " time(ms) "
						+ Long.toString(System.currentTimeMillis() - timeBegin));
			}
			if ((diff = difference(scoresCurrent, scoresOld)) <= threshold)
				break;
		}
		if (log.isDebugEnabled() && diff > threshold) {
			log.debug("did not converge, diff = " + diff + ", dampingVector = "
					+ dampingVector);
		}
		return scoresCurrent;
	}

	/**
	 * perform one iteration of pagerank
	 * 
	 * @param currentScores
	 * @param cg
	 * @return
	 */
	public Map<Integer, Double> pagerankIter(
			Map<Integer, Double> currentScores,
			Map<Integer, Double> dampingVector, ConceptGraph cg,
			double dampingFactor, double N) {
		Map<Integer, Double> newScores = new HashMap<Integer, Double>();
		if (dampingVector == null) {
			// the constant probability of randomly surfing into this node,
			// adjusted by damping factor
			double jump = ((1 - dampingFactor) / N);
			double initialValue = 1 / N;
			// the basic pagerank iteration with uniform damping vector
			// iterate over all nodes
			for (ConcRel c : cg.getConceptList()) {
				double score = 0d;
				// get nodes pointing at node c
				for (ConcRel in : c.getParents()) {
					// get the pagerank for node p which is pointing at c
					// if this is the first iteration, currentScores is null so
					// use the initial pagerank
					double prIn = currentScores == null ? initialValue
							: currentScores.get(in.getNodeIndex());
					// add the pagerank divided by the number of nodes p is
					// pointing at
					score += (prIn / (double) in.getChildren().size());
				}
				// adjust for uniform damping
				double adjusted = (score * dampingFactor) + jump;
				newScores.put(c.getNodeIndex(), adjusted);
			}
			// for (ConcRel c : cg.getConceptMap().values()) {
			// double score = 0d;
			// // get nodes pointing at node c
			// for (ConcRel in : c.getParents()) {
			// // get the pagerank for node p which is pointing at c
			// // if this is the first iteration, currentScores is null so
			// // use the initial pagerank
			// double prIn = currentScores == null ? initialValue
			// : currentScores.get(in.getConceptID());
			// // add the pagerank divided by the number of nodes p is
			// // pointing at
			// score += (prIn / (double) in.getChildren().size());
			// }
			// // adjust for uniform damping
			// double adjusted = (score * dampingFactor) + jump;
			// newScores.put(c.getConceptID(), adjusted);
			// }
		} else {
			// pagerank with non-uniform damping vector (topic vector).
			// because of the non-uniform damping vector, few nodes will have a
			// non-zero pagerank.
			// optimized so that we only iterate over nodes with non-zero
			// pagerank.
			// propagate from non-zero nodes to linked nodes
			// we assume currentScores is non-null - it is initialized to the
			// damping vector.
			// iterate over nodes that have a pagerank, and propagate the
			// pagerank to out-links.
			for (Map.Entry<Integer, Double> scoreEntry : currentScores
					.entrySet()) {
				// page (concept id)
				Integer index = scoreEntry.getKey();
				// pagerank
				double score = scoreEntry.getValue();
				// get concept id
				ConcRel cr = cg.getConceptList().get(index);
				// get number of out-links
				double nOutlinks = (double) cr.getChildren().size();
				if (nOutlinks > 0) {
					// propagate pagerank to out-links (children)
					for (ConcRel crOut : cr.getChildren()) {
						// get current pagerank value for target page
						double childScore = 0d;
						Double childScoreD = newScores
								.get(crOut.getNodeIndex());
						if (childScoreD != null)
							childScore = childScoreD.doubleValue();
						// add the pagerank/|links|
						childScore += (score / nOutlinks);
						newScores.put(crOut.getNodeIndex(), childScore);
					}
				}
			}
			// we just added the contribution of pages to newScores sum(score).
			// adjust: convert to (d)*sum(score) + (1-d)*v_i
			for (Map.Entry<Integer, Double> scoreEntry : newScores.entrySet()) {
				// v_i
				Double v_i = dampingVector.get(scoreEntry.getKey());
				// 1-c * v_i
				double v_i_adj = v_i != null ? v_i * (1 - dampingFactor) : 0d;
				double adjusted = (scoreEntry.getValue() * dampingFactor)
						+ v_i_adj;
				scoreEntry.setValue(adjusted);
			}
			//
			//
			// for (Map.Entry<String, Double> scoreEntry : currentScores
			// .entrySet()) {
			// // page (concept id)
			// String page = scoreEntry.getKey();
			// // pagerank
			// double score = scoreEntry.getValue();
			// // get concept id
			// ConcRel cr = cg.getConceptMap().get(page);
			// // get number of out-links
			// double nOutlinks = (double) cr.getChildren().size();
			// if (nOutlinks > 0) {
			// // propagate pagerank to out-links (children)
			// for (ConcRel crOut : cr.getChildren()) {
			// // get current pagerank value for target page
			// double childScore = 0d;
			// Double childScoreD = newScores
			// .get(crOut.getConceptID());
			// if (childScoreD != null)
			// childScore = childScoreD.doubleValue();
			// // add the pagerank/|links|
			// childScore += (score / nOutlinks);
			// newScores.put(crOut.getConceptID(), childScore);
			// }
			// }
			// }
			// // we just added the contribution of pages to newScores
			// sum(score).
			// // adjust: convert to (d)*sum(score) + (1-d)*v_i
			// for (Map.Entry<String, Double> scoreEntry : newScores.entrySet())
			// {
			// // v_i
			// Double v_i = dampingVector.get(scoreEntry.getKey());
			// // 1-c * v_i
			// double v_i_adj = v_i != null ? v_i * (1 - dampingFactor) : 0d;
			// double adjusted = (scoreEntry.getValue() * dampingFactor)
			// + v_i_adj;
			// scoreEntry.setValue(adjusted);
			// }
		}
		return newScores;
	}

	/**
	 * compute similarity using personalized page rank, as documented in <a
	 * href=
	 * "http://ixa.si.ehu.es/Ixa/Argitalpenak/Artikuluak/1274099085/publikoak/main.pdf"
	 * >Exploring Knowledge Bases for Similarity</a>
	 * 
	 * @param concept1
	 * @param concept2
	 * @param cg
	 * @param iter
	 * @param threshold
	 * @param dampingFactor
	 * @return
	 */
	@Override
	public double sim(String concept1, String concept2, ConceptGraph cg,
			int iter, double threshold, double dampingFactor) {
		Map<Integer, Double> c1dv = new HashMap<Integer, Double>(1);
		ConcRel c1 = cg.getConceptMap().get(concept1);
		ConcRel c2 = cg.getConceptMap().get(concept2);
		if (c1 == null || c2 == null)
			return 0d;
		c1dv.put(c1.getNodeIndex(), 1d);
		double[] c1pr = this.rank2(c1dv, cg, iter, threshold, dampingFactor);
		Map<Integer, Double> c2dv = new HashMap<Integer, Double>(1);
		c2dv.put(c2.getNodeIndex(), 1d);
		double[] c2pr = this.rank2(c2dv, cg, iter, threshold, dampingFactor);
		return cosine(c1pr, c2pr);
	}

	/**
	 * cosine of two vectors
	 * 
	 * @param u
	 * @param v
	 * @return
	 */
	private <T> double cosine(Map<T, Double> u, Map<T, Double> v) {
		double uu = 0d;
		double uv = 0d;
		double vv = 0d;
		if (u.isEmpty() || v.isEmpty())
			return 0d;
		// in this loop compute u*u, and u*v
		for (Map.Entry<T, Double> uEntry : u.entrySet()) {
			double ui = uEntry.getValue();
			T uC = uEntry.getKey();
			uu += ui * ui;
			Double vi = v.get(uC);
			if (vi != null)
				uv += ui * vi.doubleValue();
		}
		if (uv == 0)
			return 0d;
		// in this loop, compute v*v
		for (double vi : v.values()) {
			vv += vi * vi;
		}
		// u*v/sqrt(v*v)*sqrt(u*u)
		return uv / Math.sqrt(vv * uu);
	}

	public static void main(String args[]) {
		Options options = new Options();
		OptionGroup og = new OptionGroup();
		og.addOption(OptionBuilder
				.withArgName("concept1,concept2")
				.hasArg()
				.withDescription(
						"compute similarity for specified concept pair")
				.create("sim"));
		og.addOption(OptionBuilder
				.withArgName("concept1,concept2,...")
				.hasArg()
				.withDescription(
						"personalized pagerank vector for specified concepts ")
				.create("ppr"));
		og.setRequired(true);
		options.addOptionGroup(og);
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			Properties ytexProps = new Properties();
			ytexProps.putAll((Properties) KernelContextHolder
					.getApplicationContext().getBean("ytexProperties"));
			ytexProps.putAll(System.getProperties());
			ConceptDao conceptDao = KernelContextHolder.getApplicationContext()
					.getBean(ConceptDao.class);
			PageRankService pageRankService = KernelContextHolder
					.getApplicationContext().getBean(PageRankService.class);
			ConceptGraph cg = conceptDao.getConceptGraph(ytexProps
					.getProperty("org.apache.ctakes.ytex.conceptGraphName"));
			if (line.hasOption("sim")) {
				String cs = line.getOptionValue("sim");
				String concept[] = cs.split(",");
				System.out.println(pageRankService.sim(concept[0], concept[1],
						cg, 30, 1e-4, 0.85));
			} else if (line.hasOption("ppr")) {
				String cs = line.getOptionValue("ppr");
				String concept[] = cs.split(",");
				double weight = 1 / (double) concept.length;
				Map<String, Double> ppv = new HashMap<String, Double>();
				for (String c : concept) {
					ppv.put(c, weight);
				}
				System.out.println(pageRankService.rank(ppv, cg));
			}
		} catch (ParseException pe) {
			HelpFormatter formatter = new HelpFormatter();
			formatter
					.printHelp(
							"java "
									+ PageRankServiceImpl.class.getName()
									+ " compute personalized page rank or similarity.  used for testing purposes",
							options);
		}

	}

	@Override
	public double[] rank(Map<String, Double> dampingVector, ConceptGraph cg,
			int iter, double threshold, double dampingFactor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] rank(Map<String, Double> dampingVector, ConceptGraph cg) {
		// TODO Auto-generated method stub
		return null;
	}
}
