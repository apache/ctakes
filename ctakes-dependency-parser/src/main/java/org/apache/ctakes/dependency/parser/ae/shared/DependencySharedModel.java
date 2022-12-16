package org.apache.ctakes.dependency.parser.ae.shared;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DependencySharedModel implements SharedResourceObject {

   private AbstractComponent parser;
   public static final String DEFAULT_MODEL_FILE_NAME = "org/apache/ctakes/dependency/parser/models/dependency/mayo-en-dep-1.3.0.jar";
   static public final String DEFAULT_LANGUAGE = AbstractReader.LANG_EN;
   // If this is final then why don't we just use a default such as above?  Future mutability?
   final String language = DEFAULT_LANGUAGE;
   public Logger logger = Logger.getLogger( getClass().getName() );

   @Override
   public void load( final DataResource aData ) throws ResourceInitializationException {
      URI uri = aData.getUri();
//    try{
//      InputStream parserModel = (uri == null)
//          ? FileLocator.getAsStream(DEFAULT_MODEL_FILE_NAME)
//              : FileLocator.getAsStream(uri.getPath());
//
//          this.parser = EngineGetter.getComponent(parserModel, this.language, NLPLib.MODE_DEP);
//    }catch(IOException e){
//      throw new ResourceInitializationException(e);
//    }
      if ( uri != null ) {
         this.parser = getModel( uri.getPath(), this.language );
      } else {
         this.parser = getDefaultModel();
      }
   }

   public AbstractComponent getParser() {
      return parser;
   }

   static public AbstractComponent getModel( final String modelPath, final String language ) throws ResourceInitializationException {
      try {
         final InputStream modelStream = FileLocator.getAsStream( modelPath );
         return EngineGetter.getComponent( modelStream, language, NLPLib.MODE_DEP );
      } catch ( IOException e ) {
         throw new ResourceInitializationException( e );
      }
   }

   public static AbstractComponent getDefaultModel() throws ResourceInitializationException {
//    try{
//      return EngineGetter.getComponent(FileLocator.getAsStream(DEFAULT_MODEL_FILE_NAME), AbstractReader.LANG_EN, NLPLib.MODE_DEP);
//    }catch(IOException e){
//      throw new ResourceInitializationException(e);
//    }
      return getModel( DEFAULT_MODEL_FILE_NAME, DEFAULT_LANGUAGE );
   }
}
