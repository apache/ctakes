package org.apache.ctakes.dictionary.cased.wsd;


import org.apache.ctakes.dictionary.cased.lookup.DiscoveredTerm;
import org.apache.ctakes.dictionary.cased.util.textspan.MagicTextSpan;

import java.util.*;
import java.util.function.Function;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2020
 */
final public class WsdUtil {

   private WsdUtil() {
   }


   static private final Function<DiscoveredTerm, Integer> caseCompared = d -> d.matchesLookupCase() ? 1 : 0;
   static private final Function<DiscoveredTerm, Integer> skipCompared = d -> 100 - d.getTotalSkips();
   static private final Function<DiscoveredTerm, Integer> consecutiveSkipCompared = d -> 100 - d.getConsecutiveSkips();
   static private final Function<DiscoveredTerm, Integer> rankCompared = d -> 1000 - d.getRank();

   static public Map<MagicTextSpan, Collection<DiscoveredTerm>> getSemanticWsdSpanTerms(
         final Collection<DiscoveredTerm> semanticTerms,
         final Map<DiscoveredTerm, Collection<MagicTextSpan>> termSpanMap ) {
      final Map<MagicTextSpan, Collection<DiscoveredTerm>> spanTermsMap = new HashMap<>();
      for ( DiscoveredTerm term : semanticTerms ) {
         final Collection<MagicTextSpan> spans = termSpanMap.get( term );
         for ( MagicTextSpan span : spans ) {
            spanTermsMap.computeIfAbsent( span, s -> new HashSet<>() ).add( term );
         }
      }

      final Map<MagicTextSpan, Collection<DiscoveredTerm>> wsdRemovals = new HashMap<>();
      for ( Map.Entry<MagicTextSpan, Collection<DiscoveredTerm>> spanTerms : spanTermsMap.entrySet() ) {
         if ( spanTerms.getValue().size() < 2 ) {
            continue;
         }
         final DiscoveredTerm best = spanTerms.getValue().stream()
                                              .max( Comparator.comparing( caseCompared )
                                                              .thenComparing( skipCompared )
                                                              .thenComparing( consecutiveSkipCompared )
                                                              .thenComparing( DiscoveredTerm::getInstances )
                                                              .thenComparing( rankCompared ) )
                                              .orElse( null );
         if ( best != null ) {
            wsdRemovals.computeIfAbsent( spanTerms.getKey(), s -> new HashSet<>() )
                       .addAll( spanTerms.getValue() );
            wsdRemovals.get( spanTerms.getKey() ).remove( best );
         }
      }
      return wsdRemovals;
   }

}
