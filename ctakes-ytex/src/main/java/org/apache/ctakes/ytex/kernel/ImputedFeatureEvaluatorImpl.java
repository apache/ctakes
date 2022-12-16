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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.dao.ClassifierEvaluationDao;
import org.apache.ctakes.ytex.kernel.dao.ConceptDao;
import org.apache.ctakes.ytex.kernel.model.ConcRel;
import org.apache.ctakes.ytex.kernel.model.ConceptGraph;
import org.apache.ctakes.ytex.kernel.model.CrossValidationFold;
import org.apache.ctakes.ytex.kernel.model.FeatureEvaluation;
import org.apache.ctakes.ytex.kernel.model.FeatureParentChild;
import org.apache.ctakes.ytex.kernel.model.FeatureRank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import weka.core.ContingencyTables;

/**
 * Calculate the mutual information of each concept of a corpus wrt a concept
 * graph and classification task (label) and possibly a fold. We calculate the
 * following:
 * <ul>
 * <li>raw mutual information of each concept (infogain). We calculate the joint
 * distribution of concepts (X) and document classes (Y), and compute the mutual
 * information for each concept.
 * <li>mutual information inherited by parents (infogain-parent). For each
 * concept in the concept graph, we merge the joint distribution of child
 * concepts. This is done recursively.
 * <li>mutual information inherited by children from parents (infogain-child).
 * We take the top n concepts and assign their children (entire subgraph) the
 * mutual info of the parent.
 * </ul>
 * <p>
 * The mutual information of each concept is stored in the feature_rank table.
 * The related records in the feature_eval table have the following values:
 * <ul>
 * <li>type = infogain, infogain-parent, infogain-imputed, infogain-imputed-filt
 * <li>feature_set_name = conceptSetName
 * <li>param1 = conceptGraphName
 * </ul>
 * 
 * How this works in broad strokes:
 * <ul>
 * <li> {@link #evaluateCorpus(Parameters)} load instances, iterate through
 * labels
 * <li>
 * {@link #evaluateCorpusLabel(Parameters, ConceptGraph, InstanceData, String)}
 * load concept - set[document] map for the specified label, iterate through
 * folds
 * <li>
 * {@link #evaluateCorpusFold(Parameters, Map, ConceptGraph, InstanceData, String, Map, int)}
 * create raw joint distribution of each concept, compute parent joint
 * distributions, assign children mutual info of parents
 * <li> {@link #completeJointDistroForFold(Map, Map, Set, Set, String)} computes
 * raw joint distribution of each concept
 * <li>
 * {@link #propagateJointDistribution(Map, Parameters, String, int, ConceptGraph, Map)}
 * recursively compute parent joint distribution by merging joint distro of
 * children.
 * <li>{@link #storeChildConcepts(Parameters, String, int, ConceptGraph)} take
 * top ranked parent concepts, assign concepts in subtrees the mutual info of
 * parents. Only concepts that exist in the corpus are added (depends on
 * computing the infocontent of concepts with CorpusEvaluator)
 * </ul>
 * 
 * 
 * @author vijay
 * 
 */
public class ImputedFeatureEvaluatorImpl implements ImputedFeatureEvaluator {

	/**
	 * fill in map of Concept Id - bin - instance ids
	 * 
	 * @author vijay
	 * 
	 */
	public class ConceptInstanceMapExtractor implements RowCallbackHandler {
		ConceptGraph cg;
		Map<String, Map<String, Set<Long>>> conceptInstanceMap;

		ConceptInstanceMapExtractor(
				Map<String, Map<String, Set<Long>>> conceptInstanceMap,
				ConceptGraph cg) {
			this.cg = cg;
			this.conceptInstanceMap = conceptInstanceMap;
		}

		public void processRow(ResultSet rs) throws SQLException {
			String conceptId = rs.getString(1);
			long instanceId = rs.getLong(2);
			String x = rs.getString(3);
			Map<String, Set<Long>> binInstanceMap = conceptInstanceMap
					.get(conceptId);
			if (binInstanceMap == null) {
				// use the conceptId from the concept to save memory
				binInstanceMap = new HashMap<String, Set<Long>>(2);
				conceptInstanceMap.put(conceptId, binInstanceMap);
			}
			Set<Long> instanceIds = binInstanceMap.get(x);
			if (instanceIds == null) {
				instanceIds = new HashSet<Long>();
				binInstanceMap.put(x, instanceIds);
			}
			instanceIds.add(instanceId);
		}
	}

	/**
	 * joint distribution of concept (x) and class (y). The bins for x and y are
	 * predetermined. Typical levels for x are 0/1 (absent/present) and -1/0/1
	 * (negated/not present/affirmed).
	 * 
	 * @author vijay
	 * 
	 */
	public static class JointDistribution {
		/**
		 * merge joint distributions into a single distribution. For each value
		 * of Y, the cells for each X bin, except for the xMerge bin, are the
		 * intersection of all the instances in each of the corresponding bins.
		 * The xMerge bin gets everything that is leftover.
		 * 
		 * @param jointDistros
		 *            list of joint distribution tables to merge
		 * @param yMargin
		 *            map of y val - instance id. this could be calculated on
		 *            the fly, but we have this information already.
		 * @param xMerge
		 *            the x val that contains everything that doesn't land in
		 *            any of the other bins.
		 * @return
		 */
		public static JointDistribution merge(
				List<JointDistribution> jointDistros,
				Map<String, Set<Long>> yMargin, String xMerge) {
			Set<String> xVals = jointDistros.get(0).xVals;
			Set<String> yVals = jointDistros.get(0).yVals;
			JointDistribution mergedDistro = new JointDistribution(xVals, yVals);
			for (String y : yVals) {
				// intersect all bins besides the merge bin
				Set<Long> xMergedInst = mergedDistro.getInstances(xMerge, y);
				// everything comes into the merge bin
				// we take out things that land in other bins
				xMergedInst.addAll(yMargin.get(y));
				// iterate over other bins
				for (String x : xVals) {
					if (!x.equals(xMerge)) {
						Set<Long> intersectIds = mergedDistro
								.getInstances(x, y);
						boolean bFirstIter = true;
						// iterate over all joint distribution tables
						for (JointDistribution distro : jointDistros) {
							if (bFirstIter) {
								// 1st iter - add all
								intersectIds.addAll(distro.getInstances(x, y));
								bFirstIter = false;
							} else {
								// subsequent iteration - intersect
								intersectIds.retainAll(distro
										.getInstances(x, y));
							}
						}
						// remove from the merge bin
						xMergedInst.removeAll(intersectIds);
					}
				}
			}
			return mergedDistro;
		}

