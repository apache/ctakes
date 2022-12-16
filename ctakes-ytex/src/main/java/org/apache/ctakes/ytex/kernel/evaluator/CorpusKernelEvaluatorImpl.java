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
package org.apache.ctakes.ytex.kernel.evaluator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.dao.DBUtil;
import org.apache.ctakes.ytex.kernel.dao.KernelEvaluationDao;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluation;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluationInstance;
import org.apache.ctakes.ytex.kernel.tree.InstanceTreeBuilder;
import org.apache.ctakes.ytex.kernel.tree.Node;
import org.apache.ctakes.ytex.kernel.tree.TreeMappingInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;


public class CorpusKernelEvaluatorImpl implements CorpusKernelEvaluator {
	protected class InstanceIDRowMapper implements RowMapper<Integer> {

		@Override
		public Integer mapRow(ResultSet rs, int arg1) throws SQLException {
			return rs.getInt(1);
		}

	}

	public class SliceEvaluator implements Callable<Object> {
		Map<Long, Node> instanceIDMap;
		int nMod;
		int nSlice;
		boolean evalTest;

		public SliceEvaluator(Map<Long, Node> instanceIDMap, int nMod,
				int nSlice, boolean evalTest) {
			this.nSlice = nSlice;
			this.nMod = nMod;
			this.instanceIDMap = instanceIDMap;
			this.evalTest = evalTest;
		}

		@Override
		public Object call() throws Exception {
			try {
				evaluateKernelOnCorpus(instanceIDMap, nMod, nSlice, evalTest);
			} catch (Exception e) {
				log.error("error on slice: " + nSlice, e);
				throw e;
			}
			return null;
		}
	}

	private static final Log log = LogFactory
			.getLog(CorpusKernelEvaluator.class);

	@SuppressWarnings("static-access")
	private static Options initOptions() {
		Options options = new Options();
		options.addOption(OptionBuilder
				.withArgName("classpath*:simSvcBeanRefContext.xml")
				.hasArg()
				.withDescription(
						"use specified beanRefContext.xml, default classpath*:simSvcBeanRefContext.xml")
				.create("beanref"));
		options.addOption(OptionBuilder
				.withArgName("kernelApplicationContext")
				.hasArg()
				.withDescription(
						"use specified applicationContext, default kernelApplicationContext")
				.create("appctx"));
		options.addOption(OptionBuilder
				.withArgName("beans-corpus.xml")
				.hasArg()
				.withDescription(
						"use specified beans.xml, no default.  This file is typically required.")
				.create("beans"));
		options.addOption(OptionBuilder
				.withArgName("yes/no")
				.hasArg()
				.withDescription(
						"should test instances be evaluated? default no.")
				.create("evalTest"));
		options.addOption(OptionBuilder
				.withArgName("instanceMap.obj")
				.hasArg()
				.withDescription(
						"load instanceMap from file system instead of from db.  Use after storing instance map.  If not specified will attempt to load from db.")
				.create("loadInstanceMap"));
		options.addOption(OptionBuilder
				.withDescription(
						"for parallelization, split the instances into mod slices")
				.hasArg().create("mod"));
		options.addOption(OptionBuilder
				.withDescription(
						"for parallelization, parameter that determines which slice we work on.  If this is not specified, nMod threads will be started to evaluate all slices in parallel.")
				.hasArg().create("slice"));
		options.addOption(new Option("help", "print this message"));
		return options;
	}

	public static void main(String args[]) throws Exception {
		Options options = initOptions();

		if (args.length == 0) {
			printHelp(options);
		} else {
			CommandLineParser parser = new GnuParser();
			try {
				// parse the command line arguments
				CommandLine line = parser.parse(options, args);
				// parse the command line arguments
				String beanRefContext = line.getOptionValue("beanref",
						"classpath*:simSvcBeanRefContext.xml");
				String contextName = line.getOptionValue("appctx",
						"kernelApplicationContext");
				String beans = line.getOptionValue("beans");
				ApplicationContext appCtx = (ApplicationContext) ContextSingletonBeanFactoryLocator
						.getInstance(beanRefContext)
						.useBeanFactory(contextName).getFactory();
				ApplicationContext appCtxSource = appCtx;
				if (beans != null) {
					appCtxSource = new FileSystemXmlApplicationContext(
							new String[] { beans }, appCtx);
				}
				evalKernel(appCtxSource, line);
			} catch (ParseException e) {
				printHelp(options);
				throw e;
			}
		}
	}

