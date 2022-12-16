package org.apache.ctakes.dictionary.cased.util.textspan;

import org.apache.ctakes.core.util.Pair;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/19/2020
 */
public final class DiscontiguousTextSpan implements MagicTextSpan {
   private final Pair<Integer> _span;
   private final Collection<MagicTextSpan> _presentTextSpans;

   private DiscontiguousTextSpan( final Pair<Integer> span, final Collection<Pair<Integer>> missingSpans ) {
      _span = span;
      _presentTextSpans = createPresentSpans( span, missingSpans );
   }

   public Pair<Integer> toIntPair() {
      return _span;
   }

   public int getBegin() {
      return _span.getValue1();
   }

   public int getEnd() {
      return _span.getValue2();
   }


   public Collection<MagicTextSpan> getPresentSpans() {
      return _presentTextSpans;
   }

   static private Collection<MagicTextSpan> createPresentSpans( final Pair<Integer> span,
                                                                final Collection<Pair<Integer>> missingSpans ) {
      final List<Pair<Integer>> missingSpanList = new ArrayList<>( missingSpans );
      missingSpanList.sort( Comparator.comparingInt( Pair::getValue1 ) );

      final Collection<MagicTextSpan> presentSpans = new HashSet<>( missingSpans.size() + 1 );
      int previousBegin = span.getValue1();
      for ( Pair<Integer> missingSpan : missingSpanList ) {
         presentSpans.add( new ContiguousTextSpan( previousBegin, missingSpan.getValue1() ) );
         previousBegin = missingSpan.getValue2();
      }
      presentSpans.add( new ContiguousTextSpan( previousBegin, span.getValue2() ) );
      return presentSpans;
   }

   public boolean containsAll( final MagicTextSpan textSpan ) {
      if ( !contains( textSpan ) ) {
         return false;
      }
      final Collection<MagicTextSpan> presentSpans = getPresentSpans();
      if ( textSpan instanceof ContiguousTextSpan ) {
         return presentSpans.stream().anyMatch( t -> t.contains( textSpan ) );
      }
      if ( textSpan instanceof DiscontiguousTextSpan ) {
         final Collection<MagicTextSpan> otherPresentSpans = ((DiscontiguousTextSpan)textSpan).getPresentSpans();
         for ( MagicTextSpan other : otherPresentSpans ) {
            if ( presentSpans.stream().noneMatch( t -> t.contains( other ) ) ) {
               return false;
            }
         }
      }
      return true;
   }

   public boolean fullyContainsAll( final MagicTextSpan textSpan ) {
      if ( !fullyContains( textSpan ) ) {
         return false;
      }
      final Collection<MagicTextSpan> presentSpans = getPresentSpans();
      if ( textSpan instanceof ContiguousTextSpan ) {
         return presentSpans.stream().anyMatch( t -> t.fullyContains( textSpan ) );
      }
      if ( textSpan instanceof DiscontiguousTextSpan ) {
         boolean fullyContains = false;
         final Collection<MagicTextSpan> otherPresentSpans = ((DiscontiguousTextSpan)textSpan).getPresentSpans();
         for ( MagicTextSpan other : otherPresentSpans ) {
            if ( presentSpans.stream().noneMatch( t -> t.contains( other ) ) ) {
               return false;
            }
            fullyContains = fullyContains
                            || presentSpans.stream().anyMatch( t -> t.fullyContains( other ) );
         }
         return fullyContains;
      }
      return true;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object other ) {
      return other instanceof DiscontiguousTextSpan
             && ((DiscontiguousTextSpan)other).getBegin() == getBegin()
             && ((DiscontiguousTextSpan)other).getEnd() == getEnd()
             && ((DiscontiguousTextSpan)other).getPresentSpans().equals( getPresentSpans() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _span.hashCode() + _presentTextSpans.hashCode();
   }

}
