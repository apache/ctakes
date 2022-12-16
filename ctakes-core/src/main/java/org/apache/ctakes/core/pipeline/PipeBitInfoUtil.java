package org.apache.ctakes.core.pipeline;


import org.apache.log4j.Logger;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/22/2016
 */
@Immutable
final public class PipeBitInfoUtil {

   static private final Logger LOGGER = Logger.getLogger( "PipeBitInfoUtil" );

   static private final String UNKNOWN_PIPE_BIT = "! Unfamiliar";

   private PipeBitInfoUtil() {
   }

   static public String getName( final Class<?> pipeBitClass ) {
      return getInfo( pipeBitClass ).name();
   }


   /**
    * @param pipeBitClass -
    * @return If the class doesn't have explicitly declared PipeBitInfo then one is generated automatically.
    */
   static public PipeBitInfo getInfo( final Class<?> pipeBitClass ) {
      final PipeBitInfo info = pipeBitClass.getAnnotation( PipeBitInfo.class );
      if ( info == null ) {
         return new AutoPipeBitInfo( pipeBitClass );
      }
      return info;
   }

   /**
    * @param pipeBitClass -
    * @return all infos for the class and its parent classes.
    * If the first class doesn't have explicitly declared PipeBitInfo then one is generated automatically.
    */
   static public Collection<PipeBitInfo> getAllInfos( final Class<?> pipeBitClass ) {
      final List<PipeBitInfo> infos = getSuperInfos( pipeBitClass.getSuperclass() );
      infos.add( 0, getInfo( pipeBitClass ) );
      return infos;
   }

   /**
    * @param pipeBitClass -
    * @return info for the given class and all of its parent classes
    */
   static private List<PipeBitInfo> getSuperInfos( final Class<?> pipeBitClass ) {
      if ( pipeBitClass == null
           || pipeBitClass.equals( CollectionReader_ImplBase.class )
           || pipeBitClass.equals( JCasAnnotator_ImplBase.class )
           || pipeBitClass.equals( CasConsumer_ImplBase.class ) ) {
         return Collections.emptyList();
      }
      final List<PipeBitInfo> infos = new ArrayList<>();
      final PipeBitInfo info = pipeBitClass.getAnnotation( PipeBitInfo.class );
      if ( info != null ) {
         infos.add( info );
      }
      infos.addAll( getSuperInfos( pipeBitClass.getSuperclass() ) );
      return infos;
   }

   /**
    * @param pipeBitClass -
    * @return annotated parameters for the class and all of its parent classes
    */
   static public Collection<ConfigurationParameter> getAllParameters( final Class<?> pipeBitClass ) {
      if ( pipeBitClass == null
           || pipeBitClass.equals( CollectionReader_ImplBase.class )
           || pipeBitClass.equals( JCasAnnotator_ImplBase.class )
           || pipeBitClass.equals( CasConsumer_ImplBase.class ) ) {
         return Collections.emptyList();
      }
      final Field[] fields = pipeBitClass.getDeclaredFields();
      final Collection<ConfigurationParameter> parameters = Arrays.stream( fields )
            .map( f -> f.getAnnotation( ConfigurationParameter.class ) )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
      parameters.addAll( getAllParameters( pipeBitClass.getSuperclass() ) );
      return parameters;
   }

   static public boolean isUnknown( final PipeBitInfo info ) {
      return info.description().startsWith( UNKNOWN_PIPE_BIT );
   }

   /**
    * @param pipeBitClass -
    * @return annotated parameters for the class and all of its parent classes
    */
   static private Collection<String> getParameterDescriptions( final Class<?> pipeBitClass ) {
      return getAllParameters( pipeBitClass ).stream()
            .map( p -> p.name() + " : " + p.description() )
            .collect( Collectors.toList() );
   }


   /**
    * A PipeBitInfo built from information available via reflection
    */
   @Immutable
   static private final class AutoPipeBitInfo implements PipeBitInfo {
      private final Class<?> _pipeBitClass;

      private AutoPipeBitInfo( final Class<?> pipeBitClass ) {
         _pipeBitClass = pipeBitClass;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String name() {
         return _pipeBitClass.getSimpleName();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String description() {
         String purpose = "Has an unknown purpose.";
         if ( CollectionReader_ImplBase.class.isAssignableFrom( _pipeBitClass ) ) {
            purpose = "Reads or creates Text and provides it to a pipeline.";
         } else if ( JCasAnnotator_ImplBase.class.isAssignableFrom( _pipeBitClass ) ) {
            purpose = "Performs some Annotation task within a pipeline.";
         } else if ( CasConsumer_ImplBase.class.isAssignableFrom( _pipeBitClass ) ) {
            purpose = "Consumes Annotations and performs some task at the end of a pipeline.";
         }
         return UNKNOWN_PIPE_BIT + " " + name() + " : " + purpose + "  Use with care.";
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Role role() {
         if ( CollectionReader_ImplBase.class.isAssignableFrom( _pipeBitClass ) ) {
            return Role.READER;
         } else if ( CasConsumer_ImplBase.class.isAssignableFrom( _pipeBitClass ) ) {
            return Role.WRITER;
         }
         return Role.ANNOTATOR;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public String[] parameters() {
         final Collection<String> parameters = getParameterDescriptions( _pipeBitClass );
         return parameters.toArray( new String[ parameters.size() ] );
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public TypeProduct[] dependencies() {
         return NO_TYPE_PRODUCTS;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public TypeProduct[] usables() {
         return NO_TYPE_PRODUCTS;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public TypeProduct[] products() {
         return NO_TYPE_PRODUCTS;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
         return PipeBitInfo.class;
      }
   }

}
