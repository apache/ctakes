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
 * Selects features via binary Krippendorff's Alpha-Reliability between the a extracted feature
 * and the outcome values they are paired with in classification instances.
 * 
 * Alpha-positive: when the positive feature value suggests positive label
 * Alpha-negative: when the positive feature value suggests negative label
 * Final Alpha = max(Alpha-positive, Alpha-negative)
 * 
 * Reference: Krippendorff, Klaus. "Computing Krippendorff's alpha reliability." Departmental Papers (ASC) (2007): 43.
 * 
 * @author Chen Lin
 * 
 */
public class BinaryAlphaFeatureSelection<OUTCOME_T> extends FeatureSelection<OUTCOME_T> {

	/**
	 * Helper class for aggregating and computing mutual Odds Ratio statistics
	 */
	private static class AlphaScorer<OUTCOME_T> implements Function<String, Double> {
		protected Multiset<OUTCOME_T> classCounts;

		protected Table<String, OUTCOME_T, Integer> featValueClassCount;

		private String positiveClass = null;

		public AlphaScorer(String posiClas) {
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
			// Coincidence matrix:
			//       | Y = 0  | Y = 1  | 
			// X = 0 | o00    | o01    | n0
			// X = 1 | o10    | o11    | n1
			//___________________________________________
			//		 | n0	  | n1	   | n=2N

			double o11 = 0, o01=0, o00 = 0; 
			double on11 = 0, on01=0, on00 = 0; //on- occurrence if positive feature suggests negative label
			double n0 =0, n1=0, n=0;

			for (OUTCOME_T clas : this.classCounts.elementSet()) {
				int numAgreement = this.featValueClassCount.contains(featureName, clas)
						? this.featValueClassCount.get(featureName, clas): 0;
				int numInstanceInThisClass = this.classCounts.count(clas);
				int numDisagreement = numInstanceInThisClass - numAgreement;
				n += numInstanceInThisClass;
				if ( clas.toString().equals("B") || clas.toString().equals("I") || clas.toString().equals(this.positiveClass )){
					o11 += 2* numAgreement;
					o01 += numDisagreement;
					on00 += 2* numDisagreement;
					on01 += numAgreement;
				}else if( this.positiveClass==null && clas.toString().equals("O")){
					o00 += 2 * numDisagreement;
					o01 += numAgreement;
					on11 += 2* numAgreement;
					on01 += numDisagreement;
				}else{
					System.err.println("Please define postive class label for odds ratio calculation.");
					System.exit(0);
				}
			}
			
			n0 = o00+o01;
			n1 = o11+o01;
			
			
			if( (n0+n1) != (2*n)){
				System.err.println("Alpha Calculation is wrong.");
				System.exit(0);
			}
			
			double alpha_positive = 1 - (2*n -1)*o01/(n0*n1);
			
			n0 = on00+on01;
			n1 = on11+on01;
			double alpha_negative = 1 - (2*n -1)*on01/(n0*n1);

			return Math.max(alpha_negative,alpha_positive);

		}
	}

	private double featureSelectionThreshold;

	private int numFeatures = 0;
	
	private String positiveClass = null;

	private AlphaScorer<OUTCOME_T> alphaFunction;

	private LinkedHashSet<String> discardedFeatureNames;

	public BinaryAlphaFeatureSelection(String name) {
		this(name, 0.0);
	}

	public BinaryAlphaFeatureSelection(String name, double threshold) {
		super(name);
		this.featureSelectionThreshold = threshold;
	}

	/**
	 * 
	 * @param name: feature selection method name
	 * @param threshold: percentage threshold to control how many features to keep
	 * @param posiClas: specify which class is the positive class
	 */
	public BinaryAlphaFeatureSelection(String name, double threshold, String posiClas) {
		super(name);
		this.featureSelectionThreshold = threshold;
		this.positiveClass = posiClas;
	}

	@Override
	public boolean apply(Feature feature) {
		return this.selectedFeatureNames.contains(this.getFeatureName(feature));
	}

	@Override
	public void train(Iterable<Instance<OUTCOME_T>> instances) {
		// aggregate statistics for all features
		this.alphaFunction = new AlphaScorer<OUTCOME_T>(positiveClass);
		for (Instance<OUTCOME_T> instance : instances) {
			OUTCOME_T outcome = instance.getOutcome();
			for (Feature feature : instance.getFeatures()) {
				if (this.isTransformable(feature)) {
					for (Feature untransformedFeature : ((TransformableFeature) feature).getFeatures()) {
						this.alphaFunction.update(this.getFeatureName(untransformedFeature), outcome, 1);
					}
				}
			}
		}


		// sort features by Odds Ratio score
		Set<String> featureNames = this.alphaFunction.featValueClassCount.rowKeySet();
		Ordering<String> ordering = Ordering.natural().onResultOf(this.alphaFunction).reverse();

		int totalFeatures = featureNames.size();
		this.numFeatures = (int) Math.round(totalFeatures*this.featureSelectionThreshold);

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
		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
		BufferedWriter diswriter = new BufferedWriter(new FileWriter(discardOut));

		for (String feature : this.selectedFeatureNames) {
			writer.append(String.format("%s\t%f\n", feature, this.alphaFunction.score(feature)));
		}
		
		for (String feature : this.discardedFeatureNames) {
			diswriter.append(String.format("%s\t%f\n", feature, this.alphaFunction.score(feature)));
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