	private static void evalKernel(ApplicationContext appCtxSource,
			CommandLine line) throws Exception {
		InstanceTreeBuilder builder = appCtxSource
				.getBean(InstanceTreeBuilder.class);
		CorpusKernelEvaluator corpusEvaluator = appCtxSource
				.getBean(CorpusKernelEvaluator.class);
		String loadInstanceMap = line.getOptionValue("loadInstanceMap");
		String strMod = line.getOptionValue("mod");
		String strSlice = line.getOptionValue("slice");
		boolean evalTest = "yes".equalsIgnoreCase(line.getOptionValue(
				"evalTest", "no"))
				|| "true".equalsIgnoreCase(line
						.getOptionValue("evalTest", "no"));
		int nMod = strMod != null ? Integer.parseInt(strMod) : 0;
		Integer nSlice = null;
		if (nMod == 0) {
			nSlice = 0;
		} else if (strSlice != null) {
			nSlice = Integer.parseInt(strSlice);
		}
		Map<Long, Node> instanceMap = null;
		if (loadInstanceMap != null) {
			instanceMap = builder.loadInstanceTrees(loadInstanceMap);
		} else {
			instanceMap = builder.loadInstanceTrees(appCtxSource
					.getBean(TreeMappingInfo.class));
		}
		if (nSlice != null) {
			corpusEvaluator.evaluateKernelOnCorpus(instanceMap, nMod, nSlice,
					evalTest);
		} else {
			corpusEvaluator.evaluateKernelOnCorpus(instanceMap, nMod, evalTest);
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"java org.apache.ctakes.ytex.kernel.evaluator.CorpusKernelEvaluatorImpl",
						options);
	}

	private DataSource dataSource;

	private String experiment;

	private int foldId = 0;

	private String instanceIDQuery;

	private Kernel instanceKernel;

	private InstanceTreeBuilder instanceTreeBuilder;

	private JdbcTemplate jdbcTemplate;

	private KernelEvaluationDao kernelEvaluationDao;

	private String label = DBUtil.getEmptyString();

	private String name;

	private double param1 = 0;

	private String param2 = DBUtil.getEmptyString();	
	private PlatformTransactionManager transactionManager;
	private TreeMappingInfo treeMappingInfo;
	private TransactionTemplate txTemplate;

	private void evalInstance(Map<Long, Node> instanceIDMap,
			KernelEvaluation kernelEvaluation, long instanceId1,
			SortedSet<Long> rightDocumentIDs) {
		if (log.isDebugEnabled()) {
			log.debug("left: " + instanceId1 + ", right: " + rightDocumentIDs);
		}
		for (long instanceId2 : rightDocumentIDs) {
			// if (instanceId1 != instanceId2) {
			final long i1 = instanceId1;
			final long i2 = instanceId2;
			final Node root1 = instanceIDMap.get(i1);
			final Node root2 = instanceIDMap.get(i2);
			if (root1 != null && root2 != null) {
				kernelEvaluationDao.storeKernel(kernelEvaluation, i1, i2,
						instanceKernel.evaluate(root1, root2));
			}
		}
	}

	@Override
	public void evaluateKernelOnCorpus() {
		final Map<Long, Node> instanceIDMap = instanceTreeBuilder
				.loadInstanceTrees(treeMappingInfo);
		this.evaluateKernelOnCorpus(instanceIDMap, 0, 0, false);
	}

	@Override
	public void evaluateKernelOnCorpus(Map<Long, Node> instanceIDMap, int nMod,
			boolean evalTest) throws InterruptedException {
		ExecutorService svc = Executors.newFixedThreadPool(nMod);
		List<Callable<Object>> taskList = new ArrayList<Callable<Object>>(nMod);
		for (int nSlice = 1; nSlice <= nMod; nSlice++) {
			taskList.add(new SliceEvaluator(instanceIDMap, nMod, nSlice,
					evalTest));
		}
		svc.invokeAll(taskList);
		svc.shutdown();
		svc.awaitTermination(60 * 4, TimeUnit.MINUTES);
	}

