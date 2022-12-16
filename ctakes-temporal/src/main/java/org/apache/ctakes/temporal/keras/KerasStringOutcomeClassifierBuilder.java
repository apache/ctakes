package org.apache.ctakes.temporal.keras;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.apache.uima.util.Level;
import org.cleartk.ml.jar.JarStreams;

import com.google.common.annotations.Beta;
import com.google.common.io.Files;

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
public class KerasStringOutcomeClassifierBuilder extends ScriptStringOutcomeClassifierBuilder<ScriptStringOutcomeClassifier> {

  @Override
  public void packageClassifier(File dir, JarOutputStream modelStream) throws IOException {
    super.packageClassifier(dir, modelStream);
    
    JarStreams.putNextJarEntry(modelStream, "outcome-lookup.txt", new File(dir, "outcome-lookup.txt"));
    
    int modelNum = 0;
    while(true){
      File modelArchFile = new File(dir, getArchFilename(modelNum));
      File modelWeightsFile = new File(dir, getWeightsFilename(modelNum));
      if(!modelArchFile.exists()) break;
      
      JarStreams.putNextJarEntry(modelStream, modelArchFile.getName(), modelArchFile.getAbsoluteFile());
      JarStreams.putNextJarEntry(modelStream, modelWeightsFile.getName(), modelWeightsFile.getAbsoluteFile());
      modelNum++;
    }
  }
    
  @Override
  protected void unpackageClassifier(JarInputStream modelStream)
      throws IOException {
    super.unpackageClassifier(modelStream);
    
    
    // create the model dir to unpack all the model files
    this.modelDir = Files.createTempDir();
    
    // grab the script dir from the manifest:
    this.scriptDir = new File(modelStream.getManifest().getMainAttributes().getValue(SCRIPT_DIR_PARAM));
    
    extractFileToDir(modelDir, modelStream, "outcome-lookup.txt");

    int modelNum = 0;
    while(true){
      String archFn = getArchFilename(modelNum);
      String wtsFn = getWeightsFilename(modelNum);
      
      try{
        extractFileToDir(modelDir, modelStream, archFn);
        extractFileToDir(modelDir, modelStream, wtsFn);
      }catch(IOException e){
        logger.log(Level.WARNING, "Encountered the following exception: " + e.getMessage());
        break;
      }
      modelNum++;
    }
  }
  
  
  @Override
  protected KerasStringOutcomeClassifier newClassifier() {
    return new KerasStringOutcomeClassifier(this.featuresEncoder, this.outcomeEncoder, this.modelDir, this.scriptDir);
  }

  private static String getArchFilename(int num){
    return "model_"+num+".json";
  }
  
  private static String getWeightsFilename(int num){
    return "model_"+num+".h5";
  }
}
