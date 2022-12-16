package org.apache.ctakes.gui.dictionary.umls;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/22/2020
 */
public enum MrsabIndex {
   RSAB( 3 ), SON( 4 ), SVER( 6 ), CFR( 15 );
   // abbrevation, name, version, CUIs count,

   final public int _index;

   MrsabIndex( final int index ) {
      _index = index;
   }

}
