package org.apache.ctakes.dictionary.cased.util.bsv;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
public class StringArrayCreator implements BsvObjectCreator<String[]> {

   private final int _columnCount;

   public StringArrayCreator() {
      this( Integer.MAX_VALUE );
   }

   public StringArrayCreator( final int columnCount ) {
      _columnCount = columnCount;
   }

   public String[] createBsvObject( final String[] columns ) {
      if ( _columnCount != Integer.MAX_VALUE && columns.length != _columnCount ) {
         return null;
      }
      return columns;
   }


}
