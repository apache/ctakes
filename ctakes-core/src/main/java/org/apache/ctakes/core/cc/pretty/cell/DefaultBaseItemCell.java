package org.apache.ctakes.core.cc.pretty.cell;

import org.apache.ctakes.core.util.textspan.TextSpan;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/6/2015
 */
public final class DefaultBaseItemCell extends AbstractItemCell implements BaseItemCell {

   final private String _text;
   final private String _pos;

   public DefaultBaseItemCell( final TextSpan textSpan, final String text, final String pos ) {
      super( textSpan );
      _text = text;
      _pos = pos == null ? "" : pos;
   }

   /**
    * {@inheritDoc}
    *
    * @return the maximum length from the text and pos
    */
   @Override
   public int getWidth() {
      return Math.max( getTextSpan().getWidth(), _pos.length() );
   }

   /**
    * {@inheritDoc}
    *
    * @return 2.  One line each for text and pos
    */
   @Override
   public int getHeight() {
      return 2;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getText() {
      return _text;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPos() {
      return _pos;
   }

   /**
    * {@inheritDoc}
    *
    * @return the text from the document for line index 0 and the part of speech for line index 1
    */
   @Override
   public String getLineText( final int lineIndex ) {
      switch ( lineIndex ) {
         case 0:
            return _text;
         case 1:
            return _pos;
      }
      return "";
   }

}
