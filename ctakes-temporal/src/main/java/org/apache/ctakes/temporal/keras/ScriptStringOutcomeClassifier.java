package org.apache.ctakes.temporal.keras;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.encoder.features.FeaturesEncoder;
import org.cleartk.ml.encoder.outcome.OutcomeEncoder;
import org.cleartk.ml.jar.Classifier_ImplBase;
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
public abstract class ScriptStringOutcomeClassifier extends Classifier_ImplBase<FeatureVector, String, Integer> {
  File modelDir = null;
  Process classifierProcess = null;
  PrintStream toClassifier = null;
  BufferedReader reader = null;
  BufferedReader errReader = null;
  Logger logger = UIMAFramework.getLogger(ScriptStringOutcomeClassifier.class);

  public ScriptStringOutcomeClassifier(
      FeaturesEncoder<FeatureVector> featuresEncoder,
      OutcomeEncoder<String, Integer> outcomeEncoder,
      File modelDir,
      File scriptDir) {
    super(featuresEncoder, outcomeEncoder);
    this.modelDir = modelDir;
    
    File classifyScript = null;
    for(File file : scriptDir.listFiles()){
      if(file.getName().startsWith("classify.sh")){
        if(classifyScript != null){
          throw new RuntimeException("There are multiple files named classify.*");
        }
        classifyScript = file;
      }
    }
    
    if(classifyScript == null){
      throw new RuntimeException("There are no files named classify.*");
    }
    
    try {
      this.classifierProcess = Runtime.getRuntime().exec(new String[]{
          classifyScript.getAbsolutePath(),
          modelDir.getAbsolutePath()});
      // start the classifier process running, give it a chance to read the model, and
      // set classifierProcess to the running classifier
      toClassifier = new PrintStream(classifierProcess.getOutputStream());
      reader = new BufferedReader(new InputStreamReader(classifierProcess.getInputStream()));
      errReader = new BufferedReader(new InputStreamReader(classifierProcess.getErrorStream()));
      errReader.readLine(); // read line about which backend it is using.
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public String classify(List<Feature> features)
      throws CleartkProcessingException {
    // Encode the features and pass them to the standard input of the classifier process
    // and then read the standard output prediction, which will be in the string format expected by
    // the annotator.    

    StringBuilder buf = new StringBuilder();
    
//    for (FeatureVector.Entry featureNode : this.featuresEncoder.encodeAll(features)) {
//      buf.append(String.format(Locale.US, " %d:%.7f", featureNode.index, featureNode.value));  
//    }
    for (int i = 0; i < features.size(); i ++){
    	buf.append(features.get(i).getValue());
    	if( i < features.size()-1){
    		buf.append(" ");
    	}
    }

    this.toClassifier.println(buf);
    this.toClassifier.flush();
    
    String line = "";
    String eLine = "";
    try {
      line = reader.readLine();
      if(line == null){
         while((eLine = errReader.readLine()) != null){
           logger.log(Level.SEVERE, eLine);
         }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    return line;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    
    this.toClassifier.print('\n');
    classifierProcess.waitFor();
  }
}
