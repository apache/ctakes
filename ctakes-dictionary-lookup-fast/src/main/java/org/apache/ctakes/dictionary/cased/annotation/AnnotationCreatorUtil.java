package org.apache.ctakes.dictionary.cased.annotation;

import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.dictionary.cased.encoder.CodeSchema;
import org.apache.ctakes.dictionary.cased.encoder.TermEncoding;
import org.apache.ctakes.dictionary.cased.lookup.DiscoveredTerm;
import org.apache.ctakes.dictionary.cased.util.textspan.ContiguousTextSpan;
import org.apache.ctakes.dictionary.cased.util.textspan.MagicTextSpan;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.dictionary.lookup2.util.TuiCodeUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/26/2020
 */
@Immutable
final public class AnnotationCreatorUtil {

   static private final Logger LOGGER = Logger.getLogger( "AnnotationCreatorUtil" );

   static private final TermEncoding NULL_CODE = new TermEncoding( "", "" );

   private AnnotationCreatorUtil() {
   }


   static public Map<DiscoveredTerm, Collection<MagicTextSpan>> mapTermSpans(
         final Map<Pair<Integer>, Collection<DiscoveredTerm>> allDiscoveredTermsMap ) {
      final Map<DiscoveredTerm, Collection<MagicTextSpan>> termSpanMap = new HashMap<>();
      for ( Map.Entry<Pair<Integer>, Collection<DiscoveredTerm>> spanTerms : allDiscoveredTermsMap.entrySet() ) {
         final MagicTextSpan textSpan = new ContiguousTextSpan( spanTerms.getKey() );
         spanTerms.getValue().forEach( t -> termSpanMap.computeIfAbsent( t, s -> new HashSet<>() ).add( textSpan ) );
      }
      return termSpanMap;
   }


   static public void createAnnotations( final JCas jcas,
                                         final Pair<Integer> textSpan,
                                         final Collection<DiscoveredTerm> discoveredTerms,
                                         final Map<DiscoveredTerm, Collection<TermEncoding>> termEncodingMap,
                                         final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      discoveredTerms.forEach( t
            -> createAnnotation( jcas, textSpan, t, termEncodingMap.get( t ), reassignSemantics ) );
   }

   static private void createAnnotation( final JCas jcas,
                                         final Pair<Integer> textSpan,
                                         final DiscoveredTerm discoveredTerm,
                                         final Collection<TermEncoding> termEncodings,
                                         final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final SemanticGroup bestGroup
            = SemanticGroup.getBestGroup( getSemanticGroups( termEncodings, reassignSemantics ) );
      final IdentifiedAnnotation annotation = bestGroup
            .getCreator()
            .apply( jcas );
      annotation.setTypeID( bestGroup.getCode() );
      annotation.setBegin( textSpan.getValue1() );
      annotation.setEnd( textSpan.getValue2() );
      annotation.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );

      final String cui = CuiCodeUtil.getInstance().getAsCui( discoveredTerm.getCuiCode() );
      Collection<String> tuis = getTuis( termEncodings );
      if ( tuis.isEmpty() ) {
         tuis = Collections.singletonList( SemanticTui.UNKNOWN.name() );
      }
      final String prefText = getPreferredText( termEncodings );

