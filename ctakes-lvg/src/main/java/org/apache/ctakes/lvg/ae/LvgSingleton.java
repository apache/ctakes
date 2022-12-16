package org.apache.ctakes.lvg.ae;


import org.apache.ctakes.lvg.resource.LvgCmdApiResourceImpl;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Holds a single instance of the LvgAnnotator.
 * Use this singleton from an annotator instead of the LvgAnnotator directly to help prevent thread safety issues.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/14/2017
 */
enum LvgSingleton {
   INSTANCE;

   static public LvgSingleton getInstance() {
      return INSTANCE;
   }

   static public final String PROPERTIES_PATH = "org/apache/ctakes/lvg/data/config/lvg.properties";

   private final Logger LOGGER = Logger.getLogger( "LvgSingleton" );
   private final Object LOCK = new Object();

   private final LvgAnnotator _lvgAnnotator;

   private ExternalResourceDescription _lvgCmdApi;
   private boolean _initialized;


   LvgSingleton() {
      _lvgAnnotator = new LvgAnnotator();
   }

   /**
    * Calls initialize on the single LVG instance if and only if it has not already been initialized
    */
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      synchronized ( LOCK ) {
         if ( !_initialized ) {
            _initialized = true;
            _lvgAnnotator.initialize( context );
         }
      }
   }

   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      synchronized ( LOCK ) {
         _lvgAnnotator.process( jCas );
      }
   }


   public ExternalResourceDescription getDefaultLvgCmdApi() throws IOException {
      synchronized ( LOCK ) {
         if ( _lvgCmdApi != null ) {
            return _lvgCmdApi;
         }
         
         URL url = LvgAnnotator.class.getClassLoader().getResource( PROPERTIES_PATH );
         if (url!=null) {
        	 LOGGER.info("URL for lvg.properties =" + url.getFile());
         } else {
             String absolutePath = "/tmp/";
             LOGGER.info("URL==null");
             LOGGER.info("Unable to find " + PROPERTIES_PATH + ".");
             LOGGER.info("Copying files and directories to under " + absolutePath);
             File lvgFile = new File(LvgAnnotator.copyLvgFiles(absolutePath));
             url = lvgFile.toURI().toURL();
         }

         _lvgCmdApi = ExternalResourceFactory.createExternalResourceDescription( LvgCmdApiResourceImpl.class, url );
         return _lvgCmdApi;
      }
   }

}
