package org.apache.ctakes.core.cc.pretty.cell;

import org.apache.ctakes.core.util.textspan.TextSpan;


/**
 * Container class for information about an item (text, annotation, etc.) cell - used for calculating print layout.
 */
public interface ItemCell {

   /**
    * @return text span for the original document text in the item cell
    */
   TextSpan getTextSpan();

   /**
    * @return width in characters required to accommodate this item cell
    */
   int getWidth();

   /**
    * @return height in lines required to accommodate this item cell
    */
   int getHeight();

   /**
    * @return the original document text for this item cell
    */
   String getText();

   /**
    * @param lineIndex index of the line required to write this item cell
    * @return text to be written on the given line to represent this item cell
    */
   String getLineText( int lineIndex );
}
