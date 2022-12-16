package org.apache.ctakes.dictionary.cased.table.column;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
public enum SchemaCode {
   CUI( 1 ),
   SCHEMA_CODE( 2 );

   final private int _column;

   SchemaCode( final int column ) {
      _column = column;
   }

   public int getColumn() {
      return _column;
   }

}
