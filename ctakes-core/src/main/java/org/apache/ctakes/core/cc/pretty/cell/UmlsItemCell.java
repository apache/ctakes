package org.apache.ctakes.core.cc.pretty.cell;


/**
 * Item Cell for an umls entity
 */
public interface UmlsItemCell extends ItemCell {

   // Return Code used to indicate that a full entity span should be filled with an indicator character, e.g. '='
   String ENTITY_FILL = "ENTITY_FILL";

   /**
    * @return true if the umls entity represented by this item cell is negated
    */
   boolean isNegated();

}
