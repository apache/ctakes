package org.apache.ctakes.gui.pipeline.bit.parameter;


import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/20/2017
 */
@Immutable
final public class DefaultParameterHolder implements ParameterHolder {

   static private final Logger LOGGER = Logger.getLogger( "DefaultParameterHolder" );

   private final List<ConfigurationParameter> _parameters;
   private final Map<ConfigurationParameter, String> _typeMap;

   /**
    * @param pipeBitClass -
    */
   public DefaultParameterHolder( final Class<?> pipeBitClass ) {
      _typeMap = ParameterMapper.createParameterTypeMap( pipeBitClass );
      _parameters = new ArrayList<>( _typeMap.keySet() );
      _parameters.sort( ( p1, p2 ) -> p1.name().compareToIgnoreCase( p2.name() ) );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getParameterCount() {
      return _parameters.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ConfigurationParameter getParameter( final int index ) {
      return _parameters.get( index );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getParameterClass( final int index ) {
      return _typeMap.get( getParameter( index ) );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getParameterName( final int index ) {
      return _parameters.get( index ).name();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getParameterDescription( final int index ) {
      return _parameters.get( index ).description();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isParameterMandatory( final int index ) {
      return _parameters.get( index ).mandatory();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String[] getParameterValue( final int index ) {
      return _parameters.get( index ).defaultValue();
   }

}
