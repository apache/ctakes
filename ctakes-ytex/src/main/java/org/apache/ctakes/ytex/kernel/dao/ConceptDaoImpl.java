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
package org.apache.ctakes.ytex.kernel.dao;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import org.apache.ctakes.ytex.kernel.FileUtil;
import org.apache.ctakes.ytex.kernel.IntrinsicInfoContentEvaluator;
import org.apache.ctakes.ytex.kernel.KernelContextHolder;
import org.apache.ctakes.ytex.kernel.model.ConcRel;
import org.apache.ctakes.ytex.kernel.model.ConceptGraph;
import org.hibernate.SessionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ConceptDaoImpl implements ConceptDao {
	private static final String CONCEPT_GRAPH_PATH = "org/apache/ctakes/ytex/conceptGraph/";
	/**
	 * the default concept id for the root. override with -Dytex.defaultRootId
	 */
	private static final String DEFAULT_ROOT_ID = "C0000000";
	/**
	 * ignore forbidden concepts. list Taken from umls-interface. f concept is
	 * one of the following just return #C1274012|Ambiguous concept (inactive
	 * concept) if($concept=~/C1274012/) { return 1; } #C1274013|Duplicate
	 * concept (inactive concept) if($concept=~/C1274013/) { return 1; }
	 * #C1276325|Reason not stated concept (inactive concept)
	 * if($concept=~/C1276325/) { return 1; } #C1274014|Outdated concept
	 * (inactive concept) if($concept=~/C1274014/) { return 1; }
	 * #C1274015|Erroneous concept (inactive concept) if($concept=~/C1274015/) {
	 * return 1; } #C1274021|Moved elsewhere (inactive concept)
	 * if($concept=~/C1274021/) { return 1; } #C1443286|unapproved attribute
	 * if($concept=~/C1443286/) { return 1; } #C1274012|non-current concept -
	 * ambiguous if($concept=~/C1274012/) { return 1; } #C2733115|limited status
	 * concept if($concept=~/C2733115/) { return 1; }
	 */
	private static final String defaultForbiddenConceptArr[] = new String[] {
			"C1274012", "C1274013", "C1276325", "C1274014", "C1274015",
			"C1274021", "C1443286", "C1274012", "C2733115" };
	private static Set<String> defaultForbiddenConcepts;
	private static final Log log = LogFactory.getLog(ConceptDaoImpl.class);

	static {
		defaultForbiddenConcepts = new HashSet<String>();
		defaultForbiddenConcepts.addAll(Arrays
				.asList(defaultForbiddenConceptArr));
	}

	/**
	 * create a concept graph.
	 * 
	 * This expects a property file in the classpath under
	 * CONCEPT_GRAPH_PATH/[name].xml
	 * <p/>
	 * If the properties file is found in a directory, the concept graph will be
	 * written there.
	 * <p/>
	 * Else (e.g. if the props file is coming from a jar), the concept graph
	 * will be written to the directory specified via the system property/ytex
	 * property 'org.apache.ctakes.ytex.conceptGraphDir'
	 * <p/>
	 * Else if the 'org.apache.ctakes.ytex.conceptGraphDir' property is not
	 * defined, the concept graph will be written to the conceptGraph
	 * subdirectory relative to ytex.properties (if ytex.properties is in a
	 * directory).
	 * 
	 * @param args
	 */
	@SuppressWarnings("static-access")
	public static void main(String args[]) throws ParseException, IOException {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("name")
				.hasArg()
				.isRequired()
				.withDescription(
						"name of concept graph.  A property file with the name "
								+ CONCEPT_GRAPH_PATH
								+ "/[name].xml must exist on the classpath")
				.create("name"));
		try {
			CommandLineParser parser = new GnuParser();
			CommandLine line = parser.parse(options, args);
			String name = line.getOptionValue("name");
			String propRes = CONCEPT_GRAPH_PATH + name + ".xml";
			URL url = ConceptDaoImpl.class.getClassLoader()
					.getResource(propRes);
			if (url == null) {
				System.out.println("properties file could not be located: "
						+ propRes);
				return;
			}
			// load properties
			Properties props = new Properties();
			InputStream is = ConceptDaoImpl.class.getClassLoader()
					.getResourceAsStream(propRes);
			try {
				props.loadFromXML(is);
			} finally {
				is.close();
			}
			// determine directory for concept graph - attempt to put in same
			// dir as props
			File fDir = null;
			if ("file".equals(url.getProtocol())) {
				File f;
				try {
					f = new File(url.toURI());
				} catch (URISyntaxException e) {
					f = new File(url.getPath());
				}
				fDir = f.getParentFile();
			}
			String conceptGraphQuery = props
					.getProperty("ytex.conceptGraphQuery");
			String strCheckCycle = props.getProperty("ytex.checkCycle", "true");
			String forbiddenConceptList = props
					.getProperty("ytex.forbiddenConcepts");
			Set<String> forbiddenConcepts;
			if (forbiddenConceptList != null) {
				forbiddenConcepts = new HashSet<String>();
				forbiddenConcepts.addAll(Arrays.asList(forbiddenConceptList
						.split(",")));
			} else {
				forbiddenConcepts = defaultForbiddenConcepts;
			}
			boolean checkCycle = true;
			if ("false".equalsIgnoreCase(strCheckCycle)
					|| "no".equalsIgnoreCase(strCheckCycle))
				checkCycle = false;
			if (!Strings.isNullOrEmpty(name)
					&& !Strings.isNullOrEmpty(conceptGraphQuery)) {
				KernelContextHolder
						.getApplicationContext()
						.getBean(ConceptDao.class)
						.createConceptGraph(
								fDir != null ? fDir.getAbsolutePath() : null,
								name, conceptGraphQuery, checkCycle,
								forbiddenConcepts);
			} else {
				printHelp(options);
			}
		} catch (ParseException pe) {
			printHelp(options);
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + ConceptDaoImpl.class.getName()
				+ " generate concept graph", options);
	}

	private IntrinsicInfoContentEvaluator intrinsicInfoContentEvaluator;

	private JdbcTemplate jdbcTemplate;

	private SessionFactory sessionFactory;

	private Properties ytexProperties;

	/**
	 * add the relationship to the concept map
	 * 
	 * @param conceptMap
	 * @param conceptIndexMap
	 * @param conceptList
	 * @param roots
	 * @param conceptPair
	 */
	private void addRelation(ConceptGraph cg, Set<String> roots,
			String childCUI, String parentCUI, boolean checkCycle,
			Set<String> forbiddenConcepts) {
		if (forbiddenConcepts.contains(childCUI)
				|| forbiddenConcepts.contains(parentCUI)) {
			// ignore relationships to useless concepts
			if (log.isDebugEnabled())
				log.debug("skipping relation because of forbidden concept: par="
						+ parentCUI + " child=" + childCUI);
			return;
		}
		// ignore self relations
		if (!childCUI.equals(parentCUI)) {
			boolean parNull = false;
			// get parent from cui map
			ConcRel crPar = cg.getConceptMap().get(parentCUI);
			if (crPar == null) {
				parNull = true;
				// parent not in cui map - add it
				crPar = cg.addConcept(parentCUI);
				// this is a candidate root - add it to the set of roots
				roots.add(parentCUI);
			}
			// get the child cui
			ConcRel crChild = cg.getConceptMap().get(childCUI);
			// crPar already has crChild, return
			if (crChild != null && crPar.getChildren().contains(crChild))
				return;
			// avoid cycles - don't add child cui if it is an ancestor
			// of the parent. if the child is not yet in the map, then it can't
			// possibly induce a cycle.
			// if the parent is not yet in the map, it can't induce a cycle
			// else check for cycles
			// @TODO: this is very inefficient. implement feedback arc algo
			boolean bCycle = !parNull && crChild != null && checkCycle
					&& checkCycle(crPar, crChild);
			if (bCycle) {
				log.warn("skipping relation that induces cycle: par="
						+ parentCUI + ", child=" + childCUI);
			} else {
				if (crChild == null) {
					// child not in cui map - add it
					crChild = cg.addConcept(childCUI);
				} else {
					// remove the cui from the list of candidate roots
					if (roots.contains(childCUI))
						roots.remove(childCUI);
				}
				// link child to parent and vice-versa
				crPar.getChildren().add(crChild);
				crChild.getParents().add(crPar);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.dao.ConceptDao#createConceptGraph
	 */
	@Override
	public void createConceptGraph(String dir, String name, String query,
			final boolean checkCycle, final Set<String> forbiddenConcepts)
			throws IOException {
		ConceptGraph conceptGraph = this.readConceptGraph(name);
		if (conceptGraph != null) {
			if (log.isWarnEnabled())
				log.warn("createConceptGraph(): concept graph already exists, will not create a new one.  Delete existing concept graph if you want to recreate it.");
		} else {
			String outputDir = dir;
			if (Strings.isNullOrEmpty(outputDir)) {
				outputDir = getDefaultConceptGraphDir();
			}
			if (Strings.isNullOrEmpty(outputDir)) {
				throw new IllegalArgumentException(
						"could not determine default concept graph directory; please set property org.apache.ctakes.ytex.conceptGraphDir");
			}
			if (log.isInfoEnabled())
				log.info("createConceptGraph(): file not found, creating concept graph from database.");
			final ConceptGraph cg = new ConceptGraph();
			final Set<String> roots = new HashSet<String>();
			this.jdbcTemplate.query(query, new RowCallbackHandler() {
				int nRowsProcessed = 0;

				@Override
				public void processRow(ResultSet rs) throws SQLException {
					String child = rs.getString(1);
					String parent = rs.getString(2);
					addRelation(cg, roots, child, parent, checkCycle,
							forbiddenConcepts);
					nRowsProcessed++;
					if (nRowsProcessed % 10000 == 0) {
						log.info("processed " + nRowsProcessed + " edges");
					}
				}
			});
			// set the root
			// if there is only one potential root, use it
			// else use a synthetic root and add all the roots as its children
			String rootId = null;
			if (log.isDebugEnabled())
				log.debug("roots: " + roots);
			if (roots.size() == 1) {
				rootId = roots.iterator().next();
			} else {
				rootId = System
						.getProperty("org.apache.ctakes.ytex.defaultRootId",
								DEFAULT_ROOT_ID);
				ConcRel crRoot = cg.addConcept(rootId);
				for (String crChildId : roots) {
					ConcRel crChild = cg.getConceptMap().get(crChildId);
					crRoot.getChildren().add(crChild);
					crChild.getParents().add(crRoot);
				}
			}
			cg.setRoot(rootId);
			// can't get the maximum depth unless we're sure there are no
			// cycles
			if (checkCycle) {
				log.info("computing intrinsic info for concept graph: " + name);
				this.intrinsicInfoContentEvaluator
						.evaluateIntrinsicInfoContent(name, outputDir, cg);
			}
			writeConceptGraph(outputDir, name, cg);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.kernel.dao.ConceptDao#getConceptGraph(java.util
	 * .Set)
	 */
	public ConceptGraph getConceptGraph(String name) {
		ConceptGraph cg = this.readConceptGraph(name);
		if (cg != null) {
			this.initializeConceptGraph(cg);
			if (log.isInfoEnabled()) {
				log.info(String.format("concept graph %s, vertices: %s", name,
						cg.getConceptList().size()));
			}
		}
		return cg;
	}

	private File urlToFile(URL url) {
		if (url != null && "file".equals(url.getProtocol())) {
			File f;
			try {
				f = new File(url.toURI());
			} catch (URISyntaxException e) {
				f = new File(url.getPath());
			}
			return f;
		} else {
			return null;
		}

	}

	/**
	 * use value of org.apache.ctakes.ytex.conceptGraphDir if defined. else try
	 * to determine ytex.properties location and use the conceptGraph directory
	 * there. else return null
	 * 
	 * @return
	 */
	public String getDefaultConceptGraphDir() {
		String cdir = System.getProperty(
				"org.apache.ctakes.ytex.conceptGraphDir", ytexProperties
						.getProperty("org.apache.ctakes.ytex.conceptGraphDir"));
		// default to [ytex.properties directory]/conceptGraph
		if (Strings.isNullOrEmpty(cdir)) {
			URL url = this.getClass().getResource(
					"/org/apache/ctakes/ytex/ytex.properties");
			File f = urlToFile(url);
			if (f != null) {
				File baseDir = f.getParentFile();
				if (baseDir.exists() && baseDir.isDirectory()) {
					cdir = baseDir.getAbsolutePath() + File.separator
							+ "conceptGraph";
				}
			}
		}
		return cdir;
	}

	public DataSource getDataSource(DataSource ds) {
		return this.jdbcTemplate.getDataSource();
	}

	public IntrinsicInfoContentEvaluator getIntrinsicInfoContentEvaluator() {
		return intrinsicInfoContentEvaluator;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public Properties getYtexProperties() {
		return ytexProperties;
	}

	private boolean checkCycle(ConcRel crPar, ConcRel crChild) {
		HashSet<Integer> visitedNodes = new HashSet<Integer>();
		return hasAncestor(crPar, crChild, visitedNodes);
	}

	/**
	 * check cycle.
	 * 
	 * @param crPar
	 *            parent
	 * @param crChild
	 *            child that should not be an ancestor of parent
	 * @param visitedNodes
	 *            nodes we've visited in our search. keep track of this to avoid
	 *            visiting the same node multiple times
	 * @return true if crChild is an ancestor of crPar
	 */
	private boolean hasAncestor(ConcRel crPar, ConcRel crChild,
			HashSet<Integer> visitedNodes) {
		// see if we've already visited this node - if yes then no need to redo
		// this
		if (visitedNodes.contains(crPar.getNodeIndex()))
			return false;
		// see if we're the same
		if (crPar.getNodeIndex() == crChild.getNodeIndex())
			return true;
		// recurse
		for (ConcRel c : crPar.getParents()) {
			if (hasAncestor(c, crChild, visitedNodes))
				return true;
		}
		// add ourselves to the set of visited nodes so we no not to revisit
		// this
		visitedNodes.add(crPar.getNodeIndex());
		return false;
	}

	/**
	 * replace cui strings in concrel with references to other nodes. initialize
	 * the concept list
	 * 
	 * @param cg
	 * @return
	 */
	private ConceptGraph initializeConceptGraph(ConceptGraph cg) {
		ImmutableMap.Builder<String, ConcRel> mb = new ImmutableMap.Builder<String, ConcRel>();
		for (ConcRel cr : cg.getConceptList()) {
			// use adjacency list representation for concept graphs that have
			// cycles
			if (cg.getDepthMax() > 0)
				cr.constructRel(cg.getConceptList());
			mb.put(cr.getConceptID(), cr);
		}
		cg.setConceptMap(mb.build());
		return cg;
	}

	private ConceptGraph readConceptGraph(String name) {
		ObjectInputStream is = null;
		try {
			// try loading from classpath
			InputStream resIs = this.getClass().getClassLoader()
					.getResourceAsStream(CONCEPT_GRAPH_PATH + name + ".gz");
			if (resIs == null) {
				String cdir = this.getDefaultConceptGraphDir();
				if (cdir == null) {
					throw new IllegalArgumentException(
							"could not determine default concept graph directory; please set property org.apache.ctakes.ytex.conceptGraphDir");
				}
				File f = new File(cdir + "/" + name + ".gz");
				log.info("could not load conceptGraph from classpath, attempt to load from: "
						+ f.getAbsolutePath());
				if (f.exists()) {
					resIs = new FileInputStream(f);
				} else {
					log.info(f.getAbsolutePath()
							+ " not found, cannot load concept graph");
				}
			} else {
				log.info("loading concept graph from "
						+ this.getClass().getClassLoader()
								.getResource(CONCEPT_GRAPH_PATH + name + ".gz"));
			}
			if (resIs != null) {
				is = new ObjectInputStream(new BufferedInputStream(
						new GZIPInputStream(resIs)));
				return (ConceptGraph) is.readObject();
			} else {
				log.info("could not load conceptGraph: " + name);
				return null;
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	public void setDataSource(DataSource ds) {
		this.jdbcTemplate = new JdbcTemplate(ds);
	}

	public void setIntrinsicInfoContentEvaluator(
			IntrinsicInfoContentEvaluator intrinsicInfoContentEvaluator) {
		this.intrinsicInfoContentEvaluator = intrinsicInfoContentEvaluator;
	}

	// /**
	// * get maximum depth of graph.
	// *
	// * @param roots
	// * @param conceptMap
	// * @return
	// */
	// private int calculateDepthMax(String rootId, Map<String, ConcRel>
	// conceptMap) {
	// ConcRel crRoot = conceptMap.get(rootId);
	// return crRoot.depthMax();
	// }

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setYtexProperties(Properties ytexProperties) {
		this.ytexProperties = new Properties(ytexProperties);
		this.ytexProperties.putAll(System.getProperties());
	}

	// /**
	// * add parent to all descendants of crChild
	// *
	// * @param crPar
	// * @param crChild
	// * @param ancestorCache
	// */
	// private void updateDescendants(Set<Integer> ancestorsPar, ConcRel
	// crChild,
	// Map<Integer, Set<Integer>> ancestorCache, int depth) {
	// if (ancestorCache != null) {
	// Set<Integer> ancestors = ancestorCache.get(crChild.nodeIndex);
	// if (ancestors != null)
	// ancestors.addAll(ancestorsPar);
	// // recurse
	// for (ConcRel crD : crChild.getChildren()) {
	// updateDescendants(ancestorsPar, crD, ancestorCache, depth + 1);
	// }
	// }
	// }

	/**
	 * write the concept graph, create parent directories as required
	 * 
	 * @param name
	 * @param cg
	 */
	private void writeConceptGraph(String dir, String name, ConceptGraph cg) {
		ObjectOutputStream os = null;
		String outputDir = dir;
		File cgFile = new File(outputDir + "/" + name + ".gz");
		log.info("writing concept graph: " + cgFile.getAbsolutePath());
		if (!cgFile.getParentFile().exists())
			cgFile.getParentFile().mkdirs();
		try {
			os = new ObjectOutputStream(new BufferedOutputStream(
					new GZIPOutputStream(new FileOutputStream(cgFile))));
			// replace the writable list with an immutable list
			cg.setConceptList(ImmutableList.copyOf(cg.getConceptList()));
			os.writeObject(cg);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

}
