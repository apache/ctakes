package org.apache.ctakes.core.util;

import org.apache.ctakes.core.ae.StartFinishLogger;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.HashMap;
import java.util.Map;

/**
 * This factory can load plain old java .properties files and pass the specified properties as parameters for AE creation.
 * There may be some way to get values directly into the root UimaContext, but for now this works with UimaFit parameters.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/8/2016
 */
public enum PropertyAeFactory {
   INSTANCE;

   static public PropertyAeFactory getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "PropertyAeFactory" );


   // Use a single hashmap so that multiple properties files can be used
   final private Map<String, Object> _properties = new HashMap<>();


   /**
    * Add key value pairs to the stored properties
    *
    * @param parameters ket value pairs
    */
   synchronized public void addParameters( final Object... parameters ) {
      if ( parameters.length == 0 ) {
         LOGGER.warn( "No parameters specified." );
         return;
      }
      if ( parameters.length % 2 != 0 ) {
         LOGGER.error( "Odd number of parameters provided.  Should be key value pairs." );
         return;
      }
      addToMap( _properties, parameters );
   }

   /**
    * Add key value pairs to the stored properties
    *
    * @param parameters ket value pairs
    */
   synchronized public void addIfEmptyParameters( final Object... parameters ) {
      if ( parameters.length == 0 ) {
         LOGGER.warn( "No parameters specified." );
         return;
      }
      if ( parameters.length % 2 != 0 ) {
         LOGGER.error( "Odd number of parameters provided.  Should be key value pairs." );
         return;
      }
      for ( int i = 0; i < parameters.length; i += 2 ) {
         String name;
         if ( parameters[ i ] instanceof String ) {
            name = (String) parameters[ i ];
         } else {
            LOGGER.warn( "Parameter " + i + " not a String, using " + parameters[ i ].toString() );
            name = parameters[ i ].toString();
         }
         if ( _properties.containsKey( name ) && parameters[ i + 1 ].toString().isEmpty() ) {
            LOGGER.info( "Parameter " + name + " has value " + _properties.get( name )
                  + " ; ignoring empty value" );
            continue;
         }
         _properties.put( name, parameters[ i + 1 ] );
      }
   }

   /**
    * @param parameterMap map of parameter names and values
    * @return array of Objects representing name value pairs
    */
   static private Object[] createParameters( final Map<String, Object> parameterMap ) {
      final Object[] parameters = new Object[ parameterMap.size() * 2 ];
      int i = 0;
      for ( Map.Entry<String, Object> entry : parameterMap.entrySet() ) {
         parameters[ i ] = entry.getKey();
         parameters[ i + 1 ] = entry.getValue();
         i += 2;
      }
      return parameters;
   }

   /**
    * @param parameters parameters possibly not loaded by this factory
    * @return new parameter arrays containing parameters loaded by this factory and followed by specified parameters
    */
   synchronized private Object[] getAllParameters( final Object... parameters ) {
      if ( parameters.length == 0 ) {
         return createParameters( _properties );
      }
      if ( parameters.length % 2 != 0 ) {
         LOGGER.error( "Odd number of parameters provided.  Should be key value pairs." );
         return createParameters( _properties );
      }
      if ( _properties.isEmpty() ) {
         return parameters;
      }
      final Map<String, Object> parameterMap = new HashMap<>( _properties );
      addToMap( parameterMap, parameters );
      return createParameters( parameterMap );
   }

   /**
    * @param readerClass Collection Reader class
    * @param parameters  parameters for the main component
    * @return Description with specified parameters plus those loaded from properties
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public CollectionReaderDescription createReaderDescription( final Class<? extends CollectionReader> readerClass,
                                                               final Object... parameters )
         throws ResourceInitializationException {
      final Object[] allParameters = getAllParameters( parameters );
      return CollectionReaderFactory.createReaderDescription( readerClass, allParameters );
   }

   /**
    * This method should be avoided.  See the bottom of https://uima.apache.org/d/uimafit-current/api/index.html
    *
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Engine with specified parameters plus those loaded from properties
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngine createEngine( final Class<? extends AnalysisComponent> classType,
                                       final Object... parameters )
         throws ResourceInitializationException {
      final AnalysisEngineDescription description = createDescription( classType, parameters );
      final Object[] allParameters = getAllParameters( parameters );
      return AnalysisEngineFactory.createEngine( description, allParameters );
   }

   /**
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Description with specified parameters plus those loaded from properties
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngineDescription createDescription( final Class<? extends AnalysisComponent> classType,
                                                       final Object... parameters )
         throws ResourceInitializationException {
      final Object[] allParameters = getAllParameters( parameters );
      return AnalysisEngineFactory.createEngineDescription( classType, allParameters );
   }

   /**
    * This method should be avoided.  See the bottom of https://uima.apache.org/d/uimafit-current/api/index.html
    *
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Engine with specified parameters plus those loaded from properties that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngine createLoggedEngine( final Class<? extends AnalysisComponent> classType,
                                             final Object... parameters )
         throws ResourceInitializationException {
      final Object[] allParameters = getAllParameters( parameters );
      return StartFinishLogger.createLoggedEngine( classType, allParameters );
   }

   /**
    * @param classType  main component
    * @param parameters parameters for the main component
    * @return Description with specified parameters plus those loaded from properties that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   public AnalysisEngineDescription createLoggedDescription( final Class<? extends AnalysisComponent> classType,
                                                             final Object... parameters )
         throws ResourceInitializationException {
      final Object[] allParameters = getAllParameters( parameters );
      return StartFinishLogger.createLoggedDescription( classType, allParameters );
   }

   /**
    * @param mainDescription main component description
    * @return Description with specified parameters plus those loaded from properties that is wrapped with a simple Logger AE that logs the Start and Finish of the process
    * @throws ResourceInitializationException if UimaFit has a problem
    */
   static public AnalysisEngineDescription createLoggedDescription( final AnalysisEngineDescription mainDescription )
         throws ResourceInitializationException {
      return StartFinishLogger.createLoggedDescription( mainDescription );
   }

   /**
    * @param map        map to hold parameters.
    * @param parameters Any values already present in map will be overwritten by parameters with same key.
    */
   static private void addToMap( final Map<String, Object> map, final Object... parameters ) {
      for ( int i = 0; i < parameters.length; i += 2 ) {
         if ( parameters[ i ] instanceof String ) {
            map.put( (String) parameters[ i ], parameters[ i + 1 ] );
         } else {
            LOGGER.warn( "Parameter " + i + " not a String, using " + parameters[ i ].toString() );
            map.put( parameters[ i ].toString(), parameters[ i + 1 ] );
         }
      }
   }

}
