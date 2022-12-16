package org.apache.ctakes.dictionary.cased.annotation;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.dictionary.cased.lookup.DiscoveredTerm;
import org.apache.ctakes.dictionary.cased.util.textspan.MagicTextSpan;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/24/2020
 */
@Immutable
final public class SubsumptionUtil {


   private SubsumptionUtil() {
   }

   static public Map<MagicTextSpan, Collection<DiscoveredTerm>> mapSpanTerms(
         final SemanticGroup semanticGroup,
         final Map<SemanticGroup, Collection<DiscoveredTerm>> semanticTermsMap,
         final Map<DiscoveredTerm, Collection<MagicTextSpan>> termSpanMap ) {
      final Collection<DiscoveredTerm> semanticTerms = semanticTermsMap.get( semanticGroup );
      if ( semanticTerms == null || semanticTerms.isEmpty() ) {
         return Collections.emptyMap();
      }
      return mapSpanTerms( semanticTerms, termSpanMap );
   }

   static private Map<MagicTextSpan, Collection<DiscoveredTerm>> mapSpanTerms(
         final Collection<DiscoveredTerm> discoveredTerms,
         final Map<DiscoveredTerm, Collection<MagicTextSpan>> termSpanMap ) {
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> spanTerms = new HashMap<>();
      for ( DiscoveredTerm term : discoveredTerms ) {
         final Collection<MagicTextSpan> termSpans = termSpanMap.get( term );
         if ( termSpans == null ) {
            continue;
         }
         termSpans.forEach(
               p -> spanTerms.computeIfAbsent( p, s -> new HashSet<>() )
                             .add( term ) );
      }
      return spanTerms;
   }


   static public Map<MagicTextSpan, Collection<DiscoveredTerm>> mapFullySubsumedTermSpans(
         final List<MagicTextSpan> subsumingSpans,
         final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumableSpanTermsMap ) {
      final List<MagicTextSpan> possiblySubsumedSpans = new ArrayList<>( subsumableSpanTermsMap.keySet() );
//      possiblySubsumedSpans.sort( Comparator.comparingInt( MagicTextSpan::getBegin ) );

      final Collection<MagicTextSpan> subsumedSpans = getFullySubsumedSpans( subsumingSpans, possiblySubsumedSpans );
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumedLikeTermMap
            = new HashMap<>( subsumableSpanTermsMap );
      subsumedLikeTermMap.keySet().retainAll( subsumedSpans );
      return subsumedLikeTermMap;
   }

   static public Map<MagicTextSpan, Collection<DiscoveredTerm>> mapSubsumedOrSameTermSpans(
         final List<MagicTextSpan> subsumingSpans,
         final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumableSpanTermsMap ) {
      final List<MagicTextSpan> possiblySubsumedSpans = new ArrayList<>( subsumableSpanTermsMap.keySet() );
//      possiblySubsumedSpans.sort( Comparator.comparingInt( MagicTextSpan::getBegin ) );

      final Collection<MagicTextSpan> subsumedSpans = getSubsumedOrSameSpans( subsumingSpans, possiblySubsumedSpans );
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> subsumedLikeTermMap
            = new HashMap<>( subsumableSpanTermsMap );
      subsumedLikeTermMap.keySet().retainAll( subsumedSpans );
      return subsumedLikeTermMap;
   }

   /**
    * Refine a collection of dictionary terms to only contain the most specific variations:
    * "colon cancer" instead of "cancer", performed by span inclusion /complete containment, not overlap
    */
   static private Collection<MagicTextSpan> getFullySubsumedSpans(
         final List<MagicTextSpan> subsumingSpans,
         final List<MagicTextSpan> possiblySubsumedSpans ) {
      final Collection<MagicTextSpan> subsumedSpans = new HashSet<>();

      // Subsuming spans start at the begin of the document and move forward
      for ( MagicTextSpan subsumingSpan : subsumingSpans ) {
         for ( MagicTextSpan possiblySubsumedSpan : possiblySubsumedSpans ) {
            if ( subsumingSpan.fullyContainsAll( possiblySubsumedSpan ) ) {
               subsumedSpans.add( possiblySubsumedSpan );
            }
         }
      }
      return subsumedSpans;
   }


   /**
    * Refine a collection of dictionary terms to only contain the most specific variations:
    * "headache medicine" instead of "headache", performed by span inclusion /complete containment, not overlap
    */
   static public Collection<MagicTextSpan> getSubsumedOrSameSpans(
         final List<MagicTextSpan> subsumingSpans,
         final List<MagicTextSpan> possiblySubsumedSpans ) {
      final Collection<MagicTextSpan> subsumedSpans = new HashSet<>();
      // Subsuming spans start at the begin of the document and move forward
      for ( MagicTextSpan subsumingSpan : subsumingSpans ) {
         for ( MagicTextSpan possiblySubsumedSpan : possiblySubsumedSpans ) {
            if ( subsumingSpan.containsAll( possiblySubsumedSpan ) ) {
               subsumedSpans.add( possiblySubsumedSpan );
            }
         }
      }
      return subsumedSpans;
   }


}
