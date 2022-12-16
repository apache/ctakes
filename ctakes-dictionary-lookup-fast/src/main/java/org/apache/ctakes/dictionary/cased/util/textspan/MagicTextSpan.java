package org.apache.ctakes.dictionary.cased.util.textspan;


import org.apache.ctakes.core.util.Pair;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/19/2020
 */
public interface MagicTextSpan {

   int getBegin();

   int getEnd();

   default Pair<Integer> toIntPair() {
      return new Pair<>( getBegin(), getEnd() );
   }

   default int getWidth() {
      return getEnd() - getBegin();
   }

   /**
    * NOTE: TextSpans are begin inclusive end exclusive.
    * So, 1 is subtracted from the end when comparing to another begin
    *
    * @param textSpan another textspan
    * @return true if there is overlap between the two text spans
    */
   default boolean overlaps( final MagicTextSpan textSpan ) {
      return !(textSpan.getEnd() - 1 < getBegin()) && !(textSpan.getBegin() > getEnd() - 1);
   }

   default boolean contains( final MagicTextSpan textSpan ) {
      return getBegin() <= textSpan.getBegin() && textSpan.getEnd() <= getEnd();
   }

   default boolean fullyContains( final MagicTextSpan textSpan ) {
      return (getBegin() < textSpan.getBegin() && textSpan.getEnd() <= getEnd())
             || (getBegin() <= textSpan.getBegin() && textSpan.getEnd() < getEnd());
   }

   /**
    * For discontiguous spans, every part of this span must include every part of that span.
    *
    * @param textSpan -
    * @return -
    */
   default boolean containsAll( MagicTextSpan textSpan ) {
      return contains( textSpan );
   }

   /**
    * For discontiguous spans, every part of this span must include every part of that span.
    *
    * @param textSpan -
    * @return -
    */
   default boolean fullyContainsAll( MagicTextSpan textSpan ) {
      return fullyContains( textSpan );
   }

}
