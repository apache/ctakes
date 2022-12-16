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
package org.apache.ctakes.ytex.weka;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.apache.ctakes.ytex.kernel.BagOfWordsDecorator;
import org.apache.ctakes.ytex.kernel.BagOfWordsExporter;
import org.apache.ctakes.ytex.kernel.dao.KernelEvaluationDao;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluation;
import org.apache.ctakes.ytex.kernel.model.KernelEvaluationInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

/**
 * @deprecated don't know if this works ...
 * @author vijay
 *
 */
public class GramMatrixExporterImpl extends WekaBagOfWordsExporterImpl implements
		GramMatrixExporter {
	private JdbcTemplate jdbcTemplate;
	private KernelEvaluationDao kernelEvaluationDao;
	private WekaBagOfWordsExporter bagOfWordsExporter;

	public enum GramMatrixType {
		WEKA, LIBSVM
	};

	public KernelEvaluationDao getKernelEvaluationDao() {
		return kernelEvaluationDao;
	}

	public BagOfWordsExporter getBagOfWordsExporter() {
		return bagOfWordsExporter;
	}

	public void setBagOfWordsExporter(BagOfWordsExporter bagOfWordsExporter) {
		this.bagOfWordsExporter = (WekaBagOfWordsExporter)bagOfWordsExporter;
	}

	public void setKernelEvaluationDao(KernelEvaluationDao kernelEvaluationDao) {
		this.kernelEvaluationDao = kernelEvaluationDao;
	}

	public void setDataSource(DataSource ds) {
		this.jdbcTemplate = new JdbcTemplate(ds);
	}

	public DataSource getDataSource() {
		return this.jdbcTemplate.getDataSource();
	}

	/**
	 * add gramIndex attribute to arff file
	 * 
	 * @author vijay
	 */
	public static class GramMatrixArffDecorator implements BagOfWordsDecorator {
		public static final String INDEX_NAME = "gramIndex";
		public static final String INDEX_NAME_NOMINAL = "gramIndexN";
		Map<Long, Integer> instanceIdToIndexMap;

		public GramMatrixArffDecorator(
				Map<Long, Integer> instanceIdToIndexMap) {
			this.instanceIdToIndexMap = instanceIdToIndexMap;
		}

		@Override
		public void decorateNumericInstanceWords(
				Map<Long, SortedMap<String, Double>> instanceNumericWords,
				SortedSet<String> numericWords) {
			for (Map.Entry<Long, Integer> instanceIdToIndex : instanceIdToIndexMap
					.entrySet()) {
				long instanceId = instanceIdToIndex.getKey();
				int index = instanceIdToIndex.getValue();
				if (!instanceNumericWords.containsKey(instanceId)) {
					instanceNumericWords.put(instanceId,
							new TreeMap<String, Double>());
				}
				instanceNumericWords.get(instanceId).put(INDEX_NAME,
						(double) index);
			}
			numericWords.add(INDEX_NAME);
		}

		/**
		 * do nothing
		 */
		@Override
		public void decorateNominalInstanceWords(
				Map<Long, SortedMap<String, String>> instanceNominalWords,
				Map<String, SortedSet<String>> nominalWordValueMap) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.ctakes.ytex.kernel.GramMatrixExporter#exportGramMatrix(java.lang.String)
	 */
	@Override
	public void exportGramMatrix(String propertyFile) throws IOException {
		Properties props = new Properties();
		BufferedWriter matrixWriter = null;
		InputStream in = null;
		try {
			// map of instance id -> index in gram matrix
			final Map<Long, Integer> instanceIdToIndexMap = new HashMap<Long, Integer>();
			// map of instance id -> class label
			final Map<Long, String> instanceIDClassLabel = new TreeMap<Long, String>();
			in = new FileInputStream(propertyFile);
			if (propertyFile.endsWith(".xml"))
				props.loadFromXML(in);
			else
				props.load(in);
			matrixWriter = new BufferedWriter(new FileWriter(props
					.getProperty("matrixFile")));
			String kernelEvaluationNames = props
					.getProperty("kernelEvaluationNames");
			//TODO
			KernelEvaluation kernelEvaluation = null;
			Set<String> setKernelEvaluationNames = new HashSet<String>();
			Collections.addAll(setKernelEvaluationNames, kernelEvaluationNames
					.split(","));
			initializeInstanceIndices(props.getProperty("instanceClassQuery"),
					instanceIDClassLabel, instanceIdToIndexMap);
			this.bagOfWordsExporter.exportBagOfWords(propertyFile,
					new GramMatrixArffDecorator(instanceIdToIndexMap));
			exportGramMatrix(matrixWriter, kernelEvaluation,
					GramMatrixType.WEKA, instanceIDClassLabel,
					instanceIdToIndexMap);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception e) {
			}
			try {
				if (matrixWriter != null)
					matrixWriter.close();
			} catch (Exception e) {
			}
		}
	}

	protected Map<Long, Integer> exportGramMatrix(BufferedWriter writer,
			final KernelEvaluation kernelEvaluation, GramMatrixType matrixType,
			final Map<Long, String> instanceIDClassLabel,
			final Map<Long, Integer> instanceToIndexMap) throws IOException {
		// allocate gram matrix
		final double[][] gramMatrix = new double[instanceIDClassLabel.size()][instanceIDClassLabel
				.size()];
		// fill in gram matrix
		// do this row by row - pulling the entire matrix at once is too slow
		// do this in a separate transaction so that the 1st level cache doesn't
		// fill up
		for (Map.Entry<Long, Integer> instanceIdIndex : instanceToIndexMap
				.entrySet()) {
			final int indexThis = instanceIdIndex.getValue();
			final long instanceId = instanceIdIndex.getKey();
			txNew.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus arg0) {
					for (KernelEvaluationInstance keval : getKernelEvaluationDao()
							.getAllKernelEvaluationsForInstance(
									kernelEvaluation, instanceId)) {
						Integer indexOther = null;
						if (instanceId != keval.getInstanceId1()) {
							indexOther = instanceToIndexMap.get(keval
									.getInstanceId1());
						} else if (instanceId != keval.getInstanceId2()) {
							indexOther = instanceToIndexMap.get(keval
									.getInstanceId2());
						} else {
							indexOther = indexThis;
						}
						if (indexOther != null) {
							gramMatrix[indexThis][indexOther] = keval
									.getSimilarity();
							gramMatrix[indexOther][indexThis] = keval
									.getSimilarity();
						}
					}
					return null;
				}
			});
		}
		// may run into memory issues/ transaction timeout with getting all
		// kernel evals in one go
		// for (KernelEvaluation keval : this.getKernelEvaluationDao()
		// .getAllKernelEvaluations(kernelEvaluationNames)) {
		// Integer index1 = instanceToIndexMap.get(keval.getInstanceId1());
		// Integer index2 = instanceToIndexMap.get(keval.getInstanceId2());
		// if (index1 != null && index2 != null) {
		// gramMatrix[index1][index2] = keval.getSimilarity();
		// gramMatrix[index2][index1] = keval.getSimilarity();
		// }
		// }
		// set diagonal if not set (typically K<x,x> = 1 so this need not be
		// stored
		for (int j = 0; j < gramMatrix.length; j++) {
			if (gramMatrix[j][j] == 0)
				gramMatrix[j][j] = 1;
		}
		if (matrixType == GramMatrixType.WEKA) {
			exportWekaGramMatrix(gramMatrix, writer);
		}
		return instanceToIndexMap;
	}

	private void initializeInstanceIndices(String instanceClassQuery,
			final Map<Long, String> instanceIDClassLabel,
			final Map<Long, Integer> instanceToIndexMap) {
		// read in all instance ids
		jdbcTemplate.query(instanceClassQuery, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				instanceIDClassLabel.put(rs.getLong(1), rs.getString(2));
			}
		});
		int i = 0;
		for (Long instanceId : instanceIDClassLabel.keySet()) {
			instanceToIndexMap.put(instanceId, i);
			i++;
		}
	}

	private void exportWekaGramMatrix(double[][] gramMatrix,
			BufferedWriter writer) throws IOException {
		// header contains rows columns
		writer.write(Integer.toString(gramMatrix.length));
		writer.write("\t");
		writer.write(Integer.toString(gramMatrix.length));
		writer.write("\t");
		writer.newLine();
		for (int nRow = 0; nRow < gramMatrix.length; nRow++) {
			double row[] = gramMatrix[nRow];
			for (int nCol = 0; nCol < row.length; nCol++) {
				writer.write(Double.toString(row[nCol]));
				writer.write("\t");
			}
			writer.newLine();
		}
	}
}
