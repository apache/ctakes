package org.apache.ctakes.core.cc.pretty.row;

import org.apache.ctakes.core.cc.pretty.cell.ItemCell;

import java.util.Collection;
import java.util.Map;

/**
 * A row of item cells that represent part of a sentence and its annotations
 */
public interface ItemRow {

   /**
    * Attempt to add an item cell to this row
    *
    * @param itemCell -
    * @return true if the item cell was added to this row
    */
   boolean addItemCell( ItemCell itemCell );

   /**
    * @return height in lines required to accommodate this item cell
    */
   int getHeight();

   /**
    * @return item cells in this row
    */
   Collection<ItemCell> getItemCells();

   /**
    * @param lineIndex         index of the line required to write this item cell
    * @param rowWidth          width in characters of the row
    * @param offsetAdjustedMap map of original document offsets to adjusted printable offsets
    * @return text to be written on the given line to represent this item row
    */
   String getTextLine( int lineIndex, int rowWidth, Map<Integer, Integer> offsetAdjustedMap );

}
