package org.apache.ctakes.gui.pipeline.bit.parameter;


import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/19/2017
 */
final public class ParameterMapper {

   static private final Logger LOGGER = Logger.getLogger( "ParameterMapper" );

   private ParameterMapper() {
   }

   /**
    * @param pipeBitClass -
    * @return Configuration Parameters and Field class types for the Pipe Bit and all of its parent classes
    */
   static public Map<ConfigurationParameter, String> createParameterTypeMap( final Class<?> pipeBitClass ) {
      final Map<ConfigurationParameter, String> parameterMap = new HashMap<>();
      final Collection<Class<?>> inheritables = new ArrayList<>();
      final Class<?>[] interfaces = pipeBitClass.getInterfaces();
      if ( interfaces != null && interfaces.length > 0 ) {
         inheritables.addAll( Arrays.asList( interfaces ) );
      }
      if ( pipeBitClass.getSuperclass() != null ) {
         inheritables.add( pipeBitClass.getSuperclass() );
      }
      inheritables.stream().map( ParameterMapper::createParameterTypeMap ).forEach( parameterMap::putAll );
      final Field[] fields = pipeBitClass.getDeclaredFields();
      Arrays.stream( fields )
            .filter( f -> f.getAnnotation( ConfigurationParameter.class ) != null )
            .forEach( f -> parameterMap
                  .put( f.getAnnotation( ConfigurationParameter.class ), f.getType().getSimpleName() ) );
      return parameterMap;
   }


   /**
    * @param pipeBitClass -
    * @return Configuration Parameters and default values for the Pipe Bit and all of its parent classes
    */
   static public Map<ConfigurationParameter, String[]> createParameterDefaultsMap( final Class<?> pipeBitClass ) {
      final Map<ConfigurationParameter, String[]> parameterMap = new HashMap<>();
      final Collection<Class<?>> inheritables = new ArrayList<>();
      final Class<?>[] interfaces = pipeBitClass.getInterfaces();
      if ( interfaces != null && interfaces.length > 0 ) {
         inheritables.addAll( Arrays.asList( interfaces ) );
      }
      if ( pipeBitClass.getSuperclass() != null ) {
         inheritables.add( pipeBitClass.getSuperclass() );
      }
      inheritables.stream().map( ParameterMapper::createParameterDefaultsMap ).forEach( parameterMap::putAll );
      final Field[] fields = pipeBitClass.getDeclaredFields();
      Arrays.stream( fields )
            .map( f -> f.getAnnotation( ConfigurationParameter.class ) )
            .filter( Objects::nonNull )
            .forEach( cp -> parameterMap.put( cp, cp.defaultValue() ) );
      return parameterMap;
   }

}
