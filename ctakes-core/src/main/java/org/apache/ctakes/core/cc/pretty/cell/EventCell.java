package org.apache.ctakes.core.cc.pretty.cell;

import org.apache.ctakes.core.util.textspan.TextSpan;

import static org.apache.ctakes.core.cc.pretty.cell.DefaultUmlsItemCell.ENTITY_FILL;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/9/2015
 */
public class EventCell extends AbstractItemCell {

   static private final int EVENT_SPAN = 5;
   static private final String EVENT_TEXT = "Event";
   static private final int NEGATED_SPAN = 8;
   static private final String NEGATED_TEXT = "Negated";

   final private boolean _negated;

   public EventCell( final TextSpan textSpan, final int polarity ) {
      super( textSpan );
      _negated = polarity < 0;
   }

   /**
    * {@inheritDoc}
    *
    * @return the maximum of the document text length and the length of "Event" (5)
    */
   @Override
   public int getWidth() {
      return Math.max( getTextSpan().getWidth(), isNegated() ? NEGATED_SPAN : EVENT_SPAN );
   }

   /**
    * {@inheritDoc}
    *
    * @return the 1 for the text span representation line + 1 for the label + 1 if negated
    */
   @Override
   public int getHeight() {
      return 2 + (isNegated() ? 1 : 0);
   }

   /**
    * {@inheritDoc}
    *
    * @return {@link UmlsItemCell#ENTITY_FILL}
    */
   @Override
   public String getText() {
      return ENTITY_FILL;
   }

   /**
    * {@inheritDoc}
    */
//   @Override
   public boolean isNegated() {
      return _negated;
   }

   /**
    * {@inheritDoc}
    *
    * @return {@link UmlsItemCell#ENTITY_FILL} for index 0, Semantic types and Cuis for lines after that, then negated
    */
   @Override
   public String getLineText( final int lineIndex ) {
      switch ( lineIndex ) {
         case 0:
            return ENTITY_FILL;
         case 1:
            return EVENT_TEXT;
         case 2:
            if ( isNegated() ) {
               return NEGATED_TEXT;
            }
      }
      return "";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object other ) {
      return other instanceof EventCell
             && getTextSpan().equals( ((EventCell)other).getTextSpan() )
             && isNegated() == ((EventCell)other).isNegated();
   }

   public int hashCode() {
      return 2 * getTextSpan().hashCode() + (isNegated() ? 1 : 0);
   }

}
