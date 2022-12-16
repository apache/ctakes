package org.apache.ctakes.dictionary.cased.lookup;


import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
@Immutable
final public class DiscoveredTerm {

   private final long _cuiCode;
   private final int _consecutiveSkips;
   private final int _totalSkips;
   private final boolean _matchesLookupCase;
   private final int _rank;
   private final int _instances;

   public DiscoveredTerm( final CandidateTerm candidateTerm ) {
      this( candidateTerm, 0, 0 );
   }

   public DiscoveredTerm( final CandidateTerm candidateTerm,
                          final int consecutiveSkips,
                          final int totalSkips ) {
      _cuiCode = candidateTerm.getCuiCode();
      _consecutiveSkips = consecutiveSkips;
      _totalSkips = totalSkips;
      _matchesLookupCase = candidateTerm.matchesLookupCase();
      _rank = candidateTerm.getRank();
      _instances = candidateTerm.getInstances();
   }

   public long getCuiCode() {
      return _cuiCode;
   }

   public boolean matchesLookupCase() {
      return _matchesLookupCase;
   }

   /**
    * @return rank, where 1 is the "best".
    */
   public int getRank() {
      return _rank;
   }

   /**
    * @return number of source vocabularies that have this synonym for this cui.
    */
   public int getInstances() {
      return _instances;
   }

   public int getTotalSkips() {
      return _totalSkips;
   }

   public int getConsecutiveSkips() {
      return _consecutiveSkips;
   }

}