		protected double[][] contingencyTable;
		/**
		 * the entropy of X. Calculated once and returned as needed.
		 */
		protected Double entropyX = null;
		/**
		 * the entropy of X*Y. Calculated once and returned as needed.
		 */
		protected Double entropyXY = null;
		/**
		 * A y*x table where the cells hold the instance ids. We use the
		 * instance ids instead of counts so we can merge the tables.
		 */
		protected SortedMap<String, SortedMap<String, Set<Long>>> jointDistroTable;
		/**
		 * the possible values of X (e.g. concept)
		 */
		protected Set<String> xVals;

		/**
		 * the possible values of Y (e.g. text)
		 */
		protected Set<String> yVals;

		/**
		 * set up the joint distribution table.
		 * 
		 * @param xVals
		 *            the possible x values (bins)
		 * @param yVals
		 *            the possible y values (bins)
		 */
		public JointDistribution(Set<String> xVals, Set<String> yVals) {
			this.xVals = xVals;
			this.yVals = yVals;
			jointDistroTable = new TreeMap<String, SortedMap<String, Set<Long>>>();
			for (String yVal : yVals) {
				SortedMap<String, Set<Long>> yMap = new TreeMap<String, Set<Long>>();
				jointDistroTable.put(yVal, yMap);
				for (String xVal : xVals) {
					yMap.put(xVal, new HashSet<Long>());
				}
			}
		}

		public JointDistribution(Set<String> xVals, Set<String> yVals,
				Map<String, Set<Long>> xMargin, Map<String, Set<Long>> yMargin,
				String xLeftover) {
			this.xVals = xVals;
			this.yVals = yVals;
			jointDistroTable = new TreeMap<String, SortedMap<String, Set<Long>>>();
			for (String yVal : yVals) {
				SortedMap<String, Set<Long>> yMap = new TreeMap<String, Set<Long>>();
				jointDistroTable.put(yVal, yMap);
				for (String xVal : xVals) {
					yMap.put(xVal, new HashSet<Long>());
				}
			}
			for (Map.Entry<String, Set<Long>> yEntry : yMargin.entrySet()) {
				// iterate over 'rows' i.e. the class names
				String yName = yEntry.getKey();
				Set<Long> yInst = new HashSet<Long>(yEntry.getValue());
				// iterate over 'columns' i.e. the values of x
				for (Map.Entry<String, Set<Long>> xEntry : xMargin.entrySet()) {
					// copy the instances
					Set<Long> foldXInst = jointDistroTable.get(yName).get(
							xEntry.getKey());
					foldXInst.addAll(xEntry.getValue());
					// keep only the ones that are in this fold
					foldXInst.retainAll(yInst);
					// remove the instances for this value of x from the set of
					// all instances
					yInst.removeAll(foldXInst);
				}
				if (yInst.size() > 0) {
					// add the leftovers to the leftover bin
					jointDistroTable.get(yEntry.getKey()).get(xLeftover)
							.addAll(yInst);
				}
			}

		}

		// /**
		// * add an instance to the joint probability table
		// *
		// * @param x
		// * @param y
		// * @param instanceId
		// */
		// public void addInstance(String x, String y, int instanceId) {
		// // add the current row to the bin matrix
		// SortedMap<String, Set<Integer>> xMap = jointDistroTable.get(y);
		// if (xMap == null) {
		// xMap = new TreeMap<String, Set<Integer>>();
		// jointDistroTable.put(y, xMap);
		// }
		// Set<Integer> instanceSet = xMap.get(x);
		// if (instanceSet == null) {
		// instanceSet = new HashSet<Integer>();
		// xMap.put(x, instanceSet);
		// }
		// instanceSet.add(instanceId);
		// }

		// /**
		// * finalize the joint probability table wrt the specified instances.
		// If
		// * we are doing this per fold, then not all instances are going to be
		// in
		// * each fold. Limit to the instances in the specified fold.
		// * <p>
		// * Also, we might not have filled in all the cells. if necessary, put
		// * instances in the 'leftover' cell, fill it in based on the marginal
		// * distribution of the instances wrt classes.
		// *
		// * @param yMargin
		// * map of values of y to the instances with that value
		// * @param xLeftover
		// * the value of x to assign the the leftover instances
		// */
		// public JointDistribution complete(Map<String, Set<Integer>> xMargin,
		// Map<String, Set<Integer>> yMargin, String xLeftover) {
		// JointDistribution foldDistro = new JointDistribution(this.xVals,
		// this.yVals);
		// for (Map.Entry<String, Set<Integer>> yEntry : yMargin.entrySet()) {
		// // iterate over 'rows' i.e. the class names
		// String yName = yEntry.getKey();
		// Set<Integer> yInst = new HashSet<Integer>(yEntry.getValue());
		// // iterate over 'columns' i.e. the values of x
		// for (Map.Entry<String, Set<Integer>> xEntry : this.jointDistroTable
		// .get(yName).entrySet()) {
		// // copy the instances
		// Set<Integer> foldXInst = foldDistro.jointDistroTable.get(
		// yName).get(xEntry.getKey());
		// foldXInst.addAll(xEntry.getValue());
		// // keep only the ones that are in this fold
		// foldXInst.retainAll(yInst);
		// // remove the instances for this value of x from the set of
		// // all instances
		// yInst.removeAll(foldXInst);
		// }
		// if (yInst.size() > 0) {
		// // add the leftovers to the leftover bin
		// foldDistro.jointDistroTable.get(yEntry.getKey())
		// .get(xLeftover).addAll(yInst);
		// }
		// }
		// return foldDistro;
		// }

