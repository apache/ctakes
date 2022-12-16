package org.apache.ctakes.gui.dictionary.cased.term;


import org.apache.ctakes.core.util.StringUtil;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2020
 */
final public class CustomTermLine implements TermLine {


   final private String[] _columns;

   public CustomTermLine( final String line ) {
      _columns = StringUtil.fastSplit( line, '|' );
   }

   public String getCui() {
      return _columns[ 0 ];
   }

   public String getText() {
      return _columns[ 1 ];
   }

   public int getPrefScore() {
      return _columns.length > 2 ? getPrefScore( _columns[ 2 ] ) : TermLine.getHalfScore();
   }

   public String getSource() {
      return "CUSTOM";
   }

   public String getCode() {
      return getCui();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return String.join( " | ", _columns );
   }


   private int getPrefScore( final String text ) {
      if ( text.isEmpty() ) {
         return TermLine.getHalfScore();
      }
      try {
         final int parseInt = Integer.parseInt( text );
         return Math.min( Math.max( 0, parseInt ), TermLine.getMaxScore() );
      } catch ( NumberFormatException nfE ) {
         return TermLine.getHalfScore();
      }
   }


}
