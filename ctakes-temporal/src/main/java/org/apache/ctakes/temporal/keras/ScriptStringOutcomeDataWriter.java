package org.apache.ctakes.temporal.keras;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.initializable.Initializable;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.encoder.features.BooleanEncoder;
import org.cleartk.ml.encoder.features.FeatureVectorFeaturesEncoder;
import org.cleartk.ml.encoder.features.NumberEncoder;
import org.cleartk.ml.encoder.features.StringEncoder;
import org.cleartk.ml.encoder.outcome.StringToIntegerOutcomeEncoder;
import org.cleartk.ml.jar.DataWriter_ImplBase;
import org.cleartk.ml.util.featurevector.FeatureVector;

public abstract class ScriptStringOutcomeDataWriter<T extends ScriptStringOutcomeClassifierBuilder<ScriptStringOutcomeClassifier>> 
  extends  DataWriter_ImplBase<T, FeatureVector, String,Integer> implements Initializable {

  public static final String PARAM_SCRIPT_DIR = "DataWriterScriptDirectory";
  @ConfigurationParameter(name=PARAM_SCRIPT_DIR)
  public String dir;
  
  public ScriptStringOutcomeDataWriter(File outputDirectory)
      throws FileNotFoundException {
    super(outputDirectory);
    FeatureVectorFeaturesEncoder fe = new FeatureVectorFeaturesEncoder();
    fe.addEncoder(new NumberEncoder());
    fe.addEncoder(new BooleanEncoder());
    fe.addEncoder(new StringEncoder());
    this.setFeaturesEncoder(fe);
    this.setOutcomeEncoder(new StringToIntegerOutcomeEncoder());
  }

  @Override
  protected void writeEncoded(FeatureVector features, Integer outcome)
      throws CleartkProcessingException {
    this.trainingDataWriter.print(outcome);
    for (FeatureVector.Entry featureNode : features) {
      this.trainingDataWriter.format(Locale.US, " %d:%.7f", featureNode.index, featureNode.value);  
    }
    this.trainingDataWriter.println();
  }

  @Override
  public void initialize(UimaContext context)
      throws ResourceInitializationException {
    this.dir = (String) context.getConfigParameterValue(PARAM_SCRIPT_DIR);
    this.classifierBuilder.setScriptDirectory(this.dir);
  }
}
