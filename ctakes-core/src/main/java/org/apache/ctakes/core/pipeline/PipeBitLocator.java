package org.apache.ctakes.core.pipeline;


import org.apache.log4j.Logger;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.ResourceInitializationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Utility methods to find annotation engines and collection readers without and package specified.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/18/2017
 */
@SuppressWarnings( "unchecked" )
public enum PipeBitLocator {
   INSTANCE;

   static public PipeBitLocator getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "PipeBitFinder" );
   static private final Object[] EMPTY_OBJECT_ARRAY = new Object[ 0 ];
   static private final String[] CTAKES_PACKAGES
         = { "core",
             "context.tokenizer",
             "postagger",
             "chunker",
             "dictionary.lookup2",
             "dictionary.lookup.fast",
             "dictionary.lookup.cased",
             "dictionary.cased",
             "assertion",
             "assertion.medfacts.cleartk",
             "clinicalpipeline",
             "clinical.pipeline",
             "constituency.parser",
             "relationextractor",
             "relation.extractor",
             "coreference",
             "dependency.parser",
             "temporal",
             "pbj",
             "drug-ner",
             "necontexts",
             "ne.contexts",
             "preprocessor",
             "sideeffect",
             "smokingstatus",
             "smoking.status",
             "dictionary.lookup",
             "template.filler",
             "lvg",
             "examples" };


   private final Collection<String> _userPackages = new HashSet<>();

   public Collection<String> getCtakesPackages() {
      return Arrays.stream( CTAKES_PACKAGES )
            .map( p -> "org.apache.ctakes." + p )
            .collect( Collectors.toList() );
   }

   /**
    * @return user package or directory
    */
   public Collection<String> getUserPackages() {
      return Collections.unmodifiableCollection( _userPackages );
   }

   /**
    * Add some user package or directory to the known path
    *
    * @param packagePath user package or directory
    */
   public void addUserPackage( final String packagePath ) {
      _userPackages.add( packagePath );
   }

   /**
    * @param className fully-specified or simple name of an ae or cc component class
    * @return discovered class for ae or cc
    * @throws ResourceInitializationException if the class could not be found
    */
   public Class<? extends AnalysisComponent> getComponentClass( final String className ) throws
         ResourceInitializationException {
      Class componentClass;
      try {
         componentClass = Class.forName( className );
      } catch ( ClassNotFoundException cnfE ) {
         componentClass = getPackagedComponent( className );
      }
      if ( componentClass == null ) {
         throw new ResourceInitializationException(
               "No Analysis Component found for " + className, EMPTY_OBJECT_ARRAY );
      }
      assertClassType( componentClass, AnalysisComponent.class );
      return componentClass;
   }

   /**
    * @param className fully-specified or simple name of an ae or cc component class
    * @return discovered class for ae or cc
    */
   private Class<? extends AnalysisComponent> getPackagedComponent( final String className ) {
      Class componentClass;
      for ( String packageName : _userPackages ) {
         componentClass = getPackagedClass( packageName, className, AnalysisComponent.class );
         if ( componentClass != null ) {
            return componentClass;
         }
      }
      for ( String packageName : getCtakesPackages() ) {
         componentClass = getPackagedClass( packageName + ".ae", className, AnalysisComponent.class );
         if ( componentClass != null ) {
            return componentClass;
         }
         componentClass = getPackagedClass( packageName + ".cc", className, AnalysisComponent.class );
         if ( componentClass != null ) {
            return componentClass;
         }
         componentClass = getPackagedClass( packageName, className, AnalysisComponent.class );
         if ( componentClass != null ) {
            return componentClass;
         }
      }
      return null;
   }

   /**
    * @param className fully-specified or simple name of a cr Collection Reader class
    * @return a class for the reader
    * @throws ResourceInitializationException if the class could not be found or instantiated
    */
   public Class<? extends CollectionReader> getReaderClass( final String className )
         throws ResourceInitializationException {
      Class readerClass;
      try {
         readerClass = Class.forName( className );
      } catch ( ClassNotFoundException cnfE ) {
         readerClass = getPackagedReader( className );
      }
      if ( readerClass == null ) {
         throw new ResourceInitializationException( "No Collection Reader found for " + className, EMPTY_OBJECT_ARRAY );
      }
      assertClassType( readerClass, CollectionReader.class );
      return readerClass;
   }

   /**
    * @param className simple name of a cr Collection Reader class
    * @return discovered class for a cr
    */
   private Class<? extends CollectionReader> getPackagedReader( final String className ) {
      Class readerClass;
      for ( String packageName : _userPackages ) {
         readerClass = getPackagedClass( packageName, className, CollectionReader.class );
         if ( readerClass != null ) {
            return readerClass;
         }
      }
      for ( String packageName : getCtakesPackages() ) {
         readerClass = getPackagedClass( packageName + ".cr", className, CollectionReader.class );
         if ( readerClass != null ) {
            return readerClass;
         }
         readerClass = getPackagedClass( packageName, className, CollectionReader.class );
         if ( readerClass != null ) {
            return readerClass;
         }
      }
      return null;
   }

   /**
    * @param packageName     possible package for class
    * @param className       simple name for class
    * @param wantedClassType desired superclass type
    * @return discovered class or null if no proper class was discovered
    */
   static private Class<?> getPackagedClass( final String packageName, final String className,
                                             final Class<?> wantedClassType ) {
      try {
         Class<?> classType = Class.forName( packageName + "." + className );
         if ( isClassType( classType, wantedClassType ) ) {
            return classType;
         }
      } catch ( ClassNotFoundException cnfE ) {
         // do nothing
      }
      return null;
   }

   /**
    * This requires that the component class has a static createAnnotatorDescription method with no parameters
    *
    * @param className component class for which a descriptor should be created
    * @param values    optional parameter values for the descriptor creator
    * @return a description generated for the component
    * @throws ResourceInitializationException if anything went wrong with finding the class or the method,
    *                                         or invoking the method to get an AnalysisEngineDescription
    */
   public AnalysisEngineDescription createDescription( final String className, final Object... values )
         throws ResourceInitializationException {
      final Class<? extends AnalysisComponent> componentClass = getComponentClass( className );
      Method method;
      try {
         if ( values.length == 0 ) {
            method = componentClass.getMethod( "createAnnotatorDescription" );
         } else {
            method = componentClass.getMethod( "createAnnotatorDescription", getValueTypes( values ) );
         }
      } catch ( NoSuchMethodException nsmE ) {
         try {
            if ( values.length == 0 ) {
               method = componentClass.getMethod( "createEngineDescription" );
            } else {
               method = componentClass.getMethod( "createEngineDescription", getValueTypes( values ) );
            }
         } catch ( NoSuchMethodException nsmE2 ) {
            LOGGER.error( "No createAnnotatorDescription or createEngineDescription method in " + className );
            throw new ResourceInitializationException( nsmE2 );
         }
      }
      try {
         final Object invocation = method.invoke( null, values );
         if ( !AnalysisEngineDescription.class.isInstance( invocation ) ) {
            LOGGER.error( method.getName() + " in " + className + " returned an "
                  + invocation.getClass().getName() + " not an AnalysisEngineDescription" );
            throw new ResourceInitializationException();
         }
         return (AnalysisEngineDescription) invocation;
      } catch ( IllegalAccessException | InvocationTargetException multE ) {
         LOGGER.error( "Could not invoke " + method.getName() + " on " + className );
         throw new ResourceInitializationException( multE );
      }
   }

   /**
    * The java reflection getMethod does not handle autoboxing/unboxing.
    * So, we assume that Integer and Boolean parameter values will actually be primitives.
    *
    * @param values parameter value objects
    * @return parameter value class types, unboxing to primitives where needed
    */
   static private Class<?>[] getValueTypes( final Object... values ) {
      final Class<?>[] classArray = new Class[ values.length ];
      for ( int i = 0; i < values.length; i++ ) {
         final Class<?> type = values[ i ].getClass();
         if ( type.equals( Integer.class ) ) {
            classArray[ i ] = int.class;
         } else if ( type.equals( Boolean.class ) ) {
            classArray[ i ] = boolean.class;
         } else {
            classArray[ i ] = type;
         }
      }
      return classArray;
   }

   /**
    * @param classType       class type to test
    * @param wantedClassType wanted class type
    * @throws ResourceInitializationException if the class type does not extend the wanted class type
    */
   static private void assertClassType( final Class<?> classType, final Class<?> wantedClassType )
         throws ResourceInitializationException {
      if ( !isClassType( classType, wantedClassType ) ) {
         throw new ResourceInitializationException(
               "Not " + wantedClassType.getSimpleName() + " " + classType.getName(), EMPTY_OBJECT_ARRAY );
      }
   }

   /**
    * @param classType       class type to test
    * @param wantedClassType wanted class type
    * @return true if the class type extends the wanted class type
    */
   static private boolean isClassType( final Class<?> classType, final Class<?> wantedClassType ) {
      return wantedClassType.isAssignableFrom( classType );
   }


}
