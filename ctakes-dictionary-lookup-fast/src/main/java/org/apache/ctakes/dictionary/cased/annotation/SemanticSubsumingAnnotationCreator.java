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

import static org.apache.ctakes.core.util.annotation.SemanticGroup.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/19/2020
 */
@Immutable
final public class SemanticSubsumingAnnotationCreator implements AnnotationCreator {

   static private final Logger LOGGER = Logger.getLogger( "SemanticSubsumingAnnotationCreator" );

   public SemanticSubsumingAnnotationCreator() {
   }


   static private final Map<SemanticGroup, Collection<SemanticGroup>> SUBSUME_MAP
         = new EnumMap<>( SemanticGroup.class );

   static {
      SUBSUME_MAP.put( ANATOMY, EnumSet.of( DRUG, DISORDER, FINDING, PROCEDURE, LAB, PHENOMENON, ENTITY ) );
      //
      SUBSUME_MAP.put( DRUG, EnumSet.of( LAB, PHENOMENON, ENTITY, EVENT ) );
      //
      SUBSUME_MAP.put( DISORDER, EnumSet.of( DRUG, FINDING, LAB, PHENOMENON, ENTITY, EVENT ) );
      //
      SUBSUME_MAP.put( FINDING, EnumSet.of( LAB, PHENOMENON, ENTITY, EVENT ) );
      // "Oral Surgery"
      SUBSUME_MAP.put( PROCEDURE, EnumSet.of( LAB, PHENOMENON, EVENT ) );
      //
//      SUBSUME_MAP.put( ANATOMY, EnumSet.of( DRUG, DISORDER, FINDING, LAB, PHENOMENON, ENTITY ) );
      //
//      SUBSUME_MAP.put( CLINICAL_ATTRIBUTE, EnumSet.of( ENTITY ) );
      // may be wanted even within procedure, procedure probably wanted within device.  Maybe Anatomy?
//      SUBSUME_MAP.put( DEVICE, EnumSet.of( ENTITY ) );
      //
//      SUBSUME_MAP.put( LAB, EnumSet.of( PHENOMENON, ENTITY, EVENT ) );
      //
//      SUBSUME_MAP.put( PHENOMENON, EnumSet.of( ENTITY ) );
      //   SUBJECT
      //   TITLE
      //   EVENT
      //   ENTITY
      //   TIME
      //   MODIFIER
      //   LAB_MODIFIER
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
         final Collection<SemanticGroup> subsumedGroups
               = SUBSUME_MAP.getOrDefault( subsumingGroup, Collections.emptyList() );
         final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumedTermsMap
               = getSemanticSubsumedSpanTerms(
               subsumingGroup, subsumedGroups, semanticTermsMap, termSpanMap );

         for ( Map.Entry<MagicTextSpan, Collection<DiscoveredTerm>> subsumedTerms : subsumedTermsMap.entrySet() ) {
            allDiscoveredTermsMap.getOrDefault( subsumedTerms.getKey().toIntPair(), new HashSet<>() )
                                 .removeAll( subsumedTerms.getValue() );
            semanticTerms.removeAll( subsumedTerms.getValue() );
            for ( SemanticGroup subsumedGroup : subsumedGroups ) {
               semanticTermsMap.getOrDefault( subsumedGroup, new HashSet<>() ).removeAll( subsumedTerms.getValue() );
            }
         }

         // WSD
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


   static public Map<MagicTextSpan, Collection<DiscoveredTerm>> getSemanticSubsumedSpanTerms(
         final SemanticGroup subsumingGroup,
         final Collection<SemanticGroup> subsumedGroups,
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
//      subsumingSpans.sort( Comparator.comparingInt( MagicTextSpan::getEnd ) );
      // Remove smaller terms of the same semantic group
      if ( subsumingSpanTermsMap.size() > 1 ) {
         subsumedSpanTermsMap.putAll( SubsumptionUtil.mapFullySubsumedTermSpans( subsumingSpans, subsumingSpanTermsMap ) );
         if ( subsumedGroups.isEmpty() ) {
            return subsumedSpanTermsMap;
         }
         subsumingSpans.removeAll( subsumedSpanTermsMap.keySet() );
      }

      // Remove smaller or the same span terms of the other semantic groups
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumableSpanTermsMap = new HashMap<>();
      for ( SemanticGroup group : subsumedGroups ) {
         final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumedGroupSpanTermsMap
               = SubsumptionUtil.mapSpanTerms( group, semanticTermsMap, termSpanMap );
         for ( Map.Entry<MagicTextSpan, Collection<DiscoveredTerm>> subsumedGroupSpanTerms
               : subsumedGroupSpanTermsMap.entrySet() ) {
            subsumableSpanTermsMap.computeIfAbsent( subsumedGroupSpanTerms.getKey(),
                  t -> new HashSet<>() ).addAll( subsumedGroupSpanTerms.getValue() );
         }
      }

      if ( subsumableSpanTermsMap.isEmpty() ) {
         return subsumedSpanTermsMap;
      }
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumedGroupsSpanTermsMap
            = SubsumptionUtil.mapSubsumedOrSameTermSpans( subsumingSpans, subsumableSpanTermsMap );
      for ( Map.Entry<MagicTextSpan, Collection<DiscoveredTerm>> subsumedGroupsSpanTerms
            : subsumedGroupsSpanTermsMap.entrySet() ) {
         subsumedSpanTermsMap.computeIfAbsent( subsumedGroupsSpanTerms.getKey(),
               t -> new HashSet<>() ).addAll( subsumedGroupsSpanTerms.getValue() );
      }
      return subsumedSpanTermsMap;
   }


}
