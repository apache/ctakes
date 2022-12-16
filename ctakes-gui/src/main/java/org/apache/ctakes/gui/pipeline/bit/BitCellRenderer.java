package org.apache.ctakes.gui.pipeline.bit;


import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/13/2017
 */
final public class BitCellRenderer implements ListCellRenderer<Object> {

   static private final Logger LOGGER = Logger.getLogger( "BitCellRenderer" );


   private final ListCellRenderer<Object> _delegate = new DefaultListCellRenderer();

   @Override
   public Component getListCellRendererComponent(
         final JList<?> list,
         final Object value,
         final int index,
         final boolean isSelected,
         final boolean cellHasFocus ) {
      final Component renderer = _delegate.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

      PipeBitPainter.getInstance().paintObject( renderer, value, isSelected );

      return renderer;
   }

}
