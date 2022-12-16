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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.ytex.kernel.metric.ConceptInfo;
import org.apache.ctakes.ytex.kernel.model.ClassifierEvaluation;
import org.apache.ctakes.ytex.kernel.model.CrossValidationFold;
import org.apache.ctakes.ytex.kernel.model.FeatureEvaluation;
import org.apache.ctakes.ytex.kernel.model.FeatureParentChild;
import org.apache.ctakes.ytex.kernel.model.FeatureRank;


public interface ClassifierEvaluationDao {

	public abstract void saveClassifierEvaluation(ClassifierEvaluation eval,
			Map<Integer, String> irClassMap, boolean saveInstanceEval);

	public abstract void saveFold(CrossValidationFold fold);

	public abstract void deleteCrossValidationFoldByName(String name,
			String splitName);

	public abstract void saveFeatureEvaluation(
			FeatureEvaluation featureEvaluation, List<FeatureRank> features);

	public abstract void deleteFeatureEvaluationByNameAndType(
			String corpusName, String featureSetName, String type);

	/**
	 * 
	 * @param eval
	 *            evaluation to save
	 * @param saveInstanceEval
	 *            save instance level evaluations - default false
	 * @param saveIRStats
	 *            save IR statistics - default true
	 * @param excludeTargetClassId
	 *            for semi-supervised learners, don't want to include the
	 *            unlabeled instances in computation of ir statistics. this
	 *            specifies the class id of the unlabeled instances (default 0)
	 */
	public void saveClassifierEvaluation(ClassifierEvaluation eval,
			Map<Integer, String> irClassMap, boolean saveInstanceEval,
			boolean saveIRStats, Integer excludeTargetClassId);

	public abstract CrossValidationFold getCrossValidationFold(
			String corpusName, String splitName, String label, int run, int fold);

	public List<FeatureRank> getTopFeatures(String corpusName,
			String featureSetName, String label, String type, Integer foldId,
			double param1, String param2, Integer parentConceptTopThreshold);

	public List<FeatureRank> getThresholdFeatures(String corpusName,
			String featureSetName, String label, String type, Integer foldId,
			double param1, String param2,
			double parentConceptEvaluationThreshold);

	public abstract void deleteFeatureEvaluation(String corpusName,
			String featureSetName, String label, String evaluationType,
			Integer foldId, Double param1, String param2);

	public abstract Map<String, Double> getFeatureRankEvaluations(
			String corpusName, String featureSetName, String label,
			String evaluationType, Integer foldId, double param1, String param2);

	public abstract Map<String, Double> getFeatureRankEvaluations(
			Set<String> featureNames, String corpusName, String featureSetName,
			String label, String evaluationType, Integer foldId, double param1,
			String param2);

	public abstract Map<String, FeatureRank> getFeatureRanks(
			Set<String> featureNames, String corpusName, String featureSetName,
			String label, String evaluationType, Integer foldId, double param1,
			String param2);

	public abstract List<Object[]> getCorpusCuiTuis(String corpusName,
			String conceptGraphName, String conceptSetName);

	public abstract Map<String, Double> getInfoContent(String corpusName,
			String conceptGraphName, String conceptSet);

	public abstract List<ConceptInfo> getIntrinsicInfoContent(
			String conceptGraphName);

	public abstract void saveFeatureParentChild(FeatureParentChild parchd);

	public abstract List<FeatureRank> getImputedFeaturesByPropagatedCutoff(
			String corpusName, String conceptSetName, String label,
			String evaluationType, String conceptGraphName,
			String propEvaluationType, int propRankCutoff);

	public abstract Double getMaxFeatureEvaluation(String corpusName,
			String featureSetName, String label, String evaluationType,
			Integer foldId, double param1, String param2);

	public abstract Integer getMaxDepth(String conceptGraphName);
}