		public double[][] getContingencyTable() {
			if (contingencyTable == null) {
				contingencyTable = new double[this.yVals.size()][this.xVals
						.size()];
				int i = 0;
				for (String yVal : yVals) {
					int j = 0;
					for (String xVal : xVals) {
						contingencyTable[i][j] = jointDistroTable.get(yVal)
								.get(xVal).size();
						j++;
					}
					i++;
				}
			}
			return contingencyTable;
		}

		public double getEntropyX() {
			double probs[] = new double[xVals.size()];
			Arrays.fill(probs, 0d);
			if (entropyX == null) {
				double nTotal = 0;
				for (Map<String, Set<Long>> xInstance : this.jointDistroTable
						.values()) {
					int i = 0;
					for (Set<Long> instances : xInstance.values()) {
						double nCell = (double) instances.size();
						nTotal += nCell;
						probs[i] += nCell;
						i++;
					}
				}
				for (int i = 0; i < probs.length; i++)
					probs[i] /= nTotal;
				entropyX = entropy(probs);
			}
			return entropyX;
		}

		public double getEntropyXY() {
			double probs[] = new double[xVals.size() * yVals.size()];
			Arrays.fill(probs, 0d);
			if (entropyXY == null) {
				double nTotal = 0;
				int i = 0;
				for (Map<String, Set<Long>> xInstance : this.jointDistroTable
						.values()) {
					for (Set<Long> instances : xInstance.values()) {
						probs[i] = (double) instances.size();
						nTotal += probs[i];
						i++;
					}
				}
				for (int j = 0; j < probs.length; j++)
					probs[j] /= nTotal;
				entropyXY = entropy(probs);
			}
			return entropyXY;
		}

		public double getInfoGain() {
			return ContingencyTables.entropyOverColumns(getContingencyTable())
					- ContingencyTables
							.entropyConditionedOnRows(getContingencyTable());
		}

		public Set<Long> getInstances(String x, String y) {
			return jointDistroTable.get(y).get(x);
		}

		public double getMutualInformation(double entropyY) {
			return entropyY + this.getEntropyX() - this.getEntropyXY();
		}

