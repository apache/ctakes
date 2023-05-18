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
package org.apache.ctakes.assertion.eval;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.eval.util.ConfusionMatrix;

import java.io.Serializable;
import java.util.*;

/**
 * Stores statistics for comparing {@link Annotation}s extracted by a system to gold
 * {@link Annotation}s.
 * 
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Steven Bethard
 */
public class AnnotationStatisticsCompact<OUTCOME_TYPE extends Comparable<? super OUTCOME_TYPE>> implements
    Serializable {

  private static final long serialVersionUID = 1L;

  private final Multiset<OUTCOME_TYPE> referenceOutcomes;

  private final Multiset<OUTCOME_TYPE> predictedOutcomes;

  private final Multiset<OUTCOME_TYPE> correctOutcomes;

  private final ConfusionMatrix<OUTCOME_TYPE> confusionMatrix;

  /**
   * Creates a {@link Function} that converts an {@link Annotation} into a hashable representation
   * of its begin and end offsets.
   * 
   * The {@link Function} created by this method is suitable for passing to the first
   * {@link Function} argument of {@link #add(Collection, Collection, Function, Function)}.
   */
  public static <ANNOTATION_TYPE extends Annotation> Function<ANNOTATION_TYPE, Span> annotationToSpan() {
    return new Function<ANNOTATION_TYPE, Span>() {
      @Override
      public Span apply(ANNOTATION_TYPE annotation) {
        return new Span(annotation);
      }
    };
  }

  /**
   * Creates a {@link Function} that extracts a feature value from a {@link TOP}.
   * 
   * The {@link Function} created by this method is suitable for passing to the second
   * {@link Function} argument of {@link #add(Collection, Collection, Function, Function)}.
   * 
   * @param featureName
   *          The name of the feature whose value is to be extracted.
   */
  public static <ANNOTATION_TYPE extends TOP> Function<ANNOTATION_TYPE, String> annotationToFeatureValue(
      final String featureName) {
    return new Function<ANNOTATION_TYPE, String>() {
      @Override
      public String apply(ANNOTATION_TYPE annotation) {
        Feature feature = annotation.getType().getFeatureByBaseName(featureName);
        return annotation.getFeatureValueAsString(feature);
      }
    };
  }

  /**
   * Creates a {@link Function} that always returns null.
   * 
   * This may be useful when only the span of the offset is important, but you still need to pass in
   * the final argument of {@link #add(Collection, Collection, Function, Function)}.
   */
  public static <ANNOTATION_TYPE, OUTCOME_TYPE> Function<ANNOTATION_TYPE, OUTCOME_TYPE> annotationToNull() {
    return new Function<ANNOTATION_TYPE, OUTCOME_TYPE>() {
      @Override
      public OUTCOME_TYPE apply(ANNOTATION_TYPE annotation) {
        return null;
      }
    };
  }

  /**
   * Add all statistics together.
   * 
   * This is often useful for combining individual fold statistics that result from methods like
   *
   * @param statistics
   *          The sequence of statistics that should be combined.
   * @return The combination of all the individual statistics.
   */
  public static <OUTCOME_TYPE extends Comparable<? super OUTCOME_TYPE>> AnnotationStatisticsCompact<OUTCOME_TYPE> addAll(
      Iterable<AnnotationStatisticsCompact<OUTCOME_TYPE>> statistics) {
    AnnotationStatisticsCompact<OUTCOME_TYPE> result = new AnnotationStatisticsCompact<>();
    for (AnnotationStatisticsCompact<OUTCOME_TYPE> item : statistics) {
      result.addAll(item);
    }
    return result;
  }

  /**
   * Create an AnnotationStatisticsCompact that compares {@link Annotation}s based on their begin and end
   * offsets, plus a {@link Feature} of the {@link Annotation} that represents the outcome or label.
   */
  public AnnotationStatisticsCompact() {
    this.referenceOutcomes = HashMultiset.create();
    this.predictedOutcomes = HashMultiset.create();
    this.correctOutcomes = HashMultiset.create();
    this.confusionMatrix = new ConfusionMatrix<>();
  }

  /**
   * Update the statistics, comparing the reference annotations to the predicted annotations.
   * 
   * Annotations are considered to match if they have the same character offsets in the text. All
   * outcomes (e.g. as returned in {@link #confusions()}) will be <code>null</code>.
   * 
   * @param referenceAnnotations
   *          The reference annotations, typically identified by humans.
   * @param predictedAnnotations
   *          The predicted annotations, typically identified by a model.
   */
  public <ANNOTATION_TYPE extends Annotation> void add(
      Collection<? extends ANNOTATION_TYPE> referenceAnnotations,
      Collection<? extends ANNOTATION_TYPE> predictedAnnotations) {
    this.add(
        referenceAnnotations,
        predictedAnnotations,
        AnnotationStatisticsCompact.annotationToSpan(),
        AnnotationStatisticsCompact.annotationToNull());
  }

  /**
   * Update the statistics, comparing the reference annotations to the predicted annotations.
   * 
   * Annotations are considered to match if they have the same span (according to
   * {@code annotationToSpan}) and if they have the same outcome (according to
   * {@code annotationToOutcome}).
   * 
   * @param referenceAnnotations
   *          The reference annotations, typically identified by humans.
   * @param predictedAnnotations
   *          The predicted annotations, typically identified by a model.
   * @param annotationToSpan
   *          A function that defines how to convert an annotation into a hashable object that
   *          represents the span of that annotation. The {@link #annotationToSpan()} method
   *          provides an example function that could be used here.
   * @param annotationToOutcome
   *          A function that defines how to convert an annotation into an object that represents
   *          the outcome (or "label") assigned to that annotation. The
   *          {@link #annotationToFeatureValue(String)} method provides a sample function that could
   *          be used here.
   */
  public <ANNOTATION_TYPE, SPAN_TYPE> void add(
      Collection<? extends ANNOTATION_TYPE> referenceAnnotations,
      Collection<? extends ANNOTATION_TYPE> predictedAnnotations,
      Function<ANNOTATION_TYPE, SPAN_TYPE> annotationToSpan,
      Function<ANNOTATION_TYPE, OUTCOME_TYPE> annotationToOutcome) {

    // map gold spans to their outcomes
    Map<SPAN_TYPE, OUTCOME_TYPE> referenceSpanOutcomes = new HashMap<>();
    for (ANNOTATION_TYPE ann : referenceAnnotations) {
      referenceSpanOutcomes.put(annotationToSpan.apply(ann), annotationToOutcome.apply(ann));
    }

    // map system spans to their outcomes
    Map<SPAN_TYPE, OUTCOME_TYPE> predictedSpanOutcomes = new HashMap<>();
    for (ANNOTATION_TYPE ann : predictedAnnotations) {
      predictedSpanOutcomes.put(annotationToSpan.apply(ann), annotationToOutcome.apply(ann));
    }

    // update the gold and system outcomes
    this.referenceOutcomes.addAll(referenceSpanOutcomes.values());
    this.predictedOutcomes.addAll(predictedSpanOutcomes.values());

    // determine the outcomes that were correct
    Set<SPAN_TYPE> intersection = new HashSet<>( referenceSpanOutcomes.keySet() );
    intersection.retainAll(predictedSpanOutcomes.keySet());
    for (SPAN_TYPE span : intersection) {
      OUTCOME_TYPE goldOutcome = referenceSpanOutcomes.get(span);
      OUTCOME_TYPE systemOutcome = predictedSpanOutcomes.get(span);
      if (Objects.equal(goldOutcome, systemOutcome)) {
        this.correctOutcomes.add(goldOutcome);
      }
    }

    // update the confusion matrix
    Set<SPAN_TYPE> union = new HashSet<>();
    union.addAll(referenceSpanOutcomes.keySet());
    union.addAll(predictedSpanOutcomes.keySet());
    for (SPAN_TYPE span : union) {
      OUTCOME_TYPE goldOutcome = referenceSpanOutcomes.get(span);
      OUTCOME_TYPE systemOutcome = predictedSpanOutcomes.get(span);
      this.confusionMatrix.add(goldOutcome, systemOutcome);
    }
  }

  /**
   * Adds all the statistics collected by another AnnotationStatisticsCompact to this one.
   * 
   * @param that
   *          The other statistics that should be added to this one.
   */
  public void addAll(AnnotationStatisticsCompact<OUTCOME_TYPE> that) {
    this.referenceOutcomes.addAll(that.referenceOutcomes);
    this.predictedOutcomes.addAll(that.predictedOutcomes);
    this.correctOutcomes.addAll(that.correctOutcomes);
    this.confusionMatrix.add(that.confusionMatrix);
  }

  public int countCorrectOutcomes() {
    return this.correctOutcomes.size();
  }

  public int countCorrectOutcomes(OUTCOME_TYPE outcome) {
    return this.correctOutcomes.count(outcome);
  }

  public int countPredictedOutcomes() {
    return this.predictedOutcomes.size();
  }

  public int countPredictedOutcomes(OUTCOME_TYPE outcome) {
    return this.predictedOutcomes.count(outcome);
  }

  public int countReferenceOutcomes() {
    return this.referenceOutcomes.size();
  }

  public int countReferenceOutcomes(OUTCOME_TYPE outcome) {
    return this.referenceOutcomes.count(outcome);
  }

  @SafeVarargs
  public final int countFalseNegatives( OUTCOME_TYPE... positiveOutcomes ) {
    int numReferenceOutcomes = this.countReferenceOutcomes();
    int numPredictedOutcomes = this.countPredictedOutcomes();
    if (numReferenceOutcomes != numPredictedOutcomes) {
      throw new IllegalStateException(
          String.format(
              "Expected number equal number of references outcomes and predicted outcomes.  Had reference outcomes=%d, predicted outcomes=%d",
              numReferenceOutcomes,
              numPredictedOutcomes ) );
//              this.countPredictedOutcomes()));
    }
    int totalFalseNegatives = 0;
    for (OUTCOME_TYPE positiveOutcome : positiveOutcomes) {
      totalFalseNegatives += this.countReferenceOutcomes(positiveOutcome)
          - this.countCorrectOutcomes(positiveOutcome);
    }
    return totalFalseNegatives;
  }

  @SafeVarargs
  public final int countFalsePositives( OUTCOME_TYPE... positiveOutcomes ) {
    int numReferenceOutcomes = this.countReferenceOutcomes();
    int numPredictedOutcomes = this.countPredictedOutcomes();
    if (numReferenceOutcomes != numPredictedOutcomes) {
      throw new IllegalStateException(
          String.format(
              "Expected number equal number of references outcomes and predicted outcomes.  Had reference outcomes=%d, predicted outcomes=%d",
              numReferenceOutcomes,
              numPredictedOutcomes ) );
//              this.countPredictedOutcomes()));
    }
    int totalFalsePositives = 0;
    for (OUTCOME_TYPE positiveOutcome : positiveOutcomes) {
      totalFalsePositives += this.countPredictedOutcomes(positiveOutcome)
          - this.countCorrectOutcomes(positiveOutcome);
    }

    return totalFalsePositives;
  }

  @SafeVarargs
  public final int countTrueNegatives( OUTCOME_TYPE... positiveOutcomes ) {
    int numReferenceOutcomes = this.countReferenceOutcomes();
    int numPredictedOutcomes = this.countPredictedOutcomes();
    if (numReferenceOutcomes != numPredictedOutcomes) {
      throw new IllegalStateException(
          String.format(
              "Expected number equal number of references outcomes and predicted outcomes.  Had reference outcomes=%d, predicted outcomes=%d",
              numReferenceOutcomes,
              numPredictedOutcomes ) );
//              this.countPredictedOutcomes()));
    }
    int totalTrueNegatives = this.countCorrectOutcomes();

    for (OUTCOME_TYPE positiveOutcome : positiveOutcomes) {
      totalTrueNegatives -= this.countCorrectOutcomes(positiveOutcome);
    }

    return totalTrueNegatives;

  }

  @SafeVarargs
  public final int countTruePositives( OUTCOME_TYPE... positiveOutcomes ) {
    int numReferenceOutcomes = this.countReferenceOutcomes();
    int numPredictedOutcomes = this.countPredictedOutcomes();
    if (numReferenceOutcomes != numPredictedOutcomes) {
      throw new IllegalStateException(
          String.format(
              "Expected number equal number of references outcomes and predicted outcomes.  Had reference outcomes=%d, predicted outcomes=%d",
              numReferenceOutcomes,
              numPredictedOutcomes ) );
//              this.countPredictedOutcomes()));
    }

    int totalTruePositives = 0;
    for (OUTCOME_TYPE positiveOutcome : positiveOutcomes) {
      totalTruePositives += this.countCorrectOutcomes(positiveOutcome);
    }
    return totalTruePositives;
  }

  /**
   * Returns the {@link ConfusionMatrix} tabulating reference outcomes matched to predicted
   * outcomes.
   * 
   * @return The confusion matrix.
   */
  public ConfusionMatrix<OUTCOME_TYPE> confusions() {
    return this.confusionMatrix;
  }

  public double precision() {
    int nSystem = this.countPredictedOutcomes();
    return nSystem == 0 ? 1.0 : ((double) this.countCorrectOutcomes()) / nSystem;
  }

  public double precision(OUTCOME_TYPE outcome) {
    int nSystem = this.countPredictedOutcomes(outcome);
    return nSystem == 0 ? 1.0 : ((double) this.countCorrectOutcomes(outcome)) / nSystem;
  }

  public double recall() {
    int nGold = this.countReferenceOutcomes();
    return nGold == 0 ? 1.0 : ((double) this.countCorrectOutcomes()) / nGold;
  }

  public double recall(OUTCOME_TYPE outcome) {
    int nGold = this.countReferenceOutcomes(outcome);
    return nGold == 0 ? 1.0 : ((double) this.countCorrectOutcomes(outcome)) / nGold;
  }

  public double f(double beta) {
    double p = this.precision();
    double r = this.recall();
    double num = (1 + beta * beta) * p * r;
    double den = (beta * beta * p) + r;
    return den == 0.0 ? 0.0 : num / den;
  }

  public double f(double beta, OUTCOME_TYPE outcome) {
    double p = this.precision(outcome);
    double r = this.recall(outcome);
    double num = (1 + beta * beta) * p * r;
    double den = (beta * beta * p) + r;
    return den == 0.0 ? 0.0 : num / den;
  }

  public double f1() {
    return this.f(1.0);
  }

  public double f1(OUTCOME_TYPE outcome) {
    return f(1.0, outcome);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("P\tR\tF1\t#gold\t#system\t#correct\n");
    result.append(String.format(
        "%.3f\t%.3f\t%.3f\t%d\t%d\t%d\tOVERALL\n",
        this.precision(),
        this.recall(),
        this.f1(),
        this.referenceOutcomes.size(),
        this.predictedOutcomes.size(),
        this.correctOutcomes.size()));
    List<OUTCOME_TYPE> outcomes = new ArrayList<>(this.referenceOutcomes.elementSet());
    if (outcomes.size() > 1) {
      Collections.sort(outcomes);
      for (OUTCOME_TYPE outcome : outcomes) {
        result.append(String.format(
            "%.3f\t%.3f\t%.3f\t%d\t%d\t%d\t%s\n",
            this.precision(outcome),
            this.recall(outcome),
            this.f1(outcome),
            this.referenceOutcomes.count(outcome),
            this.predictedOutcomes.count(outcome),
            this.correctOutcomes.count(outcome),
            outcome));
      }
    }
    return result.toString();
  }

  public String toTsv() {
	StringBuilder result = new StringBuilder();
	result.append(String.format(
			"%s\t%.3f\t%s\t%.3f\t%s\t%.3f\t",
			"All",this.precision(),
			"All",this.recall(),
			"All",this.f1()));
	List<OUTCOME_TYPE> outcomes = new ArrayList<>(this.referenceOutcomes.elementSet());
	if (outcomes.size() > 1) {
		Collections.sort(outcomes);
		for (OUTCOME_TYPE outcome : outcomes) {
			result.append(String.format(
					"%s\t%.3f\t%s\t%.3f\t%s\t%.3f\t",
					outcome,this.precision(outcome),
					outcome,this.recall(outcome),
					outcome,this.f1(outcome)));
		}
	}
	result.append("\n");
	return result.toString();
}

  
  private static class Span {

    public int end;

    public int begin;

    public Span(Annotation annotation) {
      this.begin = annotation.getBegin();
      this.end = annotation.getEnd();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.begin, this.end);
    }

    // TODO: use consistent naming convention (obj vs o vs object) in equals
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      final Span that = (Span) obj;
      return  this.begin == that.begin &&
              this.end == that.end;
    }

    @Override
    public String toString() {
      ToStringHelper helper = Objects.toStringHelper(this);
      helper.add("begin", this.begin);
      helper.add("end", this.end);
      return helper.toString();
    }
  }
}
