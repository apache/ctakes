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
package org.apache.ctakes.ytex.kernel.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FeatureRank implements Serializable {
	
	/**
	 * sort the features, set the rank correspondingly.  
	 * @param featureRankList
	 * @param comp
	 * @return return the original list, but sorted
	 */
	public static List<FeatureRank> sortFeatureRankList(List<FeatureRank> featureRankList, Comparator<FeatureRank> comp) {
		Collections.sort(featureRankList, comp);
		for (int i = 0; i < featureRankList.size(); i++) {
			featureRankList.get(i).setRank(i + 1);
		}
		return featureRankList;
	}
	
	/**
	 * Sort features by ascending evaluation score
	 * 
	 * @author vijay
	 * 
	 */
	public static class FeatureRankAsc implements Comparator<FeatureRank> {

		@Override
		public int compare(FeatureRank o1, FeatureRank o2) {
			if (o1.getEvaluation() > o2.getEvaluation()) {
				return 1;
			} else if (o1.getEvaluation() == o2.getEvaluation()) {
				return o1.getFeatureName().compareTo(o2.getFeatureName());
			} else {
				return -1;
			}
		}
	}

	/**
	 * Sort features by descending evaluation score if two features have the
	 * same score, order them by name
	 * 
	 * @author vijay
	 * 
	 */
	public static class FeatureRankDesc implements Comparator<FeatureRank> {

		@Override
		public int compare(FeatureRank o1, FeatureRank o2) {
			if (o1.getEvaluation() > o2.getEvaluation()) {
				return -1;
			} else if (o1.getEvaluation() == o2.getEvaluation()) {
				return o1.getFeatureName().compareTo(o2.getFeatureName());
			} else {
				return 1;
			}
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double evaluation;

	private FeatureEvaluation featureEval;
	private String featureName;
	private int rank;
	private int featureRankId;

	public int getFeatureRankId() {
		return featureRankId;
	}

	public void setFeatureRankId(int featureRankId) {
		this.featureRankId = featureRankId;
	}

	public FeatureRank() {
	}

	public FeatureRank(String featureName, double evaluation) {
		this.featureName = featureName;
		this.evaluation = evaluation;
	}

	public FeatureRank(FeatureEvaluation featureEval, String featureName,
			double evaluation) {
		this.featureEval = featureEval;
		this.featureName = featureName;
		this.evaluation = evaluation;
	}
	public FeatureRank(FeatureEvaluation featureEval, String featureName,
			double evaluation, int rank) {
		this.featureEval = featureEval;
		this.featureName = featureName;
		this.evaluation = evaluation;
		this.rank = rank;
	}	

	public double getEvaluation() {
		return evaluation;
	}

	public FeatureEvaluation getFeatureEval() {
		return featureEval;
	}

	public String getFeatureName() {
		return featureName;
	}

	public int getRank() {
		return rank;
	}

	public void setEvaluation(double evaluation) {
		this.evaluation = evaluation;
	}

	public void setFeatureEval(FeatureEvaluation featureEval) {
		this.featureEval = featureEval;
	}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	@Override
	public String toString() {
		return "FeatureRank [featureName=" + featureName + ", evaluation="
				+ evaluation + ", rank=" + rank + "]";
	}
}
