package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.apache.ctakes.fhir.element.FhirElementFactory.CODING_PART_OF_SPEECH;
import static org.apache.ctakes.fhir.element.FhirElementFactory.CODING_TYPE_SYSTEM;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
final public class BaseTokenParser implements FhirBasicParser<BaseToken> {

   static private final Logger LOGGER = Logger.getLogger( "BaseTokenParser" );

   public BaseToken parseResource( final JCas jCas, final Basic resource ) {
      String className = "";
      String pos = "";
      final CodeableConcept codeableConcept = resource.getCode();
      for ( Coding coding : codeableConcept.getCoding() ) {
         final String system = coding.getSystem();
         if ( system.equals( CODING_TYPE_SYSTEM ) ) {
            className = coding.getCode();
         } else if ( system.equals( CODING_PART_OF_SPEECH ) ) {
            pos = coding.getCode();
         }
      }
      final BaseToken baseToken = createBaseToken( jCas, className );
      baseToken.setPartOfSpeech( pos );
      addTextSpan( baseToken, resource, LOGGER );
      return baseToken;
   }


   static private BaseToken createBaseToken( final JCas jCas, final String className ) {
      try {
         final Class<?> clazz = Class.forName( className );
         if ( BaseToken.class.isAssignableFrom( clazz ) ) {
            final Constructor<?> constructor = clazz.getConstructor( JCas.class );
            return (BaseToken)constructor.newInstance( jCas );
         } else {
            LOGGER.error( "Cannot create a ctakes base token for class " + className );
         }
      } catch ( ClassNotFoundException | NoSuchMethodException
            | InstantiationException
            | IllegalAccessException
            | InvocationTargetException multE ) {
         LOGGER.error( "Cannot determine ctakes base token type for class " + className );
      }
      return new BaseToken( jCas );
   }

}