		/**
		 * print out joint distribution table
		 */
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append(this.getClass().getCanonicalName());
			b.append(" [jointDistro=(");
			Iterator<Entry<String, SortedMap<String, Set<Long>>>> yIter = this.jointDistroTable
					.entrySet().iterator();
			while (yIter.hasNext()) {
				Entry<String, SortedMap<String, Set<Long>>> yEntry = yIter
						.next();
				Iterator<Entry<String, Set<Long>>> xIter = yEntry.getValue()
						.entrySet().iterator();
				while (xIter.hasNext()) {
					Entry<String, Set<Long>> xEntry = xIter.next();
					b.append(xEntry.getValue().size());
					if (xIter.hasNext())
						b.append(", ");
				}
				if (yIter.hasNext())
					b.append("| ");
			}
			b.append(")]");
			return b.toString();
		}
	}

	/**
	 * We are passing around quite a few parameters. It gets to be a pain, so
	 * put everything in an object.
	 * 
	 * @author vijay
	 * 
	 */
	public static class Parameters {
		String classFeatureQuery;
		String conceptGraphName;
		String conceptSetName;
		String corpusName;
		String freqQuery;
		double imputeWeight;

		String labelQuery;
		MeasureType measure;
		double minInfo;
		Double parentConceptEvalThreshold;
		Integer parentConceptTopThreshold;
		String splitName;
		String xLeftover;
		String xMerge;
		Set<String> xVals;

		public Parameters() {

		}

		public Parameters(Properties props) {
			corpusName = props.getProperty("org.apache.ctakes.ytex.corpusName");
			conceptGraphName = props.getProperty("org.apache.ctakes.ytex.conceptGraphName");
			conceptSetName = props.getProperty("org.apache.ctakes.ytex.conceptSetName");
			splitName = props.getProperty("org.apache.ctakes.ytex.splitName");
			labelQuery = props.getProperty("instanceClassQuery");
			classFeatureQuery = props.getProperty("org.apache.ctakes.ytex.conceptInstanceQuery");
			freqQuery = props.getProperty("org.apache.ctakes.ytex.freqQuery");
			minInfo = Double.parseDouble(props.getProperty("min.info", "1e-4"));
			String xValStr = props.getProperty("org.apache.ctakes.ytex.xVals", "0,1");
			xVals = new HashSet<String>();
			xVals.addAll(Arrays.asList(xValStr.split(",")));
			xLeftover = props.getProperty("org.apache.ctakes.ytex.xLeftover", "0");
			xMerge = props.getProperty("org.apache.ctakes.ytex.xMerge", "1");
			this.measure = MeasureType.valueOf(props.getProperty(
					"org.apache.ctakes.ytex.measure", "INFOGAIN"));
			parentConceptEvalThreshold = FileUtil.getDoubleProperty(props,
					"org.apache.ctakes.ytex.parentConceptEvalThreshold", null);
			parentConceptTopThreshold = parentConceptEvalThreshold == null ? FileUtil
					.getIntegerProperty(props,
							"org.apache.ctakes.ytex.parentConceptTopThreshold", 25) : null;
			imputeWeight = FileUtil.getDoubleProperty(props,
					"org.apache.ctakes.ytex.imputeWeight", 1d);
		}

		public String getClassFeatureQuery() {
			return classFeatureQuery;
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

		public String getFreqQuery() {
			return freqQuery;
		}

		public double getImputeWeight() {
			return imputeWeight;
		}

		public String getLabelQuery() {
			return labelQuery;
		}

		public MeasureType getMeasure() {
			return measure;
		}

		public double getMinInfo() {
			return minInfo;
		}

		public Double getParentConceptEvalThreshold() {
			return parentConceptEvalThreshold;
		}

		public Integer getParentConceptTopThreshold() {
			return parentConceptTopThreshold;
		}

		public String getSplitName() {
			return splitName;
		}

		public String getxLeftover() {
			return xLeftover;
		}

		public String getxMerge() {
			return xMerge;
		}

		public Set<String> getxVals() {
			return xVals;
		}
	}

	// /**
	// * iterates through query results and computes infogain
	// *
	// * @author vijay
	// *
	// */
	// public class JointDistroExtractor implements RowCallbackHandler {
	// /**
	// * key - fold
	// * <p/>
	// * value - map of concept id - joint distribution
	// */
	// private Map<String, JointDistribution> jointDistroMap;
	// private Set<String> xVals;
	// private Set<String> yVals;
	// private Map<Integer, String> instanceClassMap;
	//
	// public JointDistroExtractor(
	// Map<String, JointDistribution> jointDistroMap,
	// Set<String> xVals, Set<String> yVals,
	// Map<Integer, String> instanceClassMap) {
	// super();
	// this.xVals = xVals;
	// this.yVals = yVals;
	// this.jointDistroMap = jointDistroMap;
	// this.instanceClassMap = instanceClassMap;
	// }
	//
	// public void processRow(ResultSet rs) throws SQLException {
	// int instanceId = rs.getInt(1);
	// String conceptId = rs.getString(2);
	// String x = rs.getString(3);
	// String y = instanceClassMap.get(instanceId);
	// JointDistribution distro = jointDistroMap.get(conceptId);
	// if (distro == null) {
	// distro = new JointDistribution(xVals, yVals);
	// jointDistroMap.put(conceptId, distro);
	// }
	// distro.addInstance(x, y, instanceId);
	// }
	// }

	private static final Log log = LogFactory
			.getLog(ImputedFeatureEvaluatorImpl.class);

	protected static double entropy(double[] classProbs) {
		double entropy = 0;
		double log2 = Math.log(2);
		for (double prob : classProbs) {
			if (prob > 0)
				entropy += prob * Math.log(prob) / log2;
		}
		return entropy * -1;
	}

	/**
	 * calculate entropy from a list/array of probabilities
	 * 
	 * @param classProbs
	 * @return
	 */
	protected static double entropy(Iterable<Double> classProbs) {
		double entropy = 0;
		double log2 = Math.log(2);
		for (double prob : classProbs) {
			if (prob > 0)
				entropy += prob * Math.log(prob) / log2;
		}
		return entropy * -1;
	}

	@SuppressWarnings("static-access")
	public static void main(String args[]) throws ParseException, IOException {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("prop")
				.hasArg()
				.isRequired()
				.withDescription(
						"property file with queries and other parameters. todo desc")
				.create("prop"));
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			if (!KernelContextHolder.getApplicationContext()
					.getBean(ImputedFeatureEvaluator.class)
					.evaluateCorpus(line.getOptionValue("prop"))) {
				printHelp(options);
			}
		} catch (ParseException pe) {
			printHelp(options);
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"java "
								+ ImputedFeatureEvaluatorImpl.class.getName()
								+ " calculate raw, propagated, and imputed infogain for each feature",
						options);
	}

	protected ClassifierEvaluationDao classifierEvaluationDao;

	protected ConceptDao conceptDao;

	private InfoContentEvaluator infoContentEvaluator;

	protected JdbcTemplate jdbcTemplate;

	protected KernelUtil kernelUtil;
	protected NamedParameterJdbcTemplate namedParamJdbcTemplate;

	protected PlatformTransactionManager transactionManager;

	private Properties ytexProperties = null;

	/**
	 * recursively add children of cr to childConcepts
	 * 
	 * @param childConcepts
	 * @param cr
	 */
	private void addSubtree(Set<String> childConcepts, ConcRel cr) {
		childConcepts.add(cr.getConceptID());
		for (ConcRel crc : cr.getChildren()) {
			addSubtree(childConcepts, crc);
		}
	}

	private JointDistribution calcMergedJointDistribution(
			Map<String, JointDistribution> conceptJointDistroMap,
			Map<String, Integer> conceptDistMap, ConcRel cr,
			Map<String, JointDistribution> rawJointDistroMap,
			Map<String, Set<Long>> yMargin, String xMerge, double minInfo,
			List<String> path) {
		if (conceptJointDistroMap.containsKey(cr.getConceptID())) {
			return conceptJointDistroMap.get(cr.getConceptID());
		} else {
			List<JointDistribution> distroList = new ArrayList<JointDistribution>(
					cr.getChildren().size() + 1);
			int distance = -1;
			// if this concept is in the raw joint distro map, add it to the
			// list of joint distributions to merge
			if (rawJointDistroMap.containsKey(cr.getConceptID())) {
				JointDistribution rawJointDistro = rawJointDistroMap.get(cr
						.getConceptID());
				distroList.add(rawJointDistro);
				distance = 0;
			}
			// get the joint distributions of children
			for (ConcRel crc : cr.getChildren()) {
				List<String> pathChild = new ArrayList<String>(path.size() + 1);
				pathChild.addAll(path);
				pathChild.add(crc.getConceptID());
				// recurse - get joint distribution of children
				JointDistribution jdChild = calcMergedJointDistribution(
						conceptJointDistroMap, conceptDistMap, crc,
						rawJointDistroMap, yMargin, xMerge, minInfo, pathChild);
				if (jdChild != null) {
					distroList.add(jdChild);
					if (distance != 0) {
						// look at children's distance from raw data, add 1
						int distChild = conceptDistMap.get(crc.getConceptID());
						if (distance == -1 || (distChild + 1) < distance) {
							distance = distChild + 1;
						}
					}
				}
			}
			// merge the joint distributions
			JointDistribution mergedDistro;
			if (distroList.size() > 0) {
				if (distroList.size() == 1) {
					// only one joint distro - trivial merge
					mergedDistro = distroList.get(0);
				} else {
					// multiple joint distros - merge them into a new one
					mergedDistro = JointDistribution.merge(distroList, yMargin,
							xMerge);
				}
				// if (log.isDebugEnabled()) {
				// log.debug("path = " + path + ", distroList = " + distroList
				// + ", distro = " + mergedDistro);
				// }
			} else {
				// no joint distros to merge - null
				mergedDistro = null;
			}
			// save this in the map
			conceptJointDistroMap.put(cr.getConceptID(), mergedDistro);
			if (distance > -1)
				conceptDistMap.put(cr.getConceptID(), distance);
			return mergedDistro;
		}
	}

	/**
	 * 
	 */
	private double calculateFoldEntropy(Map<String, Set<Long>> classCountMap) {
		int total = 0;
		List<Double> classProbs = new ArrayList<Double>(classCountMap.size());
		// calculate total number of instances in this fold
		for (Set<Long> instances : classCountMap.values()) {
			total += instances.size();
		}
		// calculate per-class probability in this fold
		for (Set<Long> instances : classCountMap.values()) {
			classProbs.add((double) instances.size() / (double) total);
		}
		return entropy(classProbs);
	}

	/**
	 * finalize the joint distribution tables wrt a fold.
	 * 
	 * @param jointDistroMap
	 * @param yMargin
	 * @param yVals
	 * @param xVals
	 * @param xLeftover
	 */
	private Map<String, JointDistribution> completeJointDistroForFold(
			Map<String, Map<String, Set<Long>>> conceptInstanceMap,
			Map<String, Set<Long>> yMargin, Set<String> xVals,
			Set<String> yVals, String xLeftover) {
		//
		Map<String, JointDistribution> foldJointDistroMap = new HashMap<String, JointDistribution>(
				conceptInstanceMap.size());
		for (Map.Entry<String, Map<String, Set<Long>>> conceptInstance : conceptInstanceMap
				.entrySet()) {
			foldJointDistroMap.put(
					conceptInstance.getKey(),
					new JointDistribution(xVals, yVals, conceptInstance
							.getValue(), yMargin, xLeftover));
		}
		return foldJointDistroMap;
	}

	/**
	 * delete the feature evaluations before we insert them
	 * 
	 * @param params
	 * @param label
	 * @param foldId
	 */
	private void deleteFeatureEval(Parameters params, String label, int foldId) {

		for (String type : new String[] { params.getMeasure().getName(),
				params.getMeasure().getName() + SUFFIX_PROP,
				params.getMeasure().getName() + SUFFIX_IMPUTED,
				params.getMeasure().getName() + SUFFIX_IMPUTED_FILTERED })
			this.classifierEvaluationDao.deleteFeatureEvaluation(
					params.getCorpusName(), params.getConceptSetName(), label,
					type, foldId, 0d, params.getConceptGraphName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.CorpusLabelEvaluator#evaluateCorpus(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.Double, java.util.Set, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean evaluateCorpus(final Parameters params) {
		if (!(params.getCorpusName() != null
				&& params.getConceptGraphName() != null
				&& params.getLabelQuery() != null && params
				.getClassFeatureQuery() != null))
			return false;
		ConceptGraph cg = conceptDao.getConceptGraph(params
				.getConceptGraphName());
		InstanceData instanceData = kernelUtil.loadInstances(params
				.getLabelQuery());
		for (String label : instanceData.getLabelToInstanceMap().keySet()) {
			evaluateCorpusLabel(params, cg, instanceData, label);
		}
		return true;
	}

	@Override
	public boolean evaluateCorpus(String propFile) throws IOException {
		Properties props = new Properties();
		// put org.apache.ctakes.ytex properties in props
		props.putAll(this.getYtexProperties());
		// override org.apache.ctakes.ytex properties with propfile
		props.putAll(FileUtil.loadProperties(propFile, true));
		return this.evaluateCorpus(new Parameters(props));
	}

	private void evaluateCorpusFold(Parameters params,
			Map<String, Set<Long>> yMargin, ConceptGraph cg,
			InstanceData instanceData, String label,
			Map<String, Map<String, Set<Long>>> conceptInstanceMap, int foldId) {
		if (log.isInfoEnabled())
			log.info("evaluateCorpusFold() label = " + label + ", fold = "
					+ foldId);
		deleteFeatureEval(params, label, foldId);

		// get the entropy of Y for this fold
		double yEntropy = this.calculateFoldEntropy(yMargin);
		// get the joint distribution of concepts and instances
		Map<String, JointDistribution> rawJointDistro = this
				.completeJointDistroForFold(conceptInstanceMap, yMargin, params
						.getxVals(),
						instanceData.getLabelToClassMap().get(label), params
								.getxLeftover());
		List<FeatureRank> listRawRanks = new ArrayList<FeatureRank>(
				rawJointDistro.size());
		FeatureEvaluation feRaw = saveFeatureEvaluation(rawJointDistro, params,
				label, foldId, yEntropy, "", listRawRanks);
		// propagate across graph and save
		propagateJointDistribution(rawJointDistro, params, label, foldId, cg,
				yMargin);
		// store children of top concepts
		storeChildConcepts(listRawRanks, params, label, foldId, cg, true);
		storeChildConcepts(listRawRanks, params, label, foldId, cg, false);
	}

	/**
	 * evaluate corpus on label
	 * 
	 * @param classFeatureQuery
	 * @param minInfo
	 * @param xVals
	 * @param xLeftover
	 * @param xMerge
	 * @param eval
	 * @param cg
	 * @param instanceData
	 * @param label
	 * @param parentConceptTopThreshold
	 * @param parentConceptEvalThreshold
	 */
	private void evaluateCorpusLabel(Parameters params, ConceptGraph cg,
			InstanceData instanceData, String label) {
		if (log.isInfoEnabled())
			log.info("evaluateCorpusLabel() label = " + label);
		Map<String, Map<String, Set<Long>>> conceptInstanceMap = loadConceptInstanceMap(
				params.getClassFeatureQuery(), cg, label);
		for (int run : instanceData.getLabelToInstanceMap().get(label).keySet()) {
			for (int fold : instanceData.getLabelToInstanceMap().get(label)
					.get(run).keySet()) {
				int foldId = this.getFoldId(params, label, run, fold);
				// evaluate for the specified fold training set
				// construct map of class - [instance ids]
				Map<String, Set<Long>> yMargin = getFoldYMargin(instanceData,
						label, run, fold);
				evaluateCorpusFold(params, yMargin, cg, instanceData, label,
						conceptInstanceMap, foldId);
			}
		}
	}

	public ClassifierEvaluationDao getClassifierEvaluationDao() {
		return classifierEvaluationDao;
	}

	public ConceptDao getConceptDao() {
		return conceptDao;
	}

	public DataSource getDataSource(DataSource ds) {
		return this.jdbcTemplate.getDataSource();
	}

	private int getFoldId(Parameters params, String label, int run, int fold) {
		// figure out fold id
		int foldId = 0;
		if (run > 0 && fold > 0) {
			CrossValidationFold cvFold = this.classifierEvaluationDao
					.getCrossValidationFold(params.getCorpusName(),
							params.getSplitName(), label, run, fold);
			if (cvFold != null) {
				foldId = cvFold.getCrossValidationFoldId();
			} else {
				log.warn("could not find cv fold, name="
						+ params.getCorpusName() + ", run=" + run + ", fold="
						+ fold);
			}
		}
		return foldId;
	}

	private Map<String, Set<Long>> getFoldYMargin(InstanceData instanceData,
			String label, int run, int fold) {
		Map<Long, String> instanceClassMap = instanceData
				.getLabelToInstanceMap().get(label).get(run).get(fold)
				.get(true);
		Map<String, Set<Long>> yMargin = new HashMap<String, Set<Long>>();
		for (Map.Entry<Long, String> instanceClass : instanceClassMap
				.entrySet()) {
			Set<Long> instanceIds = yMargin.get(instanceClass.getValue());
			if (instanceIds == null) {
				instanceIds = new HashSet<Long>();
				yMargin.put(instanceClass.getValue(), instanceIds);
			}
			instanceIds.add(instanceClass.getKey());
		}
		return yMargin;
	}

	public InfoContentEvaluator getInfoContentEvaluator() {
		return infoContentEvaluator;
	}

	public KernelUtil getKernelUtil() {
		return kernelUtil;
	}

	public Properties getYtexProperties() {
		return ytexProperties;
	}

	private FeatureEvaluation initFeatureEval(Parameters params, String label,
			int foldId, String type) {
		FeatureEvaluation feval = new FeatureEvaluation();
		feval.setCorpusName(params.getCorpusName());
		feval.setLabel(label);
		feval.setCrossValidationFoldId(foldId);
		feval.setParam2(params.getConceptGraphName());
		feval.setEvaluationType(type);
		feval.setFeatureSetName(params.getConceptSetName());
		return feval;
	}

	/**
	 * load the map of concept - instances
	 * 
	 * @param classFeatureQuery
	 * @param cg
	 * @param label
	 * @return
	 */
	private Map<String, Map<String, Set<Long>>> loadConceptInstanceMap(
			String classFeatureQuery, ConceptGraph cg, String label) {
		Map<String, Map<String, Set<Long>>> conceptInstanceMap = new HashMap<String, Map<String, Set<Long>>>();
		Map<String, Object> args = new HashMap<String, Object>(1);
		if (label != null && label.length() > 0) {
			args.put("label", label);
		}
		ConceptInstanceMapExtractor ex = new ConceptInstanceMapExtractor(
				conceptInstanceMap, cg);
		this.namedParamJdbcTemplate.query(classFeatureQuery, args, ex);
		return conceptInstanceMap;
	}

	/**
	 * 'complete' the joint distribution tables wrt a fold (yMargin). propagate
	 * the joint distribution of all concepts recursively.
	 * 
	 * @param rawJointDistroMap
	 * @param labelEval
	 * @param cg
	 * @param yMargin
	 * @param xMerge
	 * @param minInfo
	 */
	private FeatureEvaluation propagateJointDistribution(
			Map<String, JointDistribution> rawJointDistroMap,
			Parameters params, String label, int foldId, ConceptGraph cg,
			Map<String, Set<Long>> yMargin) {
		// get the entropy of Y for this fold
		double yEntropy = this.calculateFoldEntropy(yMargin);
		// allocate a map to hold the results of the propagation across the
		// concept graph
		Map<String, JointDistribution> conceptJointDistroMap = new HashMap<String, JointDistribution>(
				cg.getConceptMap().size());
		Map<String, Integer> conceptDistMap = new HashMap<String, Integer>();
		// recurse
		calcMergedJointDistribution(conceptJointDistroMap, conceptDistMap, cg
				.getConceptMap().get(cg.getRoot()), rawJointDistroMap, yMargin,
				params.getxMerge(), params.getMinInfo(),
				Arrays.asList(new String[] { cg.getRoot() }));
		List<FeatureRank> listPropRanks = new ArrayList<FeatureRank>(
				conceptJointDistroMap.size());
		return this.saveFeatureEvaluation(conceptJointDistroMap, params, label,
				foldId, yEntropy, SUFFIX_PROP, listPropRanks);
	}

	private List<FeatureRank> rank(MeasureType measureType,
			FeatureEvaluation fe,
			Map<String, JointDistribution> rawJointDistro, double yEntropy,
			List<FeatureRank> featureRankList) {
		for (Map.Entry<String, JointDistribution> conceptJointDistro : rawJointDistro
				.entrySet()) {
			JointDistribution d = conceptJointDistro.getValue();
			if (d != null) {
				double evaluation;
				if (MeasureType.MUTUALINFO.equals(measureType)) {
					evaluation = d.getMutualInformation(yEntropy);
				} else {
					evaluation = d.getInfoGain();
				}
				if (evaluation > 1e-3) {
					FeatureRank r = new FeatureRank(fe,
							conceptJointDistro.getKey(), evaluation);
					featureRankList.add(r);
				}
			}
		}
		return FeatureRank.sortFeatureRankList(featureRankList,
				new FeatureRank.FeatureRankDesc());
	}

	private FeatureEvaluation saveFeatureEvaluation(
			Map<String, JointDistribution> rawJointDistro, Parameters params,
			String label, int foldId, double yEntropy, String suffix,
			List<FeatureRank> listRawRanks) {
		FeatureEvaluation fe = initFeatureEval(params, label, foldId, params
				.getMeasure().getName() + suffix);
		this.classifierEvaluationDao.saveFeatureEvaluation(
				fe,
				rank(params.getMeasure(), fe, rawJointDistro, yEntropy,
						listRawRanks));
		return fe;
	}

	public void setClassifierEvaluationDao(
			ClassifierEvaluationDao classifierEvaluationDao) {
		this.classifierEvaluationDao = classifierEvaluationDao;
	}

	public void setConceptDao(ConceptDao conceptDao) {
		this.conceptDao = conceptDao;
	}

	public void setDataSource(DataSource ds) {
		this.jdbcTemplate = new JdbcTemplate(ds);
		this.namedParamJdbcTemplate = new NamedParameterJdbcTemplate(ds);
	}

	// private CorpusLabelEvaluation initCorpusLabelEval(CorpusEvaluation eval,
	// String label, String splitName, int run, int fold) {
	// Integer foldId = getFoldId(eval, label, splitName, run, fold);
	// // see if the labelEval is already there
	// CorpusLabelEvaluation labelEval = corpusDao.getCorpusLabelEvaluation(
	// eval.getCorpusName(), eval.getConceptGraphName(),
	// eval.getConceptSetName(), label, foldId);
	// if (labelEval == null) {
	// // not there - add it
	// labelEval = new CorpusLabelEvaluation();
	// labelEval.setCorpus(eval);
	// labelEval.setFoldId(foldId);
	// labelEval.setLabel(label);
	// corpusDao.addCorpusLabelEval(labelEval);
	// }
	// return labelEval;
	// }

	public void setInfoContentEvaluator(
			InfoContentEvaluator infoContentEvaluator) {
		this.infoContentEvaluator = infoContentEvaluator;
	}

	// /**
	// * create the corpusEvaluation if it doesn't exist
	// *
	// * @param corpusName
	// * @param conceptGraphName
	// * @param conceptSetName
	// * @return
	// */
	// private CorpusEvaluation initEval(String corpusName,
	// String conceptGraphName, String conceptSetName) {
	// CorpusEvaluation eval = this.corpusDao.getCorpus(corpusName,
	// conceptGraphName, conceptSetName);
	// if (eval == null) {
	// eval = new CorpusEvaluation();
	// eval.setConceptGraphName(conceptGraphName);
	// eval.setConceptSetName(conceptSetName);
	// eval.setCorpusName(corpusName);
	// this.corpusDao.addCorpus(eval);
	// }
	// return eval;
	// }

	public void setKernelUtil(KernelUtil kernelUtil) {
		this.kernelUtil = kernelUtil;
	}

	//
	// private void saveLabelStatistic(String conceptID,
	// JointDistribution distroMerged, JointDistribution distroRaw,
	// CorpusLabelEvaluation labelEval, double yEntropy, double minInfo,
	// int distance) {
	// double miMerged = distroMerged.getMutualInformation(yEntropy);
	// double miRaw = distroRaw != null ? distroRaw
	// .getMutualInformation(yEntropy) : 0;
	// if (miMerged > minInfo || miRaw > minInfo) {
	// ConceptLabelStatistic stat = new ConceptLabelStatistic();
	// stat.setCorpusLabel(labelEval);
	// stat.setMutualInfo(miMerged);
	// if (distroRaw != null)
	// stat.setMutualInfoRaw(miRaw);
	// stat.setConceptId(conceptID);
	// stat.setDistance(distance);
	// this.corpusDao.addLabelStatistic(stat);
	// }
	// }

	public void setYtexProperties(Properties ytexProperties) {
		this.ytexProperties = ytexProperties;
	}

	/**
	 * save the children of the 'top' parent concepts.
	 * 
	 * @param labelEval
	 * @param parentConceptTopThreshold
	 * @param parentConceptEvalThreshold
	 * @param cg
	 * @param bAll
	 *            impute to all concepts/concepts actually in corpus. if we are
	 *            imputing to all concepts, filter by infocontent (this includes
	 *            hypernyms of concepts in the corpus). else only impute to
	 *            conrete concepts in the corpus
	 */
	public void storeChildConcepts(List<FeatureRank> listRawRanks,
			Parameters params, String label, int foldId, ConceptGraph cg,
			boolean bAll) {
		// only include concepts that actually occur in the corpus
		Map<String, Double> conceptICMap = bAll ? classifierEvaluationDao
				.getInfoContent(params.getCorpusName(),
						params.getConceptGraphName(),
						params.getConceptSetName()) : this.infoContentEvaluator
				.getFrequencies(params.getFreqQuery());
		// get the raw feature evaluations. The imputed feature evaluation is a
		// mixture of the parent feature eval and the raw feature eval.
		Map<String, Double> conceptRawEvalMap = new HashMap<String, Double>(
				listRawRanks.size());
		for (FeatureRank r : listRawRanks) {
			conceptRawEvalMap.put(r.getFeatureName(), r.getEvaluation());
		}
		// this map will get filled with the links between parent and child
		// concepts for imputation
		Map<FeatureRank, Set<FeatureRank>> childParentMap = bAll ? null
				: new HashMap<FeatureRank, Set<FeatureRank>>();
		// .getFeatureRankEvaluations(params.getCorpusName(),
		// params.getConceptSetName(), null,
		// InfoContentEvaluator.INFOCONTENT, 0,
		// params.getConceptGraphName());
		// get the top parent concepts - use either top N, or those with a
		// cutoff greater than the specified threshold
		// List<ConceptLabelStatistic> listConceptStat =
		// parentConceptTopThreshold != null ? this.corpusDao
		// .getTopCorpusLabelStat(labelEval, parentConceptTopThreshold)
		// : this.corpusDao.getThresholdCorpusLabelStat(labelEval,
		// parentConceptMutualInfoThreshold);
		String propagatedType = params.getMeasure().getName() + SUFFIX_PROP;
		List<FeatureRank> listConceptStat = params
				.getParentConceptTopThreshold() != null ? this.classifierEvaluationDao
				.getTopFeatures(params.getCorpusName(),
						params.getConceptSetName(), label, propagatedType,
						foldId, 0, params.getConceptGraphName(),
						params.getParentConceptTopThreshold())
				: this.classifierEvaluationDao.getThresholdFeatures(
						params.getCorpusName(), params.getConceptSetName(),
						label, propagatedType, foldId, 0,
						params.getConceptGraphName(),
						params.getParentConceptEvalThreshold());
		FeatureEvaluation fe = this.initFeatureEval(params, label, foldId,
				params.getMeasure().getName()
						+ (bAll ? SUFFIX_IMPUTED : SUFFIX_IMPUTED_FILTERED));
		// map of concept id to children and the 'best' statistic
		Map<String, FeatureRank> mapChildConcept = new HashMap<String, FeatureRank>();
		// get all the children of the parent concepts
		for (FeatureRank parentConcept : listConceptStat) {
			updateChildren(parentConcept, mapChildConcept, fe, cg,
					conceptICMap, conceptRawEvalMap, childParentMap,
					params.getImputeWeight(), params.getMinInfo());
		}
		// save the imputed feature ranks
		List<FeatureRank> features = new ArrayList<FeatureRank>(
				mapChildConcept.values());
		FeatureRank.sortFeatureRankList(features,
				new FeatureRank.FeatureRankDesc());
		this.classifierEvaluationDao.saveFeatureEvaluation(fe, features);
		if (!bAll) {
			// save the parent-child links
			for (Map.Entry<FeatureRank, Set<FeatureRank>> childParentEntry : childParentMap
					.entrySet()) {
				FeatureRank child = childParentEntry.getKey();
				for (FeatureRank parent : childParentEntry.getValue()) {
					FeatureParentChild parchd = new FeatureParentChild();
					parchd.setFeatureRankParent(parent);
					parchd.setFeatureRankChild(child);
					this.classifierEvaluationDao.saveFeatureParentChild(parchd);
				}
			}
		}
	}

	/**
	 * add the children of parentConcept to mapChildConcept. Assign the child
	 * the best mutual information value of the parent.
	 * 
	 * @param parentConcept
	 * @param mapChildConcept
	 * @param labelEval
	 * @param cg
	 * @param parentChildMap
	 * @param conceptRawEvalMap
	 */
	private void updateChildren(FeatureRank parentConcept,
			Map<String, FeatureRank> mapChildConcept, FeatureEvaluation fe,
			ConceptGraph cg, Map<String, Double> conceptICMap,
			Map<String, Double> conceptRawEvalMap,
			Map<FeatureRank, Set<FeatureRank>> childParentMap,
			double imputeWeight, double minInfo) {
		ConcRel cr = cg.getConceptMap().get(parentConcept.getFeatureName());
		Set<String> childConcepts = new HashSet<String>();
		addSubtree(childConcepts, cr);
		for (String childConceptId : childConcepts) {
			// only add the child to the map if it exists in the corpus
			if (conceptICMap.containsKey(childConceptId)) {
				FeatureRank chd = mapChildConcept.get(childConceptId);
				// create the child if it does not already exist
				if (chd == null) {
					chd = new FeatureRank(fe, childConceptId, 0d);
					mapChildConcept.put(childConceptId, chd);
				}
				// give the child the mutual info of the parent with the highest
				// score
				double rawEvaluation = conceptRawEvalMap
						.containsKey(childConceptId) ? conceptRawEvalMap
						.get(childConceptId) : minInfo;
				double imputedEvaluation = (imputeWeight * parentConcept
						.getEvaluation())
						+ ((1 - imputeWeight) * rawEvaluation);
				if (chd.getEvaluation() < imputedEvaluation) {
					chd.setEvaluation(imputedEvaluation);
				}
				// add the relationship to the parentChildMap
				// do this only if the childParentMap is not null
				if (childParentMap != null) {
					Set<FeatureRank> parents = childParentMap.get(chd);
					if (parents == null) {
						parents = new HashSet<FeatureRank>(10);
						childParentMap.put(chd, parents);
					}
					parents.add(parentConcept);
				}
			}
		}
	}
}
