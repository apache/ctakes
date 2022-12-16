package org.apache.ctakes.gui.pipeline.bit.user;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterMapper;
import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/19/2017
 */
final public class DefaultUserBit implements UserBit {

   static private final Logger LOGGER = Logger.getLogger( "DefaultUserBit" );

   private final PipeBitInfo _pipeBitInfo;
   private final Class<?> _pipeBitClass;
   private String _name;
   private final List<ConfigurationParameter> _parameters;
   private final Map<ConfigurationParameter, String> _typeMap;
   private final Map<ConfigurationParameter, String[]> _parameterValues;

   public DefaultUserBit( final PipeBitInfo pipeBitInfo, final Class<?> pipeBitClass ) {
      _pipeBitInfo = pipeBitInfo;
      _pipeBitClass = pipeBitClass;
      _typeMap = ParameterMapper.createParameterTypeMap( pipeBitClass );
      _parameterValues = ParameterMapper.createParameterDefaultsMap( pipeBitClass );
      _parameters = new ArrayList<>( _typeMap.keySet() );
      _parameters.sort( ( p1, p2 ) -> p1.name().compareToIgnoreCase( p2.name() ) );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getBitName() {
      if ( _name == null || _name.isEmpty() ) {
         return _pipeBitInfo.name();
      }
      return _name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setBitName( final String name ) {
      _name = name;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PipeBitInfo getPipeBitInfo() {
      return _pipeBitInfo;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Class<?> getPipeBitClass() {
      return _pipeBitClass;
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
      return _parameterValues.get( getParameter( index ) );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setParameterValue( final int index, final String... values ) {
      _parameterValues.put( getParameter( index ), values );
   }

}
