/*
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
import java.util.LinkedHashSet;
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
 * Selects features via Chi-squared statistics between the features extracted from its sub-extractor
 * and the outcome values they are paired with in classification instances.
 * 
 * @author Chen Lin
 * 
 */
public class Chi2FeatureSelection<OUTCOME_T> extends FeatureSelection<OUTCOME_T> {

	/**
	 * Helper class for aggregating and computing mutual Chi2 statistics
	 */
	private static class Chi2Scorer<OUTCOME_T> implements Function<String, Double> {
		protected Multiset<OUTCOME_T> classCounts;

		protected Table<String, OUTCOME_T, Integer> featValueClassCount;

		private boolean yates = false;

		public Chi2Scorer(boolean yate) {
			this.classCounts = HashMultiset.<OUTCOME_T> create();
			this.featValueClassCount = HashBasedTable.<String, OUTCOME_T, Integer> create();
			this.yates = yate;
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
			//      | class1  | class2  | class3  | sum
			// posi |         |         |         | posiFeatCount
			// nega |         |         |         | negaFeatCount
			//      | outcnt1 | outcnt2 | outcnt3 | n

			int numOfClass = this.classCounts.elementSet().size();
			int[] posiOutcomeCounts = new int[numOfClass];
			int[] outcomeCounts = new int[numOfClass];
			int classId = 0;
			int posiFeatCount = 0;
			for (OUTCOME_T clas : this.classCounts.elementSet()) {
				posiOutcomeCounts[classId] = this.featValueClassCount.contains(featureName, clas)
						? this.featValueClassCount.get(featureName, clas)
								: 0;
						posiFeatCount += posiOutcomeCounts[classId];
						outcomeCounts[classId] = this.classCounts.count(clas);
						classId++;
			}

			int n = this.classCounts.size();
			int negaFeatCount = n - posiFeatCount;

			double chi2val = 0.0;

			if (posiFeatCount == 0 || posiFeatCount == n) { // all instances have same value on this
				// feature, degree of freedom = 0
				return chi2val;
			}

			for (int lbl = 0; lbl < numOfClass; lbl++) {
				// for positive part of feature:
				double expected = (outcomeCounts[lbl] / (double) n) * (posiFeatCount);
				if (expected > 0) {
					double diff = Math.abs(posiOutcomeCounts[lbl] - expected);
					if (this.yates ) { // apply Yate's correction
						diff -= 0.5;
					}
					if (diff > 0)
						chi2val += Math.pow(diff, 2) / expected;
				}

				// for negative part of feature:
				expected = (outcomeCounts[lbl] / (double) n) * (negaFeatCount);
				double observ = outcomeCounts[lbl] - posiOutcomeCounts[lbl];
				if (expected > 0) {
					double diff = Math.abs(observ - expected);
					if (this.yates) { // apply Yate's correction
						diff -= 0.5;
					}
					if (diff > 0)
						chi2val += Math.pow(diff, 2) / expected;
				}
			}

			return chi2val;
		}

	}

	/**
	 * the percentage of total features that would be returned. range from 0% - 100%, i.e. [0,1]
	 */
	private double chi2Threshold;

	private int numFeatures = 0;

	private Chi2Scorer<OUTCOME_T> chi2Function;

	private boolean yates = false;

	private LinkedHashSet<String> discardedFeatureNames;

	public Chi2FeatureSelection(String name) {
		this(name, 0.0);
	}

	public Chi2FeatureSelection(String name, double threshold) {
		super(name);
		this.chi2Threshold = threshold;
	}

	/**
	 * Constructor that can let use control the yate's correction
	 * @param name
	 * @param threshold
	 * @param yates : true for using yate's correction, false for turn off yate's correction
	 */
	public Chi2FeatureSelection(String name, double threshold, boolean yates) {
		super(name);
		this.chi2Threshold = threshold;
		this.yates = yates;
	}
	@Override
	public boolean apply(Feature feature) {
		return this.selectedFeatureNames.contains(this.getFeatureName(feature));
	}

	@Override
	public void train(Iterable<Instance<OUTCOME_T>> instances) {
		//check if chi2Threshold is bigger whithin range:
		if(this.chi2Threshold<0 || this.chi2Threshold>1){
			System.err.println("Feature Selection threshold should be from 0 to 1");
			System.exit(0);
		}

		// aggregate statistics for all features
		this.chi2Function = new Chi2Scorer<OUTCOME_T>(this.yates);
		for (Instance<OUTCOME_T> instance : instances) {
			OUTCOME_T outcome = instance.getOutcome();
			for (Feature feature : instance.getFeatures()) {
				if (this.isTransformable(feature)) {
					for (Feature untransformedFeature : ((TransformableFeature) feature).getFeatures()) {
						this.chi2Function.update(this.getFeatureName(untransformedFeature), outcome, 1);
					}
				}
			}
		}


		//    // keep only large chi2 valued features
		//    this.selectedFeatureNames = Sets.newHashSet();
		//    for (String featureName : this.chi2Function.featValueClassCount.rowKeySet()) {
		//      if (this.chi2Function.score(featureName) > this.chi2Threshold) {
		//        this.selectedFeatureNames.add(featureName);
		//      }
		//    }

		// sort features by Chi2 information score
		Set<String> featureNames = this.chi2Function.featValueClassCount.rowKeySet();
		Ordering<String> ordering = Ordering.natural().onResultOf(this.chi2Function).reverse();

		int totalFeatures = featureNames.size();
		this.numFeatures = (int) Math.round(totalFeatures*this.chi2Threshold);

		// keep only the top N features
		this.selectedFeatureNames = Sets.newLinkedHashSet(ordering.immutableSortedCopy(featureNames).subList(
				0,
				this.numFeatures));
		this.discardedFeatureNames = Sets.newLinkedHashSet(ordering.immutableSortedCopy(featureNames).subList(this.numFeatures, totalFeatures));

		this.isTrained = true;
	}

	@Override
	public void save(URI uri) throws IOException {
		if (!this.isTrained) {
			throw new IllegalStateException("Cannot save before training");
		}
		File out = new File(uri);
		final String uriPath = uri.getPath();
		final int lastIndex = uriPath.lastIndexOf('.');
		final String discardPath = (lastIndex >= 0 ? uriPath.substring(0, lastIndex) : uriPath ) + "_discarded.dat";
		final File discardOut = new File( discardPath );
//		File discardOut = new File(uri.getPath().substring(0,uri.getPath().lastIndexOf(".")) + "_discarded.dat");
		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
		BufferedWriter diswriter = new BufferedWriter(new FileWriter(discardOut));

		for (String feature : this.selectedFeatureNames) {
			writer.append(String.format("%s\t%f\n", feature, this.chi2Function.score(feature)));
		}
		
		for (String feature : this.discardedFeatureNames ){
			diswriter.append(String.format("%s\t%f\n", feature, this.chi2Function.score(feature)));
		}

		writer.close();
		diswriter.close();
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
