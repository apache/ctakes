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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.ytex.kernel.dao.ClassifierEvaluationDao;
import org.apache.ctakes.ytex.kernel.dao.KernelEvaluationDao;
import org.apache.ctakes.ytex.kernel.model.CrossValidationFold;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluation;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluationInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


public class KernelUtilImpl implements KernelUtil {
	private static final Log log = LogFactory.getLog(KernelUtilImpl.class);
	private ClassifierEvaluationDao classifierEvaluationDao;

	private JdbcTemplate jdbcTemplate = null;

	private KernelEvaluationDao kernelEvaluationDao = null;
	private PlatformTransactionManager transactionManager;
	private FoldGenerator foldGenerator = null;

	public FoldGenerator getFoldGenerator() {
		return foldGenerator;
	}

	public void setFoldGenerator(FoldGenerator foldGenerator) {
		this.foldGenerator = foldGenerator;
	}

	private Map<Long, Integer> createInstanceIdToIndexMap(
			SortedSet<Long> instanceIDs) {
		Map<Long, Integer> instanceIdToIndexMap = new HashMap<Long, Integer>(
				instanceIDs.size());
		int i = 0;
		for (Long instanceId : instanceIDs) {
			instanceIdToIndexMap.put(instanceId, i);
			i++;
		}
		return instanceIdToIndexMap;
	}

