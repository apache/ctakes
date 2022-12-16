package org.apache.ctakes.temporal.keras;

import java.io.File;

import org.cleartk.ml.encoder.features.FeaturesEncoder;
import org.cleartk.ml.encoder.outcome.OutcomeEncoder;
import org.cleartk.ml.util.featurevector.FeatureVector;

import com.google.common.annotations.Beta;

/**
 * <br>
 * Copyright (c) 2016, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Tim Miller
 * @version 2.0.1
 * 
 */
@Beta
public class KerasStringOutcomeClassifier extends ScriptStringOutcomeClassifier {

  public KerasStringOutcomeClassifier(
      FeaturesEncoder<FeatureVector> featuresEncoder,
      OutcomeEncoder<String, Integer> outcomeEncoder, File modelDir,
      File scriptDir) {
    super(featuresEncoder, outcomeEncoder, modelDir, scriptDir);
  }
}
