package org.apache.ctakes.core.cc.pretty.cell;

/**
 * Item cell for most basic item: text and part of speech
 */
public interface BaseItemCell extends ItemCell {

   /**
    * @return part of speech for the item in this cell
    */
   String getPos();

}
