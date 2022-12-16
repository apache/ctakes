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
package org.apache.ctakes.assertion.attributes.features.selection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.transform.TransformableFeature;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * <br>
 * Copyright (c) 2007-2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * <p>
 * 
 * Selects features via mutual information statistics between the features extracted from its
 * sub-extractor and the outcome values they are paired with in classification instances.
 * 
 * @author Lee Becker
 * 
 */
public class MutualInformationFeatureSelection<OUTCOME_T> extends FeatureSelection<OUTCOME_T> {

  /**
   * Specifies how scores for each outcome should be combined/aggregated into a single score
   */
  public static enum CombineScoreMethod implements Function<Map<?, Double>, Double> {
    AVERAGE {
      public Double apply(Map<?, Double> input) {
        Collection<Double> scores = input.values();
        int size = scores.size();
        double total = 0;
        for (Double score : scores) {
          total += score;
        }
        return total / size;
      }
    },
    MAX {
      @Override
      public Double apply(Map<?, Double> input) {
        return Ordering.natural().max(input.values());
      }
    }
  }

  /**
   * Helper class for aggregating and computing mutual information statistics
   */
  public static class MutualInformationStats<OUTCOME_T> {
    protected Multiset<OUTCOME_T> classCounts;

    protected Table<String, OUTCOME_T, Integer> classConditionalCounts;

    protected double smoothingCount;

    public MutualInformationStats(double smoothingCount) {
      this.classCounts = HashMultiset.<OUTCOME_T> create();
      this.classConditionalCounts = HashBasedTable.<String, OUTCOME_T, Integer> create();
      this.smoothingCount += smoothingCount;
    }

    public void update(String featureName, OUTCOME_T outcome, int occurrences) {
      Integer count = this.classConditionalCounts.get(featureName, outcome);
      if (count == null) {
        count = 0;
      }
      this.classConditionalCounts.put(featureName, outcome, count + occurrences);
      this.classCounts.add(outcome, occurrences);
    }

    public double mutualInformation(String featureName, OUTCOME_T outcome) {
      // notation index of 0 means false, 1 mean true
      int[] featureCounts = new int[2];
      int[] outcomeCounts = new int[2];
      int[][] featureOutcomeCounts = new int[2][2];

      int n = this.classCounts.size();
      featureCounts[1] = sum(this.classConditionalCounts.row(featureName).values());
      featureCounts[0] = n - featureCounts[1];
      outcomeCounts[1] = this.classCounts.count(outcome);
      outcomeCounts[0] = n - outcomeCounts[1];

      featureOutcomeCounts[1][1] = this.classConditionalCounts.contains(featureName, outcome)
          ? this.classConditionalCounts.get(featureName, outcome)
          : 0;
      featureOutcomeCounts[1][0] = featureCounts[1] - featureOutcomeCounts[1][1];
      featureOutcomeCounts[0][1] = outcomeCounts[1] - featureOutcomeCounts[1][1];
      featureOutcomeCounts[0][0] = n - featureCounts[1] - outcomeCounts[1]
          + featureOutcomeCounts[1][1];

      double information = 0.0;
      for (int nFeature = 0; nFeature <= 1; nFeature++) {
        for (int nOutcome = 0; nOutcome <= 1; nOutcome++) {
          featureOutcomeCounts[nFeature][nOutcome] += smoothingCount;
          information += (double) featureOutcomeCounts[nFeature][nOutcome]
              / (double) n
              * Math.log(((double) n * featureOutcomeCounts[nFeature][nOutcome])
                  / ((double) featureCounts[nFeature] * outcomeCounts[nOutcome]));
        }
      }

      return information;
    }

    private static int sum(Collection<Integer> values) {
      int total = 0;
      for (int v : values) {
        total += v;
      }
      return total;
    }

    public void save(URI outputURI) throws IOException {
      File out = new File(outputURI);
      BufferedWriter writer = null;
      writer = new BufferedWriter(new FileWriter(out));

      // Write out header
      writer.append("Mutual Information Data\n");
      writer.append("Feature\t");
      writer.append(Joiner.on("\t").join(this.classConditionalCounts.columnKeySet()));
      writer.append("\n");

      // Write out Mutual Information data
      for (String featureName : this.classConditionalCounts.rowKeySet()) {
        writer.append(featureName);
        for (OUTCOME_T outcome : this.classConditionalCounts.columnKeySet()) {
          writer.append("\t");
          writer.append(String.format("%f", this.mutualInformation(featureName, outcome)));
        }
        writer.append("\n");
      }
      writer.append("\n");
      writer.append(this.classConditionalCounts.toString());
      writer.close();
    }

