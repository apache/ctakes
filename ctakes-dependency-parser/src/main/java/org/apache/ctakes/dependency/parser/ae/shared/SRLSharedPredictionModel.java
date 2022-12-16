package org.apache.ctakes.dependency.parser.ae.shared;

import org.apache.uima.resource.ResourceInitializationException;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;

public class SRLSharedPredictionModel extends SRLSharedModel {
  public static final String DEFAULT_PRED_MODEL_FILE_NAME = 
      "org/apache/ctakes/dependency/parser/models/pred/mayo-en-pred-1.3.0.jar";
  
  @Override
  protected String getMode() {
    return NLPLib.MODE_PRED;
  }
  
  public static AbstractComponent getDefaultModel() throws ResourceInitializationException{
    return SRLSharedModel.getUriComponent(DEFAULT_PRED_MODEL_FILE_NAME, AbstractReader.LANG_EN, NLPLib.MODE_PRED);
  }
}
