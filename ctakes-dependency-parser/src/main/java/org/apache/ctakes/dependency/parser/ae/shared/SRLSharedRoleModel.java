package org.apache.ctakes.dependency.parser.ae.shared;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;
import org.apache.uima.resource.ResourceInitializationException;

public class SRLSharedRoleModel extends SRLSharedModel {
  public static final String DEFAULT_ROLE_MODEL_FILE_NAME
  = "org/apache/ctakes/dependency/parser/models/role/mayo-en-role-1.3.0.jar";
  
  @Override
  protected String getMode() {
    return NLPLib.MODE_ROLE;
  }

  public static AbstractComponent getDefaultModel() throws ResourceInitializationException{
    return SRLSharedModel.getUriComponent(DEFAULT_ROLE_MODEL_FILE_NAME, AbstractReader.LANG_EN, NLPLib.MODE_ROLE);
  }
}