      final Collection<UmlsConcept> umlsConcepts = new HashSet<>();
      for ( String tui : tuis ) {
         termEncodings.stream()
                      .filter( isPrefTextEncoding.negate() )
                      .filter( isTuiEncoding.negate() )
                      .map( e -> createUmlsConcept( jcas, cui, tui, prefText, e ) )
                      .forEach( umlsConcepts::add );
      }
      if ( umlsConcepts.isEmpty() ) {
         // There were no special (e.g. snomedCT) encodings.
         tuis.forEach( t -> umlsConcepts.add( createUmlsConcept( jcas, cui, t, prefText ) ) );
      }
      final FSArray conceptArr = new FSArray( jcas, umlsConcepts.size() );
      int arrIdx = 0;
      for ( UmlsConcept umlsConcept : umlsConcepts ) {
         conceptArr.set( arrIdx, umlsConcept );
         arrIdx++;
      }
      annotation.setOntologyConceptArr( conceptArr );
      annotation.addToIndexes();
   }


   static private String getPreferredText( final Collection<TermEncoding> termEncodings ) {
      return termEncodings.stream()
                          .filter( CodeSchema.PREFERRED_TEXT::isSchema )
                          .map( TermEncoding::getSchemaCode )
                          .map( Object::toString )
                          .distinct()
                          .collect( Collectors.joining( ";" ) );
   }

   static private final Predicate<TermEncoding> isPrefTextEncoding
         = CodeSchema.PREFERRED_TEXT::isSchema;


   static private String getTui( final Collection<TermEncoding> termEncodings ) {
      return termEncodings.stream()
                          .filter( CodeSchema.TUI::isSchema )
                          .map( TermEncoding::getSchemaCode )
                          .map( AnnotationCreatorUtil::parseTuiValue )
                          .map( TuiCodeUtil::getAsTui )
                          .distinct()
                          .collect( Collectors.joining( ";" ) );
   }

   static private Collection<String> getTuis( final Collection<TermEncoding> termEncodings ) {
      return termEncodings.stream()
                          .filter( CodeSchema.TUI::isSchema )
                          .map( TermEncoding::getSchemaCode )
                          .map( AnnotationCreatorUtil::parseTuiValue )
                          .map( TuiCodeUtil::getAsTui )
                          .collect( Collectors.toSet() );
   }

   static private final Predicate<TermEncoding> isTuiEncoding = CodeSchema.TUI::isSchema;

   static private UmlsConcept createUmlsConcept( final JCas jcas,
                                                 final String cui,
                                                 final String tui,
                                                 final String preferredText ) {
      return createUmlsConcept( jcas, cui, tui, preferredText, NULL_CODE );
   }

   static private UmlsConcept createUmlsConcept( final JCas jcas,
                                                 final String cui,
                                                 final String tui,
                                                 final String preferredText,
                                                 final TermEncoding termEncoding ) {
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCui( cui );
      if ( tui != null ) {
         umlsConcept.setTui( tui );
      }
      if ( preferredText != null && !preferredText.isEmpty() ) {
         umlsConcept.setPreferredText( preferredText );
      }
      umlsConcept.setCodingScheme( termEncoding.getSchema() );
      umlsConcept.setCode( termEncoding.getSchemaCode()
                                       .toString() );
      return umlsConcept;
   }


   static public Map<SemanticGroup, Collection<DiscoveredTerm>> mapSemanticTerms(
         final Map<DiscoveredTerm, Collection<TermEncoding>> termEncodingMap,
         final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final Map<SemanticGroup, Collection<DiscoveredTerm>> semanticTermMap = new EnumMap<>( SemanticGroup.class );
      for ( Map.Entry<DiscoveredTerm, Collection<TermEncoding>> discoveredEncodings : termEncodingMap.entrySet() ) {
         getSemanticGroups( discoveredEncodings.getValue(), reassignSemantics )
               .forEach( g -> semanticTermMap.computeIfAbsent( g, s -> new HashSet<>() )
                                             .add( discoveredEncodings.getKey() ) );
      }
      return semanticTermMap;
   }


   static private Collection<SemanticGroup> getSemanticGroups(
         final Collection<TermEncoding> termEncodings,
         final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final Collection<SemanticGroup> groups = termEncodings.stream()
                                                            .filter( CodeSchema.TUI::isSchema )
                                                            .map( e -> getSemanticGroup( e, reassignSemantics ) )
                                                            .collect( Collectors.toSet() );
      if ( groups.isEmpty() ) {
         return Collections.singletonList( SemanticGroup.UNKNOWN );
      }
      return groups;
   }


   static private SemanticGroup getSemanticGroup( final TermEncoding tuiEncoding,
                                                  final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final Object object = tuiEncoding.getSchemaCode();
      if ( object instanceof Integer ) {
         return getSemanticGroup( (Integer)object, reassignSemantics );
      }
      return getSemanticGroup( parseTuiValue( object ), reassignSemantics );
   }

   static private SemanticGroup getSemanticGroup( final int tuiCode,
                                                  final Map<SemanticTui, SemanticGroup> reassignSemantics ) {
      final SemanticTui tui = SemanticTui.getTui( tuiCode );
      if ( !reassignSemantics.isEmpty() ) {
         final SemanticGroup reassignGroup = reassignSemantics.get( tui );
         if ( reassignGroup != null ) {
            return reassignGroup;
         }
      }
      return tui.getGroup();
   }


   static private int parseTuiValue( final Object object ) {
      try {
         return Integer.parseInt( object.toString() );
      } catch ( NumberFormatException nfE ) {
         return SemanticTui.UNKNOWN.getCode();
      }
   }


//   static private Map<DiscoveredTerm, Collection<SemanticGroup>> mapTermSemantics(
//         final Map<DiscoveredTerm, Collection<TermEncoding>> termEncodingMap,
//         final Map<SemanticTui,SemanticGroup> reassignSemantics ) {
//      final Map<DiscoveredTerm, Collection<SemanticGroup>> termSemanticsMap = new HashMap<>( termEncodingMap.size() );
//      termEncodingMap.forEach( (k,v) -> termSemanticsMap.put( k, getSemanticGroups( v, reassignSemantics) ) );
//      return termSemanticsMap;
//   }


//   static private Map<TermEncoding,SemanticGroup> mapEncodingSemantics( final Collection<TermEncoding> termEncodings,
//                                                                        final Map<SemanticTui,SemanticGroup> reassignSemantics ) {
//      return termEncodings.stream()
//                          .collect( Collectors.toMap( Function.identity(),
//                                e -> getSemanticGroup( e, reassignSemantics ) ) );
//   }


}
