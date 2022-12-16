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
package org.apache.ctakes.ytex.kernel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.dao.ClassifierEvaluationDao;
import org.apache.ctakes.ytex.kernel.dao.ConceptDao;
import org.apache.ctakes.ytex.kernel.model.ConcRel;
import org.apache.ctakes.ytex.kernel.model.ConceptGraph;
import org.apache.ctakes.ytex.kernel.model.FeatureEvaluation;
import org.apache.ctakes.ytex.kernel.model.FeatureRank;


public class IntrinsicInfoContentEvaluatorImpl implements
		IntrinsicInfoContentEvaluator {
	public static class IntrinsicICInfo {
		private ConcRel concept;

		private int leafCount = 0;

		private int subsumerCount = 0;

		public IntrinsicICInfo(ConcRel concept) {
			this.concept = concept;
		}

		public ConcRel getConcept() {
			return concept;
		}

		public int getLeafCount() {
			return leafCount;
		}

		public int getSubsumerCount() {
			return subsumerCount;
		}

		public void setConcept(ConcRel concept) {
			this.concept = concept;
		}

		public void setLeafCount(int leafCount) {
			this.leafCount = leafCount;
		}

		public void setSubsumerCount(int subsumerCount) {
			this.subsumerCount = subsumerCount;
		}
	}

	private static final Log log = LogFactory
			.getLog(IntrinsicInfoContentEvaluatorImpl.class);
	private static final double log2adjust = 1d / Math.log(2);

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Properties props = (Properties) KernelContextHolder
				.getApplicationContext().getBean("ytexProperties");
		props.putAll(System.getProperties());
		if (!props.containsKey("org.apache.ctakes.ytex.conceptGraphName")) {
			System.err.println("error: org.apache.ctakes.ytex.conceptGraphName not specified");
			System.exit(1);
		} else {
			IntrinsicInfoContentEvaluator corpusEvaluator = KernelContextHolder
					.getApplicationContext().getBean(
							IntrinsicInfoContentEvaluator.class);
			corpusEvaluator.evaluateIntrinsicInfoContent(props);
			System.exit(0);
		}
	}

	private ClassifierEvaluationDao classifierEvaluationDao;

	private ConceptDao conceptDao;

	private double computeIC(IntrinsicICInfo icInfo, int maxLeaves) {
		// |leaves(c)|/|subsumers(c)| + 1
		double denom = log2adjust
				* Math.log((double) icInfo.getLeafCount()
						/ (double) icInfo.getSubsumerCount() + 1d);
		// max_leaves + 1
		double num = log2adjust * Math.log((double) maxLeaves + 1d);
		if (denom == Double.NaN || num == Double.NaN) {
			log.error("IC = NaN for " + icInfo.getConcept().getConceptID()
					+ ", leafCount=" + icInfo.getLeafCount()
					+ ", subsumerCount = " + icInfo.getSubsumerCount());
			return -1d;
		} else
			return num - denom;
	}

	/**
	 * recursively compute the number of leaves. fill in the icInfoMap as we go
	 * along
	 * 
	 * @param concept
	 *            concept for which we should get the leaves
	 * @param leafCache
	 *            cache of concept's leaves
	 * @param icInfoMap
	 *            to be updated with leaf counts
	 * @param cg
	 * @param w
	 * @param visitedNodes
	 *            list of nodes that have already been visited - we don't need
	 *            to revisit them when getting the leaves
	 * @return
	 * @throws IOException
	 */
	private HashSet<Integer> getLeaves(ConcRel concept,
			SoftReference<HashSet<Integer>>[] leafCache,
			Map<String, IntrinsicICInfo> icInfoMap, ConceptGraph cg,
			BufferedWriter w, HashSet<Integer> visitedNodes) throws IOException {
		// look in cache
		SoftReference<HashSet<Integer>> refLeaves = leafCache[concept.getNodeIndex()];
		if (refLeaves != null && refLeaves.get() != null) {
			return refLeaves.get();
		}
		// not in cache - compute recursively
		HashSet<Integer> leaves = new HashSet<Integer>();
		leafCache[concept.getNodeIndex()] = new SoftReference<HashSet<Integer>>(leaves);
		if (concept.isLeaf()) {
			// for leaves, just add the concept id
			leaves.add(concept.getNodeIndex());
		} else {
			IntrinsicICInfo icInfo = icInfoMap.get(concept.getConceptID());
			// have we already computed the leaf count for this node?
			// if yes, then we can ignore previously visited nodes
			// if no, then compute it now and revisit previously visited nodes
			// if we have to
			boolean needLeaves = (icInfo != null && icInfo.getLeafCount() == 0);
			HashSet<Integer> visitedNodesLocal = visitedNodes;
			if (needLeaves || visitedNodesLocal == null) {
				// allocate a set to keep track of nodes we've already visited
				// so that we don't revisit them. if we have already computed
				// this node's leaf count then we reuse whatever the caller gave
				// us if non null, else allocate a new one.
				// if we haven't already computed this node's leaf count,
				// allocate a new set to avoid duplications in the traversal for
				// this node
				visitedNodesLocal = new HashSet<Integer>();
			}
			// for inner nodes, recurse
			for (ConcRel child : concept.getChildren()) {
				// if we've already visited a node, then don't bother adding
				// that node's leaves - we already have them
				if (!visitedNodesLocal.contains(child.getNodeIndex())) {
					leaves.addAll(getLeaves(child, leafCache, icInfoMap, cg, w,
							visitedNodesLocal));
				}
			}
			// add this node to the set of visited nodes so we know not to
			// revisit. This is only of importance if the caller gave us
			// a non-empty set.
			if (visitedNodes != null && visitedNodes != visitedNodesLocal) {
				visitedNodes.add(concept.getNodeIndex());
				visitedNodes.addAll(visitedNodesLocal);
			}
			// update the leaf count if we haven't done so already
			if (needLeaves) {
				icInfo.setLeafCount(leaves.size());
				// output leaves if desired
				if (w != null) {
					w.write(concept.getConceptID());
					w.write("\t");
					w.write(Integer.toString(leaves.size()));
					w.write("\t");
					Iterator<Integer> iter = leaves.iterator();
					while (iter.hasNext()) {
						w.write(cg.getConceptList().get(iter.next())
								.getConceptID());
						w.write(" ");
					}
					w.newLine();
				}
			}
		}
		return leaves;
	}

	// /**
	// * add/update icInfoMap entry for concept with the concept's leaf count
	// *
	// * @param concept
	// * @param icInfoMap
	// * @param w
	// * @param subsumerMap
	// * @throws IOException
	// */
	// private void computeLeafCount(ConcRel concept,
	// Map<String, IntrinsicICInfo> icInfoMap,
	// SoftReference<TIntSet>[] leafCache, ConceptGraph cg,
	// BufferedWriter w) throws IOException {
	// // see if we already computed this
	// IntrinsicICInfo icInfo = icInfoMap.get(concept.getConceptID());
	// if (icInfo != null && icInfo.getLeafCount() > 0) {
	// return;
	// }
	// // if not, figure it out
	// if (icInfo == null) {
	// icInfo = new IntrinsicICInfo(concept);
	// icInfoMap.put(concept.getConceptID(), icInfo);
	// }
	// // for leaves the default (0) is correct
	// if (!concept.isLeaf()) {
	// TIntSet leaves = this.getLeaves(concept, leafCache);
	// icInfo.setLeafCount(leaves.size());
	// if (w != null) {
	// w.write(concept.getConceptID());
	// w.write("\t");
	// w.write(Integer.toString(leaves.size()));
	// w.write("\t");
	// TIntIterator iter = leaves.iterator();
	// while (iter.hasNext()) {
	// w.write(cg.getConceptList().get(iter.next()).getConceptID());
	// w.write(" ");
	// }
	// w.newLine();
	// }
	// }
	// // recurse to parents
	// for (ConcRel parent : concept.getParents()) {
	// computeLeafCount(parent, icInfoMap, leafCache, cg, w);
	// }
	// }

	/**
	 * add/update icInfoMap entry for concept with the concept's subsumer count
	 * 
	 * @param concept
	 * @param icInfoMap
	 * @param subsumerMap
	 * @param w
	 * @throws IOException
	 */
	private void computeSubsumerCount(ConcRel concept,
			Map<String, IntrinsicICInfo> icInfoMap,
			Map<String, Set<String>> subsumerMap, short[] depthArray,
			BufferedWriter w) throws IOException {
		// see if we already computed this
		IntrinsicICInfo icInfo = icInfoMap.get(concept.getConceptID());
		if (icInfo != null && icInfo.getSubsumerCount() > 0) {
			return;
		}
		// if not, figure it out
		if (icInfo == null) {
			icInfo = new IntrinsicICInfo(concept);
			icInfoMap.put(concept.getConceptID(), icInfo);
		}
		Set<String> subsumers = this.getSubsumers(concept, subsumerMap,
				depthArray);
		if (w != null) {
			w.write(concept.getConceptID());
			w.write("\t");
			w.write(Integer.toString(subsumers.size()));
			w.write("\t");
			w.write(subsumers.toString());
			w.newLine();
		}
		icInfo.setSubsumerCount(subsumers.size());
		// recursively compute the children's subsumer counts
		for (ConcRel child : concept.getChildren()) {
			computeSubsumerCount(child, icInfoMap, subsumerMap, depthArray, w);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.kernel.IntrinsicInfoContentEvaluator#evaluateIntrinsicInfoContent
	 * (java.lang.String)
	 */
	@Override
	public void evaluateIntrinsicInfoContent(final Properties props)
			throws IOException {
		String conceptGraphName = props.getProperty("org.apache.ctakes.ytex.conceptGraphName");
		String conceptGraphDir = props.getProperty("org.apache.ctakes.ytex.conceptGraphDir",
				System.getProperty("java.io.tmpdir"));
		ConceptGraph cg = this.conceptDao.getConceptGraph(conceptGraphName);
		evaluateIntrinsicInfoContent(conceptGraphName, conceptGraphDir, cg);
	}

	@Override
	public void evaluateIntrinsicInfoContent(String conceptGraphName,
			String conceptGraphDir, ConceptGraph cg) throws IOException {
		log.info("computing subsumer counts");
		// compute the subsumer count
		Map<String, IntrinsicICInfo> icInfoMap = new HashMap<String, IntrinsicICInfo>();
		Map<String, Set<String>> subsumerMap = new WeakHashMap<String, Set<String>>();
		short[] depthArray = new short[cg.getConceptList().size()];
		BufferedWriter w = null;
		try {
			w = this.getOutputFile(conceptGraphName, conceptGraphDir,
					"subsumer");
			computeSubsumerCount(cg.getConceptMap().get(cg.getRoot()),
					icInfoMap, subsumerMap, depthArray, w);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
				}
			}
		}
		subsumerMap = null;
		log.info("computing max leaves");
		// get the leaves in this concept graph
		Set<String> leafSet = null;
		try {
			w = this.getOutputFile(conceptGraphName, conceptGraphDir, "allleaf");
			leafSet = this.getAllLeaves(cg, w);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
				}
			}
		}
		log.info("computing leaf counts");
		@SuppressWarnings("unchecked")
		SoftReference<HashSet<Integer>>[] leafCache = (SoftReference<HashSet<Integer>>[]) Array
				.newInstance((new SoftReference<HashSet<Integer>>(new HashSet<Integer>()))
						.getClass(), cg.getConceptList().size());
		// compute leaf count of all concepts in this graph
		try {
			w = this.getOutputFile(conceptGraphName, conceptGraphDir, "leaf");
			// for (String leaf : leafSet) {
			// computeLeafCount(cg.getConceptMap().get(leaf), icInfoMap,
			// leafCache, cg, w);
			// }
			this.getLeaves(cg.getConceptMap().get(cg.getRoot()), leafCache,
					icInfoMap, cg, w, null);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
				}
			}
		}
		leafCache = null;
		log.info("storing intrinsic ic");
		storeIntrinsicIC(conceptGraphName, leafSet.size(), icInfoMap,
				depthArray, cg);
		log.info("finished computing intrinsic ic");
	}

	private BufferedWriter getOutputFile(final String conceptGraphName,
			final String conceptGraphDir, String type) throws IOException {
		if ("true".equalsIgnoreCase(System
				.getProperty("org.apache.ctakes.ytex.ic.debug", "false"))) {
			return new BufferedWriter(new FileWriter(FileUtil.addFilenameToDir(
					conceptGraphDir, conceptGraphName + "-" + type + ".txt")));
		} else
			return null;
	}

	public Set<String> getAllLeaves(ConceptGraph cg, BufferedWriter w)
			throws IOException {
		Set<String> leafSet = new HashSet<String>();
		for (Map.Entry<String, ConcRel> con : cg.getConceptMap().entrySet()) {
			if (con.getValue().isLeaf()) {
				leafSet.add(con.getValue().getConceptID());
			}
		}
		if (w != null) {
			w.write(Integer.toString(leafSet.size()));
			w.write("\t");
			w.write(leafSet.toString());
			w.newLine();
		}
		return leafSet;
	}

	public ClassifierEvaluationDao getClassifierEvaluationDao() {
		return classifierEvaluationDao;
	}

	public ConceptDao getConceptDao() {
		return conceptDao;
	}

	// private TIntSet getLeaves(ConcRel concept,
	// SoftReference<TIntSet>[] leafCache) {
	// // look in cache
	// SoftReference<TIntSet> refLeaves = leafCache[concept.getNodeIndex()];
	// if (refLeaves != null && refLeaves.get() != null) {
	// return refLeaves.get();
	// }
	// // not in cache - compute recursively
	// TIntSet leaves = new TIntHashSet();
	// leafCache[concept.getNodeIndex()] = new SoftReference<TIntSet>(leaves);
	// if (concept.isLeaf()) {
	// // for leaves, just add the concept id
	// leaves.add(concept.getNodeIndex());
	// } else {
	// // for inner nodes, recurse
	// for (ConcRel child : concept.getChildren()) {
	// leaves.addAll(getLeaves(child, leafCache));
	// }
	// }
	// return leaves;
	// }

	/**
	 * recursively compute the subsumers of a concept
	 * 
	 * @param concept
	 * @param subsumerMap
	 * @return
	 */
	private Set<String> getSubsumers(ConcRel concept,
			Map<String, Set<String>> subsumerMap, short depthArray[]) {
		// look in cache
		if (subsumerMap.containsKey(concept.getConceptID()))
			return subsumerMap.get(concept.getConceptID());
		// not in cache - compute recursively
		Set<String> subsumers = new HashSet<String>();
		boolean calcDepth = depthArray[concept.getNodeIndex()] == 0;
		short parentMaxDepth = 0;
		if (concept.getParents() != null && !concept.getParents().isEmpty()) {
			// parents - recurse
			for (ConcRel parent : concept.getParents()) {
				subsumers.addAll(getSubsumers(parent, subsumerMap, depthArray));
				// get the deepest parent
				if (calcDepth) {
					short parentDepth = depthArray[parent.getNodeIndex()];
					if (parentDepth > parentMaxDepth)
						parentMaxDepth = parentDepth;
				}
			}
		}
		if (calcDepth)
			depthArray[concept.getNodeIndex()] = (short) (parentMaxDepth + 1);
		// add the concept itself to the set of subsumers
		subsumers.add(concept.getConceptID());
		// add this to the cache - copy the key so that this can be gc'ed as
		// needed
		subsumerMap.put(new String(concept.getConceptID()), subsumers);
		return subsumers;
	}

	public void setClassifierEvaluationDao(
			ClassifierEvaluationDao classifierEvaluationDao) {
		this.classifierEvaluationDao = classifierEvaluationDao;
	}

	public void setConceptDao(ConceptDao conceptDao) {
		this.conceptDao = conceptDao;
	}

	private void storeIntrinsicIC(String conceptGraphName, int maxLeaves,
			Map<String, IntrinsicICInfo> icInfoMap, short depthArray[],
			ConceptGraph cg) {
		FeatureEvaluation fe = new FeatureEvaluation();
		fe.setEvaluationType("intrinsic-infocontent");
		fe.setParam2(conceptGraphName);
		List<FeatureRank> listFeatureRank = new ArrayList<FeatureRank>(
				icInfoMap.size());
		double maxIC = 0d;
		short maxDepth = 0;
		for (IntrinsicICInfo icInfo : icInfoMap.values()) {
			ConcRel cr = icInfo.getConcept();
			short depth = depthArray[cr.getNodeIndex()];
			cr.setDepth(depth);
			if (depth > maxDepth)
				maxDepth = depth;
			double ic = computeIC(icInfo, maxLeaves);
			cr.setIntrinsicInfoContent(ic);
			if (ic > maxIC)
				maxIC = ic;
			if (log.isDebugEnabled())
				log.debug(icInfo.getConcept().getConceptID() + "=" + ic);
			listFeatureRank.add(new FeatureRank(fe, icInfo.getConcept()
					.getConceptID(), ic, depthArray[icInfo.getConcept()
					.getNodeIndex()]));
		}
		cg.setDepthMax(maxDepth);
		cg.setIntrinsicICMax(maxIC);
		if ("true".equalsIgnoreCase(System
				.getProperty("org.apache.ctakes.ytex.ic.debug", "false"))) {
			this.classifierEvaluationDao.deleteFeatureEvaluation(null, null,
					null, fe.getEvaluationType(), null, 0d, conceptGraphName);
			this.classifierEvaluationDao.saveFeatureEvaluation(fe,
					listFeatureRank);
		}
	}
}
