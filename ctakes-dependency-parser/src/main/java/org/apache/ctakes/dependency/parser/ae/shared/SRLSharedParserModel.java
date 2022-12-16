package org.apache.ctakes.dependency.parser.ae.shared;

import org.apache.uima.resource.ResourceInitializationException;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;

public class SRLSharedParserModel extends SRLSharedModel {
  public static final String DEFAULT_SRL_MODEL_FILE_NAME = 
      "org/apache/ctakes/dependency/parser/models/srl/mayo-en-srl-1.3.0.jar";

  @Override
  protected String getMode() {
    return NLPLib.MODE_SRL;
  }

  public static AbstractComponent getDefaultModel() throws ResourceInitializationException {
    return SRLSharedModel.getUriComponent(DEFAULT_SRL_MODEL_FILE_NAME, AbstractReader.LANG_EN, NLPLib.MODE_SRL);
  }
}