	@Override
	public void fillGramMatrix(final KernelEvaluation kernelEvaluation,
			final SortedSet<Long> trainInstanceLabelMap,
			final double[][] trainGramMatrix) {
		// final Set<String> kernelEvaluationNames = new HashSet<String>(1);
		// kernelEvaluationNames.add(name);
		// prepare map of instance id to gram matrix index
		final Map<Long, Integer> trainInstanceToIndexMap = createInstanceIdToIndexMap(trainInstanceLabelMap);

		// iterate through the training instances
		for (Map.Entry<Long, Integer> instanceIdIndex : trainInstanceToIndexMap
				.entrySet()) {
			// index of this instance
			final int indexThis = instanceIdIndex.getValue();
			// id of this instance
			final long instanceId = instanceIdIndex.getKey();
			// get all kernel evaluations for this instance in a new transaction
			// don't want too many objects in hibernate session
			TransactionTemplate t = new TransactionTemplate(
					this.transactionManager);
			t.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
			t.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus arg0) {
					List<KernelEvaluationInstance> kevals = getKernelEvaluationDao()
							.getAllKernelEvaluationsForInstance(
									kernelEvaluation, instanceId);
					for (KernelEvaluationInstance keval : kevals) {
						// determine the index of the instance
						Integer indexOtherTrain = null;
						long instanceIdOther = instanceId != keval
								.getInstanceId1() ? keval.getInstanceId1()
								: keval.getInstanceId2();
						// look in training set for the instance id
						indexOtherTrain = trainInstanceToIndexMap
								.get(instanceIdOther);
						if (indexOtherTrain != null) {
							trainGramMatrix[indexThis][indexOtherTrain] = keval
									.getSimilarity();
							trainGramMatrix[indexOtherTrain][indexThis] = keval
									.getSimilarity();
						}
					}
					return null;
				}
			});
		}
		// put 1's in the diagonal of the training gram matrix
		for (int i = 0; i < trainGramMatrix.length; i++) {
			if (trainGramMatrix[i][i] == 0)
				trainGramMatrix[i][i] = 1;
		}
	}

	public ClassifierEvaluationDao getClassifierEvaluationDao() {
		return classifierEvaluationDao;
	}

	public DataSource getDataSource() {
		return jdbcTemplate.getDataSource();
	}

	public KernelEvaluationDao getKernelEvaluationDao() {
		return kernelEvaluationDao;
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	@Override
	public double[][] loadGramMatrix(SortedSet<Long> instanceIds, String name,
			String splitName, String experiment, String label, int run,
			int fold, double param1, String param2) {
		int foldId = 0;
		double[][] gramMatrix = null;
		if (run != 0 && fold != 0) {
			CrossValidationFold f = this.classifierEvaluationDao
					.getCrossValidationFold(name, splitName, label, run, fold);
			if (f != null)
				foldId = f.getCrossValidationFoldId();
		}
		KernelEvaluation kernelEval = this.kernelEvaluationDao.getKernelEval(
				name, experiment, label, foldId, param1, param2);
		if (kernelEval == null) {
			log.warn("could not find kernelEvaluation.  name=" + name
					+ ", experiment=" + experiment + ", label=" + label
					+ ", fold=" + fold + ", run=" + run);
		} else {
			gramMatrix = new double[instanceIds.size()][instanceIds.size()];
			fillGramMatrix(kernelEval, instanceIds, gramMatrix);
		}
		return gramMatrix;
	}

	/**
	 * this can be very large - avoid loading the entire jdbc ResultSet into
	 * memory
	 */
	@Override
	public InstanceData loadInstances(String strQuery) {
		final InstanceData instanceLabel = new InstanceData();
		PreparedStatement s = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			// jdbcTemplate.query(strQuery, new RowCallbackHandler() {
			RowCallbackHandler ch = new RowCallbackHandler() {

				@Override
				public void processRow(ResultSet rs) throws SQLException {
					String label = "";
					int run = 0;
					int fold = 0;
					boolean train = true;
					long instanceId = rs.getLong(1);
					String className = rs.getString(2);
					if (rs.getMetaData().getColumnCount() >= 3)
						train = rs.getBoolean(3);
					if (rs.getMetaData().getColumnCount() >= 4) {
						label = rs.getString(4);
						if (label == null)
							label = "";
					}
					if (rs.getMetaData().getColumnCount() >= 5)
						fold = rs.getInt(5);
					if (rs.getMetaData().getColumnCount() >= 6)
						run = rs.getInt(6);
					// get runs for label
					SortedMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>> runToInstanceMap = instanceLabel
							.getLabelToInstanceMap().get(label);
					if (runToInstanceMap == null) {
						runToInstanceMap = new TreeMap<Integer, SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>>();
						instanceLabel.getLabelToInstanceMap().put(label,
								runToInstanceMap);
					}
					// get folds for run
					SortedMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>> foldToInstanceMap = runToInstanceMap
							.get(run);
					if (foldToInstanceMap == null) {
						foldToInstanceMap = new TreeMap<Integer, SortedMap<Boolean, SortedMap<Long, String>>>();
						runToInstanceMap.put(run, foldToInstanceMap);
					}
					// get train/test set for fold
					SortedMap<Boolean, SortedMap<Long, String>> ttToClassMap = foldToInstanceMap
							.get(fold);
					if (ttToClassMap == null) {
						ttToClassMap = new TreeMap<Boolean, SortedMap<Long, String>>();
						foldToInstanceMap.put(fold, ttToClassMap);
					}
					// get instances for train/test set
					SortedMap<Long, String> instanceToClassMap = ttToClassMap
							.get(train);
					if (instanceToClassMap == null) {
						instanceToClassMap = new TreeMap<Long, String>();
						ttToClassMap.put(train, instanceToClassMap);
					}
					// set the instance class
					instanceToClassMap.put(instanceId, className);
					// add the class to the labelToClassMap
					SortedSet<String> labelClasses = instanceLabel
							.getLabelToClassMap().get(label);
					if (labelClasses == null) {
						labelClasses = new TreeSet<String>();
						instanceLabel.getLabelToClassMap().put(label,
								labelClasses);
					}
					if (!labelClasses.contains(className))
						labelClasses.add(className);
				}
			};
			conn = this.jdbcTemplate.getDataSource().getConnection();
			s = conn.prepareStatement(strQuery,
					java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			if ("MySQL".equals(conn.getMetaData().getDatabaseProductName())) {
				s.setFetchSize(Integer.MIN_VALUE);
			} else if (s.getClass().getName()
					.equals("com.microsoft.sqlserver.jdbc.SQLServerStatement")) {
				try {
					BeanUtils.setProperty(s, "responseBuffering", "adaptive");
				} catch (IllegalAccessException e) {
					log.warn("error setting responseBuffering", e);
				} catch (InvocationTargetException e) {
					log.warn("error setting responseBuffering", e);
				}
			}
			rs = s.executeQuery();
			while (rs.next()) {
				ch.processRow(rs);
			}
		} catch (SQLException j) {
			log.error("loadInstances failed", j);
			throw new RuntimeException(j);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
			if (s != null) {
				try {
					s.close();
				} catch (SQLException e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		return instanceLabel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.DataExporter#loadProperties(java.lang.String,
	 * java.util.Properties)
	 */
	@Override
	public void loadProperties(String propertyFile, Properties props)
			throws FileNotFoundException, IOException,
			InvalidPropertiesFormatException {
		InputStream in = null;
		try {
			in = new FileInputStream(propertyFile);
			if (propertyFile.endsWith(".xml"))
				props.loadFromXML(in);
			else
				props.load(in);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public void setClassifierEvaluationDao(
			ClassifierEvaluationDao classifierEvaluationDao) {
		this.classifierEvaluationDao = classifierEvaluationDao;
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setKernelEvaluationDao(KernelEvaluationDao kernelEvaluationDao) {
		this.kernelEvaluationDao = kernelEvaluationDao;
	}

	public void setTransactionManager(
			PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void generateFolds(InstanceData instanceLabel, Properties props) {
		int folds = Integer.parseInt(props.getProperty("folds"));
		int runs = Integer.parseInt(props.getProperty("runs", "1"));
		int minPerClass = Integer.parseInt(props
				.getProperty("minPerClass", "0"));
		Integer randomNumberSeed = props.containsKey("rand") ? Integer
				.parseInt(props.getProperty("rand")) : null;
		instanceLabel.setLabelToInstanceMap(foldGenerator.generateRuns(
				instanceLabel.getLabelToInstanceMap(), folds, minPerClass,
				randomNumberSeed, runs));
	}

	/**
	 * assign numeric indices to string class names
	 * 
	 * @param labelToClasMap
	 * @param labelToClassIndexMap
	 */
	@Override
	public void fillLabelToClassToIndexMap(
			Map<String, SortedSet<String>> labelToClasMap,
			Map<String, BiMap<String, Integer>> labelToClassIndexMap) {
		for (Map.Entry<String, SortedSet<String>> labelToClass : labelToClasMap
				.entrySet()) {
			BiMap<String, Integer> classToIndexMap = HashBiMap.create();
			labelToClassIndexMap.put(labelToClass.getKey(), classToIndexMap);
			int nIndex = 1;
			for (String className : labelToClass.getValue()) {
				Integer classNumber = null;
				try {
					classNumber = Integer.parseInt(className);
				} catch (NumberFormatException fe) {
				}
				if (classNumber == null) {
					classToIndexMap.put(className, nIndex++);
				} else {
					classToIndexMap.put(className, classNumber);
				}
			}
		}
	}

	/**
	 * export the class id to class name map.
	 * 
	 * @param classIdMap
	 * @param label
	 * @param run
	 * @param fold
	 * @throws IOException
	 */
	public void exportClassIds(String outdir, Map<String, Integer> classIdMap,
			String label) throws IOException {
		// construct file name
		String filename = FileUtil.getScopedFileName(outdir, label, null, null,
				"class.properties");
		Properties props = new Properties();
		for (Map.Entry<String, Integer> entry : classIdMap.entrySet()) {
			props.put(entry.getValue().toString(), entry.getKey());
		}
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(filename));
			props.store(w, "class id to class name map");
		} finally {
			if (w != null) {
				w.close();
			}
		}
	}
}
