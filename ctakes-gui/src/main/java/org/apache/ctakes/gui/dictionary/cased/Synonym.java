package org.apache.ctakes.gui.dictionary.cased;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/14/2020
 */
public enum Synonym {
   CUI( 1, Long.class ),
   PREFIX( 2, String.class ),
   INDEX_WORD( 3, String.class ),
   SUFFIX( 4, String.class ),
   RANK( 5, Integer.class ),
   INSTANCES( 6, Integer.class );

   final private int _column;
   final private Class<?> _class;

   Synonym( final int column, final Class<?> clazz ) {
      _column = column;
      _class = clazz;
   }

   public int getColumn() {
      return _column;
   }

   public Class<?> getClassType() {
      return _class;
   }

}
