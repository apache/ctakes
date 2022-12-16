package org.apache.ctakes.fhir.element;


import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.ctakes.fhir.element.FhirElementFactory.*;

/**
 * Utility to assist with the creation of ctakes objects from fhir objects.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
final public class FhirElementParser {

   static private final Logger LOGGER = Logger.getLogger( "FhirElementParser" );

   private FhirElementParser() {
   }

   static public String getIdName( final String id ) {
      final String[] splits = StringUtil.fastSplit( id, DIVIDER_CHAR );
      if ( splits.length >= 3 ) {
         return splits[ 1 ];
      }
      return id;
   }

   // TODO handle subjects
   static public String getSubjectId( final Basic basic ) {
      final Reference reference = basic.getSubject();
      if ( reference == null ) {
         return "";
      }
      return reference.getResource()
            .getIdElement()
            .getValue();
   }

   static public Pair<Integer> getTextSpan( final Collection<Extension> extensions ) {
      int begin = Integer.MAX_VALUE;
      int end = Integer.MIN_VALUE;
      for ( Extension extension : extensions ) {
         final String url = extension.getUrl();
         if ( isCtakesFhirExt( url, SPAN_BEGIN_EXT ) ) {
            begin = parseInteger( extension );
            if ( end != Integer.MIN_VALUE ) {
               break;
            }
         } else if ( isCtakesFhirExt( url, SPAN_END_EXT ) ) {
            end = parseInteger( extension );
            if ( begin != Integer.MAX_VALUE ) {
               break;
            }
         }
      }
      if ( end < begin ) {
         LOGGER.error( "Could not parse text span." );
         return null;
      }
      return new Pair<>( begin, end );
   }

   static public int getTextSpanBegin( final DomainResource resource ) {
      final String url = createCtakesFhirUrl( SPAN_BEGIN_EXT );
      return resource.getExtensionsByUrl( url )
            .stream()
            .mapToInt( FhirElementParser::parseInteger )
            .min()
            .orElse( Integer.MAX_VALUE );
   }

   static public int getTextSpanEnd( final DomainResource resource ) {
      final String url = createCtakesFhirUrl( SPAN_END_EXT );
      return resource.getExtensionsByUrl( url )
            .stream()
            .mapToInt( FhirElementParser::parseInteger )
            .max()
            .orElse( Integer.MIN_VALUE );
   }

   static public Collection<UmlsConcept> getUmlsConceptArray( final JCas jCas,
                                                              final CodeableConcept codeableConcept ) {
      final List<Coding> codings = codeableConcept.getCoding();
      String cui = "";
      String tui = "";
      final Collection<UmlsConcept> umlsConcepts = new ArrayList<>();
      for ( Coding coding : codings ) {
         final String codeType = coding.getSystem();
         switch ( codeType ) {
            case CODING_CUI:
               cui = coding.getCode();
               continue;
            case CODING_TUI:
               tui = coding.getCode();
         }
      }
      for ( Coding coding : codings ) {
         final String codingSystem = coding.getSystem();
         if ( codingSystem.equals( CODING_CUI )
               || codingSystem.equals( CODING_TUI )
               || codingSystem.equals( CODING_TYPE_SYSTEM )
               || codingSystem.equals( CODING_SEMANTIC ) ) {
            continue;
         }
         final String code = coding.getCode();
         final String preferredText = coding.getDisplay();
         final UmlsConcept umlsConcept = new UmlsConcept( jCas );
         umlsConcept.setCodingScheme( codingSystem );
         umlsConcept.setCui( cui );
         if ( !tui.isEmpty() ) {
            umlsConcept.setTui( tui );
         }
         umlsConcept.setCode( code );
         if ( preferredText != null && !preferredText.isEmpty() ) {
            umlsConcept.setPreferredText( preferredText );
         }
         umlsConcepts.add( umlsConcept );
      }
      return umlsConcepts;
   }

   static public String getCode( final CodeableConcept codeableConcept, final String schema ) {
      return codeableConcept.getCoding()
            .stream()
            .filter( c -> c.getSystem()
                  .equals( schema ) )
            .findFirst()
            .map( Coding::getCode )
            .orElse( "" );
   }

   static public List<String> getExtensionValues( final String url, final List<? extends IBaseExtension> extensions ) {
      return extensions.stream()
            .filter( e -> e.getUrl()
                  .equals( url ) )
            .map( IBaseExtension::getValue )
            .filter( StringType.class::isInstance )
            .map( e -> ((StringType) e).getValue() )
            .collect( Collectors.toList() );
   }

   static public boolean hasExtension( final String extension, final List<Extension> extensions ) {
      final String url = createCtakesFhirUrl( extension );
      return extensions.stream()
            .anyMatch( e -> e.getUrl()
                  .equals( url ) );
   }

   static public String parseString( final Extension extension ) {
      IPrimitiveType primitiveType = extension.getValueAsPrimitive();
      final Object value = primitiveType.getValue();
      if ( value instanceof String ) {
         return (String) value;
      }
      LOGGER.error( "Could not parse String for " + extension.getUrl() + " from " + value );
      return value.toString();
   }

   static public int parseInteger( final Extension extension ) {
      IPrimitiveType primitiveType = extension.getValueAsPrimitive();
      final Object value = primitiveType.getValue();
      if ( value instanceof Integer ) {
         return (int) value;
      }
      LOGGER.error( "Could not parse integer for " + extension.getUrl() + " from " + value );
      return -1;
   }


}
