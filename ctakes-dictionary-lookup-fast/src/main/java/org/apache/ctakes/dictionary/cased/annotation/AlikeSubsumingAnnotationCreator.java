package org.apache.ctakes.dictionary.cased.annotation;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.dictionary.cased.encoder.TermEncoding;
import org.apache.ctakes.dictionary.cased.lookup.DiscoveredTerm;
import org.apache.ctakes.dictionary.cased.util.textspan.MagicTextSpan;
import org.apache.ctakes.dictionary.cased.wsd.WsdUtil;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import java.util.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/19/2020
 */
@Immutable
final public class AlikeSubsumingAnnotationCreator implements AnnotationCreator {

   static private final Logger LOGGER = Logger.getLogger( "AlikeSubsumingAnnotationCreator" );

   public AlikeSubsumingAnnotationCreator() {
   }


   public void createAnnotations( final JCas jCas,
                                  final Map<Pair<Integer>, Collection<DiscoveredTerm>> allDiscoveredTermsMap,
                                  final Map<DiscoveredTerm, Collection<TermEncoding>> termEncodingMap,
                                  final Map<SemanticTui, SemanticGroup> reassignSemantics ) {

      final Map<SemanticGroup, Collection<DiscoveredTerm>> semanticTermsMap
            = AnnotationCreatorUtil.mapSemanticTerms( termEncodingMap, reassignSemantics );

      final Map<DiscoveredTerm, Collection<MagicTextSpan>> termSpanMap
            = AnnotationCreatorUtil.mapTermSpans( allDiscoveredTermsMap );


      for ( SemanticGroup subsumingGroup : SemanticGroup.values() ) {
         final Collection<DiscoveredTerm> semanticTerms = semanticTermsMap.get( subsumingGroup );
         if ( semanticTerms == null || semanticTerms.isEmpty() ) {
            continue;
         }

         final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumedTermsMap
               = getSubsumedSpanTerms( subsumingGroup, semanticTermsMap, termSpanMap );

         for ( Map.Entry<MagicTextSpan, Collection<DiscoveredTerm>> subsumedTerms : subsumedTermsMap.entrySet() ) {
            allDiscoveredTermsMap.getOrDefault( subsumedTerms.getKey().toIntPair(), new HashSet<>() )
                                 .removeAll( subsumedTerms.getValue() );
            semanticTerms.removeAll( subsumedTerms.getValue() );
         }

         final Map<MagicTextSpan, Collection<DiscoveredTerm>> wsdedTermsMap
               = WsdUtil.getSemanticWsdSpanTerms( semanticTerms, termSpanMap );

         for ( Map.Entry<MagicTextSpan, Collection<DiscoveredTerm>> wsdedTerms : wsdedTermsMap.entrySet() ) {
            allDiscoveredTermsMap.getOrDefault( wsdedTerms.getKey().toIntPair(), new HashSet<>() )
                                 .removeAll( wsdedTerms.getValue() );
         }

      }

      allDiscoveredTermsMap.forEach(
            ( k, v ) -> AnnotationCreatorUtil.createAnnotations( jCas, k, v, termEncodingMap, reassignSemantics ) );
   }


   static public Map<MagicTextSpan, Collection<DiscoveredTerm>> getSubsumedSpanTerms(
         final SemanticGroup subsumingGroup,
         final Map<SemanticGroup, Collection<DiscoveredTerm>> semanticTermsMap,
         final Map<DiscoveredTerm, Collection<MagicTextSpan>> termSpanMap ) {
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumedSpanTermsMap = new HashMap<>();
      // Get subsuming spans and their corresponding terms.
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumingSpanTermsMap
            = SubsumptionUtil.mapSpanTerms( subsumingGroup, semanticTermsMap, termSpanMap );
      if ( subsumingSpanTermsMap.isEmpty() ) {
         // No subsuming Spans.
         return Collections.emptyMap();
      }
      // List of spans for subsuming terms, sorted by end character index.
      final List<MagicTextSpan> subsumingSpans = new ArrayList<>( subsumingSpanTermsMap.keySet() );
      subsumingSpans.sort( Comparator.comparingInt( MagicTextSpan::getEnd ) );
      // Remove smaller terms of the same semantic group
      if ( subsumingSpanTermsMap.size() > 1 ) {
         subsumedSpanTermsMap.putAll( SubsumptionUtil.mapFullySubsumedTermSpans( subsumingSpans, subsumingSpanTermsMap ) );
      }
      return subsumedSpanTermsMap;
   }


}