    public Function<String, Double> getScoreFunction(final CombineScoreMethod combineScoreMethod) {
      return new Function<String, Double>() {

        @Override
        public Double apply(String featureName) {
          Set<OUTCOME_T> outcomes = classConditionalCounts.columnKeySet();
          Map<OUTCOME_T, Double> featureOutcomeMI = Maps.newHashMap();
          for (OUTCOME_T outcome : outcomes) {
            featureOutcomeMI.put(outcome, mutualInformation(featureName, outcome));
          }
          return combineScoreMethod.apply(featureOutcomeMI);
        }
      };
    }
  }

  private MutualInformationStats<OUTCOME_T> mutualInfoStats;

  private int numFeatures;

  private CombineScoreMethod combineScoreMethod;

  private double smoothingCount;

  public MutualInformationFeatureSelection(String name) {
    this(name, CombineScoreMethod.MAX, 1.0, 10);
  }

  public MutualInformationFeatureSelection(String name, int numFeatures) {
    this(name, CombineScoreMethod.MAX, 1.0, numFeatures);
  }

  public MutualInformationFeatureSelection(
      String name,
      CombineScoreMethod combineScoreMethod,
      double smoothingCount,
      int numFeatures) {
    super(name);
    this.combineScoreMethod = combineScoreMethod;
    this.smoothingCount = smoothingCount;
    this.numFeatures = numFeatures;
  }

  @Override
  public void train(Iterable<Instance<OUTCOME_T>> instances) {
    // aggregate statistics for all features and classes
    this.mutualInfoStats = new MutualInformationStats<OUTCOME_T>(this.smoothingCount);
    for (Instance<OUTCOME_T> instance : instances) {
      OUTCOME_T outcome = instance.getOutcome();
      for (Feature feature : instance.getFeatures()) {
        if (this.isTransformable(feature)) {
          for (Feature untransformedFeature : ((TransformableFeature) feature).getFeatures()) {
            mutualInfoStats.update(this.getFeatureName(untransformedFeature), outcome, 1);
          }
        }
      }
    }

    // sort features by mutual information score
    Set<String> featureNames = mutualInfoStats.classConditionalCounts.rowKeySet();
    Function<String, Double> scoreFunction = this.mutualInfoStats.getScoreFunction(this.combineScoreMethod);
    Ordering<String> ordering = Ordering.natural().onResultOf(scoreFunction).reverse();

    // keep only the top N features
    this.selectedFeatureNames = Sets.newLinkedHashSet(ordering.immutableSortedCopy(featureNames).subList(
        0,
        this.numFeatures));
    this.isTrained = true;
  }

  @Override
  public void save(URI uri) throws IOException {
    if (!this.isTrained) {
      throw new IOException("MutualInformationFeatureExtractor: Cannot save before training.");
    }
    File out = new File(uri);
    BufferedWriter writer = new BufferedWriter(new FileWriter(out));
    writer.append("CombineScoreType\t");
    writer.append(this.combineScoreMethod.toString());
    writer.append('\n');

    for (String featureName : this.selectedFeatureNames) {
      writer.append(featureName);
      writer.append('\n');
    }

    writer.close();
  }

  @Override
  public void load(URI uri) throws IOException {
    this.selectedFeatureNames = Sets.newLinkedHashSet();
    File in = new File(uri);
    BufferedReader reader = new BufferedReader(new FileReader(in));

    // First line specifies the combine utility type
    this.combineScoreMethod = CombineScoreMethod.valueOf(reader.readLine().split("\t")[1]);

    // The rest of the lines are feature + selection scores
    String line = null;
    int n = 0;
    while ((line = reader.readLine()) != null && n < this.numFeatures) {
      String featureName = line.trim();
      this.selectedFeatureNames.add(featureName);
      n++;
    }

    reader.close();
    this.isTrained = true;
  }
}
