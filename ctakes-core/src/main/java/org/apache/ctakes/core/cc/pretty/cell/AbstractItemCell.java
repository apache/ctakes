package org.apache.ctakes.core.cc.pretty.cell;

import org.apache.ctakes.core.util.textspan.TextSpan;

/**
 * Contains the text span
 */
public abstract class AbstractItemCell implements ItemCell {

   final private TextSpan _textSpan;

   public AbstractItemCell( final TextSpan textSpan ) {
      _textSpan = textSpan;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public TextSpan getTextSpan() {
      return _textSpan;
   }

}
