package org.apache.ctakes.dictionary.cased.util.textspan;

import org.apache.ctakes.core.util.Pair;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/19/2020
 */
public final class ContiguousTextSpan implements MagicTextSpan {
   private final Pair<Integer> _span;

   public ContiguousTextSpan( final int begin, final int end ) {
      this( new Pair<>( begin, end ) );
   }

   public ContiguousTextSpan( final Pair<Integer> span ) {
      _span = span;
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

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object other ) {
      return other instanceof ContiguousTextSpan
             && ((ContiguousTextSpan)other).getBegin() == getBegin()
             && ((ContiguousTextSpan)other).getEnd() == getEnd();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _span.hashCode();
   }


}
