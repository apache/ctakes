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
package org.apache.ctakes.temporal.ae.feature.selection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.transform.TransformableFeature;

import com.google.common.base.Function;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * 
 * Selects features via Odds Ratio statistics between the features extracted from its sub-extractor
 * and the outcome values they are paired with in classification instances.
 * 
 * @author Chen Lin
 * 
 */
public class OddsRatioFeatureSelection<OUTCOME_T> extends FeatureSelection<OUTCOME_T> {

	/**
	 * Helper class for aggregating and computing mutual Odds Ratio statistics
	 */
	private static class OddsRatioScorer<OUTCOME_T> implements Function<String, Double> {
		protected Multiset<OUTCOME_T> classCounts;

		protected Table<String, OUTCOME_T, Integer> featValueClassCount;

		private String positiveClass = null;

		public OddsRatioScorer(String posiClas) {
			this.classCounts = HashMultiset.<OUTCOME_T> create();
			this.featValueClassCount = HashBasedTable.<String, OUTCOME_T, Integer> create();
			this.positiveClass = posiClas;
		}

		public void update(String featureName, OUTCOME_T outcome, int occurrences) {
			Integer count = this.featValueClassCount.get(featureName, outcome);
			if (count == null) {
				count = 0;
			}
			this.featValueClassCount.put(featureName, outcome, count + occurrences);
			this.classCounts.add(outcome, occurrences);
		}

		public Double apply(String featureName) {
			return this.score(featureName);
		}

		public double score(String featureName) {
			// notation index of 0 means false, 1 mean true
			// Contingency Table:
			//       | Y = 1  | Y = 0  | 
			// X = 1 | n11    | n10    | posiFeatCount
			// X = 0 | n01    | n00    | negaFeatCount

			int n11 = 1, n10=1, n01=1, n00 = 1; //add one smoothing?

			for (OUTCOME_T clas : this.classCounts.elementSet()) {
				int numPositiveFeature = this.featValueClassCount.contains(featureName, clas)
						? this.featValueClassCount.get(featureName, clas): 0;
				int numNegativeFeature = this.classCounts.count(clas) - numPositiveFeature;
				if ( clas.toString().equals("B") || clas.toString().equals("I") || clas.toString().equals(this.positiveClass )){
					n11 += numPositiveFeature;
					n01 += numNegativeFeature;
				}else if( this.positiveClass==null && clas.toString().equals("O")){
					n10 += numPositiveFeature;
					n00 += numNegativeFeature;
				}else{
					System.err.println("Please define postive class label for odds ratio calculation.");
					System.exit(0);
				}
			}
			double oddsratio = Math.log(n11) + Math.log(n00) - Math.log(n10) - Math.log(n01);

			return oddsratio;

		}
	}

	private double oddsRatioThreshold;

	private int numFeatures = 0;
	
	private String positiveClass = null;

	private OddsRatioScorer<OUTCOME_T> oddsRatioFunction;

	public OddsRatioFeatureSelection(String name) {
		this(name, 0.0);
	}

	public OddsRatioFeatureSelection(String name, double threshold) {
		super(name);
		this.oddsRatioThreshold = threshold;
	}

	/**
	 * 
	 * @param name: feature selection method name
	 * @param threshold: percentage threshold to control how many features to keep
	 * @param posiClas: specify which class is the positive class
	 */
	public OddsRatioFeatureSelection(String name, double threshold, String posiClas) {
		super(name);
		this.oddsRatioThreshold = threshold;
		this.positiveClass = posiClas;
	}

	@Override
	public boolean apply(Feature feature) {
		return this.selectedFeatureNames.contains(this.getFeatureName(feature));
	}

	@Override
	public void train(Iterable<Instance<OUTCOME_T>> instances) {
		// aggregate statistics for all features
		this.oddsRatioFunction = new OddsRatioScorer<OUTCOME_T>(positiveClass);
		for (Instance<OUTCOME_T> instance : instances) {
			OUTCOME_T outcome = instance.getOutcome();
			for (Feature feature : instance.getFeatures()) {
				if (this.isTransformable(feature)) {
					for (Feature untransformedFeature : ((TransformableFeature) feature).getFeatures()) {
						this.oddsRatioFunction.update(this.getFeatureName(untransformedFeature), outcome, 1);
					}
				}
			}
		}


		// sort features by Odds Ratio score
		Set<String> featureNames = this.oddsRatioFunction.featValueClassCount.rowKeySet();
		Ordering<String> ordering = Ordering.natural().onResultOf(this.oddsRatioFunction).reverse();

		int totalFeatures = featureNames.size();
		this.numFeatures = (int) Math.round(totalFeatures*this.oddsRatioThreshold);

		// keep only the top N features
		this.selectedFeatureNames = Sets.newLinkedHashSet(ordering.immutableSortedCopy(featureNames).subList(
				0,
				this.numFeatures));

		this.isTrained = true;
	}

	@Override
	public void save(URI uri) throws IOException {
		if (!this.isTrained) {
			throw new IllegalStateException("Cannot save before training");
		}
		File out = new File(uri);
		BufferedWriter writer = new BufferedWriter(new FileWriter(out));

		for (String feature : this.selectedFeatureNames) {
			writer.append(String.format("%s\t%f\n", feature, this.oddsRatioFunction.score(feature)));
		}

		writer.close();
	}

	@Override
	public void load(URI uri) throws IOException {
		this.selectedFeatureNames = Sets.newLinkedHashSet();
		File in = new File(uri);
		BufferedReader reader = new BufferedReader(new FileReader(in));

		// The lines are <feature-name>\t<feature-score>
		String line = null;
		int n = 0;
		while ((line = reader.readLine()) != null && n < this.numFeatures) {
			String[] featureValuePair = line.split("\t");
			this.selectedFeatureNames.add(featureValuePair[0]);
			n++;
		}

		reader.close();
		this.isTrained = true;

	}
}
