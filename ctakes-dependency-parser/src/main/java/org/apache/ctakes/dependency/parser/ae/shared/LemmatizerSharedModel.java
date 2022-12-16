package org.apache.ctakes.dependency.parser.ae.shared;

import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.reader.AbstractReader;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class LemmatizerSharedModel implements SharedResourceObject {

   public static final String ENG_LEMMATIZER_DATA_FILE = "org/apache/ctakes/dependency/parser/models/lemmatizer/dictionary-1.3.1.jar";

   private AbstractMPAnalyzer lemmatizer = null;
   static public final String DEFAULT_LANGUAGE = AbstractReader.LANG_EN;
   // If this is final then why don't we just use a default such as above?  Future mutability?
   final String language = DEFAULT_LANGUAGE;
   public Logger logger = Logger.getLogger( getClass().getName() );

   @Override
   public void load( final DataResource aData ) throws ResourceInitializationException {
      URI modelUri = aData.getUri();
//    try{
//      InputStream lemmatizerModel = (modelUri == null)
//          ? FileLocator.getAsStream(ENG_LEMMATIZER_DATA_FILE)
//              : FileLocator.getAsStream(modelUri.getPath());
//
//      this.lemmatizer = EngineGetter.getMPAnalyzer(language, lemmatizerModel);
//    }catch(IOException e){
//      throw new ResourceInitializationException(e);
//    }
      if ( modelUri != null ) {
         this.lemmatizer = getAnalyzer( modelUri.getPath(), this.language );
      } else {
         this.lemmatizer = getDefaultAnalyzer();
      }
   }

   public AbstractMPAnalyzer getLemmatizerModel() {
      return this.lemmatizer;
   }

   static public AbstractMPAnalyzer getAnalyzer( final String dataPath, final String language ) throws ResourceInitializationException {
      try {
         final InputStream dataStream = FileLocator.getAsStream( dataPath );
         return EngineGetter.getMPAnalyzer( language, dataStream );
      } catch ( IOException e ) {
         throw new ResourceInitializationException( e );
      }
   }

   static public AbstractMPAnalyzer getDefaultAnalyzer() throws ResourceInitializationException {
      return getAnalyzer( ENG_LEMMATIZER_DATA_FILE, DEFAULT_LANGUAGE );
   }

}
