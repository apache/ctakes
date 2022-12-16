package org.apache.ctakes.fhir.resource;

import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.apache.ctakes.fhir.element.FhirElementFactory.CODING_TYPE_SYSTEM;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
final public class AnnotationParser implements FhirBasicParser<Annotation> {

   static private final Logger LOGGER = Logger.getLogger( "AnnotationBasicParser" );

   public Annotation parseResource( final JCas jCas, final Basic resource ) {
      String className = "";
      final CodeableConcept codeableConcept = resource.getCode();
      for ( Coding coding : codeableConcept.getCoding() ) {
         final String system = coding.getSystem();
         if ( system.equals( CODING_TYPE_SYSTEM ) ) {
            className = coding.getCode();
         }
      }
      final Annotation annotation = createAnnotation( jCas, className );
      addTextSpan( annotation, resource, LOGGER );
      return annotation;
   }


   static private Annotation createAnnotation( final JCas jCas, final String className ) {
      try {
         final Class<?> clazz = Class.forName( className );
         if ( Annotation.class.isAssignableFrom( clazz ) ) {
            final Constructor<?> constructor = clazz.getConstructor( JCas.class );
            return (Annotation) constructor.newInstance( jCas );
         } else {
            LOGGER.error( "Cannot create a ctakes annotation for class " + className );
         }
      } catch ( ClassNotFoundException | NoSuchMethodException
            | InstantiationException
            | IllegalAccessException
            | InvocationTargetException multE ) {
         LOGGER.error( "Cannot determine ctakes annotation type for class " + className );
      }
      return new Annotation( jCas );
   }

}
