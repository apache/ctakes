package org.apache.ctakes.core.util;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.SofaID;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.Session;
import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.Settings;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * While unconventional, it is occasionally useful to easily add or change configuration parameters in a uima context.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/5/2018
 */
final public class MutableUimaContext implements UimaContext {

   static private final String DEFAULT_GROUP = "DEFAULT_GROUP";

   private final UimaContext _delegate;

   private final Map<String, Map<String, Object>> _mutableConfigParameters;

   /**
    * @param uimaContext a uimacontext upon which to build.
    */
   public MutableUimaContext( final UimaContext uimaContext ) {
      _delegate = uimaContext;
      _mutableConfigParameters = new HashMap<>();
      _mutableConfigParameters.put( DEFAULT_GROUP, new HashMap<>() );
   }

   /**
    * Add a configuration parameter to the default parameter group.
    *
    * @param name  name of parameter
    * @param value value of parameter
    */
   public void setConfigParameter( final String name, final Object value ) {
      setConfigParameter( DEFAULT_GROUP, name, value );
   }

   /**
    * Add a configuration parameter to the given parameter group.
    *
    * @param groupName name of the parameter group
    * @param name      name of parameter
    * @param value     value of parameter
    */
   public void setConfigParameter( final String groupName, final String name, final Object value ) {
      _mutableConfigParameters.computeIfAbsent( groupName, m -> new HashMap<>() ).put( name, value );
   }

   private Object getMutableParameterValue( final String groupName, final String name ) {
      final Map<String, Object> map = _mutableConfigParameters.get( groupName );
      if ( map != null ) {
         return map.get( name );
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getConfigParameterValue( final String name ) {
      final Object mutable = getMutableParameterValue( DEFAULT_GROUP, name );
      if ( mutable != null ) {
         return mutable;
      }
      return _delegate.getConfigParameterValue( name );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getConfigParameterValue( final String groupName, final String name ) {
      final Object mutable = getMutableParameterValue( groupName, name );
      if ( mutable != null ) {
         return mutable;
      }
      return _delegate.getConfigParameterValue( groupName, name );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String[] getConfigurationGroupNames() {
      final Collection<String> groupNames = Arrays.asList( _delegate.getConfigParameterNames() );
      _mutableConfigParameters.keySet().stream().filter( g -> !groupNames.contains( g ) ).forEach( groupNames::add );
      return groupNames.toArray( new String[ groupNames.size() ] );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String[] getConfigParameterNames( final String groupName ) {
      final Collection<String> names = Arrays.asList( _delegate.getConfigParameterNames( groupName ) );
      final Map<String, Object> map = _mutableConfigParameters.get( groupName );
      if ( map != null ) {
         map.keySet().stream().filter( g -> !names.contains( g ) ).forEach( names::add );
      }
      return names.toArray( new String[ names.size() ] );

   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String[] getConfigParameterNames() {
      return Arrays.stream( getConfigurationGroupNames() )
                   .map( this::getConfigParameterNames )
                   .flatMap( Arrays::stream )
                   .toArray( String[]::new );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getSharedSettingValue( final String name ) throws ResourceConfigurationException {
      return _delegate.getSharedSettingValue( name );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String[] getSharedSettingArray( final String name ) throws ResourceConfigurationException {
      return _delegate.getSharedSettingArray( name );

   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String[] getSharedSettingNames() {
      return _delegate.getSharedSettingNames();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Settings getExternalOverrides() {
      return _delegate.getExternalOverrides();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public org.apache.uima.util.Logger getLogger() {
      return _delegate.getLogger();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InstrumentationFacility getInstrumentationFacility() {
      return _delegate.getInstrumentationFacility();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public URL getResourceURL( final String name ) throws ResourceAccessException {
      return _delegate.getResourceURL( name );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public URI getResourceURI( final String name ) throws ResourceAccessException {
      return _delegate.getResourceURI( name );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getResourceFilePath( final String name ) throws ResourceAccessException {
      return _delegate.getResourceFilePath( name );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream getResourceAsStream( final String name ) throws ResourceAccessException {
      return _delegate.getResourceAsStream( name );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getResourceObject( final String name ) throws ResourceAccessException {
      return _delegate.getResourceObject( name );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public URL getResourceURL( final String name, String[] params ) throws ResourceAccessException {
      return _delegate.getResourceURL( name, params );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public URI getResourceURI( final String name, String[] params ) throws ResourceAccessException {
      return _delegate.getResourceURI( name, params );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getResourceFilePath( final String name, String[] params ) throws ResourceAccessException {
      return _delegate.getResourceFilePath( name, params );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream getResourceAsStream( final String name, String[] params ) throws ResourceAccessException {
      return _delegate.getResourceAsStream( name, params );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getResourceObject( final String name, String[] params ) throws ResourceAccessException {
      return _delegate.getResourceObject( name, params );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getDataPath() {
      return _delegate.getDataPath();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Session getSession() {
      return _delegate.getSession();
   }

   /**
    * {@inheritDoc}
    *
    * @deprecated
    */
   @Override
   @Deprecated
   public SofaID mapToSofaID( final String var1 ) {
      return _delegate.mapToSofaID( var1 );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String mapSofaIDToComponentSofaName( final String var1 ) {
      return _delegate.mapSofaIDToComponentSofaName( var1 );
   }

   /**
    * {@inheritDoc}
    *
    * @deprecated
    */
   @Override
   @Deprecated
   public SofaID[] getSofaMappings() {
      return _delegate.getSofaMappings();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public <T extends AbstractCas> T getEmptyCas( final Class<T> var1 ) {
      return _delegate.getEmptyCas( var1 );
   }

}

