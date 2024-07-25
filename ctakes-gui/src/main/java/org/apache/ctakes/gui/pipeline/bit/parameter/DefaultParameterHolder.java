package org.apache.ctakes.gui.pipeline.bit.parameter;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/20/2017
 */
@Immutable
final public class DefaultParameterHolder implements ParameterHolder {

   static private final Logger LOGGER = LogManager.getLogger( "DefaultParameterHolder" );

   private final List<ConfigurationParameter> _parameters;
   private final Map<ConfigurationParameter, String> _typeMap;

   /**
    * @param pipeBitClass -
    */
   public DefaultParameterHolder( final Class<?> pipeBitClass ) {
      _typeMap = ParameterMapper.createParameterTypeMap( pipeBitClass );
      _parameters = new ArrayList<>( _typeMap.keySet() );
      _parameters.sort( new ParamComparator() );
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

   static private final class ParamComparator implements Comparator<ConfigurationParameter>  {
      @Override
      public int compare( final ConfigurationParameter param1, final ConfigurationParameter param2 ) {
         if ( param1.mandatory() == param2.mandatory() ) {
            return String.CASE_INSENSITIVE_ORDER.compare( param1.name(), param2.name() );
         }
         return param1.mandatory() ? -1 : 1;
      }
   }


}
