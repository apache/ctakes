package org.apache.ctakes.core.cc.pretty.cell;

import org.apache.ctakes.core.util.textspan.TextSpan;

import static org.apache.ctakes.core.cc.pretty.cell.DefaultUmlsItemCell.ENTITY_FILL;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/9/2015
 */
public class TimexCell extends AbstractItemCell {

   // Return Code used to indicate that a full entity span should be filled with an indicator character, e.g. '-'
//   static public final String TIMEX_FILL = "TIMEX_FILL";

   static private final int TIMEX_SPAN = 5;
   static private final String TIMEX_TEXT = "Timex";

   public TimexCell( final TextSpan textSpan ) {
      super( textSpan );
   }

   /**
    * {@inheritDoc}
    *
    * @return the maximum of the document text length and the length of "Timex" (5)
    */
   @Override
   public int getWidth() {
      return Math.max( getTextSpan().getWidth(), TIMEX_SPAN );
   }

   /**
    * {@inheritDoc}
    *
    * @return the 1 for the text span representation line + 1 for the label
    */
   @Override
   public int getHeight() {
      return 2;
   }

   /**
    * {@inheritDoc}
    *
    * @return {@link org.apache.ctakes.core.cc.pretty.cell.UmlsItemCell#ENTITY_FILL}
    */
   @Override
   public String getText() {
      return ENTITY_FILL;
   }

   /**
    * {@inheritDoc}
    *
    * @return {@link org.apache.ctakes.core.cc.pretty.cell.UmlsItemCell#ENTITY_FILL} for index 0, Semantic types and Cuis for lines after that, then negated
    */
   @Override
   public String getLineText( final int lineIndex ) {
      switch ( lineIndex ) {
         case 0:
            return ENTITY_FILL;
         case 1:
            return TIMEX_TEXT;
      }
      return "";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object other ) {
      return other instanceof TimexCell
             && getTextSpan().equals( ((TimexCell)other).getTextSpan() );
   }

   public int hashCode() {
      return 3 * getTextSpan().hashCode();
   }

}