	public void evaluateKernelOnCorpus(final Map<Long, Node> instanceIDMap,
			int nMod, int nSlice, boolean evalTest) {
		KernelEvaluation kernelEvaluationTmp = new KernelEvaluation();
		kernelEvaluationTmp.setExperiment(this.getExperiment());
		kernelEvaluationTmp.setFoldId(this.getFoldId());
		kernelEvaluationTmp.setLabel(this.getLabel());
		kernelEvaluationTmp.setCorpusName(this.getName());
		kernelEvaluationTmp.setParam1(getParam1());
		kernelEvaluationTmp.setParam2(getParam2());
		final KernelEvaluation kernelEvaluation = this.kernelEvaluationDao
				.storeKernelEval(kernelEvaluationTmp);
		final List<Long> documentIds = new ArrayList<Long>();
		final List<Long> testDocumentIds = new ArrayList<Long>();
		loadDocumentIds(documentIds, testDocumentIds, instanceIDQuery);
		if (!evalTest) {
			// throw away the test ids if we're not going to evaluate them
			testDocumentIds.clear();
		}
		int nStart = 0;
		int nEnd = documentIds.size();
		int total = documentIds.size();
		if (nMod > 0) {
			nMod = Math.min(total, nMod);
		}
		if (nMod > 0 && nSlice > nMod) {
			log.info("more slices than documents, skipping slice: " + nSlice);
			return;
		}
		if (nMod > 0) {
			int sliceSize = total / nMod;
			nStart = sliceSize * (nSlice - 1);
			if (nSlice != nMod)
				nEnd = nStart + sliceSize;
		}
		for (int i = nStart; i < nEnd; i++) {
			// left hand side of kernel evaluation
			final long instanceId1 = documentIds.get(i);
			if (log.isInfoEnabled())
				log.info("evaluating kernel for instance_id1 = " + instanceId1);
			// list of instance ids right hand side of kernel evaluation
			final SortedSet<Long> rightDocumentIDs = new TreeSet<Long>(
					testDocumentIds);
			if (i < documentIds.size()) {
				// rightDocumentIDs.addAll(documentIds.subList(i + 1,
				// documentIds.size() - 1));
				rightDocumentIDs.addAll(documentIds.subList(i,
						documentIds.size()));
			}
			// remove instances already evaluated
			for (KernelEvaluationInstance kEval : this.kernelEvaluationDao
					.getAllKernelEvaluationsForInstance(kernelEvaluation,
							instanceId1)) {
				rightDocumentIDs
						.remove(instanceId1 == kEval.getInstanceId1() ? kEval
								.getInstanceId2() : kEval.getInstanceId1());
			}
			// kernel evaluations for this instance are done in a single tx
			// hibernate can batch insert these
			txTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus arg0) {
					evalInstance(instanceIDMap, kernelEvaluation, instanceId1,
							rightDocumentIDs);
					return null;
				}
			});

		}
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public String getExperiment() {
		return experiment;
	}

	public int getFoldId() {
		return foldId;
	}

	public String getInstanceIDQuery() {
		return instanceIDQuery;
	}

	public Kernel getInstanceKernel() {
		return instanceKernel;
	}

	public InstanceTreeBuilder getInstanceTreeBuilder() {
		return instanceTreeBuilder;
	}

	public KernelEvaluationDao getKernelEvaluationDao() {
		return kernelEvaluationDao;
	}

	public String getLabel() {
		return label;
	}

	public String getName() {
		return name;
	}

	public double getParam1() {
		return param1;
	}

	public String getParam2() {
		return param2;
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public TreeMappingInfo getTreeMappingInfo() {
		return treeMappingInfo;
	}

	/**
	 * load the document ids from the instanceIDQuery
	 * 
	 * @param documentIds
	 * @param testDocumentIds
	 * @param instanceIDQuery
	 */
	private void loadDocumentIds(final List<Long> documentIds,
			final List<Long> testDocumentIds, final String instanceIDQuery) {
		txTemplate.execute(new TransactionCallback<Object>() {
			@Override
			public List<Integer> doInTransaction(TransactionStatus arg0) {
				jdbcTemplate.query(instanceIDQuery, new RowCallbackHandler() {
					Boolean trainFlag = null;

					/**
					 * <ul>
					 * <li>1st column - document id
					 * <li>2nd column - optional - train/test flag (train = 1)
					 * </ul>
					 */
					@Override
					public void processRow(ResultSet rs) throws SQLException {
						if (trainFlag == null) {
							// see how many columns there are
							// if we have 2 columsn, then we assume that the 2nd
							// column has the train/test flag
							// else we assume everything is training data
							trainFlag = rs.getMetaData().getColumnCount() == 2;
						}
						long id = rs.getLong(1);
						int train = trainFlag.booleanValue() ? rs.getInt(2) : 1;
						if (train != 0) {
							documentIds.add(id);
						} else {
							testDocumentIds.add(id);
						}
					}
				});
				return null;
			}
		});
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);		
	}

	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}

	public void setFoldId(int foldId) {
		this.foldId = foldId;
	}

	public void setInstanceIDQuery(String instanceIDQuery) {
		this.instanceIDQuery = instanceIDQuery;
	}

	public void setInstanceKernel(Kernel instanceKernel) {
		this.instanceKernel = instanceKernel;
	}

	public void setInstanceTreeBuilder(InstanceTreeBuilder instanceTreeBuilder) {
		this.instanceTreeBuilder = instanceTreeBuilder;
	}

	public void setKernelEvaluationDao(KernelEvaluationDao kernelEvaluationDao) {
		this.kernelEvaluationDao = kernelEvaluationDao;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setParam1(double param1) {
		this.param1 = param1;
	}

	public void setParam2(String param2) {
		this.param2 = param2;
	}

	public void setTransactionManager(
			PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		txTemplate = new TransactionTemplate(this.transactionManager);
		txTemplate
				.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
	}

	public void setTreeMappingInfo(TreeMappingInfo treeMappingInfo) {
		this.treeMappingInfo = treeMappingInfo;
	}
}
