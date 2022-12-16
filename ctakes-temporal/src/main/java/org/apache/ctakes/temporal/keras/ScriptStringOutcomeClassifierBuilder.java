package org.apache.ctakes.temporal.keras;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.ml.jar.ClassifierBuilder_ImplBase;
import org.cleartk.ml.jar.JarStreams;
import org.cleartk.ml.util.featurevector.FeatureVector;


public abstract class ScriptStringOutcomeClassifierBuilder<T extends ScriptStringOutcomeClassifier> extends ClassifierBuilder_ImplBase<T, FeatureVector, String, Integer> {
  public static final Attributes.Name SCRIPT_DIR_PARAM = new Attributes.Name("ScriptDirectory");
  protected static final Logger logger = UIMAFramework.getLogger(ScriptStringOutcomeClassifierBuilder.class);
  
  protected File modelDir = null;
  protected File scriptDir = null;
  
  public void setScriptDirectory(String scriptDir){
    Attributes atts = this.manifest.getMainAttributes();
    atts.put(SCRIPT_DIR_PARAM, scriptDir); 
  }
  
  @Override
  public File getTrainingDataFile(File dir) {
    return new File(dir, "training-data.liblinear");
  }

  @Override
  public void trainClassifier(File dir, String... args) throws Exception {
    // args[0] should be path to directory with model training code:
    // args[1-] is the set of arguments the program takes.
    // dir is by convention the first argument that the training script takes.
    
    if(this.scriptDir == null){
      this.scriptDir = new File(this.manifest.getMainAttributes().getValue(SCRIPT_DIR_PARAM));
    }
    // first find the train script:
    File trainScript = null;
    
    for(File file : this.scriptDir.listFiles()){
      if(file.getName().startsWith("train.")){//calls train.sh
        if(trainScript != null){
          throw new RuntimeException("There are multiple files named train.*");
        }
        trainScript = file;
      }
    }
    if(trainScript == null) throw new RuntimeException("ERROR: Train directory does not contain any scripts named train.*");
    StringBuilder cmdArgs = new StringBuilder();
    for(int i = 0; i < args.length; i++){
      cmdArgs.append(args[i]);
      cmdArgs.append(' ');
    }
    String arg2 = "";
    if(cmdArgs.length() > 0){
      arg2 = cmdArgs.substring(0, cmdArgs.length()-1);
    }
    Process p = Runtime.getRuntime().exec(new String[]{
        trainScript.getAbsolutePath(),
        dir.getAbsolutePath(),
        arg2
        });

    String line = "";

    BufferedReader reader =
        new BufferedReader(new InputStreamReader(p.getInputStream()));
    while ((line = reader.readLine()) != null) {
      logger.log(Level.INFO, line);
    }
    BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    while((line = errReader.readLine()) != null){
      logger.log(Level.WARNING, line);
    }
    int ret = p.waitFor();
    if(ret != 0){
      throw new RuntimeException();
    }
  }

  protected static void extractFileToDir(File dir, JarInputStream modelStream, String fn) throws IOException{
    JarStreams.getNextJarEntry(modelStream, fn);
    File outFile = new File(dir, fn);
    try(FileOutputStream fos = new FileOutputStream(outFile)){
      byte[] byteArray = new byte[1024];
      int i;
      while ((i = modelStream.read(byteArray)) > 0) 
      {
        //Write the bytes to the output stream
        fos.write(byteArray, 0, i);
      }
    }
  }

}
