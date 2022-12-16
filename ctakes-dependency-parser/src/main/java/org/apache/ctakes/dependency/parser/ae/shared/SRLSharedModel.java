package org.apache.ctakes.dependency.parser.ae.shared;

import java.io.IOException;
import java.net.URI;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.reader.AbstractReader;

public abstract class SRLSharedModel implements SharedResourceObject {

  protected AbstractComponent component;
  protected String language = AbstractReader.LANG_EN;
  
  @Override
  public void load(DataResource aData) throws ResourceInitializationException {
    URI modelUri = aData.getUri();
    this.component = getUriComponent(modelUri.toString(), this.language, this.getMode() );
  }

  public AbstractComponent getComponent(){
    return this.component;
  }
  
  public static AbstractComponent getUriComponent(String uri, String lang, String mode) throws ResourceInitializationException{
    try {
      return EngineGetter.getComponent( FileLocator.getAsStream(uri), lang, mode );
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }
  
  protected abstract String getMode();  
}
